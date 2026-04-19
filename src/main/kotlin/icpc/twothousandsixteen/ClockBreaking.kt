package icpc.twothousandsixteen

import lcd7.*
import lcd7.Segment.*
import lcd7.SegmentCondition.*
import util.allIndexed
import java.io.InputStream
import java.io.OutputStream

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/clock
 * The core business models are general, so I've separated those into their own package & source file.
 * NOTE: this implementation passes the first 3 test cases on kattis.com, but fails the fourth.
 */

const val DEBUG = false
fun printDebug(cs: CharSequence) {
   if (DEBUG) {
      println(cs)
   }
}

/**
 * A TimeDisplayAssessment contains a DigitCondition for each of the TimeDigits, and a SegmentCondition
 * for each of the 2 dots of the colon.
 */
class TimeDisplayAssessment(
   val hourTensDigitCondition: DigitCondition,
   val hourOnesDigitCondition: DigitCondition,
   val minuteTensDigitCondition: DigitCondition,
   val minuteOnesDigitCondition: DigitCondition,
   val upperColonCondition: SegmentCondition,
   val lowerColonCondition: SegmentCondition,
) {

   fun serialize(outputStream: OutputStream) {
      val writer = outputStream.bufferedWriter(Charsets.US_ASCII)
      writer.use {
         writer.appendLine(".${hourTensDigitCondition.serializeSegment(HORIZ_UPPER)}...${hourOnesDigitCondition.serializeSegment(HORIZ_UPPER)}.....${minuteTensDigitCondition.serializeSegment(HORIZ_UPPER)}...${minuteOnesDigitCondition.serializeSegment(HORIZ_UPPER)}.")
         val line2 = "${hourTensDigitCondition.serializeSegment(VERT_UPPER_LEFT)}..${hourTensDigitCondition.serializeSegment(VERT_UPPER_RIGHT)}.${hourOnesDigitCondition.serializeSegment(VERT_UPPER_LEFT)}..${hourOnesDigitCondition.serializeSegment(VERT_UPPER_RIGHT)}...${minuteTensDigitCondition.serializeSegment(VERT_UPPER_LEFT)}..${minuteTensDigitCondition.serializeSegment(VERT_UPPER_RIGHT)}.${minuteOnesDigitCondition.serializeSegment(VERT_UPPER_LEFT)}..${minuteOnesDigitCondition.serializeSegment(VERT_UPPER_RIGHT)}"
         writer.appendLine(line2)
         writer.appendLine(line2.replaceRange(10, 11, upperColonCondition.serialize(VERT_UPPER_RIGHT)))
         writer.appendLine(".${hourTensDigitCondition.serializeSegment(HORIZ_MIDDLE)}...${hourOnesDigitCondition.serializeSegment(HORIZ_MIDDLE)}.....${minuteTensDigitCondition.serializeSegment(HORIZ_MIDDLE)}...${minuteOnesDigitCondition.serializeSegment(HORIZ_MIDDLE)}.")
         val line6 = "${hourTensDigitCondition.serializeSegment(VERT_LOWER_LEFT)}..${hourTensDigitCondition.serializeSegment(VERT_LOWER_RIGHT)}.${hourOnesDigitCondition.serializeSegment(VERT_LOWER_LEFT)}..${hourOnesDigitCondition.serializeSegment(VERT_LOWER_RIGHT)}...${minuteTensDigitCondition.serializeSegment(VERT_LOWER_LEFT)}..${minuteTensDigitCondition.serializeSegment(VERT_LOWER_RIGHT)}.${minuteOnesDigitCondition.serializeSegment(VERT_LOWER_LEFT)}..${minuteOnesDigitCondition.serializeSegment(VERT_LOWER_RIGHT)}"
         writer.appendLine(line6.replaceRange(10, 11, lowerColonCondition.serialize(VERT_UPPER_RIGHT)))
         writer.appendLine(line6)
         writer.appendLine(".${hourTensDigitCondition.serializeSegment(HORIZ_LOWER)}...${hourOnesDigitCondition.serializeSegment(HORIZ_LOWER)}.....${minuteTensDigitCondition.serializeSegment(HORIZ_LOWER)}...${minuteOnesDigitCondition.serializeSegment(HORIZ_LOWER)}.")
      }
   }

   override fun toString(): String {
      return "TimeDisplayAssessment(hourTensDigitCondition=$hourTensDigitCondition, hourOnesDigitCondition=$hourOnesDigitCondition, minuteTensDigitCondition=$minuteTensDigitCondition, minuteOnesDigitCondition=$minuteOnesDigitCondition, upperColonCondition=$upperColonCondition, lowerColonCondition=$lowerColonCondition)"
   }

   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as TimeDisplayAssessment

      if (hourTensDigitCondition != other.hourTensDigitCondition) return false
      if (hourOnesDigitCondition != other.hourOnesDigitCondition) return false
      if (minuteTensDigitCondition != other.minuteTensDigitCondition) return false
      if (minuteOnesDigitCondition != other.minuteOnesDigitCondition) return false
      if (upperColonCondition != other.upperColonCondition) return false
      return lowerColonCondition == other.lowerColonCondition
   }

   override fun hashCode(): Int {
      var result = hourTensDigitCondition.hashCode()
      result = 31 * result + hourOnesDigitCondition.hashCode()
      result = 31 * result + minuteTensDigitCondition.hashCode()
      result = 31 * result + minuteOnesDigitCondition.hashCode()
      result = 31 * result + upperColonCondition.hashCode()
      result = 31 * result + lowerColonCondition.hashCode()
      return result
   }
}

