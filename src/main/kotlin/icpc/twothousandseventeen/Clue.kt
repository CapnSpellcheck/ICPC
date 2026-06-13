package icpc.twothousandseventeen

import util.hardAssert
import java.io.InputStream
import java.io.OutputStream
import java.util.*

enum class Card(val type: Type) {
   A(Type.PERSON), B(Type.PERSON), C(Type.PERSON), D(Type.PERSON), E(Type.PERSON), F(Type.PERSON),
   G(Type.WEAPON), H(Type.WEAPON), I(Type.WEAPON), J(Type.WEAPON), K(Type.WEAPON), L(Type.WEAPON),
   M(Type.ROOM), N(Type.ROOM), O(Type.ROOM), P(Type.ROOM), Q(Type.ROOM), R(Type.ROOM), S(Type.ROOM), T(Type.ROOM), U(Type.ROOM)
   ;
   enum class Type { PERSON, WEAPON, ROOM }

   companion object {
      val PersonCards = arrayOf(A, B, C, D, E, F)
      val WeaponCards = arrayOf(G, H, I, J, K, L)
      val RoomCards = arrayOf(M, N, O, P, Q, R, S, T, U)
      inline fun cardsOfType(type: Type): Array<Card> = when (type) {
         Type.PERSON -> PersonCards
         Type.WEAPON -> WeaponCards
         Type.ROOM -> RoomCards
      }
   }
}

typealias Suggestion = Triple<Card, Card, Card>
typealias Deduction = Triple<Card?, Card?, Card?>

const val NO_PLAYER_PRESENTED: Short = 3
const val STARTING_PLAYER: Short = 1
const val NUMBER_OF_PLAYERS: Short = 4

class Examination(val suggestion: Suggestion, val numberOfPasses: Short, val presentedCard: Card? = null) {
   override fun toString(): String {
      return "Examination(suggestion=$suggestion, numberOfPasses=$numberOfPasses, presentedCard=$presentedCard)"
   }
}

abstract class Hand {
   open fun couldntPresentForSuggestion(suggestion: Suggestion) {}
   open fun presentedForSuggestion(suggestion: Suggestion, cardHolders: Map<Card, Hand>) {}
   open fun presentedForMySuggestion(card: Card) {}
}

class MyHand(cards: Array<Card>) : Hand() {
   val cardsHas = EnumSet.of(cards[0], cards[1], cards[2], cards[3], cards[4])
}

class CompetitorHand(val numberOfCards: Int, val game: ClueGame, val playerNo: Int) : Hand() {
   val cardsDoesntHave = EnumSet.noneOf(Card::class.java)
   val cardsHas = EnumSet.noneOf(Card::class.java)
   private val hasOneOf = mutableListOf<EnumSet<Card>>()

   override fun couldntPresentForSuggestion(suggestion: Suggestion) {
      cardsDoesntHave.add(suggestion.first)
      cardsDoesntHave.add(suggestion.second)
      cardsDoesntHave.add(suggestion.third)
      // We can reduce these cards from `hasOneOf` groups
      reduce(suggestion.first)
      reduce(suggestion.second)
      reduce(suggestion.third)
   }

   override fun presentedForSuggestion(suggestion: Suggestion, cardHolders: Map<Card, Hand>) {
      // deduce!
      val hasOneOf_ = EnumSet.of(suggestion.first, suggestion.second, suggestion.third)
      if (cardHolders[suggestion.first]?.let { it != this } == true)
         hasOneOf_.remove(suggestion.first)
      if (cardHolders[suggestion.second]?.let { it != this } == true)
         hasOneOf_.remove(suggestion.second)
      if (cardHolders[suggestion.third]?.let { it != this } == true)
         hasOneOf_.remove(suggestion.third)
      hardAssert(hasOneOf_.isNotEmpty())

      println("presentedForSuggestion: hand $this, hasOneOf_ = $hasOneOf_")
      if (hasOneOf_.size == 1) {
         holding(hasOneOf_.first())
      } else if (cardsHas.size < numberOfCards) {
         hasOneOf.add(hasOneOf_)
      }
   }

