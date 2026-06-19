package icpc.twothousandseventeen

import util.StringOutputStream
import java.io.StringBufferInputStream
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.random.Random

class ClueTest {
   @Test fun testSample1() {
      val game = ClueGame(arrayOf(Card.B, Card.I, Card.P, Card.C, Card.F))
      val exams = arrayOf(Examination(Suggestion(Card.A, Card.G, Card.M), 3))
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.A, Card.G, Card.M), deduction)
   }

   @Test fun testSample1IO() {
      val input = """
         1
         B I P C F
         A G M - - -
      """.trimIndent() + "\n"
      val sos = StringOutputStream()
      ClueGameIO(StringBufferInputStream(input), sos)
      assertEquals("AGM", sos.toString())
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

   @Test fun testReductionFromSameHandByCouldntPresent() {
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.C, Card.I, Card.R), 0, Card.C),
         Examination(Suggestion(Card.E, Card.H, Card.M), 1),
         Examination(Suggestion(Card.A, Card.H, Card.N), 1, Card.N),
         Examination(Suggestion(Card.B, Card.G, Card.U), 0, Card.B),
         Examination(Suggestion(Card.D, Card.G, Card.M), 1, Card.D),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.F, null, null), deduction)
   }

   @Test fun testWhenCompetitorHandIsDeducedThenAppliesCardsDoesntHave() {
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.E, Card.L, Card.S), 2, Card.S),
         Examination(Suggestion(Card.B, Card.K, Card.N), 1),
         Examination(Suggestion(Card.E, Card.G, Card.M), 0),
         Examination(Suggestion(Card.B, Card.J, Card.P), 0, Card.B),
         Examination(Suggestion(Card.A, Card.G, Card.T), 2, Card.T),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(null, Card.L, null), deduction)
   }

   @Test fun testReductionFromPositiveElimination() {
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.A, Card.L, Card.T), 2, Card.T),
         Examination(Suggestion(Card.D, Card.G, Card.N), 0),
         Examination(Suggestion(Card.F, Card.G, Card.S), 0),
         Examination(Suggestion(Card.A, Card.G, Card.M), 0, Card.G),
         Examination(Suggestion(Card.C, Card.G, Card.N), 0, Card.C),
         Examination(Suggestion(Card.C, Card.K, Card.M), 1),
         Examination(Suggestion(Card.E, Card.G, Card.N), 0),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.F, Card.L, null), deduction)
   }

   @Test fun testPresentedForSuggestionWithCardsDoesntHave() {
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.E, Card.K, Card.S), 2, Card.K),
         Examination(Suggestion(Card.E, Card.I, Card.S), 0),
         Examination(Suggestion(Card.A, Card.G, Card.M), 1, Card.M),
         Examination(Suggestion(Card.D, Card.H, Card.R), 1),
         Examination(Suggestion(Card.B, Card.J, Card.N), 1, Card.J),
         Examination(Suggestion(Card.C, Card.J, Card.O), 0),
         Examination(Suggestion(Card.E, Card.H, Card.M), 0),
         Examination(Suggestion(Card.C, Card.G, Card.Q), 0, Card.G),
         Examination(Suggestion(Card.D, Card.H, Card.R), 0, Card.H),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(null, Card.L, null), deduction)
   }

   @Test fun testDoesCoverRequire() {
      var groups = hashSetOf(
         EnumSet.of(Card.A, Card.B),
      )

      assertFalse(doesCoverRequire(hashSetOf(), 1))
      assertTrue(doesCoverRequire(groups, 1))

      assertFalse(doesCoverRequire(groups, 2))
      groups = hashSetOf(
         EnumSet.of(Card.A, Card.B, Card.C),
         EnumSet.of(Card.D, Card.E),
         )
      assertTrue(doesCoverRequire(groups, 2))
      groups = hashSetOf(
         EnumSet.of(Card.A, Card.B, Card.C),
         EnumSet.of(Card.A, Card.D),
         EnumSet.of(Card.B, Card.D),
      )
      assertTrue(doesCoverRequire(groups, 2))
      assertFalse(doesCoverRequire(groups, 3))
      groups = hashSetOf(
         EnumSet.of(Card.A, Card.B, Card.C),
         EnumSet.of(Card.D, Card.E),
         EnumSet.of(Card.F, Card.G),
      )
      assertTrue(doesCoverRequire(groups, 3))
      groups = hashSetOf(
         EnumSet.of(Card.A, Card.B,),
         EnumSet.of(Card.A, Card.C),
         EnumSet.of(Card.D, Card.E),
         EnumSet.of(Card.D, Card.Q),
         EnumSet.of(Card.F, Card.G),
         EnumSet.of(Card.F, Card.R),
      )
      assertTrue(doesCoverRequire(groups, 3))
      assertFalse(doesCoverRequire(groups, 4))
   }

   @Test fun testDoesCoverExist() {
      assertFalse(doesCoverExist(hashSetOf(EnumSet.of(Card.A)), 0))
      assertTrue(doesCoverExist(hashSetOf(), 0))

      assertTrue(doesCoverExist(hashSetOf(EnumSet.of(Card.A, Card.B), EnumSet.of(Card.B, Card.C)), 1))
      assertTrue(doesCoverExist(hashSetOf(EnumSet.of(Card.A, Card.B), EnumSet.of(Card.B, Card.C)), 2))

      assertFalse(doesCoverExist(hashSetOf(EnumSet.of(Card.A, Card.B), EnumSet.of(Card.D, Card.C)), 1))
      assertTrue(doesCoverExist(hashSetOf(EnumSet.of(Card.A, Card.B), EnumSet.of(Card.D, Card.C)), 2))
      assertTrue(doesCoverExist(hashSetOf(EnumSet.of(Card.A, Card.B), EnumSet.of(Card.D, Card.C)), 3))

      assertFalse(doesCoverExist(hashSetOf(
         EnumSet.of(Card.A, Card.B, Card.C),
         EnumSet.of(Card.D, Card.E),
         EnumSet.of(Card.F, Card.G),
      ), 2))
      assertTrue(doesCoverExist(hashSetOf(
         EnumSet.of(Card.A, Card.B, Card.C),
         EnumSet.of(Card.D, Card.E),
         EnumSet.of(Card.F, Card.G),
      ), 3))
      assertTrue(doesCoverExist(hashSetOf(
         EnumSet.of(Card.A, Card.B,),
         EnumSet.of(Card.A, Card.C),
         EnumSet.of(Card.D, Card.E),
         EnumSet.of(Card.D, Card.Q),
         EnumSet.of(Card.F, Card.G),
         EnumSet.of(Card.F, Card.R),
      ), 3))
   }

   @Test fun testIsolatedPigeonhole1() {
      // numberOfSlots = 1
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.D, Card.I, Card.S), 1, Card.I),
         Examination(Suggestion(Card.D, Card.H, Card.T), 0),
         Examination(Suggestion(Card.E, Card.G, Card.M), 0),
         Examination(Suggestion(Card.C, Card.I, Card.R), 1),
         Examination(Suggestion(Card.C, Card.G, Card.U), 0, Card.C),
         Examination(Suggestion(Card.D, Card.G, Card.O), 0),
         Examination(Suggestion(Card.E, Card.K, Card.Q), 0),
         Examination(Suggestion(Card.E, Card.G, Card.R), 0, Card.G),
         Examination(Suggestion(Card.B, Card.L, Card.R), 1, Card.R),
         Examination(Suggestion(Card.E, Card.J, Card.N), 0),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.F, null, null), deduction)
   }

   @Test fun testIsolatedPigeonhole2() {
      // numberOfSlots = 3
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.C, Card.J, Card.U), 0, Card.C),
         Examination(Suggestion(Card.D, Card.K, Card.P), 0,),
         Examination(Suggestion(Card.E, Card.G, Card.M), 0,),
         Examination(Suggestion(Card.A, Card.G, Card.N), 0, Card.A),
         Examination(Suggestion(Card.E, Card.I, Card.U), 1, Card.I),
         Examination(Suggestion(Card.A, Card.J, Card.T), 0,),
         Examination(Suggestion(Card.B, Card.G, Card.P), 1, Card.B),
         Examination(Suggestion(Card.E, Card.L, Card.R), 2,),
         Examination(Suggestion(Card.B, Card.G, Card.N), 3),
         Examination(Suggestion(Card.C, Card.J, Card.O), 0,),
         Examination(Suggestion(Card.F, Card.K, Card.Q), 0,),
         Examination(Suggestion(Card.F, Card.L, Card.N), 0, Card.N),
         Examination(Suggestion(Card.B, Card.L, Card.R), 1, Card.R),
         Examination(Suggestion(Card.D, Card.H, Card.S), 0,),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.F, null, null), deduction)
   }

   @Test fun testNegativeIsolatedPigeonhole() {
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.A, Card.L, Card.S), 2, Card.S),
         Examination(Suggestion(Card.B, Card.G, Card.T), 1,),
         Examination(Suggestion(Card.B, Card.K, Card.U), 0,),
         Examination(Suggestion(Card.A, Card.H, Card.O), 0, Card.A),
         Examination(Suggestion(Card.B, Card.H, Card.N), 0, Card.H),
         Examination(Suggestion(Card.E, Card.L, Card.U), 1,),
         Examination(Suggestion(Card.E, Card.K, Card.P), 0,),
         Examination(Suggestion(Card.F, Card.L, Card.M), 0, Card.M),
         Examination(Suggestion(Card.C, Card.H, Card.O), 0, Card.O),
         Examination(Suggestion(Card.D, Card.I, Card.R), 0,),
         Examination(Suggestion(Card.E, Card.I, Card.U), 0,),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(null, Card.L, null), deduction)
   }

   @Test fun testTwoWayPigeonhole() {
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.E, Card.G, Card.M), 2, Card.E),
         Examination(Suggestion(Card.C, Card.I, Card.N), 0,),
         Examination(Suggestion(Card.A, Card.G, Card.M), 1,Card.G),
         Examination(Suggestion(Card.C, Card.I, Card.M), 1,),
         Examination(Suggestion(Card.D, Card.L, Card.N), 1, Card.D),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.F, null, null), deduction)
   }

   @Test fun testAllPlayerTypedPigeonholing() {
      val game = ClueGame(arrayOf(Card.A, Card.B, Card.G, Card.M, Card.N))
      val exams = arrayOf(
         Examination(Suggestion(Card.C, Card.G, Card.N), 0, Card.C),
         Examination(Suggestion(Card.A, Card.I, Card.M), 0),
         Examination(Suggestion(Card.B, Card.I, Card.S), 0),
         Examination(Suggestion(Card.F, Card.L, Card.M), 0, Card.M),
         Examination(Suggestion(Card.D, Card.J, Card.U), 1, Card.J),
         Examination(Suggestion(Card.A, Card.L, Card.R), 0),
         Examination(Suggestion(Card.E, Card.H, Card.U), 0),
         Examination(Suggestion(Card.B, Card.I, Card.P), 0, Card.B),
         Examination(Suggestion(Card.E, Card.I, Card.M), 1, Card.I),
         Examination(Suggestion(Card.D, Card.K, Card.O), 0),
         Examination(Suggestion(Card.D, Card.K, Card.N), 0),
         Examination(Suggestion(Card.A, Card.L, Card.S), 0, Card.A),
         Examination(Suggestion(Card.F, Card.I, Card.M), 1, Card.I),
         Examination(Suggestion(Card.C, Card.G, Card.T), 1,),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(Card.F, null, Card.U), deduction)
   }

   @Test fun testRuntimeException() {
      fun match(s: Suggestion, cards: List<Card>): Card? {
         val matches = mutableListOf<Card>()
         for (card in listOf(s.first, s.second, s.third)) {
            if (cards.contains(card))
               matches.add(card)
         }
         return matches.randomOrNull()
      }
      while (true) {
         // remove a card of each type
         val heldPersons = Card.cardsOfType(Card.Type.PERSON).toMutableSet()
         heldPersons.remove(heldPersons.random())
         val heldWeapons = Card.cardsOfType(Card.Type.WEAPON).toMutableSet()
         heldWeapons.remove(heldWeapons.random())
         val heldRooms = Card.cardsOfType(Card.Type.ROOM).toMutableSet()
         heldRooms.remove(heldRooms.random())

         val cardSet = mutableSetOf<Card>()
         cardSet.addAll(heldPersons)
         cardSet.addAll(heldWeapons)
         cardSet.addAll(heldRooms)

         val player1 = (1 .. 5).map { val card = cardSet.random(); cardSet.remove(card); card }
         val player2 = (1 .. 5).map { val card = cardSet.random(); cardSet.remove(card); card }
         val player3 = (1 .. 4).map { val card = cardSet.random(); cardSet.remove(card); card }
         val player4 = (1 .. 4).map { val card = cardSet.random(); cardSet.remove(card); card }
         val allPlayers = listOf(player1, player2, player3, player4)

         val examinations = mutableListOf<Examination>()
         var currentPlayer = 0 // 0 based
         // create between 1 and 50 suggestions
         repeat(Random.nextInt(1, 51)) {
            val person = Card.cardsOfType(Card.Type.PERSON).random()
            val weapon = Card.cardsOfType(Card.Type.WEAPON).random()
            val room = Card.cardsOfType(Card.Type.ROOM).random()
            val suggestion = Suggestion(person, weapon, room)
            var passes: Short = 0
            var responder = (currentPlayer + 1) % 4
            var answer = match(suggestion, allPlayers[responder])
            if (answer == null) {
               passes = 1
               responder = (responder + 1) % 4
               answer = match(suggestion, allPlayers[responder])
               if (answer == null) {
                  passes = 2
                  responder = (responder + 1) % 4
                  answer = match(suggestion, allPlayers[responder])
                  if (answer == null)
                     passes = 3
               }
            }
            examinations.add(Examination(suggestion, passes, if (currentPlayer == 0 || responder == 0) answer else null))
            currentPlayer = (currentPlayer + 1) % 4
         }

         // test it
         val game = ClueGame(player1.toTypedArray())
         game.deduce(examinations.toTypedArray())

      }
   }
}