package lcd7

import kotlin.experimental.and
import kotlin.experimental.or
import lcd7.Segment.*

/**
 * This file contains general purpose abstractions for (malfunctioning) 7-segment LCD digit displays, and
 * their use related to a 24-hour clock display.
 */
const val ZERO = 0.toByte()

/**
 * A model for the 7 segments. Named by whether they are horizontal or vertical, and their position.
 * Since there are 7 of them, it is quite handy to model them with bits in a single Byte.
 * A whole group of 7 segments can be tested for equality via comparison of their Byte.
 */
enum class Segment(val onValue: Byte) {
   HORIZ_UPPER((1 shl 0).toByte()),
   HORIZ_MIDDLE((1 shl 1).toByte()),
   HORIZ_LOWER((1 shl 2).toByte()),
   VERT_UPPER_LEFT((1 shl 3).toByte()),
   VERT_UPPER_RIGHT((1 shl 4).toByte()),
   VERT_LOWER_LEFT((1 shl 5).toByte()),
   VERT_LOWER_RIGHT((1 shl 6).toByte()),
   ;

   inline val isVertical: Boolean
      get() = this != HORIZ_UPPER && this != HORIZ_MIDDLE && this != HORIZ_LOWER
}

/**
 * The condition of an LCD segment:
 * `BURNED_OUT`: the segment cannot turn on.
 * `BURNED_IN`: the segment is always on.
 * `WORKING`: the segment lights acording to its instruction.
 * `UNKNOWN`: the condition has not been determined.
 */
enum class SegmentCondition {
   BURNED_OUT,
   BURNED_IN,
   WORKING,
   UNKNOWN;
   val isBurned: Boolean
      get() = this == BURNED_OUT || this == BURNED_IN
}

/**
 * A digit and how it is represented by a 7-segment LCD digit. It is modeled by storing a Byte
 * containing the `onValue`s of the segments that are on.
 */
enum class Digit(val debugChar: Char) {
   Zero('0', listOf(HORIZ_UPPER, HORIZ_LOWER, VERT_UPPER_RIGHT, VERT_UPPER_LEFT, VERT_LOWER_LEFT, VERT_LOWER_RIGHT)),
   One('1', listOf(VERT_UPPER_RIGHT, VERT_LOWER_RIGHT)),
   Two('2', listOf(HORIZ_UPPER, HORIZ_MIDDLE, HORIZ_LOWER, VERT_UPPER_RIGHT, VERT_LOWER_LEFT)),
   Three('3', listOf(HORIZ_UPPER, HORIZ_MIDDLE, HORIZ_LOWER, VERT_UPPER_RIGHT, VERT_LOWER_RIGHT)),
   Four('4', listOf(HORIZ_MIDDLE, VERT_UPPER_LEFT, VERT_UPPER_RIGHT, VERT_LOWER_RIGHT)),
   Five('5', listOf(HORIZ_UPPER, HORIZ_MIDDLE, HORIZ_LOWER, VERT_UPPER_LEFT, VERT_LOWER_RIGHT)),
   Six('6', listOf(HORIZ_UPPER, HORIZ_MIDDLE, HORIZ_LOWER, VERT_UPPER_LEFT, VERT_LOWER_RIGHT, VERT_LOWER_LEFT)),
   Seven('7', listOf(HORIZ_UPPER, VERT_UPPER_RIGHT, VERT_LOWER_RIGHT)),
   Eight('8', true, true, true, true, true, true, true),
   Nine('9', true, true, true, true, false, true, true),
   // Only the HOUR_TENS digit can be Null
   Null(' ', emptyList()),
   ;

   var onSegmentFlags: Byte = 0; private set // TODO: fix the constructors

   inline fun isOn(segment: Segment): Boolean =
      this.onSegmentFlags.and(segment.onValue) != ZERO

   constructor(debugChar: Char, segmentList: List<Segment>) : this(debugChar) {
      var flags: Byte = 0
      for (segment in segmentList) {
         flags = flags.or(segment.onValue)
      }
      this.onSegmentFlags = flags
   }