fun TimeDisplayAssessment?.serialize(outputStream: OutputStream) {
   if (this == null) {
      outputStream.writer().use {
         it.write("impossible")
      }
   } else
      this.serialize(outputStream)
}

fun main() {
   assessTimeDisplayIO(System.`in`, System.out)
}

fun assessTimeDisplayIO(inStream: InputStream, outStream: OutputStream) {
   val bufferedIn = inStream.bufferedReader(Charsets.US_ASCII)
   val observationCount: Int = bufferedIn.readLine().toInt()
   val observations = ArrayList<TimeDisplayObservation>(observationCount)
   repeat(observationCount) {
      val newObservation = TimeDisplayObservation.parse(listOf(
         bufferedIn.readLine(),
         bufferedIn.readLine(),
         bufferedIn.readLine(),
         bufferedIn.readLine(),
         bufferedIn.readLine(),
         bufferedIn.readLine(),
         bufferedIn.readLine(),
      ))
      observations.add(newObservation)
      bufferedIn.readLine()
   }
   val assessment = assessTimeDisplay(observations)
   assessment.serialize(outStream)
}

/**
 * The core problem solution.
 */
fun assessTimeDisplay(displayObservations: List<TimeDisplayObservation>): TimeDisplayAssessment? {
   val digitConditions = TimeDigit.entries.map { timeDigit ->
      assessWorkingOrBurned(timeDigit, displayObservations)
   }

   var possible = false

   printDebug("Digit conditions: $digitConditions")
   for (startTimeOffset in 0..< 24*60) {
      val startTime = Time(startTimeOffset)
      if (displayObservations.allIndexed { index, observation ->
         observation.matches(Time(startTimeOffset + index), digitConditions)
      }) {
         printDebug("Observations match time $startTime")
         possible = true
         accountAmbiguousConditions(startTime, displayObservations, digitConditions)
      }
   }

   if (!possible)
      return null

   var upperColonCondition = UNKNOWN
   if (displayObservations.any { !it.upperColonOn })
      upperColonCondition = BURNED_OUT
   var lowerColonCondition = UNKNOWN
   if (displayObservations.any { !it.lowerColonOn })
      lowerColonCondition = BURNED_OUT

   return TimeDisplayAssessment(
      digitConditions[0],
      digitConditions[1],
      digitConditions[2],
      digitConditions[3],
      upperColonCondition,
      lowerColonCondition
   )
}

private fun assessWorkingOrBurned(digit: TimeDigit, observations: List<TimeDisplayObservation>): DigitCondition {
   val conditionList = mutableListOf<SegmentCondition>()
   Segment.entries.mapTo(conditionList) { segment ->
      val seenOn = observations.any { it.observationOf(digit).isOn(segment) }
      val seenOff = observations.any { !it.observationOf(digit).isOn(segment) }
      if (seenOn && seenOff)
         WORKING
      else if (seenOn)
         BURNED_IN
      else if (seenOff)
         BURNED_OUT
      else
         throw IllegalArgumentException("observations cannot be empty")
   }
   return DigitCondition(conditionList)
}

