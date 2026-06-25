package icpc.twothousandseventeen

import util.hardAssert
import util.union
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.max

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/clue
 */

private const val DEBUG = false
fun printDebug(cs: CharSequence) {
   if (DEBUG) {
      println(cs)
   }
}

/**
 * A card in Clue. Players can have any subset of them, so they are in a single class. The `type` property
 * indicates its type.
 * @property type Whether this card is considered a person, weapon, or room
 */
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

      fun setOf(cards: List<Card>): EnumSet<Card> {
         val set = EnumSet.noneOf(Card::class.java)
         for (card in cards)
            set.add(card)
         return set
      }
   }
}

// A Suggestion holds 3 cards. A not-enforced prerequisite is s.first.type == PERSON && s.second.type == WEAPON &&
// s.third.type == ROOM.
typealias Suggestion = Triple<Card, Card, Card>
// The deduced state of the envelope contents.
typealias Deduction = Triple<Card?, Card?, Card?>

/**
 * What happens during a single player's turn
 * @property suggestion The cards that the player suggests, one of each type
 * @property numberOfPasses The number of consecutive players that couldn't answer a suggestion
 * @property presentedCard The card that player 1 either revealed or was revealed to.
 */
class Examination(val suggestion: Suggestion, val numberOfPasses: Short, val presentedCard: Card? = null) {
   override fun toString(): String {
      return "Examination(suggestion=$suggestion, numberOfPasses=$numberOfPasses, presentedCard=$presentedCard)"
   }
}

/**
 * A player's hand, any kind at all! A hand could be my hand, your hand, or a hand of 2 or 3 opponents.
 */
abstract class Hand {}

/**
 * My hand. I know what cards I hold, so it doesn't change during the course of the game deduction; no analysis is
 * performed on it.
 */
class MyHand(cards: Array<Card>) : Hand() {
   val cardsHas = EnumSet.of(cards[0], cards[1], cards[2], cards[3], cards[4])
}

/**
 * The hand of a single competitor.
 * @property numberOfCards The number of cards in his hand. According to the problem statement, there are always
 *   4 players, and all players know the number of cards in each player's hand.
 * @property game The game being analyzed. I use it for callbacks; it could be satisfied by an interface instead.
 * @property playerNo A number between 2 and 4, used to display in debugging logs.
 */
class CompetitorHand(val numberOfCards: Int, val game: ClueGame, val playerNo: Int) : Hand() {
   // The cards we have proven the player holds
   private val cardsHas = EnumSet.noneOf(Card::class.java)
   // Sets of cards which deduction can only prove the player is holding at least one of, typically a subset of a Suggestion
   private var hasOneOfSet = hashSetOf<EnumSet<Card>>()
   // Cards that deduction can prove the player doesn't have
   private val cardsDoesntHave = EnumSet.noneOf(Card::class.java)
   // Number of cards that the player must have, but we don't know exactly which one he holds
   private var cardsReservedForMultiHand = 0

   val isDeduced: Boolean
      get() = cardsHas.size == numberOfCards

   val hasOneGroups: Collection<EnumSet<Card>>
      get() = hasOneOfSet

   // Number of slots is the number of cards in the player's hand that are not determined. By consequence of analysis methods,
   // cards that are known to be in the multi hand do not count as slots.
   val numberOfSlots: Int
      get() = numberOfCards - cardsHas.size - cardsReservedForMultiHand

   val knownCards: Int  // used simply for debugging
      get() = cardsHas.size

   /**
    * This must be called each time a turn is made, and some player doesn't answer the suggestion. What happens is that we
    * know this hand doesn't hold any of the suggested cards.
    * @param suggestion The 3 cards that the suggester suggested
    */
   fun couldntPresentForSuggestion(suggestion: Suggestion) {
      cardsDoesntHave.add(suggestion.first)
      cardsDoesntHave.add(suggestion.second)
      cardsDoesntHave.add(suggestion.third)
      // We can reduce these cards from `hasOneOf` groups
      reduce(suggestion.first)
      reduce(suggestion.second)
      reduce(suggestion.third)
   }