   constructor(debugChar: Char, top: Boolean, upperLeft: Boolean, upperRight: Boolean, middle: Boolean, lowerLeft: Boolean, lowerRight: Boolean, bottom: Boolean) : this(debugChar) {
      var flags: Byte = 0
      if (top)
         flags = flags.or(HORIZ_UPPER.onValue)
      if (upperLeft)
         flags = flags.or(VERT_UPPER_LEFT.onValue)
      if (upperRight)
         flags = flags.or(VERT_UPPER_RIGHT.onValue)
      if (middle)
         flags = flags.or(HORIZ_MIDDLE.onValue)
      if (lowerLeft)
         flags = flags.or(VERT_LOWER_LEFT.onValue)
      if (lowerRight)
         flags = flags.or(VERT_LOWER_RIGHT.onValue)
      if (bottom)
         flags = flags.or(HORIZ_LOWER.onValue)
      this.onSegmentFlags = flags
   }
}

/**
 * DigitCondition is an encapsulation of the conditions of the 7 LCD segments. It is modelled as a light
 * wrapper around a list of 7 segment conditions. The only abstraction is that you can perform
 * subscripting with a Segment value. If you construct from a list, there are no sanity checks that
 * it has 7 elements.
 */
@JvmInline value class DigitCondition(val segmentConditions: MutableList<SegmentCondition>) {
   constructor(segmentCondition: SegmentCondition = SegmentCondition.UNKNOWN) : this(
      segmentCondition,
      segmentCondition,
      segmentCondition,
      segmentCondition,
      segmentCondition,
      segmentCondition,
      segmentCondition
   )

   constructor(
      top: SegmentCondition,
      upperLeft: SegmentCondition,
      upperRight: SegmentCondition,
      middle: SegmentCondition,
      lowerLeft: SegmentCondition,
      lowerRight: SegmentCondition,
      bottom: SegmentCondition
   ) :
      this(mutableListOf(top, middle, bottom, upperLeft, upperRight, lowerLeft, lowerRight))

   inline operator fun get(segment: Segment): SegmentCondition = segmentConditions[segment.ordinal]
   inline operator fun set(segment: Segment, value: SegmentCondition) {
      segmentConditions[segment.ordinal] = value
   }
}

/**
 * A modeling of 24-hour time format in HH:MM. Not very complicated, but it provides a streamlined way to
 * access the 4 Digits that would be displayed on a 4-digit LCD7 display at a certain time.
 * Hours are [0, 23] and minutes are [0, 59].
 * This assumes that the hour tens' digit should be completely off for times less than 10:00,
 * and is currently not configurable.
 */
class Time(val minutesOffsetFromMidnight: Int) {
   val hourDigitOne: Digit
   val hourDigitTwo: Digit
   val minuteDigitOne: Digit
   val minuteDigitTwo: Digit

   init {
      val hour = (minutesOffsetFromMidnight / 60) % 24
      val minute = minutesOffsetFromMidnight % 60
      hourDigitOne = when {
         hour < 10 -> Digit.Null
         hour < 20 -> Digit.One
         else -> Digit.Two
      }
      hourDigitTwo = when (hour % 10) {
         0 -> Digit.Zero
         1 -> Digit.One
         2 -> Digit.Two
         3 -> Digit.Three
         4 -> Digit.Four
         5 -> Digit.Five
         6 -> Digit.Six
         7 -> Digit.Seven
         8 -> Digit.Eight
         else -> Digit.Nine
      }
      minuteDigitOne = when {
         minute < 10 -> Digit.Zero
         minute < 20 -> Digit.One
         minute < 30 -> Digit.Two
         minute < 40 -> Digit.Three
         minute < 50 -> Digit.Four
         else -> Digit.Five
      }
      minuteDigitTwo = when (minute % 10) {
         0 -> Digit.Zero
         1 -> Digit.One
         2 -> Digit.Two
         3 -> Digit.Three
         4 -> Digit.Four
         5 -> Digit.Five
         6 -> Digit.Six
         7 -> Digit.Seven
         8 -> Digit.Eight
         else -> Digit.Nine
      }
   }

   fun nextMinute(): Time = Time(this.minutesOffsetFromMidnight + 1)

   override fun toString(): String {
      return "Time(minutesOffsetFromMidnight=$minutesOffsetFromMidnight) ${hourDigitOne.debugChar}${hourDigitTwo.debugChar}:${minuteDigitOne.debugChar}${minuteDigitTwo.debugChar}"
   }
}

/**
 * A very simple mechanism for accessing a Digit from a Time dynamically.
 */
