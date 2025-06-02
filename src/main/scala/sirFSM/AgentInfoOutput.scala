package sirFSM

import com.bharatsim.engine.Context
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.listeners.CSVSpecs

import scala.collection.mutable.ListBuffer

class AgentInfoOutput(context: Context) extends CSVSpecs {

  override def getHeaders: List[String] = List("AgentID", "Farmer", "ExposedOn", "InfectedOn", "RecoveredOn", "Vaccinated", "VaccinatedOn", "DaysExposed", "DaysInfected", "InfectingAgent", "InfectedAt", "NumberOfSecondaryInfections", "InfectionState")

  override def getRows(): List[List[Any]] = {
    val rows = ListBuffer.empty[List[String]]

    val graphProvider = context.graphProvider
    val label = "Person"
    val nodes = graphProvider.fetchNodes(label)

    nodes.foreach(node => {
      val person = node.as[Person]
      if(!person.isSusceptible) {
        rows.addOne(List(
          person.id.toString,
          person.isFarmer.toString,
          person.exposedOnDay.toString,
          person.infectedOnDay.toString,
          person.recoveredOnDay.toString,
          person.isVaccinated.toString,
          person.vaccinatedOnDay.toString,
          person.daysExposed.toString,
          person.daysInfected.toString,
          person.infectingAgent.toString,
          person.infectedAt.toString,
          person.agentsInfected.toString,
          person.infectionState.toString))
      }
    })
    rows.toList

  }

}

