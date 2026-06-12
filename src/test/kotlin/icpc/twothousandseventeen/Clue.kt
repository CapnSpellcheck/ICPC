package icpc.twothousandseventeen

import kotlin.test.Test
import kotlin.test.assertEquals

class ClueTest {
   @Test fun testSample1() {
      val game = ClueGame(arrayOf(Card.B, Card.I, Card.P, Card.C, Card.F))
      val exams = arrayOf(Examination(Suggestion(Card.A, Card.G, Card.M), 3))
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.A, Card.G, Card.M), deduction)
   }

   @Test fun testSample2() {
      val game = ClueGame(arrayOf(Card.B, Card.D, Card.A, Card.C, Card.H))
      val exams = arrayOf(
         Examination(Suggestion(Card.F, Card.G, Card.M), 0, Card.M),
         Examination(Suggestion(Card.F, Card.H, Card.M), 1),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.E, null, null), deduction)
   }

   @Test fun testSample3() {
      val game = ClueGame(arrayOf(Card.A, Card.C, Card.D, Card.S, Card.M))
      val exams = arrayOf(
         Examination(Suggestion(Card.B, Card.G, Card.S), 1, Card.G),
         Examination(Suggestion(Card.H, Card.S, Card.A), 2, Card.S),
         Examination(Suggestion(Card.J, Card.S, Card.C), 0)
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(null, null, null), deduction)
   }
}