enum class TimeDigit {
   HOUR_TENS, HOUR_ONES, MINUTE_TENS, MINUTE_ONES;
   fun atTime(time: Time): Digit = when (this) {
      HOUR_TENS -> time.hourDigitOne
      HOUR_ONES -> time.hourDigitTwo
      MINUTE_TENS -> time.minuteDigitOne
      MINUTE_ONES -> time.minuteDigitTwo
   }
}

///////////////////////////////////////////////////////////////////////////////
// Observations
///////////////////////////////////////////////////////////////////////////////

/**
 * A digit observation can have any set of Segments on.
 */
@JvmInline value class DigitObservation(val onSegmentFlags: Byte) {
   constructor(top: Boolean, upperLeft: Boolean, upperRight: Boolean, middle: Boolean, lowerLeft: Boolean, lowerRight: Boolean, bottom: Boolean) : this(
      (if (top) HORIZ_UPPER.onValue else ZERO)
         .or(if (upperLeft) VERT_UPPER_LEFT.onValue else ZERO)
         .or(if (upperRight) VERT_UPPER_RIGHT.onValue else ZERO)
         .or(if (middle) HORIZ_MIDDLE.onValue else ZERO)
         .or(if (lowerLeft) VERT_LOWER_LEFT.onValue else ZERO)
         .or(if (lowerRight) VERT_LOWER_RIGHT.onValue else ZERO)
         .or(if (bottom) HORIZ_LOWER.onValue else ZERO)
   )
   inline fun isOn(segment: Segment): Boolean =
      this.onSegmentFlags.and(segment.onValue) != ZERO

   /**
    * An Observation matches a Digit if the same segments are on.
    */
   inline fun matches(digit: Digit): Boolean =
      onSegmentFlags == digit.onSegmentFlags

   override fun toString(): String {
      return "DigitObservation(onSegmentFlags=${onSegmentFlags.toString(2)})"
   }

   companion object {
      inline fun fromDigit(d: Digit): DigitObservation = DigitObservation(d.onSegmentFlags)
   }
}

/**
 * An observation of an LCD time display consists of a DigitObservation for each of the 4 TimeDigits,
 * a Boolean for whether the upper colon dot was on, and a Boolean for whether the lower colon dot
 * was on.
 */
class TimeDisplayObservation(
   val hourTensDigitObservation: DigitObservation,
   val hourOnesDigitObservation: DigitObservation,
   val minuteTensDigitObservation: DigitObservation,
   val minuteOnesDigitObservation: DigitObservation,
   val upperColonOn: Boolean,
   val lowerColonOn: Boolean
) {
   /**
    * A mechanism to select a DigitObservation dynamically from a TimeDigit.
    */
   fun observationOf(digit: TimeDigit): DigitObservation = when (digit) {
      TimeDigit.HOUR_TENS -> hourTensDigitObservation
      TimeDigit.HOUR_ONES -> hourOnesDigitObservation
      TimeDigit.MINUTE_TENS -> minuteTensDigitObservation
      TimeDigit.MINUTE_ONES -> minuteOnesDigitObservation
   }

   /**
    * Returns whether this observation is possible at a specific Time with a specific DigitCondition
    * for each TimeDigit. The colon indicator is not analyzed.
    * The logic to determine DigitCondition from observations is problem dependent.
    * @param time The Time you want to determine to analyze.
    * @param digitConditions the status of the 4 7-segment Digits of the display in TimeDigit order.
    */
   fun matches(time: Time, digitConditions: List<DigitCondition>): Boolean {
      // In order for the observation to match a Time, all segments whose condition is indicated to
      // equal WORKING by the parameter, must match on/off state of a perfect display.
      // Segments whose condition is not WORKING cannot yield any meaningful assessment, so they're ignored.
      for (timeDigit in TimeDigit.entries) {
         val digitCondition = digitConditions[timeDigit.ordinal]
         val ideal = timeDigit.atTime(time)
         for (segment in Segment.entries) {
            if (digitCondition[segment] == SegmentCondition.WORKING) {
               if (ideal.isOn(segment) != observationOf(timeDigit).isOn(segment))
                  return false
            }
         }
      }
      return true
   }

   // To allow static extension functions.
   companion object {}

   override fun toString(): String {
      return "TimeDisplayObservation(hourTensDigitObservation=$hourTensDigitObservation, hourOnesDigitObservation=$hourOnesDigitObservation, minuteTensDigitObservation=$minuteTensDigitObservation, minuteOnesDigitObservation=$minuteOnesDigitObservation, upperColonOn=$upperColonOn, lowerColonOn=$lowerColonOn)"
   }
}