   /**
    * This must be called each time a turn is made, and some player answers a suggestion. The answer card was not shown to me.
    * @param suggestion The 3 cards that the suggester suggested
    * @param cardHolders This unfortunately is passed in to determine if each card in the suggestion is known the holder.
    *   An improvement would be to pass in the holder status of the 3 cards.
    */
   fun presentedForSuggestion(suggestion: Suggestion, cardHolders: Map<Card, Hand>) {
      // if the hand already holds one of the cards, the suggestion is useless
      if (cardsHas.contains(suggestion.first) || cardsHas.contains(suggestion.second) || cardsHas.contains(suggestion.third)) {
         printDebug("presentedForSuggestion: player $playerNo: suggestion useless")
         return
      }
      val hasOneOf = EnumSet.of(suggestion.first, suggestion.second, suggestion.third)
      // remove the card if we know the holder, or we know this hand doesn't hold it
      if (cardHolders[suggestion.first] != null || cardsDoesntHave.contains(suggestion.first))
         hasOneOf.remove(suggestion.first)
      if (cardHolders[suggestion.second] != null || cardsDoesntHave.contains(suggestion.second))
         hasOneOf.remove(suggestion.second)
      if (cardHolders[suggestion.third] != null || cardsDoesntHave.contains(suggestion.third))
         hasOneOf.remove(suggestion.third)
      hardAssert(hasOneOf.isNotEmpty())
      printDebug("presentedForSuggestion: hand $this, hasOneOf_ = $hasOneOf")

      // If there is only one possible card, I know this player holds it; otherwise, add this set to the sets of cards.
      if (hasOneOf.size == 1) {
         holding(hasOneOf.first())
      } else if (!isDeduced) {
         hasOneOfSet.add(hasOneOf)
      }
   }

   /**
    * Remove a specific card from `hasOneOf` groups. For each group that turns into a singleton, we can conclude
    * this hand is the holder of the remaining element.
    * @param card The card to remove
    */
   fun reduce(card: Card) {
      // We have to play a bit of a game here in that this function needs to be reentrant-safe with respect to
      // hasOneOfSet, and also we can't mutate a group while it's in the (Hash)set. First identify held cards
      // in one pass (in a very extreme case, I think you can deduce all 5 cards of player 2) or update a group of 3 to a
      // group of 2, then call `holding` for the deduced held cards.
      val deducedHolding = ArrayList<Card>(4)
      for (group in hasOneOfSet.filter { it.contains(card) }) {
         hasOneOfSet.remove(group)
         group.remove(card)
         if (group.size == 1) {
            val identified = group.first()
            deducedHolding.add(identified)
         } else {
            hasOneOfSet.add(group)
         }
      }
      for (deducedHeld in deducedHolding) {
         holding(deducedHeld)
      }
   }

   /**
    * To pigeonhole within a single hand, there must be more hasOneOf groups than unidentified cards (aka slots), and for some
    * card whose occurrence count among hasOneOf groups is maximal, the hasOneOf groups not containing that card
    * must require one less than the # of slots, and it must not be possible to fill the slots
    * without this card with the constraint of the # of slots. Therefore, the player must hold that card.
    * @return true if it was proven that a player holds one of the cards that was in one of the hasOneOfSet elements
    */
   fun isolatedPigeonhole(): Boolean {
      val hasOfSet = hasOneOfSet

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
         printDebug("isolatedPigeonhole: player $playerNo: maxOccurrences = $maxOccurrences")
         if (maxOccurrences < 2)
            return false

         for (entry in groupOccurrences.entries.filter { it.value == maxOccurrences }) {
            // remove groups containing this card, and check the cover size must be = numberOfSlots - 1
            val card = entry.key
            printDebug("isolatedPigeonhole: Trying $card")
            val groupsWithoutCard = hasOfSet.filterNot { it.contains(card) }
            if (doesCoverRequire(groupsWithoutCard, numberOfSlots - 1)) {
               // now check whether it is possible to fill the unidentified slots without card instead
               // copy the hasOfSet and remove card from all groups
               printDebug("isolatedPigeonhole: doesCoverRequire = true")
               val groupsWithCardRemoved = hasOfSet.map { it.clone().apply { remove(card) } }.toHashSet()
               groupsWithCardRemoved.remove(EMPTY_CARD_SET)
               if (!doesCoverExist(groupsWithCardRemoved, numberOfSlots)) {
                  printDebug("isolatedPigeonhole identified")
                  holding(card)
                  return true
               }
            }
         }
      }

