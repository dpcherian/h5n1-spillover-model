package sirFSM.diseaseStates

import com.bharatsim.engine.Context
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.fsm.State
import com.bharatsim.engine.graph.patternMatcher.MatchCondition._
import com.bharatsim.engine.models.{Network, StatefulAgent}
import com.bharatsim.engine.utils.Probability.biasedCoinToss
import sirFSM.InfectionStatus._
import sirFSM.{Parameters, Person}


case class InfectedState() extends State {

  override def enterAction(context: Context, agent: StatefulAgent): Unit = {
    agent.updateParam("infectionState", Infected)
    agent.updateParam("infectedOnDay", context.getCurrentStep*Parameters.dt)
  }

  override def perTickAction(context: Context, agent: StatefulAgent): Unit = {
    agent.updateParam("daysInfected", agent.asInstanceOf[Person].daysInfected + Parameters.dt)
  }

  def exitInfected(context: Context, agent: StatefulAgent): Boolean = {
     biasedCoinToss(Parameters.lambda_I * Parameters.dt)
  }

  addTransition(
    when =  exitInfected ,
      to =  context => RecoveredState()
  )

}
