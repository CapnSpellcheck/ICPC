package icpc.twothousandsixteen

import lcd7.*
import lcd7.Segment.*
import lcd7.SegmentCondition.*
import util.allIndexed

/**
 * This file contains a solution related to the problem:
 * The core business models are general, so I've separated those into their own package & source file.
 */

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

/**
 * The core problem solution.
 */
fun assessTimeDisplay(displayObservations: List<TimeDisplayObservation>): TimeDisplayAssessment? {
   if (displayObservations.size == 1) {
      val unknown = DigitCondition()
      return TimeDisplayAssessment(
         if (displayObservations[0].hourTensDigitObservation.isOn(VERT_UPPER_LEFT))
            DigitCondition().also { it[VERT_UPPER_LEFT] = BURNED_IN }
         else unknown,
         unknown,
         unknown,
         unknown,
         if (displayObservations[0].upperColonOn) UNKNOWN else BURNED_OUT,
         if (displayObservations[0].lowerColonOn) UNKNOWN else BURNED_OUT,
      )
   }

   val digitConditions: List<DigitCondition> = TimeDigit.entries.map { timeDigit ->
      assessWorkingOrBurned(timeDigit, displayObservations)
   }

   var possible = false

   for (startTimeOffset in 0..< 24*60) {
      val startTime = Time(startTimeOffset)
      if (displayObservations.allIndexed { index, observation ->
         observation.matches(Time(startTimeOffset + index), digitConditions)
      }) {
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

private fun accountAmbiguousConditions(startTime: Time, observations: List<TimeDisplayObservation>, conditions: List<DigitCondition>) {
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
         if (condition[segment].isBurned && matchesAll[timeDigit.ordinal][segment.ordinal])
            condition[segment] = UNKNOWN
      }
   }
}