      return false
   }

   /**
    * To perform a negative pigeonhole in a single hand, we must prove that that the hasOneOfSet is such that
    * all remaining slots in this hand must have cards contained in it.
    */
   fun negativeIsolatedPigeonhole(cardHolders: Map<Card, Hand>): Boolean {
      val hasOfSet = hasOneOfSet
      if (numberOfSlots == 0) return false

      if (isSaturated()) {
         printDebug("negativeIsolatedPigeonhole: player $playerNo: cover requires all slots")
         // add all cards not owned and not in hasOneOf's to cardsDoesntHave
         val flatHasOfSet = EnumSet.noneOf(Card::class.java)
         for (hasOf in hasOfSet) {
            flatHasOfSet.addAll(hasOf)
         }
         // The simple deduction first: any card that isn't mentioned in hasOneOfSet, he doesn't have
         for (card in Card.entries) {
            // additional complication due to multi-hand logic: skip over cards assigned to the multi-hand;
            // this player may have that card, even though not mentioned in hasOneOfSet
            if (!cardHolders.contains(card) && !flatHasOfSet.contains(card)) {
               printDebug("negativeIsolatedPigeonhole doesn't have $card")
               cardsDoesntHave.add(card)
            }
         }

         // An additional deduction: It is possible that even some cards in hasOneOfSet cannot be held. Suppose
         // this hand has one open slot, 2 entries in hasOneOf, and one of the cards in one of the entries
         // appears only once, i.e. in that entry. Then we know that actually, this player can't have it: it's
         // impossible to satisfy both entries of hasOneOfSet by holding only that card.
         val newDoesntHave = EnumSet.noneOf(Card::class.java)
         val triedCards = EnumSet.noneOf(Card::class.java)
         for (group in hasOfSet) {
            if (group.size > 2) {
               for (card in group) {
                  // try this card
                  if (triedCards.contains(card))
                     continue
                  triedCards.add(card)
                  val groupsWithoutCard = hasOfSet.filterNot { it.contains(card) }
                  if (!doesCoverExist(groupsWithoutCard, numberOfSlots - 1)) {
                     printDebug("negativeIsolatedPigeonhole doesn't have $card [2]")
                     newDoesntHave.add(card)
                     cardsDoesntHave.add(card)
                  }
               }
            }
         }
         // these cards are in hasOneOfSet, so manually remove them now, and rebuild hasOneOfSet
         if (newDoesntHave.isNotEmpty()) {
            val hasOfList = hasOfSet.toList()
            for (group in hasOfList) {
               group.removeAll(newDoesntHave)
            }
            hasOneOfSet = hasOfList.toHashSet()
            hasOneOfSet.remove(EMPTY_CARD_SET)
         }
      }

      // Can we deduce the remaining slots entirely by the cardsDoesntHave?
      val numberOfUnassignedCards = Card.entries.size - cardHolders.size
      if (numberOfUnassignedCards - cardsDoesntHave.count { !cardHolders.contains(it) } == numberOfSlots) {
         printDebug("negativeIsolatedPigeonhole: player $playerNo: can determine remaining cards by cardsDoesntHave")
         // find the cards not in cardsDoesntHave that are not in cardHolders
         for (card in Card.entries) {
            if (!cardsDoesntHave.contains(card) && !cardHolders.contains(card)) {
               holding(card)
            }
         }
         return true
      }
      return false
   }

   /**
    * @return True iff it requires the number of available slots to satisfy the hand's `hasOneOfSet`.
    */
   fun isSaturated(): Boolean =
      doesCoverRequire(hasOneOfSet, numberOfSlots)

   /**
    * There might be a faster way to do it -- what I do is check if it requires one card, then add 1 and see if it requires 2,
    * stop once I get false, subtract 1.
    * @return The minimum number of slots needed to satisfy the hand's `hasOneOfSet`.
    */
   fun minimumCoverSize(): Int {
      var size = 1
      while (doesCoverRequire(hasOneOfSet, size)) {
         hardAssert(size < 6)
         size += 1
      }
      return size - 1
   }

   /**
    * A wrapper to check if the hand doesn't have a card (for certain). Once a hand is deduced, it doesn't have any card
    * not in `cardsHas`. This wrapper saves me the need of adding all other cards to `cardsDoesntHave`.
    */
   fun doesntHave(card: Card): Boolean {
      return if (isDeduced) !cardsHas.contains(card) else cardsDoesntHave.contains(card)
   }

   /**
    * This needs to be called when it is proven that the hand holds a card. It should be called directly by the game when
    * I view an answer to my suggestion. It should be called by the hand itself whenever it proves that it holds a card.
    */
   fun holding(card: Card) {
      if (cardsHas.contains(card))
         return
      cardsHas.add(card)
      hardAssert(cardsHas.size <= numberOfCards)

      // We know that we can kill the 'hasOneOf' list if we've identified their full hand
      if (isDeduced) {
         hasOneOfSet = hashSetOf()
      }
      // Any `hasOneOf` groups that contains `card` should be removed because it no longer contains information.
      hasOneOfSet.removeIf { it.contains(card) }
      // The game should reduce the card from the hands of other competitors
      game.identifiedHolder(card, this)
   }

   fun removeMultiHandGroup(group: EnumSet<Card>) {
      hasOneOfSet.remove(group)
      cardsReservedForMultiHand += 1
   }

   fun readdMultiHandGroup(group: EnumSet<Card>) {
      cardsReservedForMultiHand -= 1
      hasOneOfSet.add(group)
   }

   // Used for heuristics to determine whether to run the last ditch effort
   fun numberOfUsefulCardsDoesntHave(holders: EnumMap<Card, *>): Int {
      return cardsDoesntHave.count { holders[it] == null }
   }

   override fun toString(): String {
      return "CompetitorHand(playerNo=$playerNo)"
   }

   companion object {
      val EMPTY_CARD_SET = EnumSet.noneOf(Card::class.java)
   }

}

