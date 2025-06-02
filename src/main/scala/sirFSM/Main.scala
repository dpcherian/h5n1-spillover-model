package sirFSM

import com.bharatsim.engine.ContextBuilder._
import com.bharatsim.engine._
import com.bharatsim.engine.actions.StopSimulation
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.dsl.SyntaxHelpers._
import com.bharatsim.engine.execution.Simulation
import com.bharatsim.engine.graph.GraphNode
import com.bharatsim.engine.graph.ingestion.{GraphData, Relation}
import com.bharatsim.engine.graph.patternMatcher.EmptyPattern
import com.bharatsim.engine.graph.patternMatcher.MatchCondition._
import com.bharatsim.engine.intervention.{Intervention, SingleInvocationIntervention}
import com.bharatsim.engine.listeners.{CsvOutputGenerator, SimulationListenerRegistry}
import com.bharatsim.engine.models.Agent
import com.bharatsim.engine.utils.Probability.biasedCoinToss
import sirFSM.InfectionStatus._
import sirFSM.diseaseStates._
import com.typesafe.scalalogging.LazyLogging

import java.util.Date
import scala.collection.mutable.ListBuffer
import scala.util.Random

object Main extends LazyLogging {
  var currentTime: Long = 0L

  var farmersLockedDownOnDay: Double = -1.0

  private val myTick: ScheduleUnit = new ScheduleUnit(1)
  private val myDay: ScheduleUnit = new ScheduleUnit(myTick * 2)

  def parseArgs(args: Array[String]): Unit = {

    for (arg <- args) {
      val a = arg.split("=")

      if (a.length != 2) {
        throw new Exception(s"Unsupported syntax for argument: \"" + arg + "\". Flag syntax is `name=value`, without spaces.")
      }
      else {
        val key = a(0).toUpperCase
        val value = a(1)
        ""
        key match {
          case "INPUT" => {
            Parameters.inputPath = value;
            logger.info("Set input path to " + Parameters.inputPath)
          }
          case "OUTPUT" => {
            Parameters.outputPath = value;
            logger.info("Set output path to " + Parameters.outputPath)
          }
          case "IR" => {
            Parameters.initialRecoveredFraction = value.toFloat / 100f;
            logger.info("Set initial recovered percentage to " + Parameters.initialRecoveredFraction +"%")
          }
          case "BETABH" => {
            Parameters.betaBH = value.toDouble;
            logger.info("Set bird-human beta to " + Parameters.betaBH)
          }
          case "BETAHH" => {
            Parameters.betaHH = value.toDouble;
            logger.info("Set human-human beta to " + Parameters.betaHH)
          }
          case "BETABB" => {
            Parameters.birdBeta = value.toDouble;
            logger.info("Set bird beta to " + Parameters.birdBeta)
          }
          case "LF" => {
            Parameters.lockdownFarmers = value.toBoolean;
            logger.info("Farmers will be locked down: " + Parameters.lockdownFarmers)
          }
          case "LFIT" => {
            Parameters.lockdownFarmersInfectedThreshold = value.toInt;
            logger.info("Farmers will be locked down when number of infected > " + Parameters.lockdownFarmersInfectedThreshold)
          }
          case "FILES" => {
            val op_types = value.toCharArray
            for (op <- op_types) {
              op match {
                case 'T' => {
                  Parameters.saveTotalOutput = true
                }
                case 'A' => {
                  Parameters.saveAgewiseOutput = true
                }
                case 'I' => {
                  Parameters.saveInfectionInfoOutput = true
                }
                case '+' => {
                  Parameters.saveAgentOutput = true
                }
                case _ => {
                  throw new Exception(s"Unsupported output file: \"" + op + "\". Available outputs are \"T\" (Total number), \"A\" (Age-stratified numbers), \"I\" (Infection-info output), \"H\" (GIS Heatmap output), \"W\" (Ward-wise output), \"G\" (Gridded output).")
                }
              }
            }
          }
          case "SIMDAYS" => {
            Parameters.SIMDAYS = value.toInt;
            logger.info("Set total simulation duration to " + Parameters.SIMDAYS + " days")
          }
          case "IE" => {
            Parameters.initialExposedFraction = value.toFloat/100f;
            logger.info("Set initial exposed fraction to " + Parameters.initialExposedFraction)
          }
          case "CD" => {
            Parameters.culling_date = value.toFloat;
            logger.info("Set culling date day " + Parameters.culling_date)
          }
          case "DVR" => {
            Parameters.dvr = value.toFloat;
            logger.info("Set daily vaccination rate to " + Parameters.dvr)
          }
          case "VE" => {
            if(value.toDouble>0){
              Parameters.vaccinate = true;
              Parameters.vaccineEfficacy = value.toDouble
              logger.info("Vaccinating primary and secondary contacts with a vaccine efficacy of " + Parameters.vaccineEfficacy)
            }
          }
          case "VD" => {
            Parameters.vaccineDelayDays = value.toDouble
            logger.info("Set vaccine delay to "+Parameters.vaccineDelayDays+" days after first case.")
          }
          case _ => {
            throw new Exception(s"Unsupported flag: \"" + key + "\". Available flags are \"INPUT\", \"OUTPUT\", \"IR\", \"BETABH\", \"BETAHH\", \"BETABB\", \"LF\", \"LFIT\", \"FILES\", \"SIMDAYS\", \"IE\", \"DVR\", and \"CD\".")
          }
        }
      }
    }
  }



