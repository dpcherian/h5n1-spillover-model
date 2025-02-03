package sirFSM

import com.bharatsim.engine.Context
import com.bharatsim.engine.graph.patternMatcher.MatchCondition._
import com.bharatsim.engine.listeners.CSVSpecs
import sirFSM.InfectionStatus._

class SEIROutputSpec(context: Context) extends CSVSpecs {
  override def getHeaders: List[String] =
    List(
      "Time",
      "Susceptible",
      "Exposed",
      "Infected",
      "Removed",
      "BirdFOI",
      "BirdS",
      "BirdI",
      "BirdR"
    )

  override def getRows(): List[List[Any]] = {
    val graphProvider = context.graphProvider
    val label = "Person"
    val row = List(
      context.getCurrentStep*Parameters.dt,
      graphProvider.fetchCount(label, "infectionState" equ Susceptible),
      graphProvider.fetchCount(label, "infectionState" equ Exposed),
      graphProvider.fetchCount(label, "infectionState" equ Infected),
      graphProvider.fetchCount(label, "infectionState" equ Removed),
      Parameters.birdFOI(context.getCurrentStep*Parameters.dt, isFarm = true),
      Parameters.birdSArray(context.getCurrentStep),
      Parameters.birdIArray(context.getCurrentStep),
      Parameters.birdRArray(context.getCurrentStep)
    )
    List(row)
  }
}
