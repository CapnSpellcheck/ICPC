package icpc.twothousandsixteen

import lcd7.Digit
import lcd7.DigitCondition
import lcd7.DigitObservation
import lcd7.SegmentCondition.*
import lcd7.TimeDisplayObservation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Two test cases given in the problem statement.
class ClockBreakingTest {
   @Test fun sampleTest1() {
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

   @Test fun sampleTest2() {
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
}

