package icpc.twothousandsixteen

import clock
import lcd7.Digit.*
import lcd7.DigitCondition
import lcd7.DigitObservation
import lcd7.Segment.*
import lcd7.SegmentCondition.*
import lcd7.TimeDisplayObservation
import org.junit.jupiter.api.Test
import util.StringOutputStream
import java.io.PrintStream
import java.io.StringBufferInputStream
import kotlin.experimental.or
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Test case generation helpers so I don't use the target code to construct test cases
class Generator {
   enum class Condition(val output: Char) {
      WORKING('W'),
      BURNED_OUT('0'),
      BURNED_IN('1'),
      UNKNOWN('?') // Used only for output.
      ;

      inline fun withDetectable(b: Boolean): Condition = if (b) this else UNKNOWN
   }

   companion object {
      fun generateDisplayObservation(
         hour: Int,
         minute: Int,
         hourTensCondition: List<Condition> = allWorkingCondition,
         hourOnesCondition: List<Condition> = allWorkingCondition,
         minuteTensCondition: List<Condition> = allWorkingCondition,
         minuteOnesCondition: List<Condition> = allWorkingCondition,
         upperColonWorking: Boolean = true,
         lowerColonWorking: Boolean = true,
      ): List<String>
      {
         fun outputSegment(value: Boolean, condition: Condition, width: Int = 1): String =
            when (condition) {
               Condition.WORKING -> if (value) "X" else "."
               Condition.BURNED_OUT -> "."
               Condition.BURNED_IN -> "X"
               Condition.UNKNOWN -> error("")
            }.repeat(width)
         fun outputDigit(value: Int, condition: List<Condition>, spaceAfter: Boolean, nullForZero: Boolean = false): List<String> {
            val extra = if (spaceAfter) "." else ""
            // TODO: fix BURNED_IN for NULL_DIGIT
            val digit: BooleanArray = if (value == 0 && nullForZero) NULL_DIGIT else DIGITS[value]
            val line2 = "${outputSegment(digit[1], condition[1])}..${outputSegment(digit[2], condition[2])}$extra"
            val line5 = "${outputSegment(digit[4], condition[4])}..${outputSegment(digit[5], condition[5])}$extra"
            return listOf(
               ".${outputSegment(digit[0], condition[0], 2)}.$extra",
               line2,
               line2,
               ".${outputSegment(digit[3], condition[3], 2)}.$extra",
               line5,
               line5,
               ".${outputSegment(digit[6], condition[6], 2)}.$extra",
            )
         }

         val ret: List<String> = listOf(
            outputDigit(hour / 10, hourTensCondition, true, nullForZero = true),
            outputDigit(hour % 10, hourOnesCondition, false),
            // insert colon in middle
            List(7) { i -> when {
               (i == 2 && upperColonWorking) || (i == 4 && lowerColonWorking) -> ".X."
               else -> "..."
            } },
            outputDigit(minute / 10, minuteTensCondition, true),
            outputDigit(minute % 10, minuteOnesCondition, false)
         )
            .reduce { acc, digit -> acc.mapIndexed { index, s -> s + digit[index] } }
         return ret
      }

      fun generateDisplayAssessment(
         startOffset: Int = 0,
         observationCount: Int = 1,
         hourTensCondition: List<Condition> = allWorkingCondition,
         hourOnesCondition: List<Condition> = allWorkingCondition,
         minuteTensCondition: List<Condition> = allWorkingCondition,
         minuteOnesCondition: List<Condition> = allWorkingCondition,
         upperColonWorking: Boolean = true,
         lowerColonWorking: Boolean = true,
         ): List<String>
      {
         val hourTensConditionDetectable = BooleanArray(7)
         val hourOnesConditionDetectable = BooleanArray(7)
         val minuteTensConditionDetectable = BooleanArray(7)
         val minuteOnesConditionDetectable = BooleanArray(7)
         val conditions = listOf(hourTensCondition, hourOnesCondition, minuteTensCondition, minuteOnesCondition)
         val detectables = listOf(hourTensConditionDetectable, hourOnesConditionDetectable, minuteTensConditionDetectable, minuteOnesConditionDetectable)

         // TODO: precalculate working Segments
         // determine which digit-segments can be OBSERVED to be working
         val isObservedWorking = Array(4) { BooleanArray(7) }
         for (segment in 0..< 7) {
               val hasOff = BooleanArray(4)
               val hasOn = BooleanArray(4)
               for (minuteOffset in startOffset..< startOffset + observationCount) {
                  val hour = (minuteOffset / 60) % 24
                  val minute = minuteOffset % 60
                  val hourTensDigit = if (hour / 10 == 0) NULL_DIGIT else DIGITS[hour / 10]
                  // check all digits
                  (if (hourTensDigit[segment]) hasOn else hasOff)[0] = true
                  (if (DIGITS[hour % 10][segment]) hasOn else hasOff)[1] = true
                  (if (DIGITS[minute / 10][segment]) hasOn else hasOff)[2] = true
                  (if (DIGITS[minute % 10][segment]) hasOn else hasOff)[3] = true
               }
            for (digit in 0 .. 3) {
               if (conditions[digit][segment] == Condition.WORKING)
                  isObservedWorking[digit][segment] = hasOn[digit] && hasOff[digit]
            }
         }

         // determine start times consistent with observations
         val validStartOffsets = (0 ..< 24*60).filter { searchOffset ->
            for (observationIndex in 0 ..< observationCount) {
               val observationOffset = searchOffset + observationIndex
               val actualOffset = startOffset + observationIndex
               // each digit-segment in each observation must match the actual clock but after
               // turning off BURNED_OUT, and turning on BURNED_IN digit-segments.
               val observationHour = (observationOffset / 60) % 24
               val observationMinute = observationOffset % 60
               val actualHour = (actualOffset / 60) % 24
               val actualMinute = actualOffset % 60
               for (digit in 0 .. 3) {
                  val actualSegments: BooleanArray
                  val observationSegments: BooleanArray
                  when (digit) {
                     0 -> {
                        observationSegments = if (observationHour / 10 == 0) NULL_DIGIT else DIGITS[observationHour / 10]
                        actualSegments = if (actualHour / 10 == 0) NULL_DIGIT else DIGITS[actualHour / 10]
                     }
                     1 -> {
                        observationSegments = DIGITS[observationHour % 10]
                        actualSegments = DIGITS[actualHour % 10]
                     }
                     2 -> {
                        observationSegments = DIGITS[observationMinute / 10]
                        actualSegments = DIGITS[actualMinute / 10]
                     }
                     else -> {
                        observationSegments = DIGITS[observationMinute % 10]
                        actualSegments = DIGITS[actualMinute % 10]
                     }
                  }
                  for (segment in 0 ..< 7) {
                     if (isObservedWorking[digit][segment] &&
                        observationSegments[segment] != actualSegments[segment])
                              return@filter false
                  }
               }
            }
            true
         }
         println("Possible start offsets = $validStartOffsets")
         if (validStartOffsets.isEmpty()) {
            error("No valid start offsets")
         }

         // calculate detectability of each digit-segment
         for (segment in 0 ..< 7) {
            for (digit in 0 .. 3) {
               // If the condition is WORKING, we can detect its condition if the digit-segment is both on
               // and off in observations. If the condition is BURNED*, we can only detect its condition
               // if, at all start times consistent with the observations, there is at least one deviation
               // of the digit-segment from a perfect clock within the observations.
               detectables[digit][segment] = when (conditions[digit][segment]) {
                  Condition.WORKING -> isObservedWorking[digit][segment]
                  Condition.BURNED_OUT, Condition.BURNED_IN -> {
                     val isObservedOn = conditions[digit][segment] == Condition.BURNED_IN
                     validStartOffsets.all { possibleStartOffset ->
                        (possibleStartOffset ..< possibleStartOffset + observationCount).any { observationOffset ->
                           val hour = (observationOffset / 60) % 24
                           val minute = observationOffset % 60
                           val actualDigit = when (digit) {
                              0 -> if (hour / 10 == 0) NULL_DIGIT else DIGITS[hour / 10]
                              1 -> DIGITS[hour % 10]
                              2 -> DIGITS[minute / 10]
                              else -> DIGITS[minute % 10]
                           }
                           isObservedOn != actualDigit[segment]
                        }
                     }
                  }
                  Condition.UNKNOWN -> error("")
               }
            }
         }

         fun outputCondition(condition: Condition, width: Int = 1): String =
            condition.output + if (width > 1) condition.output.toString() else ""
         fun outputDigit(condition: List<Condition>, detectables: BooleanArray, spaceAfter: Boolean): List<String> {
            val extra = if (spaceAfter) "." else ""
            val condition = condition.mapIndexed { i, condition -> condition.withDetectable(detectables[i]) }
            val line2 = "${outputCondition(condition[1])}..${outputCondition(condition[2])}$extra"
            val line5 = "${outputCondition(condition[4])}..${outputCondition(condition[5])}$extra"
            return listOf(
               ".${outputCondition(condition[0], 2)}.$extra",
               line2,
               line2,
               ".${outputCondition(condition[3], 2)}.$extra",
               line5,
               line5,
               ".${outputCondition(condition[6], 2)}.$extra",
            )
         }

         return listOf(
            outputDigit(hourTensCondition, hourTensConditionDetectable, true),
            outputDigit(hourOnesCondition, hourOnesConditionDetectable, false),
            // insert colon
            List(7) { i -> when (i) {
               2 -> if (upperColonWorking) ".?." else ".0."
               4 -> if (lowerColonWorking) ".?." else ".0."
               else -> "..."
            } },
            outputDigit(minuteTensCondition, minuteTensConditionDetectable, true),
            outputDigit(minuteOnesCondition, minuteOnesConditionDetectable, false)
         )
            .reduce { acc, digit -> acc.mapIndexed { i, s -> s + digit[i] }}
      }

      fun generateRandomConditionList(): List<Condition> {
         return List(7) { Condition.entries[Random.nextInt(0 .. 2)] }
      }

      val allWorkingCondition = listOf(Condition.WORKING, Condition.WORKING, Condition.WORKING, Condition.WORKING, Condition.WORKING, Condition.WORKING, Condition.WORKING)

      val DIGITS = arrayOf(
         booleanArrayOf(true, true, true, false, true, true, true), // 0
         booleanArrayOf(false, false, true, false, false, true, false), // 1
         booleanArrayOf(true, false, true, true, true, false, true), // 2
         booleanArrayOf(true, false, true, true, false, true, true), // 3
         booleanArrayOf(false, true, true, true, false, true, false), // 4
         booleanArrayOf(true, true, false, true, false, true, true), // 5
         booleanArrayOf(true, true, false, true, true, true, true), // 6
         booleanArrayOf(true, false, true, false, false, true, false), // 7
         booleanArrayOf(true, true, true, true, true, true, true), // 8
         booleanArrayOf(true, true, true, true, false, true, true), // 9
      )
      val NULL_DIGIT = BooleanArray(7)
   }
}

