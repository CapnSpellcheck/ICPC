package icpc.twothousandseventeen

import util.hardAssert
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.math.max

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
   private val cardsHas = EnumSet.noneOf(Card::class.java)
   private val hasOneOf = mutableListOf<EnumSet<Card>>()
   private val cardsDoesntHave = EnumSet.noneOf(Card::class.java)

   private inline val isDeduced: Boolean
      get() = cardsHas.size == numberOfCards

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
      // remove the card if we know the holder, or we know this hand doesn't hold it
      if (cardHolders[suggestion.first]?.let { it != this } == true || cardsDoesntHave.contains(suggestion.first))
         hasOneOf_.remove(suggestion.first)
      if (cardHolders[suggestion.second]?.let { it != this } == true || cardsDoesntHave.contains(suggestion.second))
         hasOneOf_.remove(suggestion.second)
      if (cardHolders[suggestion.third]?.let { it != this } == true || cardsDoesntHave.contains(suggestion.third))
         hasOneOf_.remove(suggestion.third)
      hardAssert(hasOneOf_.isNotEmpty())

      println("presentedForSuggestion: hand $this, hasOneOf_ = $hasOneOf_")
      if (hasOneOf_.size == 1) {
         holding(hasOneOf_.first())
      } else if (!isDeduced) {
         hasOneOf.add(hasOneOf_)
      }
   }

   override fun presentedForMySuggestion(card: Card) {
      holding(card)
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
            // we need to break if we've deduced, modified the collection while iterating
            if (isDeduced) {
               break
            }
         }
      }
      // Although I could remove empty hasOneOf groups, it probably isn't important to spend time doing it