  def main(args: Array[String]): Unit = {
    parseArgs(args)

    val birdSIR = getBirdSIR()
    Parameters.birdSArray = birdSIR._1
    Parameters.birdIArray = birdSIR._2
    Parameters.birdRArray = birdSIR._3

    var beforeCount = 0
    val simulation = Simulation()

    simulation.ingestData(implicit context => {
      ingestCSVData(Parameters.inputPath+".csv", csvDataExtractor)
      logger.debug("Ingestion done")
    })

    simulation.defineSimulation(implicit context => {
      Parameters.totalPopulation = context.graphProvider.fetchCount("Person", EmptyPattern())

      create12HourSchedules()

      registerAction(
        StopSimulation,
        (c: Context) => {
          (context.getCurrentStep * Parameters.dt >= Parameters.FOIZERODAY && getInfectedCount(context)==0) ||
            (context.getCurrentStep * Parameters.dt >= Parameters.SIMDAYS)
        }
      )

      beforeCount = getInfectedCount(context)

      registerAgent[Person]

      registerState[SusceptibleState]
      registerState[ExposedState]
      registerState[InfectedState]
      registerState[RecoveredState]

      if(Parameters.lockdownFarmers){
        lockdown_farmers
      }
      if (Parameters.vaccineEfficacy > 0.0) {
        vaccination
      }

      currentTime = new Date().getTime

      if (Parameters.saveTotalOutput) {
        SimulationListenerRegistry.register(
          new CsvOutputGenerator(Parameters.outputPath + "total_" + currentTime + ".csv", new SEIROutputSpec(context))
        )
      }

    })

    simulation.onCompleteSimulation { implicit context =>
      if (Parameters.saveAgentOutput) {
        val agentOutputGenerator = new CsvOutputGenerator(Parameters.outputPath + "agentinfo_" + currentTime + ".csv", new AgentInfoOutput(context))
        agentOutputGenerator.onSimulationStart(context)
        agentOutputGenerator.onStepStart(context)
        agentOutputGenerator.onSimulationEnd(context)
      }
      printStats(beforeCount)
      teardown()
    }

    val startTime = System.currentTimeMillis()
    simulation.run()
    val endTime = System.currentTimeMillis()
    logger.info("Total time: {} s", (endTime - startTime) / 1000)
  }

  private def create12HourSchedules()(implicit context: Context): Unit = {
    val employeeSchedule = (myDay, myTick)
      .add[House](0, 0)
      .add[Office](1, 1)

    val studentSchedule = (myDay, myTick)
      .add[House](0, 0)
      .add[School](1, 1)

    val quarantinedSchedule = (myDay, myTick)
      .add[House](0, 1)

    registerSchedules(
      (quarantinedSchedule, (agent: Agent, _: Context) => (agent.asInstanceOf[Person].inFarmerHH && context.activeInterventionNames.contains("lockdown_farmers")), 1),
      (employeeSchedule, (agent: Agent, _: Context) => agent.asInstanceOf[Person].age >= Parameters.studentAge, 2),
      (studentSchedule, (agent: Agent, _: Context) => agent.asInstanceOf[Person].age < Parameters.studentAge, 3)
    )
  }

