package model

import com.bharatsim.engine.models.Network

case class School(id: Long) extends Network {
  addRelation[Person]("STUDENT_OF")

  override def getContactProbability(): Double = 1
}