/**
 * It's a hand that represents more than one opponent! The multi-hand is registered as cardHolder of a card when it is known that
 * some opponent holds a card, but that opponent is not uniquely identified. This comes up in multi-hand pigeonholing.
 * The MultiCompetitorHand doesn't have its own logic (this could possibly be improved). It is more or less removed
 * during last ditch analysis because all hand allocations need to be tested.
 */
class MultiCompetitorHand : Hand() {
   val holderHasOneOfs = mutableListOf<Pair<CompetitorHand, EnumSet<Card>>>()
   val heldCards = EnumSet.noneOf(Card::class.java)
   fun holding(card: Card) {
      heldCards.add(card)
   }
}

const val STARTING_PLAYER: Short = 1
const val NUMBER_OF_PLAYERS: Short = 4

/**
 * The class that drives the deduction. Pass in the cards of player 1.
 */
class ClueGame(myCards: Array<Card>) {
   private val cardHolders = EnumMap<Card, Hand>(Card::class.java)
   private val myHand = MyHand(myCards)
   private val player2 = CompetitorHand(5, this, 2)
   private val player3 = CompetitorHand(4, this, 3)
   private val player4 = CompetitorHand(4, this, 4)
   private var deducedPerson: Card? = null
   private var deducedWeapon: Card? = null
   private var deducedRoom: Card? = null
   private var multiHand = MultiCompetitorHand()

   init {
      for (card in myCards)
         cardHolders[card] = myHand
   }

   /**
    * This is a callback from any CompetitorHand when it identifies that it holds a card (or when the game
    * calls `holding`).
    * @param card The card that `hand` was proven to hold
    * @param hand The player's hand that was proven to hold `card`
    */
   fun identifiedHolder(card: Card, hand: CompetitorHand) {
      printDebug("Identified holder $card = $hand")
      cardHolders[card] = hand
      if (hand != player2)
         player2.reduce(card)
      if (hand != player3)
         player3.reduce(card)
      if (hand != player4)
         player4.reduce(card)
   }

