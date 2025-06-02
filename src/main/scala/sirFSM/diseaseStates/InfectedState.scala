package sirFSM.diseaseStates

import com.bharatsim.engine.Context
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.fsm.State
import com.bharatsim.engine.graph.patternMatcher.MatchCondition._
import com.bharatsim.engine.models.StatefulAgent
import sirFSM.InfectionStatus._
import sirFSM.{Parameters, Person}


case class InfectedState(time: Double) extends State {

  override def enterAction(context: Context, agent: StatefulAgent): Unit = {
    agent.updateParam("infectionState", Infected)
    agent.updateParam("infectedOnDay", context.getCurrentStep*Parameters.dt)

    if(!Parameters.firstCaseRecorded){
      Parameters.firstCaseRecordedDay = context.getCurrentStep*Parameters.dt
      Parameters.firstCaseRecorded = true
    }
  }

  override def perTickAction(context: Context, agent: StatefulAgent): Unit = {
    agent.updateParam("daysInfected", agent.asInstanceOf[Person].daysInfected + Parameters.dt)
  }

  def exitInfected(context: Context, agent: StatefulAgent): Boolean = {
    if (context.getCurrentStep * Parameters.dt >= time) {
      return true
    }
    false
  }


  addTransition(
    when =  exitInfected,
      to =  context => RecoveredState()
  )

}