//      hasOneOf.removeIf { it.isEmpty() }
   }

   /**
    * To pigeonhole within a single hand, there must be more hasOneOf groups than unidentified cards (aka slots), and for some
    * card whose occurrence count among hasOneOf groups is maximal, the hasOneOf groups not containing that card
    * must require one less than the # of slots, and it must not be possible to fill the slots
    * without this card with the constraint of the # of slots.
    */
   fun isolatedPigeonhole(): Boolean {
      val numberOfSlots = numberOfCards - cardsHas.size
      val hasOfSet = hasOneOf.toHashSet()
      val emptySet = EnumSet.noneOf(Card::class.java)
      hasOfSet.remove(emptySet)
      if (hasOfSet.size > numberOfSlots) {
         val groupOccurrences = EnumMap<Card, Int>(Card::class.java)
         var maxOccurrences = 0
         for (group in hasOfSet) {
            for (card in group) {
               val occurrences = (groupOccurrences[card] ?: 0) + 1
               groupOccurrences[card] = occurrences
               maxOccurrences = max(maxOccurrences, occurrences)
            }
         }
         println("isolatedPigeonhole: player $playerNo: maxOccurences = $maxOccurrences")
         if (maxOccurrences < 2)
            return false

         for (entry in groupOccurrences.entries.filter { it.value == maxOccurrences }) {
            // remove groups containing this card, and check the cover size must be = numberOfSlots - 1
            val card = entry.key
            println("isolatedPigeonhole: Trying $card")
            val groupsWithoutCard = hasOfSet.filter { !it.contains(card) }
            if (doesCoverRequire(groupsWithoutCard.toHashSet(), numberOfSlots - 1)) {
               // now check whether it is possible to fill the unidentified slots without card instead
               // copy the hasOfSet and remove card from all groups
               println("isolatedPigeonhole: doesCoverRequire = true")
               val groupsWithCardRemoved = hasOfSet.map { it.clone().apply { remove(card) } }.toHashSet()
               groupsWithCardRemoved.remove(emptySet)
               if (!doesCoverExist(groupsWithCardRemoved, numberOfSlots)) {
                  println("isolatedPigeonhole identified")
                  holding(card)
                  return true
               }
            }
         }
      }

      return false
   }

   fun doesntHave(card: Card): Boolean {
      return if (isDeduced) !cardsHas.contains(card) else cardsDoesntHave.contains(card)
   }

   private fun holding(card: Card) {
      cardsHas.add(card)
      hardAssert(cardsHas.size <= numberOfCards)

      // We know that we can kill the 'hasOneOf' list if we've identified their full hand
      if (isDeduced) {
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

   init {
      for (card in myCards)
         cardHolders[card] = myHand
   }

   // We might be able to deduce the missing card of the type whose holder was just identified
   // if there is only one unaccounted for remaining.
   fun identifiedHolder(card: Card, hand: CompetitorHand) {
      println("Identified holder $card = $hand")
      cardHolders[card] = hand
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
         if (examination.numberOfPasses < NUMBER_OF_PLAYERS - 1) {
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

         // update player # for next turn
         suggestingPlayer = suggestingPlayer.inc()
         if (suggestingPlayer > NUMBER_OF_PLAYERS)
            suggestingPlayer = 1
      }

      applyPigeonholes()

      // apply positive elimination
      while (positiveElimination())
         Unit

      // apply negative elimination for all cards
      negativeElimination()

      return Deduction(deducedPerson, deducedWeapon, deducedRoom)
   }

   private fun applyPigeonholes() {
      while (player2.isolatedPigeonhole())
         Unit
      while (player3.isolatedPigeonhole())
         Unit
      while (player4.isolatedPigeonhole())
         Unit

      // analyze multi-player pigeonholing. This involves knowledge about how many cards of each type are left
      // with unknown holders, together with their hasOneOf groups.
   }

   private fun positiveElimination(): Boolean {
      if (deducedPerson == null && positiveElimination(Card.Type.PERSON))
         return true
      if (deducedWeapon == null && positiveElimination(Card.Type.WEAPON))
         return true
      if (deducedRoom == null && positiveElimination(Card.Type.ROOM))
         return true
      return false
   }

   /**
    * Given a type, check to see if owners have been determined for all but one.
    */
   private fun positiveElimination(type: Card.Type): Boolean {
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
         // If we know nobody has that card, we can reduce it from their hands
         player2.reduce(unaccountedCard!!)
         player3.reduce(unaccountedCard!!)
         player4.reduce(unaccountedCard!!)
         return true
      }
      return false
   }

   private fun negativeElimination() {
      if (deducedPerson == null) {
         for (card in Card.PersonCards) {
            negativeElimination(card)
         }
      }
      if (deducedWeapon == null) {
         for (card in Card.WeaponCards) {
            negativeElimination(card)
         }
      }
      if (deducedRoom == null) {
         for (card in Card.RoomCards) {
            negativeElimination(card)
         }
      }
   }

   /**
    * Identify cards for which it is known that none of the players hold.
    */
   private fun negativeElimination(card: Card) {
      when (card.type) {
         Card.Type.PERSON ->
            if (
               player2.doesntHave(card) &&
               player3.doesntHave(card) &&
               player4.doesntHave(card) &&
               !myHand.cardsHas.contains(card)
            ) {
               println("negativeElimination deducedPerson=$card")
               deducedPerson = card
            }
         Card.Type.WEAPON ->
            if (
               player2.doesntHave(card) &&
               player3.doesntHave(card) &&
               player4.doesntHave(card) &&
               !myHand.cardsHas.contains(card)
            ) {
               println("negativeElimination deducedWeapon=$card")
               deducedWeapon = card
            }
         Card.Type.ROOM ->
            if (
               player2.doesntHave(card) &&
               player3.doesntHave(card) &&
               player4.doesntHave(card) &&
               !myHand.cardsHas.contains(card)
            ) {
               println("negativeElimination deducedRoom=$card")
               deducedRoom = card
            }
      }
   }
}

fun doesCoverRequire(groups: HashSet<EnumSet<Card>>, coverSize: Int): Boolean {
   // Note: coverSize would only become negative if we have unsatisfiable constraints
   if (groups.isEmpty())
      return coverSize <= 0
   val triedCards = EnumSet.noneOf(Card::class.java)
   for (group in groups) {
      for (card in group) {
         // try this card
         if (triedCards.contains(card))
            continue
         triedCards.add(card)
         // get the subset of groups that doesn't have this card
         val groupsWithoutCard = groups.filter { !it.contains(card) }
         if (!doesCoverRequire(groupsWithoutCard.toHashSet(), coverSize - 1))
            return false
      }
   }
   return true
}

fun doesCoverExist(groups: HashSet<EnumSet<Card>>, maxCoverSize: Int): Boolean {
   if (groups.isEmpty())
      return maxCoverSize >= 0
   val triedCards = EnumSet.noneOf(Card::class.java)
   for (group in groups) {
      for (card in group) {
         // try this card
         if (triedCards.contains(card))
            continue
         triedCards.add(card)
         // get the subset of groups that doesn't have this card
         val groupsWithoutCard = groups.filter { !it.contains(card) }
         if (doesCoverExist(groupsWithoutCard.toHashSet(), maxCoverSize - 1))
            return true
      }
   }
   return false
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
      outputStream.flush()
   }
}

inline fun output(card: Card?, os: OutputStream) {
   val char = card?.name?.first() ?: '?'
   os.write(char.code)
}

fun main() {
   ClueGameIO(System.`in`, System.out)
}