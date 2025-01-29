package sirFSM.diseaseStates

import com.bharatsim.engine.Context
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.fsm.State
import com.bharatsim.engine.graph.patternMatcher.MatchCondition._
import com.bharatsim.engine.models.{Network, StatefulAgent}
import sirFSM.InfectionStatus._
import sirFSM.{Office, Parameters, Person}

import scala.collection.mutable.ListBuffer

case class SusceptibleState() extends State {

  override def enterAction(context: Context, agent: StatefulAgent): Unit = {
    agent.updateParam("infectionState", Susceptible)
  }

  override def perTickAction(context: Context, agent: StatefulAgent): Unit = {

  }

  def exitSusceptible(context: Context, agent: StatefulAgent): Boolean = {

    val schedule = context.fetchScheduleFor(agent).get

    val currentStep = context.getCurrentStep
    val placeType: String = schedule.getForStep(currentStep)

    val places = agent.getConnections(agent.getRelation(placeType).get).toList

    if (places.nonEmpty) {
      val place = places.head
      val decodedPlace = agent.asInstanceOf[Person].decodeNode(placeType, place)

      val infectedNumbers = fetchInfectedCount(decodedPlace, placeType, context)

      val allInfected = infectedNumbers(0)
      val total = infectedNumbers(1)

      val infectedFraction = allInfected.toFloat / total.toFloat

      val r = Parameters.splittableRandom.nextDouble()

      var isFarm = false
      if(placeType=="Office"){isFarm = decodedPlace.asInstanceOf[Office].isFarm}

      val BHprob = Parameters.betaBH * Parameters.birdFOI(context.getCurrentStep * Parameters.dt, isFarm) * Parameters.dt
      val HHprob = Parameters.betaHH * infectedFraction * Parameters.dt

      val agentGetsInfected = r < (BHprob + HHprob)

      // Are they infected by the background FOI?
      val agentGetsInfectedByFOI = r < BHprob

      if (agentGetsInfected) {
        agent.updateParam("infectionState", Infected)
        agent.updateParam("infectedOnDay", context.getCurrentStep * Parameters.dt)
        if(agentGetsInfectedByFOI){
          agent.updateParam("infectingAgent", "FOI")
        }
        else{
          val infecting_agent = fetchInfectingAgent(decodedPlace, placeType, context)
          agent.updateParam("infectingAgent", infecting_agent.id.toString)
          infecting_agent.updateParam("agentsInfected", infecting_agent.agentsInfected + 1)
        }

        agent.updateParam("infectedAt", placeType)
      }
      return agentGetsInfected
    }

    false
  }


  private def fetchInfectedCount(decodedPlace: Network, placeType: String, context: Context): List[Int] = {
    val cache = context.perTickCache

    val tuple = (placeType, decodedPlace.internalId)
    cache.getOrUpdate(tuple, () => fetchFromStore(decodedPlace, placeType)).asInstanceOf[List[Int]]
  }

  private def fetchFromStore(decodedPlace: Network, placeType: String): List[Int] = {
    val infectedPattern = ("infectionState" equ Infected)

    val total = decodedPlace.getConnectionCount(decodedPlace.getRelation[Person]().get, "currentLocation" equ placeType)

    if (total == 0.0) {
      return List(0, 0)
    }

    val allInfected = decodedPlace.getConnectionCount(decodedPlace.getRelation[Person]().get, ("currentLocation" equ placeType) and infectedPattern)

    List(allInfected, total)
  }


  private def fetchInfectingAgent(decodedPlace: Network, placeType: String, context: Context): Person = {
    val cache = context.perTickCache

    val tuple = ("InfectingAgentsAt" + placeType, decodedPlace.internalId)
    val infectedHereList = cache.getOrUpdate(tuple, () => fetchInfectingAgentsFromStore(decodedPlace, placeType)).asInstanceOf[List[Person]]

    val infectingAgent = infectedHereList(Parameters.splittableRandom.nextInt(infectedHereList.size))
    infectingAgent
  }

  private def fetchInfectingAgentsFromStore(decodedPlace: Network, placeType: String): List[Person] = {
    val peopleHere = decodedPlace.getConnections(decodedPlace.getRelation[Person]().get)
    val infectedHere = new ListBuffer[Person]()

    peopleHere.foreach(node => {
      val person = node.as[Person]
      if (person.isInfected && person.currentLocation == placeType) {
        infectedHere += person
      }
    })
    infectedHere.toList
  }


  addTransition(
    when = exitSusceptible,
      to = context => InfectedState()
  )


}