class ClockBreakingTest {
   // First sample given in the problem statement
   @Test fun sampleTest1Programmatic() {
      val assessment = assessTimeDisplay(listOf(
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(Null),
            hourOnesDigitObservation = DigitObservation.fromDigit(Zero),
            minuteTensDigitObservation = DigitObservation.fromDigit(Nine),
            minuteOnesDigitObservation = DigitObservation(true, false, true, true, true, true, true),
            upperColonOn = true,
            lowerColonOn = false
         ),
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(Null),
            hourOnesDigitObservation = DigitObservation.fromDigit(Zero),
            minuteTensDigitObservation = DigitObservation.fromDigit(Nine),
            minuteOnesDigitObservation = DigitObservation(true, false, true, true, true, true, true),
            upperColonOn = true,
            lowerColonOn = false
         ),
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(Null),
            hourOnesDigitObservation =
            DigitObservation(false, false, true, false, false, true, true),
            minuteTensDigitObservation = DigitObservation.fromDigit(Eight),
            minuteOnesDigitObservation =
            DigitObservation(true, false, true, false, true, true, true),
            upperColonOn = true,
            lowerColonOn = false
         ),
      ))
      assertEquals(TimeDisplayAssessment(
         hourTensDigitCondition = DigitCondition(),
         hourOnesDigitCondition = DigitCondition(WORKING, WORKING, UNKNOWN, UNKNOWN, WORKING, UNKNOWN, BURNED_IN),
         minuteTensDigitCondition = DigitCondition(UNKNOWN, UNKNOWN, BURNED_IN, BURNED_IN, WORKING, UNKNOWN, UNKNOWN),
         minuteOnesDigitCondition = DigitCondition(UNKNOWN, BURNED_OUT, UNKNOWN, WORKING, BURNED_IN, UNKNOWN, UNKNOWN),
         upperColonCondition = UNKNOWN,
         lowerColonCondition = BURNED_OUT,
      ), assessment)
   }

   @Test fun sampleTest1IO() {
      testIO("""
         3
         ......XX.....XX...XX.
         .....X..X...X..X....X
         .....X..X.X.X..X....X
         .............XX...XX.
         .....X..X......X.X..X
         .....X..X......X.X..X
         ......XX.....XX...XX.

         ......XX.....XX...XX.
         .....X..X...X..X....X
         .....X..X.X.X..X....X
         .............XX...XX.
         .....X..X......X.X..X
         .....X..X......X.X..X
         ......XX.....XX...XX.

         .............XX...XX.
         ........X...X..X....X
         ........X.X.X..X....X
         .............XX......
         ........X...X..X.X..X
         ........X...X..X.X..X
         ......XX.....XX...XX.
      """,
         """
         .??...WW.....??...??.
         ?..?.W..?...?..1.0..?
         ?..?.W..?.?.?..1.0..?
         .??...??.....11...WW.
         ?..?.W..?.0.W..?.1..?
         ?..?.W..?...W..?.1..?
         .??...11.....??...??.
      """)
   }

   // Second sample given in the problem statement
   @Test fun sampleTest2Programmatic() {
      val assessment = assessTimeDisplay(listOf(
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(One),
            hourOnesDigitObservation = DigitObservation(true, false, true, true, true, false, true),
            minuteTensDigitObservation = DigitObservation.fromDigit(Zero),
            minuteOnesDigitObservation = DigitObservation.fromDigit(Eight),
            upperColonOn = true,
            lowerColonOn = true,
         ),
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(One),
            hourOnesDigitObservation = DigitObservation(true, false, true, true, true, false, true),
            minuteTensDigitObservation = DigitObservation.fromDigit(Zero),
            minuteOnesDigitObservation = DigitObservation.fromDigit(Null),
            upperColonOn = true,
            lowerColonOn = true,
         ),
      ))
      assertNull(assessment)
   }

   @Test fun sampleTest2IO() {
      val sis = StringBufferInputStream("""
         2
         ......XX.....XX...XX.
         ...X....X...X..X.X..X
         ...X....X.X.X..X.X..X
         ......XX..........XX.
         ...X.X....X.X..X.X..X
         ...X.X......X..X.X..X
         ......XX.....XX...XX.

         ......XX.....XX......
         ...X....X...X..X.....
         ...X....X.X.X..X.....
         ......XX.............
         ...X.X....X.X..X.....
         ...X.X......X..X.....
         ......XX.....XX......
      """.trimIndent())
      val sos = StringOutputStream()

      assessTimeDisplayIO(sis, sos)
      assertEquals("impossible", sos.toString())
   }

   @Test fun testRandom() {
      repeat(1000) {
         val minuteOffset = Random.nextInt(24*60)
         val count = Random.nextInt(10 ..< 100)
         val hourTensCondition = Generator.generateRandomConditionList()
         val hourOnesCondition = Generator.generateRandomConditionList()
         val minuteTensCondition = Generator.generateRandomConditionList()
         val minuteOnesCondition = Generator.generateRandomConditionList()
         val observations = List(count) { i ->
            val obsOffset = minuteOffset + i
            val hour = (obsOffset / 60) % 24
            val minute = obsOffset % 60
            Generator.generateDisplayObservation(hour, minute, hourTensCondition, hourOnesCondition, minuteTensCondition, minuteOnesCondition)
         }
            .joinToString(separator = "\n") { obs -> obs.joinToString(separator = "\n", postfix = "\n") }

         val observationsString = "$count\n$observations\n"
         val expectedOutput = Generator.generateDisplayAssessment(minuteOffset, count, hourTensCondition, hourOnesCondition, minuteTensCondition, minuteOnesCondition)
            .joinToString("\n", postfix = "\n")

         // test it
         testIO(observationsString, expectedOutput, "Random display start time %2d:%2d count=$count conditions=[$hourTensCondition, $hourOnesCondition, $minuteTensCondition, $minuteOnesCondition]"
            .format((minuteOffset / 60) % 24, minuteOffset % 60))
      }
   }
   
   @Test fun testAgainstArup() {
      repeat(1000) {
         val minuteOffset = Random.nextInt(24*60)
         val count = Random.nextInt(1 ..< 100)
         val hourTensCondition = Generator.generateRandomConditionList()
         val hourOnesCondition = Generator.generateRandomConditionList()
         val minuteTensCondition = Generator.generateRandomConditionList()
         val minuteOnesCondition = Generator.generateRandomConditionList()
         val observations = List(count) { i ->
            val obsOffset = minuteOffset + i
            val hour = (obsOffset / 60) % 24
            val minute = obsOffset % 60
            Generator.generateDisplayObservation(hour, minute, hourTensCondition, hourOnesCondition, minuteTensCondition, minuteOnesCondition)
         }
            .joinToString(separator = "\n") { obs -> obs.joinToString(separator = "\n", postfix = "\n") }

         val observationsString = "$count\n$observations\n"
         val sis = StringBufferInputStream(observationsString)

         val arupSOS = StringOutputStream()
         PrintStream(arupSOS).use {
            clock.doIt(sis, it)
         }
         val arupAnswer = arupSOS.toString()

         sis.reset()
         val mySOS = StringOutputStream()
         assessTimeDisplayIO(sis, mySOS)
         val myAnswer = mySOS.toString()

         assertEquals(arupAnswer, myAnswer, "INPUT-----\n$observationsString")
      }
   }

   @Test fun testShuffledAgainstArup() {
      repeat(1000) {
         val minuteOffset = Random.nextInt(24*60)
         val count = Random.nextInt(2 ..< 4)
         val hourTensCondition = Generator.generateRandomConditionList()
         val hourOnesCondition = Generator.generateRandomConditionList()
         val minuteTensCondition = Generator.generateRandomConditionList()
         val minuteOnesCondition = Generator.generateRandomConditionList()
         val observations = List(count) { i ->
            val obsOffset = minuteOffset + i
            val hour = (obsOffset / 60) % 24
            val minute = obsOffset % 60
            Generator.generateDisplayObservation(hour, minute, hourTensCondition, hourOnesCondition, minuteTensCondition, minuteOnesCondition)
         }
            .shuffled()
            .joinToString(separator = "\n") { obs -> obs.joinToString(separator = "\n", postfix = "\n") }

         val observationsString = "$count\n$observations\n"
         val sis = StringBufferInputStream(observationsString)

         val arupSOS = StringOutputStream()
         PrintStream(arupSOS).use {
            clock.doIt(sis, it)
         }
         val arupAnswer = arupSOS.toString()

         sis.reset()
         val mySOS = StringOutputStream()
         assessTimeDisplayIO(sis, mySOS)
         val myAnswer = mySOS.toString()

         assertEquals(arupAnswer, myAnswer, "INPUT-----\n$observationsString")
      }
   }

   fun testIO(input: String, output: String, message: String? = null) {
      println("********* INPUT")
      print(input)
      val sis = StringBufferInputStream(input.trimIndent())
      println("********* OUTPUT (expected)")
      println(output)
      val sos = StringOutputStream()

      assessTimeDisplayIO(sis, sos)
      assertEquals(output.trimIndent() + '\n', sos.toString(), message)
   }
}