private fun accountAmbiguousConditions(
   startTime: Time,
   observations: List<TimeDisplayObservation>,
   conditions: List<DigitCondition>
) {
   printDebug("Accounting ambiguous conditions")
   var currentTime = startTime
   val matchesAll = Array(TimeDigit.entries.size) { BooleanArray(Segment.entries.size) { true } }

   for (observation in observations) {
      for (timeDigit in TimeDigit.entries) {
         val digitObservation = observation.observationOf(timeDigit)
         val digitAtTime = timeDigit.atTime(currentTime)
         for (segment in Segment.entries) {
            if (conditions[timeDigit.ordinal][segment].isBurned) {
               if (digitObservation.isOn(segment) != digitAtTime.isOn(segment)) {
                  matchesAll[timeDigit.ordinal][segment.ordinal] = false
                  continue
               }
            }
         }
      }
      currentTime = currentTime.nextMinute()
   }

   for (timeDigit in TimeDigit.entries) {
      val condition = conditions[timeDigit.ordinal]
      for (segment in Segment.entries) {
         if (condition[segment].isBurned && matchesAll[timeDigit.ordinal][segment.ordinal]) {
            printDebug("Condition UNKNOWN on digit $timeDigit segment $segment")
            condition[segment] = UNKNOWN
         }
      }
   }
}

fun TimeDisplayObservation.Companion.parse(icpc2016FormatLines: List<String>): TimeDisplayObservation {
   val CHAR_ON = 'X'
   val hourTensDigitObservation = DigitObservation(
      top = icpc2016FormatLines[0][1] == CHAR_ON,
      upperLeft = icpc2016FormatLines[1][0] == CHAR_ON,
      upperRight = icpc2016FormatLines[1][3] == CHAR_ON,
      middle = icpc2016FormatLines[3][2] == CHAR_ON,
      lowerLeft = icpc2016FormatLines[4][0] == CHAR_ON,
      lowerRight = icpc2016FormatLines[5][3] == CHAR_ON,
      bottom = icpc2016FormatLines[6][1] == CHAR_ON
   )
   val hourOnesDigitObservation = DigitObservation(
      top = icpc2016FormatLines[0][5 + 1] == CHAR_ON,
      upperLeft = icpc2016FormatLines[1][5 + 0] == CHAR_ON,
      upperRight = icpc2016FormatLines[1][5 + 3] == CHAR_ON,
      middle = icpc2016FormatLines[3][5 + 2] == CHAR_ON,
      lowerLeft = icpc2016FormatLines[4][5 + 0] == CHAR_ON,
      lowerRight = icpc2016FormatLines[5][5 + 3] == CHAR_ON,
      bottom = icpc2016FormatLines[6][5 + 1] == CHAR_ON
   )
   val minuteTensDigitObservation = DigitObservation(
      top = icpc2016FormatLines[0][12 + 1] == CHAR_ON,
      upperLeft = icpc2016FormatLines[1][12 + 0] == CHAR_ON,
      upperRight = icpc2016FormatLines[1][12 + 3] == CHAR_ON,
      middle = icpc2016FormatLines[3][12 + 2] == CHAR_ON,
      lowerLeft = icpc2016FormatLines[4][12 + 0] == CHAR_ON,
      lowerRight = icpc2016FormatLines[5][12 + 3] == CHAR_ON,
      bottom = icpc2016FormatLines[6][12 + 1] == CHAR_ON
   )
   val minuteOnesDigitObservation = DigitObservation(
      top = icpc2016FormatLines[0][17 + 1] == CHAR_ON,
      upperLeft = icpc2016FormatLines[1][17 + 0] == CHAR_ON,
      upperRight = icpc2016FormatLines[1][17 + 3] == CHAR_ON,
      middle = icpc2016FormatLines[3][17 + 2] == CHAR_ON,
      lowerLeft = icpc2016FormatLines[4][17 + 0] == CHAR_ON,
      lowerRight = icpc2016FormatLines[5][17 + 3] == CHAR_ON,
      bottom = icpc2016FormatLines[6][17 + 1] == CHAR_ON
   )
   val upperColonOn = icpc2016FormatLines[2][10] == CHAR_ON
   val lowerColonOn = icpc2016FormatLines[4][10] == CHAR_ON
   return TimeDisplayObservation(
      hourTensDigitObservation,
      hourOnesDigitObservation,
      minuteTensDigitObservation,
      minuteOnesDigitObservation,
      upperColonOn,
      lowerColonOn
   )
}

inline fun DigitCondition.serializeSegment(segment: Segment): String =
   this[segment].serialize(segment)

fun SegmentCondition.serialize(forSegment: Segment): String {
   return if (forSegment.isVertical) {
      when (this) {
         BURNED_OUT -> "0"
         BURNED_IN -> "1"
         WORKING -> "W"
         UNKNOWN -> "?"
      }
   } else {
      when (this) {
         BURNED_OUT -> "00"
         BURNED_IN -> "11"
         WORKING -> "WW"
         UNKNOWN -> "??"
      }
   }
}
