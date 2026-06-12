package icpc.twothousandseventeen

import kotlin.test.Test
import kotlin.test.assertEquals

class ClueTest {
   @Test fun testSample1() {
      val game = ClueGame(arrayOf(Card.B, Card.I, Card.P, Card.C, Card.F))
      val exams = arrayOf(Examination(Suggestion(Card.A, Card.G, Card.M), 3, null))
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.A, Card.G, Card.M), deduction)
   }
}