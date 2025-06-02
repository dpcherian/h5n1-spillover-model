package sirFSM

import com.bharatsim.engine.distributions.LogNormal

import scala.math.sqrt

object Parameters {

  val studentAge = 18

  var betaBH: Double = 5e-6

  var betaHH: Double = 0.3

  final val meanOfExposedLognormalDist : Double = 3
  final val stdOfExposedLognormalDist : Double = 1

  final val meanOfInfectedLognormalDist: Double = 7
  final val stdOfInfectedLognormalDist: Double = 3


  final val exposedDurationProbabilityDistribution = LogNormal(meanOfExposedLognormalDist, stdOfExposedLognormalDist)
  final val infectedDurationProbabilityDistribution = LogNormal(meanOfInfectedLognormalDist, stdOfInfectedLognormalDist)

  final val numberOfTicksInADay: Int = 2
  final val dt : Double = 1f/numberOfTicksInADay


  var inputPath = "./population"
  var outputPath = "./"
  var initialRecoveredFraction = 0.0
  var initialExposedFraction = 0.0

  var Nbirds = 1_000
  var birdI = 10.0

  var birdBeta = (1d/3)
  var birdGamma= (1d/7)

  var saveTotalOutput = false
  var saveAgewiseOutput = false
  var saveInfectionInfoOutput = false
  var saveAgentOutput = true

  var SIMDAYS = 200

  var culling_date=1000000.0

  var lockdownFarmers : Boolean = false
  var lockdownFarmersInfectedThreshold : Int = 10

  final val splittableRandom: RandomNumberGenerator = RandomNumberGenerator()

  var birdSArray = Array.empty[Double]
  var birdIArray = Array.empty[Double]
  var birdRArray = Array.empty[Double]

  def birdFOI(t: Double = 0, isFarm:Boolean): Double = {
    if(isFarm && t < culling_date){
     val tick = (t*numberOfTicksInADay).toInt
      birdIArray(tick)
    }
    else{
      0.0
    }
  }

  var totalPopulation : Int = 0;

  // Vaccination parameters
  var riskBasedVaccination : Boolean = true
  var vaccinate : Boolean = true
  var vaccineEfficacy : Double = 1.0
  var dvr : Double = 1d/100

  var vaccineDelayDays : Double = 7

  var firstCaseRecorded : Boolean = false
  var firstCaseRecordedDay : Double = 10000.0

  var vaccinationStartedOn : Double = 0

  var FOIZERODAYSET : Boolean = false
  var FOIZERODAY : Double = 10000.0

}
