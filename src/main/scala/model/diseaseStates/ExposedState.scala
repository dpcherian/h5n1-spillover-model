package model.diseaseStates

import com.bharatsim.engine.Context
import com.bharatsim.engine.basicConversions.decoders.DefaultDecoders._
import com.bharatsim.engine.basicConversions.encoders.DefaultEncoders._
import com.bharatsim.engine.fsm.State
import com.bharatsim.engine.models.StatefulAgent
import model.InfectionStatus._
import model.{Parameters, Person}


case class ExposedState(time: Double) extends State {

  override def enterAction(context: Context, agent: StatefulAgent): Unit = {
    agent.updateParam("infectionState", Exposed)
    agent.updateParam("exposedOnDay", context.getCurrentStep*Parameters.dt)
  }

  override def perTickAction(context: Context, agent: StatefulAgent): Unit = {
    agent.updateParam("daysExposed", agent.asInstanceOf[Person].daysExposed + Parameters.dt)
  }

  def exitExposed(context: Context, agent: StatefulAgent): Boolean = {
    if (context.getCurrentStep * Parameters.dt >= time) {
      return true
    }
    false
  }


  addTransition(
    when =  exitExposed,
      to =  context => InfectedState(time = context.getCurrentStep * Parameters.dt + Parameters.infectedDurationProbabilityDistribution.sample())
  )

}
