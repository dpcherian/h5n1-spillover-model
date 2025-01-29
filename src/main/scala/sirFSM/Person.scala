package sirFSM

import com.bharatsim.engine.Context
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.graph.GraphNode
import com.bharatsim.engine.models.{Agent, Network, Node, StatefulAgent}
import com.bharatsim.engine.utils.Probability.biasedCoinToss
import sirFSM.InfectionStatus._

case class Person(id: Long,
                  age: Int,
                  infectionState: InfectionStatus,
                  infectedOnDay: Double = -10000.0,
                  recoveredOnDay: Double = -10000.0,
                  daysInfected: Double = 0,
                  infectingAgent: String = "",
                  infectedAt: String = "",
                  agentsInfected: Int = 0,
                  currentLocation: String = "House",
                  isFarmer: Boolean = false) extends StatefulAgent {


  def isSusceptible: Boolean = infectionState == Susceptible

  def isInfected: Boolean = infectionState == Infected

  def isRecovered: Boolean = infectionState == Removed

  private val incrementInfectionDuration: Context => Unit = (context: Context) => {
    if (isInfected && context.getCurrentStep % Parameters.numberOfTicksInADay == 0) {
      updateParam("ticksInfected", daysInfected + 1)
    }
  }

  def decodeNode(classType: String, node: GraphNode): Network = {
    classType match {
      case "House" => node.as[House]
      case "Office" => node.as[Office]
      case "School" => node.as[School]
    }
  }


  private def getCurrentPlace(context: Context): Option[String] = {
    val schedule = context.fetchScheduleFor(this).get
    val currentStep = context.getCurrentStep + 1
    val placeType: String = schedule.getForStep(currentStep)

    Some(placeType)
  }

  private def updateCurrentLocation(context: Context): Unit = {
    val currentPlaceOption = getCurrentPlace(context)
    currentPlaceOption match {
      case Some(x) => {
        if (this.currentLocation != x) {
          updateParam("currentLocation", x)
        }
      }
      case _ =>
    }
  }

  addBehaviour(incrementInfectionDuration)
  addBehaviour(updateCurrentLocation)

  addRelation[House]("STAYS_AT")
  addRelation[Office]("WORKS_AT")
  addRelation[School]("STUDIES_AT")
}
