package sirFSM

import com.bharatsim.engine.models.Network

case class Office(id: Long, isFarm: Boolean = false) extends Network {
  addRelation[Person]("EMPLOYER_OF")

  override def getContactProbability(): Double = 1
}
