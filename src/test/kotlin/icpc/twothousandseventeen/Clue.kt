package icpc.twothousandseventeen

import icpc.twothousandseventeen.Card.*
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
      val game = ClueGame(arrayOf(B, I, P, C, F))
      val exams = arrayOf(Examination(Suggestion(A, G, M), 3))
      val deduction = game.deduce(exams)
      assertEquals(Deduction(A, G, M), deduction)
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
      val game = ClueGame(arrayOf(B, D, A, C, H))
      val exams = arrayOf(
         Examination(Suggestion(F, G, M), 0, M),
         Examination(Suggestion(F, H, M), 1),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(E, null, null), deduction)
   }

   @Test fun testSample3() {
      val game = ClueGame(arrayOf(A, C, D, S, M))
      val exams = arrayOf(
         Examination(Suggestion(B, G, S), 1, G),
         Examination(Suggestion(H, S, A), 2, S),
         Examination(Suggestion(J, S, C), 0)
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
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(D, H, R), 0, H),
         Examination(Suggestion(A, K, O), 1),
         Examination(Suggestion(E, K, S), 0),
         Examination(Suggestion(A, G, O), 0, A),
         Examination(Suggestion(A, I, N), 1, I),
         Examination(Suggestion(E, J, M), 0),
         Examination(Suggestion(E, H, M), 0),
         Examination(Suggestion(E, J, O), 1),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(null, L, null), deduction)
   }

   @Test fun testCouldntPresentForSuggestion() {
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(C, G, M), 0, C),
         Examination(Suggestion(D, K, Q), 0),
         Examination(Suggestion(D, J, U), 3),
         Examination(Suggestion(A, G, M), 0, G),
         Examination(Suggestion(B, K, N), 2, K),
         Examination(Suggestion(E, H, M), 1),
         Examination(Suggestion(A, H, O), 1, A),
         Examination(Suggestion(F, L, N), 0, N),
         Examination(Suggestion(A, G, M), 3),
         Examination(Suggestion(A, K, Q), 1),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(F, null, null), deduction)
   }

   @Test fun testReductionFromSameHandByCouldntPresent() {
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(C, I, R), 0, C),
         Examination(Suggestion(E, H, M), 1),
         Examination(Suggestion(A, H, N), 1, N),
         Examination(Suggestion(B, G, U), 0, B),
         Examination(Suggestion(D, G, M), 1, D),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(F, null, null), deduction)
   }

   @Test fun testWhenCompetitorHandIsDeducedThenAppliesCardsDoesntHave() {
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(E, L, S), 2, S),
         Examination(Suggestion(B, K, N), 1),
         Examination(Suggestion(E, G, M), 0),
         Examination(Suggestion(B, J, P), 0, B),
         Examination(Suggestion(A, G, T), 2, T),
      )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(null, L, null), deduction)
   }

   @Test fun testReductionFromPositiveElimination() {
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(A, L, T), 2, T),
         Examination(Suggestion(D, G, N), 0),
         Examination(Suggestion(F, G, S), 0),
         Examination(Suggestion(A, G, M), 0, G),
         Examination(Suggestion(C, G, N), 0, C),
         Examination(Suggestion(C, K, M), 1),
         Examination(Suggestion(E, G, N), 0),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(F, L, null), deduction)
   }

   @Test fun testPresentedForSuggestionWithCardsDoesntHave() {
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(E, K, S), 2, K),
         Examination(Suggestion(E, I, S), 0),
         Examination(Suggestion(A, G, M), 1, M),
         Examination(Suggestion(D, H, R), 1),
         Examination(Suggestion(B, J, N), 1, J),
         Examination(Suggestion(C, J, O), 0),
         Examination(Suggestion(E, H, M), 0),
         Examination(Suggestion(C, G, Q), 0, G),
         Examination(Suggestion(D, H, R), 0, H),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(null, L, null), deduction)
   }

   @Test fun testDoesCoverRequire() {
      var groups = hashSetOf(
         EnumSet.of(A, B),
      )

      assertFalse(doesCoverRequire(hashSetOf(), 1))
      assertTrue(doesCoverRequire(groups, 1))

      assertFalse(doesCoverRequire(groups, 2))
      groups = hashSetOf(
         EnumSet.of(A, B, C),
         EnumSet.of(D, E),
         )
      assertTrue(doesCoverRequire(groups, 2))
      groups = hashSetOf(
         EnumSet.of(A, B, C),
         EnumSet.of(A, D),
         EnumSet.of(B, D),
      )
      assertTrue(doesCoverRequire(groups, 2))
      assertFalse(doesCoverRequire(groups, 3))
      groups = hashSetOf(
         EnumSet.of(A, B, C),
         EnumSet.of(D, E),
         EnumSet.of(F, G),
      )
      assertTrue(doesCoverRequire(groups, 3))
      groups = hashSetOf(
         EnumSet.of(A, B,),
         EnumSet.of(A, C),
         EnumSet.of(D, E),
         EnumSet.of(D, Q),
         EnumSet.of(F, G),
         EnumSet.of(F, R),
      )
      assertTrue(doesCoverRequire(groups, 3))
      assertFalse(doesCoverRequire(groups, 4))
   }

   @Test fun testDoesCoverExist() {
      assertFalse(doesCoverExist(hashSetOf(EnumSet.of(A)), 0))
      assertTrue(doesCoverExist(hashSetOf(), 0))

      assertTrue(doesCoverExist(hashSetOf(EnumSet.of(A, B), EnumSet.of(B, C)), 1))
      assertTrue(doesCoverExist(hashSetOf(EnumSet.of(A, B), EnumSet.of(B, C)), 2))

      assertFalse(doesCoverExist(hashSetOf(EnumSet.of(A, B), EnumSet.of(D, C)), 1))
      assertTrue(doesCoverExist(hashSetOf(EnumSet.of(A, B), EnumSet.of(D, C)), 2))
      assertTrue(doesCoverExist(hashSetOf(EnumSet.of(A, B), EnumSet.of(D, C)), 3))

      assertFalse(doesCoverExist(hashSetOf(
         EnumSet.of(A, B, C),
         EnumSet.of(D, E),
         EnumSet.of(F, G),
      ), 2))
      assertTrue(doesCoverExist(hashSetOf(
         EnumSet.of(A, B, C),
         EnumSet.of(D, E),
         EnumSet.of(F, G),
      ), 3))
      assertTrue(doesCoverExist(hashSetOf(
         EnumSet.of(A, B,),
         EnumSet.of(A, C),
         EnumSet.of(D, E),
         EnumSet.of(D, Q),
         EnumSet.of(F, G),
         EnumSet.of(F, R),
      ), 3))
   }

   @Test fun testIsolatedPigeonhole1() {
      // numberOfSlots = 1
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(D, I, S), 1, I),
         Examination(Suggestion(D, H, T), 0),
         Examination(Suggestion(E, G, M), 0),
         Examination(Suggestion(C, I, R), 1),
         Examination(Suggestion(C, G, U), 0, C),
         Examination(Suggestion(D, G, O), 0),
         Examination(Suggestion(E, K, Q), 0),
         Examination(Suggestion(E, G, R), 0, G),
         Examination(Suggestion(B, L, R), 1, R),
         Examination(Suggestion(E, J, N), 0),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(F, null, null), deduction)
   }

   @Test fun testIsolatedPigeonhole2() {
      // numberOfSlots = 3
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(C, J, U), 0, C),
         Examination(Suggestion(D, K, P), 0,),
         Examination(Suggestion(E, G, M), 0,),
         Examination(Suggestion(A, G, N), 0, A),
         Examination(Suggestion(E, I, U), 1, I),
         Examination(Suggestion(A, J, T), 0,),
         Examination(Suggestion(B, G, P), 1, B),
         Examination(Suggestion(E, L, R), 2,),
         Examination(Suggestion(B, G, N), 3),
         Examination(Suggestion(C, J, O), 0,),
         Examination(Suggestion(F, K, Q), 0,),
         Examination(Suggestion(F, L, N), 0, N),
         Examination(Suggestion(B, L, R), 1, R),
         Examination(Suggestion(D, H, S), 0,),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(F, null, null), deduction)
   }

   @Test fun testNegativeIsolatedPigeonhole1() {
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(A, L, S), 2, S),
         Examination(Suggestion(B, G, T), 1,),
         Examination(Suggestion(B, K, U), 0,),
         Examination(Suggestion(A, H, O), 0, A),
         Examination(Suggestion(B, H, N), 0, H),
         Examination(Suggestion(E, L, U), 1,),
         Examination(Suggestion(E, K, P), 0,),
         Examination(Suggestion(F, L, M), 0, M),
         Examination(Suggestion(C, H, O), 0, O),
         Examination(Suggestion(D, I, R), 0,),
         Examination(Suggestion(E, I, U), 0,),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(null, L, null), deduction)
   }

   @Test fun testNegativeIsolatedPigeonhole2() {
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(D, L, O), 0, O),
         Examination(Suggestion(A, G, R), 0,),
         Examination(Suggestion(B, J, Q), 1, B),
         Examination(Suggestion(C, K, R), 1,),
         Examination(Suggestion(B, G, N), 3,),
         Examination(Suggestion(D, K, M), 0,),
         Examination(Suggestion(D, I, N), 1, N),
         Examination(Suggestion(F, J, M), 0, M),
         Examination(Suggestion(E, L, P), 0, P),
         Examination(Suggestion(F, L, R), 0,),
         Examination(Suggestion(F, L, M), 1, M),
         Examination(Suggestion(C, G, O), 0, G),
         Examination(Suggestion(A, G, U), 3,),
         Examination(Suggestion(C, L, M), 2, M),
         Examination(Suggestion(A, H, R), 1, A),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(F, null, U), deduction)
   }

   @Test fun testTwoWayPigeonhole() {
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(E, G, M), 2, E),
         Examination(Suggestion(C, I, N), 0,),
         Examination(Suggestion(A, G, M), 1, G),
         Examination(Suggestion(C, I, M), 1,),
         Examination(Suggestion(D, L, N), 1, D),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(F, null, null), deduction)
   }

   @Test fun testAllPlayerTypedPigeonholing() {
      val game = ClueGame(arrayOf(A, B, G, M, N))
      val exams = arrayOf(
         Examination(Suggestion(C, G, N), 0, C),
         Examination(Suggestion(A, I, M), 0),
         Examination(Suggestion(B, I, S), 0),
         Examination(Suggestion(F, L, M), 0, M),
         Examination(Suggestion(D, J, U), 1, J),
         Examination(Suggestion(A, L, R), 0),
         Examination(Suggestion(E, H, U), 0),
         Examination(Suggestion(B, I, P), 0, B),
         Examination(Suggestion(E, I, M), 1, I),
         Examination(Suggestion(D, K, O), 0),
         Examination(Suggestion(D, K, N), 0),
         Examination(Suggestion(A, L, S), 0, A),
         Examination(Suggestion(F, I, M), 1, I),
         Examination(Suggestion(C, G, T), 1,),
         )
      val deduction = game.deduce(exams)
      assertEquals(Deduction(F, null, U), deduction)
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
      return // test disabled
      while (true) {
         // remove a card of each type
         val heldPersons = Card.cardsOfType(Type.PERSON).toMutableSet()
         heldPersons.remove(heldPersons.random())
         val heldWeapons = Card.cardsOfType(Type.WEAPON).toMutableSet()
         heldWeapons.remove(heldWeapons.random())
         val heldRooms = Card.cardsOfType(Type.ROOM).toMutableSet()
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
            val person = Card.cardsOfType(Type.PERSON).random()
            val weapon = Card.cardsOfType(Type.WEAPON).random()
            val room = Card.cardsOfType(Type.ROOM).random()
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

         println("****** SETUP: player1=$player1\nexams=$examinations")

         // test it
         val game = ClueGame(player1.toTypedArray())
         game.deduce(examinations.toTypedArray())
         println("****** DONE")
      }
   }

   @Test fun testNeverHalts1() {
      val examinations = arrayOf(
         Examination(Suggestion(E, I, T), numberOfPasses=0, presentedCard=E), Examination(Suggestion(B, I, S), numberOfPasses=1, presentedCard=null), Examination(Suggestion(F, K, P), numberOfPasses=1, presentedCard=K), Examination(Suggestion(D, I, U), numberOfPasses=2, presentedCard=null), Examination(Suggestion(D, K, R), numberOfPasses=3, presentedCard=null), Examination(Suggestion(D, G, O), numberOfPasses=2, presentedCard=O), Examination(Suggestion(C, L, R), numberOfPasses=1, presentedCard=R), Examination(Suggestion(B, I, P), numberOfPasses=1, presentedCard=null), Examination(Suggestion(E, I, Q), numberOfPasses=0, presentedCard=E), Examination(Suggestion(E, L, M), numberOfPasses=0, presentedCard=null), Examination(Suggestion(A, K, N), numberOfPasses=0, presentedCard=null), Examination(Suggestion(C, K, P), numberOfPasses=0, presentedCard=K), Examination(Suggestion(C, H, R), numberOfPasses=1, presentedCard=C), Examination(Suggestion(F, H, N), numberOfPasses=1, presentedCard=null), Examination(Suggestion(D, I, N), numberOfPasses=0, presentedCard=null), Examination(Suggestion(B, L, R), numberOfPasses=0, presentedCard=R), Examination(Suggestion(F, I, S), numberOfPasses=0, presentedCard=S), Examination(Suggestion(B, K, N), numberOfPasses=1, presentedCard=null), Examination(Suggestion(A, G, S), numberOfPasses=2, presentedCard=null), Examination(Suggestion(D, H, N), numberOfPasses=3, presentedCard=null), Examination(Suggestion(F, H, P), numberOfPasses=0, presentedCard=P), Examination(Suggestion(B, I, T), numberOfPasses=1, presentedCard=null), Examination(Suggestion(B, G, T), numberOfPasses=0, presentedCard=null), Examination(Suggestion(E, I, N), numberOfPasses=1, presentedCard=null), Examination(Suggestion(C, K, S), numberOfPasses=0, presentedCard=S), Examination(Suggestion(E, I, N), numberOfPasses=1, presentedCard=null), Examination(Suggestion(C, K, R), numberOfPasses=1, presentedCard=R), Examination(Suggestion(D, K, M), numberOfPasses=0, presentedCard=K), Examination(Suggestion(B, H, U), numberOfPasses=0, presentedCard=B), Examination(Suggestion(C, K, T), numberOfPasses=0, presentedCard=null), Examination(Suggestion(D, J, U), numberOfPasses=1, presentedCard=J), Examination(Suggestion(C, G, P), numberOfPasses=1, presentedCard=null), Examination(Suggestion(A, L, U), numberOfPasses=0, presentedCard=L)
      )
      val game = ClueGame(arrayOf(J, K, R, F, O))
      game.deduce(examinations)
      val deduction = game.deduce(examinations)
   }

   @Test fun testNeverHalts2() {
      val examinations = arrayOf(
         Examination(Suggestion(F, I, S), numberOfPasses=1, presentedCard=F), Examination(Suggestion(D, L, Q), numberOfPasses=0, presentedCard=null), Examination(Suggestion(A, J, R), numberOfPasses=0, presentedCard=null), Examination(Suggestion(D, G, S), numberOfPasses=1, presentedCard=null), Examination(Suggestion(E, K, P), numberOfPasses=1, presentedCard=P), Examination(Suggestion(E, L, P), numberOfPasses=0, presentedCard=null), Examination(Suggestion(F, I, N), numberOfPasses=1, presentedCard=I), Examination(Suggestion(A, I, N), numberOfPasses=0, presentedCard=I), Examination(Suggestion(C, L, O), numberOfPasses=0, presentedCard=O), Examination(Suggestion(E, H, M), numberOfPasses=1, presentedCard=null), Examination(Suggestion(E, H, O), numberOfPasses=0, presentedCard=null), Examination(Suggestion(E, G, P), numberOfPasses=0, presentedCard=E), Examination(Suggestion(B, I, S), numberOfPasses=3, presentedCard=null), Examination(Suggestion(F, I, O), numberOfPasses=0, presentedCard=null), Examination(Suggestion(F, J, N), numberOfPasses=3, presentedCard=null), Examination(Suggestion(A, K, P), numberOfPasses=0, presentedCard=K), Examination(Suggestion(B, I, T), numberOfPasses=2, presentedCard=T), Examination(Suggestion(B, J, M), numberOfPasses=2, presentedCard=B), Examination(Suggestion(B, H, N), numberOfPasses=0, presentedCard=null), Examination(Suggestion(F, H, T), numberOfPasses=2, presentedCard=null), Examination(Suggestion(D, H, M), numberOfPasses=0, presentedCard=M), Examination(Suggestion(A, L, T), numberOfPasses=0, presentedCard=null), Examination(Suggestion(C, L, O), numberOfPasses=0, presentedCard=null), Examination(Suggestion(A, L, P), numberOfPasses=1, presentedCard=null), Examination(Suggestion(A, G, N), numberOfPasses=0, presentedCard=G), Examination(Suggestion(F, H, T), numberOfPasses=0, presentedCard=null), Examination(Suggestion(D, J, S), numberOfPasses=3, presentedCard=null), Examination(Suggestion(B, J, N), numberOfPasses=0, presentedCard=B), Examination(Suggestion(D, J, P), numberOfPasses=1, presentedCard=P), Examination(Suggestion(C, G, P), numberOfPasses=0, presentedCard=null), Examination(Suggestion(D, J, O), numberOfPasses=2, presentedCard=null), Examination(Suggestion(E, I, N), numberOfPasses=0, presentedCard=I), Examination(Suggestion(B, G, P), numberOfPasses=0, presentedCard=G), Examination(Suggestion(F, G, M), numberOfPasses=0, presentedCard=null), Examination(Suggestion(F, I, S), numberOfPasses=1, presentedCard=I), Examination(Suggestion(D, L, M), numberOfPasses=1, presentedCard=null), Examination(Suggestion(C, H, M), numberOfPasses=0, presentedCard=M), Examination(Suggestion(C, H, T), numberOfPasses=1, presentedCard=null), Examination(Suggestion(A, K, R), numberOfPasses=0, presentedCard=null), Examination(Suggestion(A, K, S), numberOfPasses=0, presentedCard=K), Examination(Suggestion(F, I, R), numberOfPasses=1, presentedCard=F), Examination(Suggestion(A, G, N), numberOfPasses=0, presentedCard=null), Examination(Suggestion(B, I, R), numberOfPasses=0, presentedCard=null), Examination(Suggestion(F, J, T), numberOfPasses=2, presentedCard=null), Examination(Suggestion(C, J, M), numberOfPasses=0, presentedCard=M), Examination(Suggestion(E, G, O), numberOfPasses=2, presentedCard=E)
      )
      val game = ClueGame(arrayOf(K, U, I, E, B))
      val deduction = game.deduce(examinations)
   }

   @Test fun testNeverHalts3() {
      val examinations = arrayOf(
         Examination(Suggestion(E, H, U), numberOfPasses=1, presentedCard=E), Examination(Suggestion(D, G, R), numberOfPasses=2, presentedCard=G), Examination(Suggestion(B, L, U), numberOfPasses=1, presentedCard=U), Examination(Suggestion(F, H, U), numberOfPasses=0, presentedCard=U), Examination(Suggestion(B, L, Q), numberOfPasses=0, presentedCard=Q), Examination(Suggestion(A, K, U), numberOfPasses=1, presentedCard=null), Examination(Suggestion(B, H, U), numberOfPasses=0, presentedCard=null), Examination(Suggestion(F, G, M), numberOfPasses=0, presentedCard=G), Examination(Suggestion(E, I, U), numberOfPasses=1, presentedCard=E), Examination(Suggestion(B, J, T), numberOfPasses=2, presentedCard=B), Examination(Suggestion(E, L, Q), numberOfPasses=2, presentedCard=null), Examination(Suggestion(F, K, N), numberOfPasses=2, presentedCard=null), Examination(Suggestion(B, J, P), numberOfPasses=0, presentedCard=P), Examination(Suggestion(D, J, Q), numberOfPasses=3, presentedCard=null), Examination(Suggestion(C, K, Q), numberOfPasses=0, presentedCard=null), Examination(Suggestion(E, H, T), numberOfPasses=1, presentedCard=null), Examination(Suggestion(A, L, T), numberOfPasses=0, presentedCard=T), Examination(Suggestion(F, L, Q), numberOfPasses=0, presentedCard=null), Examination(Suggestion(A, H, N), numberOfPasses=0, presentedCard=null), Examination(Suggestion(E, G, P), numberOfPasses=0, presentedCard=G), Examination(Suggestion(F, L, P), numberOfPasses=0, presentedCard=P), Examination(Suggestion(A, J, O), numberOfPasses=1, presentedCard=null), Examination(Suggestion(C, L, S), numberOfPasses=1, presentedCard=C), Examination(Suggestion(A, J, M), numberOfPasses=1, presentedCard=null), Examination(Suggestion(E, K, N), numberOfPasses=1, presentedCard=N), Examination(Suggestion(C, H, P), numberOfPasses=1, presentedCard=null), Examination(Suggestion(E, G, Q), numberOfPasses=1, presentedCard=G), Examination(Suggestion(D, I, U), numberOfPasses=0, presentedCard=I), Examination(Suggestion(B, L, R), numberOfPasses=0, presentedCard=R), Examination(Suggestion(C, K, M), numberOfPasses=1, presentedCard=null), Examination(Suggestion(D, K, U), numberOfPasses=0, presentedCard=null), Examination(Suggestion(B, G, P), numberOfPasses=0, presentedCard=G), Examination(Suggestion(F, L, N), numberOfPasses=1, presentedCard=F), Examination(Suggestion(F, H, Q), numberOfPasses=0, presentedCard=null), Examination(Suggestion(E, G, M), numberOfPasses=0, presentedCard=null), Examination(Suggestion(F, K, M), numberOfPasses=2, presentedCard=null), Examination(Suggestion(E, G, R), numberOfPasses=0, presentedCard=R), Examination(Suggestion(F, H, N), numberOfPasses=0, presentedCard=null)
      )
      val game = ClueGame(arrayOf(I, C, U, B, G))
      val deduction = game.deduce(examinations)
   }

   @Test fun testLastDitchEffort() {
      val examinations = arrayOf(
         Examination(Suggestion(B, J, S), numberOfPasses=0, presentedCard=J), Examination(Suggestion(E, J, O), numberOfPasses=0, presentedCard=null), Examination(Suggestion(D, K, S), numberOfPasses=1, presentedCard=K), Examination(Suggestion(B, I, T), numberOfPasses=0, presentedCard=B), Examination(Suggestion(B, J, M), numberOfPasses=0, presentedCard=J), Examination(Suggestion(E, G, P), numberOfPasses=2, presentedCard=G), Examination(Suggestion(E, I, N), numberOfPasses=0, presentedCard=null), Examination(Suggestion(E, K, P), numberOfPasses=0, presentedCard=K), Examination(Suggestion(C, G, T), numberOfPasses=2, presentedCard=T), Examination(Suggestion(E, G, O), numberOfPasses=0, presentedCard=null), Examination(Suggestion(A, J, O), numberOfPasses=2, presentedCard=null), Examination(Suggestion(F, L, P), numberOfPasses=1, presentedCard=null), Examination(Suggestion(B, L, T), numberOfPasses=1, presentedCard=L), Examination(Suggestion(F, L, S), numberOfPasses=0, presentedCard=null), Examination(Suggestion(F, L, P), numberOfPasses=2, presentedCard=null), Examination(Suggestion(B, K, O), numberOfPasses=0, presentedCard=K), Examination(Suggestion(A, K, Q), numberOfPasses=1, presentedCard=A), Examination(Suggestion(B, G, R), numberOfPasses=1, presentedCard=null), Examination(Suggestion(A, J, T), numberOfPasses=0, presentedCard=null), Examination(Suggestion(A, J, O), numberOfPasses=1, presentedCard=null), Examination(Suggestion(F, L, T), numberOfPasses=0, presentedCard=F), Examination(Suggestion(D, I, P), numberOfPasses=0, presentedCard=null), Examination(Suggestion(C, J, R), numberOfPasses=0, presentedCard=null), Examination(Suggestion(C, G, U), numberOfPasses=0, presentedCard=C), Examination(Suggestion(B, G, Q), numberOfPasses=2, presentedCard=Q), Examination(Suggestion(F, K, R), numberOfPasses=1, presentedCard=null), Examination(Suggestion(C, L, S), numberOfPasses=1, presentedCard=C), Examination(Suggestion(D, H, O), numberOfPasses=1, presentedCard=null), Examination(Suggestion(A, H, P), numberOfPasses=0, presentedCard=P), Examination(Suggestion(D, I, M), numberOfPasses=0, presentedCard=null)
      )
      val game = ClueGame(arrayOf(M, G, B, K, C))
      val deduction = game.deduce(examinations)
      assertEquals(Deduction(E, null, null), deduction)
   }

   @Test fun testCountIssue() {
      val examinations = arrayOf(
         Examination(Suggestion(F, G, R), numberOfPasses=1, presentedCard=R), Examination(Suggestion(E, J, T), numberOfPasses=0, presentedCard=null), Examination(Suggestion(E, K, M), numberOfPasses=0, presentedCard=null), Examination(Suggestion(A, H, U), numberOfPasses=0, presentedCard=U), Examination(Suggestion(F, J, O), numberOfPasses=2, presentedCard=F), Examination(Suggestion(A, I, T), numberOfPasses=0, presentedCard=null), Examination(Suggestion(A, G, U), numberOfPasses=1, presentedCard=G), Examination(Suggestion(E, K, Q), numberOfPasses=1, presentedCard=null), Examination(Suggestion(D, G, R), numberOfPasses=1, presentedCard=R), Examination(Suggestion(E, G, N), numberOfPasses=2, presentedCard=G), Examination(Suggestion(A, J, S), numberOfPasses=0, presentedCard=null), Examination(Suggestion(B, K, T), numberOfPasses=1, presentedCard=null), Examination(Suggestion(F, H, T), numberOfPasses=0, presentedCard=H), Examination(Suggestion(F, K, S), numberOfPasses=0, presentedCard=null), Examination(Suggestion(B, J, S), numberOfPasses=0, presentedCard=null), Examination(Suggestion(D, J, P), numberOfPasses=0, presentedCard=D), Examination(Suggestion(A, H, N), numberOfPasses=0, presentedCard=A), Examination(Suggestion(F, K, P), numberOfPasses=0, presentedCard=null), Examination(Suggestion(E, K, S), numberOfPasses=2, presentedCard=null), Examination(Suggestion(E, K, S), numberOfPasses=1, presentedCard=null), Examination(Suggestion(E, J, S), numberOfPasses=0, presentedCard=E), Examination(Suggestion(B, K, U), numberOfPasses=0, presentedCard=null)
      )
      val game = ClueGame(arrayOf(D, U, I, O, G))
      val deduction = game.deduce(examinations)
   }
}