   override fun presentedForMySuggestion(card: Card) {
      holding(card)
      hardAssert(cardsHas.size <= numberOfCards)
   }

   /**
    * Remove a specific card from `hasOneOf` groups. For each group that turns into a singleton, we can conclude
    * this hand is the holder of the remaining element.
    */
   fun reduce(card: Card) {
      for (group in hasOneOf) {
         group.remove(card)
         if (group.size == 1) {
            val identified = group.first()
            group.remove(identified)
            holding(identified)
            if (cardsHas.size == numberOfCards) {
               break
            }
         }
      }
      // Although I could remove empty hasOneOf groups, it probably isn't important to spend time doing it
//      hasOneOf.removeIf { it.isEmpty() }
   }

   private fun holding(card: Card) {
      cardsHas.add(card)
      // We know that we can kill the 'hasOneOf' list if we've identified their full hand
      if (cardsHas.size == numberOfCards) {
         hasOneOf.clear()
      }
      // Any `hasOneOf` groups that contains `card` should be removed because it no longer contains information
      hasOneOf.removeIf { it.contains(card) }
      // ClueGame should reduce the card from groups in other competitors
      game.identifiedHolder(card, this)
   }

   override fun toString(): String {
      return "CompetitorHand(playerNo=$playerNo)"
   }


}

class ClueGame(myCards: Array<Card>) {
   private val cardHolders = EnumMap<Card, Hand>(Card::class.java)
   private val myHand = MyHand(myCards)
   private val player2 = CompetitorHand(5, this, 2)
   private val player3 = CompetitorHand(4, this, 3)
   private val player4 = CompetitorHand(4, this, 4)
   private var deducedPerson: Card? = null
   private var deducedWeapon: Card? = null
   private var deducedRoom: Card? = null

   private val checkEliminationsByTypeBitset = BitSet(3)

   init {
      for (card in myCards)
         cardHolders[card] = myHand
   }

   // We might be able to deduce the missing card of the type whose holder was just identified
   // if there is only one unaccounted for remaining.
   fun identifiedHolder(card: Card, hand: CompetitorHand) {
      println("Identified holder $card = $hand")
      cardHolders[card] = hand
      checkEliminationsByTypeBitset.set(card.type.ordinal)
      if (hand != player2)
         player2.reduce(card)
      if (hand != player3)
         player3.reduce(card)
      if (hand != player4)
         player4.reduce(card)
   }

   fun deduce(examinations: Array<Examination>): Deduction {
      var suggestingPlayer = STARTING_PLAYER

      for (examination in examinations) {
         println("examination $examination")
         // check which competitor hands passed
         var examinee = suggestingPlayer + 1
         repeat(examination.numberOfPasses + 0) {
            val competitorHand = when (examinee) {
               2, 6 -> player2
               3, 7 -> player3
               4, -> player4
               else -> null
            }
            // this player couldn't present
            competitorHand?.couldntPresentForSuggestion(examination.suggestion)
            examinee += 1
         }

         // examinee now equals presenting player #, if numberOfPasses == 0-2
         if (examination.numberOfPasses < NO_PLAYER_PRESENTED) {
            val presentingHand = when (examinee) {
               2, 6 -> player2
               3, 7 -> player3
               4 -> player4
               else -> null
            }
            examination.presentedCard?.let { presentedCard ->
               presentingHand?.presentedForMySuggestion(presentedCard)
            } ?: run {
               // someone else saw the card
               presentingHand?.presentedForSuggestion(examination.suggestion, cardHolders)
            }
         }

         // deduce by positive elimination once per turn if needed
         if (checkEliminationsByTypeBitset.get(Card.Type.PERSON.ordinal))
            positiveElimination(Card.Type.PERSON)
         if (checkEliminationsByTypeBitset.get(Card.Type.WEAPON.ordinal))
            positiveElimination(Card.Type.WEAPON)
         if (checkEliminationsByTypeBitset.get(Card.Type.ROOM.ordinal))
            positiveElimination(Card.Type.ROOM)

         if (examination.numberOfPasses > 0) {
            negativeElimination(examination)
         }

         checkEliminationsByTypeBitset.clear()

         // early exit if solved
         if (deducedPerson != null && deducedWeapon != null && deducedRoom != null)
            break
         // update player # for next turn
         suggestingPlayer = suggestingPlayer.inc()
         if (suggestingPlayer > NUMBER_OF_PLAYERS)
            suggestingPlayer = 1
      }

      return Deduction(deducedPerson, deducedWeapon, deducedRoom)
   }

