package sirFSM

object Parameters {

  val studentAge = 18

  var betaBH: Double = 5e-6

  var betaHH: Double = 0.3
  final val lambda_I : Double = 1f/7

  final val numberOfTicksInADay: Int = 2
  final val dt : Double = 1f/numberOfTicksInADay


  var inputPath = "./hpc/birdflupop"
  var outputPath = "./"
  var initialRecoveredFraction = 0.0
  var initialInfectedFraction = 0.0

  var Nbirds = 1_000
  var birdI = 10.0

  var birdBeta = (1d/3)
  var birdGamma= (1d/7)

  var saveTotalOutput = true
  var saveAgewiseOutput = false
  var saveInfectionInfoOutput = false
  var saveAgentOutput = true

  var SIMDAYS = 200

  var culling_date=1000000.0

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

}
