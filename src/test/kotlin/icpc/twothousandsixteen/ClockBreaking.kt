package icpc.twothousandsixteen

import lcd7.Digit
import lcd7.DigitCondition
import lcd7.DigitObservation
import lcd7.SegmentCondition.*
import lcd7.TimeDisplayObservation
import org.junit.jupiter.api.Test
import util.StringOutputStream
import java.io.PrintStream
import java.io.StringBufferInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Two test cases given in the problem statement.
class ClockBreakingTest {
   @Test fun sampleTest1Programmatic() {
      val assessment = assessTimeDisplay(listOf(
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(Digit.Null),
            hourOnesDigitObservation = DigitObservation.fromDigit(Digit.Zero),
            minuteTensDigitObservation = DigitObservation.fromDigit(Digit.Nine),
            minuteOnesDigitObservation = DigitObservation(true, false, true, true, true, true, true),
            upperColonOn = true,
            lowerColonOn = false
         ),
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(Digit.Null),
            hourOnesDigitObservation = DigitObservation.fromDigit(Digit.Zero),
            minuteTensDigitObservation = DigitObservation.fromDigit(Digit.Nine),
            minuteOnesDigitObservation = DigitObservation(true, false, true, true, true, true, true),
            upperColonOn = true,
            lowerColonOn = false
         ),
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(Digit.Null),
            hourOnesDigitObservation =
            DigitObservation(false, false, true, false, false, true, true),
            minuteTensDigitObservation = DigitObservation.fromDigit(Digit.Eight),
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
      val sis = StringBufferInputStream("""
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
      """.trimIndent())
      val sos = StringOutputStream()

      assessTimeDisplayIO(sis, sos)
      assertEquals("""
         .??...WW.....??...??.
         ?..?.W..?...?..1.0..?
         ?..?.W..?.?.?..1.0..?
         .??...??.....11...WW.
         ?..?.W..?.0.W..?.1..?
         ?..?.W..?...W..?.1..?
         .??...11.....??...??.
      """.trimIndent() + '\n', sos.toString())
   }

   @Test fun sampleTest2Programmatic() {
      val assessment = assessTimeDisplay(listOf(
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(Digit.One),
            hourOnesDigitObservation = DigitObservation(true, false, true, true, true, false, true),
            minuteTensDigitObservation = DigitObservation.fromDigit(Digit.Zero),
            minuteOnesDigitObservation = DigitObservation.fromDigit(Digit.Eight),
            upperColonOn = true,
            lowerColonOn = true,
         ),
         TimeDisplayObservation(
            hourTensDigitObservation = DigitObservation.fromDigit(Digit.One),
            hourOnesDigitObservation = DigitObservation(true, false, true, true, true, false, true),
            minuteTensDigitObservation = DigitObservation.fromDigit(Digit.Zero),
            minuteOnesDigitObservation = DigitObservation.fromDigit(Digit.Null),
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
}