   /**
    * Given a type, check to see if owners have been determined for all but one.
    */
   private fun positiveElimination(type: Card.Type) {
      var unaccountedCard: Card? = null
      var numberOfUnaccountedCards = 0
      for (cardOfType in Card.cardsOfType(type)) {
         if (cardHolders[cardOfType] == null) {
            unaccountedCard = cardOfType
            numberOfUnaccountedCards += 1
         }
      }
      if (numberOfUnaccountedCards == 1) {
         println("positiveElimination $type $unaccountedCard")
         when (type) {
            Card.Type.PERSON -> deducedPerson = unaccountedCard
            Card.Type.WEAPON -> deducedWeapon = unaccountedCard
            Card.Type.ROOM -> deducedRoom = unaccountedCard
         }
      }
   }

   private fun negativeElimination(examination: Examination) {
      // Deduce missing cards in the suggestion. This check does not use cardHolders, but
      // uses the knowledge that someone didn't have the cards in the suggestion, to check
      // across all players.
      val suggestedPerson = examination.suggestion.first
      if (deducedPerson == null &&
         player2.cardsDoesntHave.contains(suggestedPerson) &&
         player3.cardsDoesntHave.contains(suggestedPerson) &&
         player4.cardsDoesntHave.contains(suggestedPerson) &&
         !myHand.cardsHas.contains(suggestedPerson))
      {
         println("negativeElimination deducedPerson=$suggestedPerson")
         deducedPerson = suggestedPerson
      }
      val suggestedWeapon = examination.suggestion.second
      if (deducedWeapon == null &&
         player2.cardsDoesntHave.contains(suggestedWeapon) &&
         player3.cardsDoesntHave.contains(suggestedWeapon) &&
         player4.cardsDoesntHave.contains(suggestedWeapon) &&
         !myHand.cardsHas.contains(suggestedWeapon))
      {
         println("negativeElimination deducedWeapon=$suggestedWeapon")
         deducedWeapon = suggestedWeapon
      }
      val suggestedRoom = examination.suggestion.third
      if (deducedRoom == null &&
         player2.cardsDoesntHave.contains(suggestedRoom) &&
         player3.cardsDoesntHave.contains(suggestedRoom) &&
         player4.cardsDoesntHave.contains(suggestedRoom) &&
         !myHand.cardsHas.contains(suggestedRoom))
      {
         println("negativeElimination deducedRoom=$suggestedRoom")
         deducedRoom = suggestedRoom
      }
   }
}

fun ClueGameIO(inputStream: InputStream, outputStream: OutputStream) {
   inputStream.bufferedReader().use { reader ->
      val count = reader.readLine().toInt()
      val myhandStr = reader.readLine()
      val myCards = Array(5) { i ->
         Card.valueOf(myhandStr[2*i].toString())
      }
      val game = ClueGame(myCards)
      val exams = Array(count) {
         val examLine = reader.readLine()
         val suggestion = Suggestion(Card.valueOf(examLine[0].toString()), Card.valueOf(examLine[2].toString()), Card.valueOf(examLine[4].toString()))
         val revealedCard = examLine.last().let {
            when (it) {
               '*', '-' -> null
               else -> Card.valueOf(it.toString())
            }
         }
         val numberOfPasses = examLine.count { it == '-' }
         Examination(suggestion, numberOfPasses.toShort(), revealedCard)
      }

      val deduction = game.deduce(exams)
      output(deduction.first, outputStream)
      output(deduction.second, outputStream)
      output(deduction.third, outputStream)
   }
}

inline fun output(card: Card?, os: OutputStream) {
   val char = card?.name?.first() ?: '?'
   os.write(char.code)
}