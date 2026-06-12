package icpc.twothousandseventeen

import util.hardAssert
import java.util.EnumMap
import java.util.EnumSet

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

class Examination(val suggestion: Suggestion, val numberOfPasses: Short, val presentedCard: Card? = null)

abstract class Hand {
   open fun couldntPresentForSuggestion(suggestion: Suggestion) {}
   open fun presentedForSuggestion(suggestion: Suggestion) {}
   open fun presentedForMySuggestion(card: Card) {}
}

class MyHand(cards: Array<Card>) : Hand() {
   val cardsHas = EnumSet.of(cards[0], cards[1], cards[2], cards[3], cards[4])
}

class CompetitorHand(val numberOfCards: Int, val game: ClueGame) : Hand() {
   val cardsDoesntHave = EnumSet.noneOf(Card::class.java)
   val cardsHas = EnumSet.noneOf(Card::class.java)
   val hasOneOf = mutableListOf<EnumSet<Card>>()
   var couldntPresentForLastSuggestion = false; private set

   override fun couldntPresentForSuggestion(suggestion: Suggestion) {
      cardsDoesntHave.add(suggestion.first)
      cardsDoesntHave.add(suggestion.second)
      cardsDoesntHave.add(suggestion.third)
      couldntPresentForLastSuggestion = true
   }

   fun newSuggestion() {
      couldntPresentForLastSuggestion = false
   }

   override fun presentedForSuggestion(suggestion: Suggestion) {
      // deduce!
      val hasOneOf_ = EnumSet.of(suggestion.first, suggestion.second, suggestion.third)
      if (game.cardHolders[suggestion.first]?.let { it != this } == true)
         hasOneOf_.remove(suggestion.first)
      if (game.cardHolders[suggestion.second]?.let { it != this } == true)
         hasOneOf_.remove(suggestion.second)
      if (game.cardHolders[suggestion.third]?.let { it != this } == true)
         hasOneOf_.remove(suggestion.third)
      hardAssert(hasOneOf_.isNotEmpty())

      if (hasOneOf_.size == 1) {
         cardsHas.add(hasOneOf_.first())
         game.identifiedHolder(hasOneOf_.first(), this)
         if (cardsHas.size == numberOfCards) {
            hasOneOf.clear()
         }
      } else if (cardsHas.size < numberOfCards)
         hasOneOf.add(hasOneOf_)
   }

   override fun presentedForMySuggestion(card: Card) {
      cardsHas.add(card)
      // deduce!

      hardAssert(cardsHas.size <= numberOfCards)
      // We know that we can kill the 'hasOneOf' list if we've identified their full hand
      if (cardsHas.size == numberOfCards) {
         hasOneOf.clear()
      }
      game.identifiedHolder(card, this)
   }
}

class ClueGame(myCards: Array<Card>) {
   val cardHolders = EnumMap<Card, Hand>(Card::class.java)
   val myHand = MyHand(myCards)

   private var deducedPerson: Card? = null
   private var deducedWeapon: Card? = null
   private var deducedRoom: Card? = null

   init {
      for (card in myCards)
         cardHolders[card] = myHand
   }

   // We might be able to deduce the missing card of the type whose holder was just identified
   // if there is only one unaccounted for remaining.
   fun identifiedHolder(card: Card, hand: CompetitorHand) {
      cardHolders[card] = hand
      var unaccountedCard: Card? = null
      var numberOfUnaccountedCards = 0
      for (cardOfType in Card.cardsOfType(card.type)) {
         if (cardHolders[cardOfType] == null) {
            unaccountedCard = cardOfType
            numberOfUnaccountedCards += 1
         }
      }
      if (numberOfUnaccountedCards == 1) {
         when (card.type) {
            Card.Type.PERSON -> deducedPerson = unaccountedCard
            Card.Type.WEAPON -> deducedWeapon = unaccountedCard
            Card.Type.ROOM -> deducedRoom = unaccountedCard
         }
      }
   }

   fun deduce(examinations: Array<Examination>): Deduction {
      val player2 = CompetitorHand(5, this)
      val player3 = CompetitorHand(4, this)
      val player4 = CompetitorHand(4, this)

      var suggestingPlayer = STARTING_PLAYER

      for (examination in examinations) {
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
               presentingHand?.presentedForSuggestion(examination.suggestion)
            }
         }

         // Deduce missing cards in the suggestion
         if (examination.numberOfPasses > 0) {
            val suggestedPerson = examination.suggestion.first
            if (player2.cardsDoesntHave.contains(suggestedPerson) &&
               player3.cardsDoesntHave.contains(suggestedPerson) &&
               player3.cardsDoesntHave.contains(suggestedPerson) &&
               !myHand.cardsHas.contains(suggestedPerson))
            {
               deducedPerson = suggestedPerson
            }
            val suggestedWeapon = examination.suggestion.second
            if (player2.cardsDoesntHave.contains(suggestedWeapon) &&
               player3.cardsDoesntHave.contains(suggestedWeapon) &&
               player3.cardsDoesntHave.contains(suggestedWeapon) &&
               !myHand.cardsHas.contains(suggestedWeapon))
            {
               deducedWeapon = suggestedWeapon
            }
            val suggestedRoom = examination.suggestion.third
            if (player2.cardsDoesntHave.contains(suggestedRoom) &&
               player3.cardsDoesntHave.contains(suggestedRoom) &&
               player3.cardsDoesntHave.contains(suggestedRoom) &&
               !myHand.cardsHas.contains(suggestedRoom))
            {
               deducedRoom = suggestedRoom
            }
         }

         // early exit if solved
         if (deducedPerson != null && deducedWeapon != null && deducedRoom != null)
            break
         // update player # for next turn
         suggestingPlayer = suggestingPlayer.inc()
         if (suggestingPlayer == NUMBER_OF_PLAYERS)
            suggestingPlayer = 1
      }

      return Deduction(deducedPerson, deducedWeapon, deducedRoom)
   }
}
