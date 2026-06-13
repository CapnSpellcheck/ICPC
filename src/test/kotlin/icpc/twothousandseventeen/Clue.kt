package icpc.twothousandseventeen

import util.StringOutputStream
import java.io.StringBufferInputStream
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

   @Test fun testSample3IO() {
      val input = """
         3
         A C M S D
         B G S - G
         A H S - - S
         C J S *
      """.trimIndent() + "\n"
      val sos = StringOutputStream()
      ClueGameIO(StringBufferInputStream(input), sos)
      assertEquals("???", sos.toString())
   }

   @Test fun testIdentifiedHolderAndReduce() {
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.D, Card.H, Card.R), 0, Card.H),
         Examination(Suggestion(Card.A, Card.K, Card.O), 1),
         Examination(Suggestion(Card.E, Card.K, Card.S), 0),
         Examination(Suggestion(Card.A, Card.G, Card.O), 0, Card.A),
         Examination(Suggestion(Card.A, Card.I, Card.N), 1, Card.I),
         Examination(Suggestion(Card.E, Card.J, Card.M), 0),
         Examination(Suggestion(Card.E, Card.H, Card.M), 0),
         Examination(Suggestion(Card.E, Card.J, Card.O), 1),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(null, Card.L, null), deduction)
   }

   @Test fun testCouldntPresentForSuggestion() {
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.C, Card.G, Card.M), 0, Card.C),
         Examination(Suggestion(Card.D, Card.K, Card.Q), 0),
         Examination(Suggestion(Card.D, Card.J, Card.U), 3),
         Examination(Suggestion(Card.A, Card.G, Card.M), 0, Card.G),
         Examination(Suggestion(Card.B, Card.K, Card.N), 2, Card.K),
         Examination(Suggestion(Card.E, Card.H, Card.M), 1),
         Examination(Suggestion(Card.A, Card.H, Card.O), 1, Card.A),
         Examination(Suggestion(Card.F, Card.L, Card.N), 0, Card.N),
         Examination(Suggestion(Card.A, Card.G, Card.M), 3),
         Examination(Suggestion(Card.A, Card.K, Card.Q), 1),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.F, null, null), deduction)
   }
}