  private def csvDataExtractor(map: Map[String, String])(implicit context: Context): GraphData = {

    val citizenId = map("AgentID").toLong
    val age : Int = map("Age").toInt
    val initialInfectionState = if (biasedCoinToss(Parameters.initialExposedFraction)) "Exposed" else "Susceptible"

    val homeId = map("HHID").toLong
    val schoolId = map("SchoolID").toLong
    val officeId = map("WorkplaceID").toLong

    val isFarm = map("IsFarm").toBoolean
    val inFarmerHH = map("InFarmerHousehold").toBoolean

    val citizen: Person = Person(
                                id=citizenId,
                                age=age,
                                infectionState = InfectionStatus.withName(initialInfectionState),
                                isFarmer=isFarm,
                                inFarmerHH=inFarmerHH
                              )

    if(initialInfectionState == "Susceptible"){
      citizen.setInitialState(SusceptibleState())
    }
    else if (initialInfectionState == "Exposed"){
      citizen.setInitialState(ExposedState(0.0 + Parameters.exposedDurationProbabilityDistribution.sample()))
    }
    else{
      citizen.setInitialState(RecoveredState())
    }

    val home = House(homeId)
    val staysAt = Relation[Person, House](citizenId, "STAYS_AT", homeId)
    val memberOf = Relation[House, Person](homeId, "HOUSES", citizenId)

    val graphData = GraphData()
    graphData.addNode(citizenId, citizen)
    graphData.addNode(homeId, home)
    graphData.addRelations(staysAt, memberOf)

    if (age >= Parameters.studentAge) {
      val office = Office(officeId, isFarm = isFarm)
      val worksAt = Relation[Person, Office](citizenId, "WORKS_AT", officeId)
      val employerOf = Relation[Office, Person](officeId, "EMPLOYER_OF", citizenId)

      graphData.addNode(officeId, office)
      graphData.addRelations(worksAt, employerOf)
    } else {
      val school = School(schoolId)
      val studiesAt = Relation[Person, School](citizenId, "STUDIES_AT", schoolId)
      val studentOf = Relation[School, Person](schoolId, "STUDENT_OF", citizenId)

      graphData.addNode(schoolId, school)
      graphData.addRelations(studiesAt, studentOf)
    }

    graphData
  }

  private def getBirdSIR(): (Array[Double], Array[Double], Array[Double]) = {
    val birdS_array = new Array[Double]((Parameters.SIMDAYS/Parameters.dt).toInt + 1)
    val birdI_array = new Array[Double]((Parameters.SIMDAYS/Parameters.dt).toInt + 1)
    val birdR_array = new Array[Double]((Parameters.SIMDAYS/Parameters.dt).toInt + 1)

    var S = Parameters.Nbirds - Parameters.birdI
    var I = Parameters.birdI
    var R = 0.0

    var t = 0.0

    while(t<Parameters.SIMDAYS){

      val dS = - Parameters.birdBeta * S * I / Parameters.Nbirds
      val dI = + Parameters.birdBeta * S * I / Parameters.Nbirds - Parameters.birdGamma * I
      val dR = + Parameters.birdGamma * I

      S += dS; I += dI; R += dR;

      birdS_array( (t/Parameters.dt).toInt ) = S;
      birdI_array( (t/Parameters.dt).toInt ) = I;
      birdR_array( (t/Parameters.dt).toInt ) = R;

      if(!Parameters.FOIZERODAYSET && I < 0.01){
        Parameters.FOIZERODAY = t
        Parameters.FOIZERODAYSET = true
      }


      t += Parameters.dt
    }

    (birdS_array, birdI_array, birdR_array)
  }


  private def lockdown_farmers(implicit context: Context): Unit = {

    var ActivatedAt: Double = -1.0
    val interventionName = "lockdown_farmers"
    val activationCondition = (context: Context) => {
      val result = getSymptomaticCount(context) >= Parameters.lockdownFarmersInfectedThreshold
      if (result) {
        farmersLockedDownOnDay = context.getCurrentStep * Parameters.dt
        logger.info("Farm locked down on day " + farmersLockedDownOnDay)
      }
      result
    }
    val firstTimeExecution = (context: Context) => {
      ActivatedAt = context.getCurrentStep * Parameters.dt
    }
    val DeactivationCondition = (context: Context) => {
      false
    }

    val intervention = SingleInvocationIntervention(interventionName, activationCondition, DeactivationCondition, firstTimeExecution)

    registerIntervention(intervention)
  }