   /**
    * The big shebang. "deduce" consist of:
    * * Digesting each examination
    * * Performing pigeonholing. Since a successful pigeonholing usually results in identifying a new card's holder,
    * *   it might be worthwhile to perform all pigeonholing again. This is indicated with a 'true' return.
    * * Performing positive elimination
    * * Performing negative elimination
    * * Reviewing the case for a last ditch search
    * @return the deduction of the envelope
    */
   fun deduce(examinations: Array<Examination>): Deduction {
      var suggestingPlayer = STARTING_PLAYER

      for (examination in examinations) {
         printDebug("turn $suggestingPlayer: examination $examination")
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
            // If `presentedCard` is non-null, I saw it
            examination.presentedCard?.let { presentedCard ->
               presentingHand?.holding(presentedCard)
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

      var isRepigeonholing = false
      while (applyPigeonholes())
         isRepigeonholing = true

      // apply positive elimination
      while (positiveElimination()) {}

      // apply negative elimination for all cards
      negativeElimination()

      lastDitchEffort()

      return Deduction(deducedPerson, deducedWeapon, deducedRoom)
   }

   private fun applyPigeonholes(): Boolean {
      printDebug("applyPigeonholes")
      var rePigeonhole = false
      rePigeonhole = rePigeonhole || player2.isolatedPigeonhole()
      rePigeonhole = rePigeonhole || player3.isolatedPigeonhole()
      rePigeonhole = rePigeonhole || player4.isolatedPigeonhole()

      // negative pigeonholing
      rePigeonhole = rePigeonhole || player2.negativeIsolatedPigeonhole(cardHolders)
      rePigeonhole = rePigeonhole || player3.negativeIsolatedPigeonhole(cardHolders)
      rePigeonhole = rePigeonhole || player4.negativeIsolatedPigeonhole(cardHolders)

      // analyze multi-player pigeonholing.
      rePigeonhole = rePigeonhole || pairPigeonhole(player2, player3, player4)
      rePigeonhole = rePigeonhole || pairPigeonhole(player3, player4, player2)
      rePigeonhole = rePigeonhole || pairPigeonhole(player4, player2, player3)
      triadPigeonhole()
      rePigeonhole = rePigeonhole || allPlayerTypedPigeonholing(Card.Type.WEAPON)
      rePigeonhole = rePigeonhole || allPlayerTypedPigeonholing(Card.Type.ROOM)
      rePigeonhole = rePigeonhole || allPlayerTypedPigeonholing(Card.Type.PERSON)

      return rePigeonhole
   }

   // In pair pigeonholing, we look for a 2-card group that is in 2 players' `hasOneOfSet`s.
   private fun pairPigeonhole(hand1: CompetitorHand, hand2: CompetitorHand, otherHand: CompetitorHand): Boolean {
      for (group in hand1.hasOneGroups) {
         if (group.size == 2 && !cardHolders.contains(group.first()) && hand2.hasOneGroups.contains(group)) {
            printDebug("twoWayPigeonhole: ${hand1.playerNo} + ${hand2.playerNo} - $group")
            for (card in group) {
               otherHand.reduce(card)
            }
            assignToMultiHand(hand1, hand2, group)
            return true
         }
      }
      return false
   }

   // For a 3-way pigeonhole, all players must have a group containing the same 2 or 3 cards.
   private fun triadPigeonhole() {
      outer@ for (group2 in player2.hasOneGroups) {
         if (cardHolders.contains(group2.firstOrNull())) {
            for (group3 in player3.hasOneGroups) {
               val merge = group2 + group3
               if (merge.size <= 3) {
                  for (group4 in player4.hasOneGroups) {
                     if (group3.isEmpty() || group4.isEmpty())
                        continue
                     val merge = merge + group4
                     if (merge.size == 3) {
                        printDebug("threeWayPigeonhole: $merge")
                        assignToMultiHand(player2, group2)
                        assignToMultiHand(player3, group3)
                        assignToMultiHand(player4, group4)
                        continue@outer
                     }
                  }
               }
            }
         }
      }
   }

   // In all-player typed pigeonholing, we use card type analysis (one card of each type is in the envelope) on
   // all opponents. Currently, I have come up with an analysis for pairs only.
   // NOTE: negativeIsolatedPigeonholing is a prerequisite for this to work correctly.
   // There's no need to do individuals: typed pigeonholing, when isolated, would be deduced by negativeIsolatedPigeonhole.
   private fun allPlayerTypedPigeonholing(type: Card.Type): Boolean {
      val numberOfUnidentifiedTypeCardsInCompetitorHands = Card.cardsOfType(type).count {
         !cardHolders.contains(it)
      } - 1
      if (numberOfUnidentifiedTypeCardsInCompetitorHands == 0)
         return false

      val p2CardsOfTypeMentionedInHasOneGroups = cardsOfTypeMentionedInHasOneGroups(player2, type)
      val p3CardsOfTypeMentionedInHasOneGroups = cardsOfTypeMentionedInHasOneGroups(player3, type)
      val p4CardsOfTypeMentionedInHasOneGroups = cardsOfTypeMentionedInHasOneGroups(player4, type)

      // Consider pairs. We must know that the third competitor provably doesn't have any of the unidentified cards.
      fun pairTypedPigeonholing(
         playerA: CompetitorHand,
         playerB: CompetitorHand,
         playerACardsOfType: EnumSet<Card>,
         playerBCardsOfType: EnumSet<Card>,
         otherHand: CompetitorHand
      ): Boolean {
         if (Card.cardsOfType(type).all { cardHolders.contains(it) || otherHand.doesntHave(it) }) {
            val union = playerACardsOfType.union(playerBCardsOfType)
            if (union.size == numberOfUnidentifiedTypeCardsInCompetitorHands) {
               if (playerA.isSaturated() && playerB.isSaturated()) {
                  printDebug("allPlayerTypedPigeonholing: players ${playerA.playerNo} + ${playerB.playerNo}: deduced type $type")
                  // the cards that p2 mentions but p3 doesn't are held by p2; symmetric for p3; mentioned
                  // by both are held by multiHand
                  for (card in union) {
                     if (!playerBCardsOfType.contains(card))
                        playerA.holding(card)
                     else if (!playerACardsOfType.contains(card))
                        playerB.holding(card)
                     else {
                        // TODO: adding to multiHand is tricky. This results in potential incorrect cases
                        cardHolders[card] = multiHand
                        multiHand.holding(card)
                     }
                  }
                  return true
               }
            }
         }
         return false
      }

      if (pairTypedPigeonholing(player2, player3, p2CardsOfTypeMentionedInHasOneGroups, p3CardsOfTypeMentionedInHasOneGroups, player4))
         return true
      if (pairTypedPigeonholing(player3, player4, p3CardsOfTypeMentionedInHasOneGroups, p4CardsOfTypeMentionedInHasOneGroups, player2))
         return true
      if (pairTypedPigeonholing(player4, player2, p4CardsOfTypeMentionedInHasOneGroups, p2CardsOfTypeMentionedInHasOneGroups, player3))
         return true

      // Is triad typed pigeonholing possible? I tried to construct a scenario, but couldn't construct one that didn't fall into
      // an existing analysis and could lead to deduction of any card in the envelope.
      return false
   }

   private fun cardsOfTypeMentionedInHasOneGroups(hand: CompetitorHand, type: Card.Type): EnumSet<Card> {
      val result = EnumSet.noneOf(Card::class.java)
      for (group in hand.hasOneGroups) {
         // each group can only have one card of a type
         group.firstOrNull { it.type == type }
            ?.let { card -> result.add(card) }
      }
      return result
   }

   /**
    * Positive elimination is the determination that all of the cards of a type except for one have been proven to be
    * held, so the other one must be in the envelope.
    * Positive elimination should be repeated if a card is identified, since the determination might simplify a hand's
    * `hasOneOfSet` items.
    */
   private fun positiveElimination(): Boolean {
      if (deducedPerson == null && positiveElimination(Card.Type.PERSON))
         return true
      if (deducedWeapon == null && positiveElimination(Card.Type.WEAPON))
         return true
      if (deducedRoom == null && positiveElimination(Card.Type.ROOM))
         return true
      return false
   }

   private fun positiveElimination(type: Card.Type): Boolean {
      var unaccountedCard: Card? = null
      var numberOfUnaccountedCards = 0
      for (cardOfType in Card.cardsOfType(type)) {
         if (!cardHolders.contains(cardOfType)) {
            unaccountedCard = cardOfType
            numberOfUnaccountedCards += 1
         }
      }
      if (numberOfUnaccountedCards == 1) {
         printDebug("positiveElimination $type $unaccountedCard")
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

   /**
    * Negative elimination is the determination that there is some card we have proven that no hands hold. So it
    * must be in the envelope. It does not need to be repeated if successful. This is basically just a shortcut compared
    * to the exhaustive search.
    */
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

   private fun negativeElimination(card: Card) {
      when (card.type) {
         Card.Type.PERSON ->
            if (
               player2.doesntHave(card) &&
               player3.doesntHave(card) &&
               player4.doesntHave(card) &&
               !myHand.cardsHas.contains(card)
            ) {
               printDebug("negativeElimination deducedPerson=$card")
               deducedPerson = card
            }
         Card.Type.WEAPON ->
            if (
               player2.doesntHave(card) &&
               player3.doesntHave(card) &&
               player4.doesntHave(card) &&
               !myHand.cardsHas.contains(card)
            ) {
               printDebug("negativeElimination deducedWeapon=$card")
               deducedWeapon = card
            }
         Card.Type.ROOM ->
            if (
               player2.doesntHave(card) &&
               player3.doesntHave(card) &&
               player4.doesntHave(card) &&
               !myHand.cardsHas.contains(card)
            ) {
               printDebug("negativeElimination deducedRoom=$card")
               deducedRoom = card
            }
      }
   }

   /**
    * As I have said, since I couldn't come up with any pigeonholing that works across all 3 opponents, there are some
    * game transcripts for which it is possible to deduce something, yet my simple analyses haven't picked it up.
    * Lemma: Assume there is no negative knowledge (cardsDoesntHave is empty for all hands). Then, if there is only
    * one unidentified card in the hands, and that slot is unconstrained, you cannot deduce it. Miniproof: that slot
    * can be one of 2 cards, and there is no way to distinguish which is held and which is in the envelope.
    * More generally, the 'slack' – the difference between the number of unidentified cards and unconstrained slots -
    * can be at most 2 in order to deduce some card in the envelope. If one card in the envelope is deduced, the difference
    * can be at most 1; if two in the envelope are deduced, slack must be zero to deduce the last card.
    * P.S. by 'unconstrained' I mean that the number of cards required to satisfy a player's remaining `hasOneOf` groups
    * (the minimum cover) is smaller than the number of slots in the player's hand.
    * Because of the presence of cardsDoesntHave, I have to fudge the slots math a bit. What I came up with below is
    *  close to the lowest unconstrained slot values that will pass on Kattis.
    */
   private fun lastDitchEffort() {
      // reset multiHand for the last ditch effort
      for (pair in multiHand.holderHasOneOfs) {
         val holder = pair.first
         holder.readdMultiHandGroup(pair.second)
      }
      for (multiHandCard in multiHand.heldCards) {
         if (cardHolders[multiHandCard] == multiHand)
            cardHolders.remove(multiHandCard)
      }

      hardAssert(cardHolders.size == myHand.cardsHas.size + player2.knownCards + player3.knownCards + player4.knownCards)

      var allowedUnconstrainedSlots = 4
      if (deducedPerson != null)
         allowedUnconstrainedSlots -= 2
      if (deducedWeapon != null)
         allowedUnconstrainedSlots -= 2
      if (deducedRoom != null)
         allowedUnconstrainedSlots -= 2

      var unconstrainedSlots = if (player2.isDeduced) 0 else player2.numberOfSlots - player2.minimumCoverSize()
      unconstrainedSlots += if (player3.isDeduced) 0 else player3.numberOfSlots - player3.minimumCoverSize()
      unconstrainedSlots += if (player4.isDeduced) 0 else player4.numberOfSlots - player4.minimumCoverSize()

      if (player2.numberOfUsefulCardsDoesntHave(cardHolders) >= 2*player2.numberOfSlots)
         unconstrainedSlots -= 1
      if (player3.numberOfUsefulCardsDoesntHave(cardHolders) >= 2*player3.numberOfSlots)
         unconstrainedSlots -= 1
      if (player4.numberOfUsefulCardsDoesntHave(cardHolders) >= 2*player4.numberOfSlots)
         unconstrainedSlots -= 1

      if (unconstrainedSlots <= allowedUnconstrainedSlots) {
         printDebug("lastDitchEffort: work it")
         exhaustiveSearch()
      }
   }

   /**
    * In the exhaustive search, we take all unassigned cards, and loop through all the combinations that the envelope
    * could hold. If we can fill the hands with the remaining unassigned cards, we note the person, weapon, and room
    * that were in the envelope. At the end, we check to see if there was just one card for each type that led to
    * satisfying the hands when it was in the envelope: that would mean that card must be in the envelope, regardless
    * of what cards actually fill the opponents' unidentified slots.
    */
   private fun exhaustiveSearch() {
      val players = arrayOf(player2, player3, player4)
      val isCardUnheld = { card: Card -> !cardHolders.contains(card) }
      val remainingPersons = Card.setOf(Card.cardsOfType(Card.Type.PERSON).filter(isCardUnheld))
      val remainingWeapons = Card.setOf(Card.cardsOfType(Card.Type.WEAPON).filter(isCardUnheld))
      val remainingRooms = Card.setOf(Card.cardsOfType(Card.Type.ROOM).filter(isCardUnheld))
      val personMatches = EnumSet.noneOf(Card::class.java)
      val weaponMatches = EnumSet.noneOf(Card::class.java)
      val roomMatches = EnumSet.noneOf(Card::class.java)
      val p3SatisfyCache = HashMap<EnumSet<Card>, Boolean>()
      val p4SatisfyCache = HashMap<EnumSet<Card>, Boolean>()
      for (envelopePerson in deducedPerson?.let { listOf(it) } ?: remainingPersons) {
         for (envelopeRoom in deducedRoom?.let { listOf(it) } ?: remainingRooms) {
            for (envelopeWeapon in deducedWeapon?.let { listOf(it) } ?: remainingWeapons) {
               val remainingCards = EnumSet.copyOf(remainingPersons)
               remainingCards.addAll(remainingWeapons)
               remainingCards.addAll(remainingRooms)
               remainingCards.remove(envelopePerson)
               remainingCards.remove(envelopeWeapon)
               remainingCards.remove(envelopeRoom)
               if (satisfyHands(players, remainingCards, p3SatisfyCache, p4SatisfyCache)) {
                  printDebug("exhaustiveSearch: match: $envelopePerson $envelopeWeapon $envelopeRoom")
                  personMatches.add(envelopePerson)
                  weaponMatches.add(envelopeWeapon)
                  roomMatches.add(envelopeRoom)
               }
            }
         }
      }
      if (deducedPerson == null && personMatches.size == 1)
         deducedPerson = personMatches.first()
      if (deducedWeapon == null && weaponMatches.size == 1)
         deducedWeapon = weaponMatches.first()
      if (deducedRoom == null && roomMatches.size == 1)
         deducedRoom = roomMatches.first()
   }

   /**
    * The tedious task of determining whether players 2, 3, and 4 could between them hold `remainingCards` in their
    * unidentified slots. I just added a memoization cache, which improved the runtime from 0.91 to 0.43 seconds
    * on Kattis.
    * @param remainingCards The cards that need to be allocated to the opponents' open slots.
    * @param p3satisfyCache The cache to be passed in (initially empty) for player 3
    * @param p4satisfyCache The cache to be passed in (initially empty) for player 4
    * @return True iff the cards COULD be in opponents' hands
    */
   private fun satisfyHands(
      players: Array<CompetitorHand>,
      remainingCards: EnumSet<Card>,
      p3satisfyCache: HashMap<EnumSet<Card>, Boolean>,
      p4satisfyCache: HashMap<EnumSet<Card>, Boolean>,
      index: Int = 0
   ): Boolean {
      if (index == players.size) {
         hardAssert(remainingCards.isEmpty())
         return true
      }
      val player = players[index]

      if (player.playerNo == 3) {
         p3satisfyCache[remainingCards]?.let { return it }
      } else if (player.playerNo == 4) {
         p4satisfyCache[remainingCards]?.let { return it }
      }

      val playerAssignments = ArrayList<Card>(player.numberOfSlots)
      var satisfied = false

      fun fillPlayerAny() {
         if (playerAssignments.size == player.numberOfSlots) {
            if (satisfyHands(players, remainingCards, p3satisfyCache, p4satisfyCache, index + 1)) {
               satisfied = true
            }
            return
         }
         for (card in remainingCards) {
            if (player.doesntHave(card))
               continue
            // Interesting fact: RegularEnumSets are safe to modify during iteration, the iterator copies the set.
            // Modifications are not visible to the iterator
            playerAssignments.add(card)
            remainingCards.remove(card)
            fillPlayerAny()
            remainingCards.add(card)
            if (satisfied)
               return
            playerAssignments.removeLast()
         }
      }

      fun fillPlayer(remainingHasOneGroups: List<EnumSet<Card>>) {
         // fulfill a hasOneGroup if there are any
         if (remainingHasOneGroups.isNotEmpty()) {
            if (playerAssignments.size == player.numberOfSlots)
               return
            for (card in remainingHasOneGroups.first()) {
               if (!remainingCards.contains(card))
                  continue
               remainingCards.remove(card)
               playerAssignments.add(card)
               fillPlayer(remainingHasOneGroups.filterNot { it.contains(card) })
               remainingCards.add(card)
               if (satisfied)
                  return
               playerAssignments.removeLast()
            }
         } else {
            // select a card from remainingCards
            fillPlayerAny()
         }
      }

      fillPlayer(player.hasOneGroups.toMutableList())
      when (player.playerNo) {
         3 -> p3satisfyCache[EnumSet.copyOf(remainingCards)] = satisfied
         4 -> p4satisfyCache[EnumSet.copyOf(remainingCards)] = satisfied
         else -> Unit
      }
      return satisfied
   }

   private fun assignToMultiHand(hand1: CompetitorHand, group: EnumSet<Card>) {
      multiHand.holderHasOneOfs.add(Pair(hand1, group))
      hand1.removeMultiHandGroup(group)
      for (card in group) {
         cardHolders[card] = multiHand
         multiHand.holding(card)
      }
   }

   private fun assignToMultiHand(hand1: CompetitorHand, hand2: CompetitorHand, group: EnumSet<Card>) {
      multiHand.holderHasOneOfs.add(Pair(hand1, group))
      multiHand.holderHasOneOfs.add(Pair(hand2, group))
      hand1.removeMultiHandGroup(group)
      hand2.removeMultiHandGroup(group)
      for (card in group) {
         cardHolders[card] = multiHand
         multiHand.holding(card)
      }
   }
}

/**
 * Used in pigeonholing, determine whether the minisets of cards could be satisfied with not less than `coverSize`.
 * @return True iff the `groups` cannot be satisfied by choosing fewer than `coverSize` cards.
 */
fun doesCoverRequire(groups: Collection<EnumSet<Card>>, coverSize: Int): Boolean {
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
         val groupsWithoutCard = groups.filterNot { it.contains(card) }
         if (!doesCoverRequire(groupsWithoutCard, coverSize - 1))
            return false
      }
   }
   return true
}

/**
 * Used in pigeonholing, determine whether it is possible to satisfy the minisets of cards with no more than
 * `maxCoverSize` cards.
 * @return True iff you can satisfy `groups` by choosing `maxCoverSize` cards, or maybe fewer.
 */
fun doesCoverExist(groups: Collection<EnumSet<Card>>, maxCoverSize: Int): Boolean {
   if (maxCoverSize < 0)
      return false
   val triedCards = EnumSet.noneOf(Card::class.java)
   for (group in groups) {
      for (card in group) {
         // try this card
         if (triedCards.contains(card))
            continue
         triedCards.add(card)
         // get the subset of groups that doesn't have this card
         val groupsWithoutCard = groups.filterNot { it.contains(card) }
         if (doesCoverExist(groupsWithoutCard, maxCoverSize - 1))
            return true
      }
   }
   return groups.isEmpty()
}

// Read/write from standard filehandles
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

fun output(card: Card?, os: OutputStream) {
   val char = card?.name?.first() ?: '?'
   os.write(char.code)
}

fun main() {
   ClueGameIO(System.`in`, System.out)
}