  private def vaccination(implicit context: Context): Unit = {

    var ActivatedAt = 0
    val interventionName = "vaccination"
    val activationCondition = (context: Context) => {
      val conditionMet = (Parameters.firstCaseRecorded) &&
                          ((context.getCurrentStep*Parameters.dt - Parameters.firstCaseRecordedDay) >= Parameters.vaccineDelayDays)
      if (conditionMet) {
        Parameters.vaccinationStartedOn = context.getCurrentStep * Parameters.dt
        logger.info("Vaccination started on day " + Parameters.vaccinationStartedOn +", and first case recorded on day "+ Parameters.firstCaseRecordedDay)
      }
      conditionMet
    }

    val firstTimeExecution = (context: Context) => {
      ActivatedAt = context.getCurrentStep
    }
    val deActivationCondition = (context: Context) => {
      false
    }

    val perTickAction = (context: Context) => {

      if (context.getCurrentStep % Parameters.numberOfTicksInADay == 0) {

        val allCandidatesIterator: Iterator[GraphNode] = context.graphProvider.fetchNodes("Person", ((("isFarmer" equ true) or ("inFarmerHH" equ true)) and ("isVaccinated" equ false)))

        var vaccinesAdministeredToday = 0
        val nshots = (Parameters.dvr * Parameters.totalPopulation).toInt

        val agentsToVaccinate = ListBuffer[Person]()

        if (Parameters.riskBasedVaccination) {
          var hrList = ListBuffer[Person]()
          var mrList = ListBuffer[Person]()
          var lrList = ListBuffer[Person]()

          allCandidatesIterator.foreach(node => {
            val person = node.as[Person]

            if (person.isFarmer) {
              hrList += person
            }
            else if (person.inFarmerHH) {
              mrList += person
            }
            else {
              lrList += person
            }
          })

          if (agentsToVaccinate.length <= nshots) {
            hrList = Random.shuffle(hrList)
            hrList.foreach(person => {
              if (!person.isVaccinated && vaccinesAdministeredToday < nshots) {
                agentsToVaccinate += person
                vaccinesAdministeredToday += 1
              }
            })
          }

          if (agentsToVaccinate.length <= nshots) {
            mrList = Random.shuffle(mrList)
            mrList.foreach(person => {
              if (!person.isVaccinated && vaccinesAdministeredToday < nshots) {
                agentsToVaccinate += person
                vaccinesAdministeredToday += 1
              }
            })
          }

          if (agentsToVaccinate.length <= nshots) {
            lrList = Random.shuffle(lrList)
            lrList.foreach(person => {
              if (!person.isVaccinated && vaccinesAdministeredToday < nshots) {
                agentsToVaccinate += person
                vaccinesAdministeredToday += 1
              }
            })
          }
//          logger.info("Unvaccinated agents remaining: (HR={}, MR={}, LR={}). Vaccines administered today: {}", hrList.length, mrList.length, lrList.length, vaccinesAdministeredToday)
        }
        else {
          var allCandidatesList = allCandidatesIterator.toList
          allCandidatesList = Random.shuffle(allCandidatesList)
          val potentialAgentsToVaccinate = allCandidatesList.take(nshots)

          potentialAgentsToVaccinate.foreach(node => {
            val person = node.as[Person]
            agentsToVaccinate += person
            vaccinesAdministeredToday += 1
          })

//          logger.info("Unvaccinated MSM remaining: {}. Vaccines administered today: {}", allCandidatesList.length, vaccinesAdministeredToday)
        }


        agentsToVaccinate.foreach(person => {
          person.updateParam("isVaccinated", true)
          person.updateParam("vaccinatedOnDay", context.getCurrentStep * Parameters.dt)
        })

      }
    }

    val intervention: Intervention = SingleInvocationIntervention(interventionName, activationCondition, deActivationCondition, firstTimeExecution, perTickAction)

    registerIntervention(intervention)
  }




  private def printStats(beforeCount: Int)(implicit context: Context): Unit = {
    val afterCountSusceptible = getSusceptibleCount(context)
    val afterCountInfected = getInfectedCount(context)
    val afterCountRecovered = getRemovedCount(context)

    logger.info("Infected before: {}", beforeCount)
    logger.info("Infected after: {}", afterCountInfected)
    logger.info("Recovered: {}", afterCountRecovered)
    logger.info("Susceptible: {}", afterCountSusceptible)
  }

  private def getSusceptibleCount(context: Context) = {
    context.graphProvider.fetchCount("Person", "infectionState" equ Susceptible)
  }

  private def getSymptomaticCount(context: Context): Int = {
    context.graphProvider.fetchCount("Person", ("infectionState" equ Infected))
  }

  private def getInfectedCount(context: Context): Int = {
    context.graphProvider.fetchCount("Person", ("infectionState" equ Infected) or  ("infectionState" equ Exposed))
  }

  private def getRemovedCount(context: Context) = {
    context.graphProvider.fetchCount("Person", "infectionState" equ Removed)
  }
}
