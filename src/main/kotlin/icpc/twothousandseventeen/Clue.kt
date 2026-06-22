package icpc.twothousandseventeen

import util.hardAssert
import util.union
import java.io.InputStream
import java.io.OutputStream
import java.util.*
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

typealias Suggestion = Triple<Card, Card, Card>
typealias Deduction = Triple<Card?, Card?, Card?>

class Examination(val suggestion: Suggestion, val numberOfPasses: Short, val presentedCard: Card? = null) {
   override fun toString(): String {
      return "Examination(suggestion=$suggestion, numberOfPasses=$numberOfPasses, presentedCard=$presentedCard)"
   }
}

abstract class Hand {
   open fun couldntPresentForSuggestion(suggestion: Suggestion) {}
   open fun presentedForSuggestion(suggestion: Suggestion, cardHolders: Map<Card, Hand>) {}
   open fun holding(card: Card) {}
}

class MyHand(cards: Array<Card>) : Hand() {
   val cardsHas = EnumSet.of(cards[0], cards[1], cards[2], cards[3], cards[4])
}

class CompetitorHand(val numberOfCards: Int, val game: ClueGame, val playerNo: Int) : Hand() {
   private val cardsHas = EnumSet.noneOf(Card::class.java)
   private var hasOneOfSet = hashSetOf<EnumSet<Card>>()
   private val cardsDoesntHave = EnumSet.noneOf(Card::class.java)
   private var cardsReservedForMultiHand = 0

   val isDeduced: Boolean
      get() = cardsHas.size == numberOfCards

   val hasOneGroups: Collection<EnumSet<Card>>
      get() = hasOneOfSet

   val numberOfSlots: Int
      get() = numberOfCards - cardsHas.size - cardsReservedForMultiHand

   override fun couldntPresentForSuggestion(suggestion: Suggestion) {
      cardsDoesntHave.add(suggestion.first)
      cardsDoesntHave.add(suggestion.second)
      cardsDoesntHave.add(suggestion.third)
      // We can reduce these cards from `hasOneOf` groups
      reduce(suggestion.first)
      reduce(suggestion.second)
      reduce(suggestion.third)
   }

   val knownCards: Int
      get() = cardsHas.size

   override fun presentedForSuggestion(suggestion: Suggestion, cardHolders: Map<Card, Hand>) {
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
      if (hasOneOf.size == 1) {
         holding(hasOneOf.first())
      } else if (!isDeduced) {
         hasOneOfSet.add(hasOneOf)
      }
   }

   /**
    * Remove a specific card from `hasOneOf` groups. For each group that turns into a singleton, we can conclude
    * this hand is the holder of the remaining element.
    */
   fun reduce(card: Card) {
      // We have to play a bit of a game here in that this function needs to be reentrant-safe with respect to
      // hasOneOfSet, and also we can't mutate a group while it's in the (Hash)set. First identify held cards
      // in one pass (in a very perverse case, I think you can deduce all 5 cards of player 2) or update a group of 3 to a
      // group of 2, then call `holding` for the deduced held cards.
      val deducedHolding = ArrayList<Card>(4)
      for (group in hasOneOfSet.toList()) {
         if (group.contains(card)) {
            // don't mutate while in the HashSet
            hasOneOfSet.remove(group)
            group.remove(card)
            hasOneOfSet.add(group)
            if (group.size == 1) {
               val identified = group.first()
               deducedHolding.add(identified)
            }
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
    * without this card with the constraint of the # of slots.
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
         for (card in Card.entries) {
            if (!cardHolders.contains(card) && !flatHasOfSet.contains(card)) {
               printDebug("negativeIsolatedPigeonhole doesn't have $card")
               cardsDoesntHave.add(card)
            }
         }

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

   fun isSaturated(): Boolean =
      doesCoverRequire(hasOneOfSet, numberOfSlots)

   fun minimumCoverSize(): Int {
      var size = 1
      while (doesCoverRequire(hasOneOfSet, size)) {
         hardAssert(size < 6)
         size += 1
      }
      return size - 1
   }

   fun doesntHave(card: Card): Boolean {
      return if (isDeduced) !cardsHas.contains(card) else cardsDoesntHave.contains(card)
   }

   override fun holding(card: Card) {
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
      // ClueGame should reduce the card from groups in other competitors
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

class MultiCompetitorHand : Hand() {
   val holderHasOneOfs = mutableListOf<Pair<CompetitorHand, EnumSet<Card>>>()
   val heldCards = EnumSet.noneOf(Card::class.java)
   override fun holding(card: Card) {
      heldCards.add(card)
   }
}

const val STARTING_PLAYER: Short = 1
const val NUMBER_OF_PLAYERS: Short = 4

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

   // We might be able to deduce the missing card of the type whose holder was just identified
   // if there is only one unaccounted for remaining.
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
      while (applyPigeonholes(isRepigeonholing))
         isRepigeonholing = true

      // apply positive elimination
      while (positiveElimination()) {}

      // apply negative elimination for all cards
      negativeElimination()

      // Finally - the last ditch effort :)
      // If all competitor's slots are saturated, it's easy to shuffle through all the possibilities
      // of each hand and see if there's a card that always ends up in the 'envelope'.
      lastDitchEffort()

      return Deduction(deducedPerson, deducedWeapon, deducedRoom)
   }

   private fun applyPigeonholes(isRepeat: Boolean): Boolean {
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

   private fun triadPigeonhole() {
      // For a 3-way hole, all players must have a group containing the same 2 or 3 cards.
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

   private fun allPlayerTypedPigeonholing(type: Card.Type): Boolean {
      // NOTE: negativeIsolatedPigeonholing is a prerequisite for this to work correctly
      val numberOfUnidentifiedTypeCardsInCompetitorHands = Card.cardsOfType(type).count {
         !cardHolders.contains(it)
      } - 1
      if (numberOfUnidentifiedTypeCardsInCompetitorHands == 0)
         return false

      val p2CardsOfTypeMentionedInHasOneGroups = cardsOfTypeMentionedInHasOneGroups(player2, type)
      val p3CardsOfTypeMentionedInHasOneGroups = cardsOfTypeMentionedInHasOneGroups(player3, type)
      val p4CardsOfTypeMentionedInHasOneGroups = cardsOfTypeMentionedInHasOneGroups(player4, type)

      // There's no need to do individuals: typed pigeonholing, when isolated, would be deduced by negativeIsolatedPigeonhole.
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
      // an existing analysis and could lead to deduction of a type.
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
    * Lemma: Assume there is no negative knowledge (cardsDoesntHave is empty for all hands). Then, if there is only
    * one unidentified card in the hands, and that slot is unconstrained, you cannot deduce it. Miniproof: that slot
    * can be one of 2 cards, and there is no way to distinguish which is held and which is in the envelope.
    * More generally, the 'slack' – the difference between the number of unidentified cards and unconstrained slots -
    * can be at most 2 in order to deduce some card in the envelope. If one card in the envelope is deduced, the difference
    * can be at most 1; if two in the envelope are deduced, slack must be zero to deduce the last card.
    * P.S. by 'unconstrained' I mean that the number of cards required to satisfy a player's remaining `hasOneOf` groups
    * (the minimum cover) is smaller than the number of slots in the player's hand.
    * Because of the presence of cardsDoesntHave, I have to fudge the slots math a bit. What I cam up with below is
    *  close to the lowest unconstrained slots values that will pass on Kattis.
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

   private fun exhaustiveSearch() {
      val players = arrayOf(player2, player3, player4)
      val isCardUnheld = { card: Card -> !cardHolders.contains(card) }
      val remainingPersons = Card.setOf(Card.cardsOfType(Card.Type.PERSON).filter(isCardUnheld))
      val remainingWeapons = Card.setOf(Card.cardsOfType(Card.Type.WEAPON).filter(isCardUnheld))
      val remainingRooms = Card.setOf(Card.cardsOfType(Card.Type.ROOM).filter(isCardUnheld))
      val personMatches = EnumSet.noneOf(Card::class.java)
      val weaponMatches = EnumSet.noneOf(Card::class.java)
      val roomMatches = EnumSet.noneOf(Card::class.java)
      for (envelopePerson in deducedPerson?.let { listOf(it) } ?: remainingPersons) {
         for (envelopeRoom in deducedRoom?.let { listOf(it) } ?: remainingRooms) {
            for (envelopeWeapon in deducedWeapon?.let { listOf(it) } ?: remainingWeapons) {
               val remainingCards = EnumSet.copyOf(remainingPersons)
               remainingCards.addAll(remainingWeapons)
               remainingCards.addAll(remainingRooms)
               remainingCards.remove(envelopePerson)
               remainingCards.remove(envelopeWeapon)
               remainingCards.remove(envelopeRoom)
               if (satisfyHands(players, remainingCards)) {
                  printDebug("exhaustiveSearch: match: $envelopePerson $envelopeWeapon $envelopeRoom")
                  personMatches.add(envelopePerson)
                  weaponMatches.add(envelopeWeapon)
                  roomMatches.add(envelopeRoom)
               }
            }
         }
      }
      if (deducedPerson == null && personMatches.size == 1) {
         deducedPerson = personMatches.first()
         printDebug("exhaustiveSearch: person unique: $deducedPerson")
      }
      if (deducedWeapon == null && weaponMatches.size == 1)
         deducedWeapon = weaponMatches.first()
      if (deducedRoom == null && roomMatches.size == 1)
         deducedRoom = roomMatches.first()
   }

   private fun satisfyHands(players: Array<CompetitorHand>, remainingCards: EnumSet<Card>, index: Int = 0): Boolean {
      if (index == players.size) {
         hardAssert(remainingCards.isEmpty())
         return true
      }

      val player = players[index]
      val playerAssignments = ArrayList<Card>(player.numberOfSlots)
      var satisfied = false

      fun fillPlayerAny() {
         if (playerAssignments.size == player.numberOfSlots) {
            if (satisfyHands(players, remainingCards, index + 1)) {
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
            if (satisfied)
               return
            playerAssignments.removeLast()
            remainingCards.add(card)
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
               if (satisfied)
                  return
               playerAssignments.removeLast()
               remainingCards.add(card)
            }
         } else {
            // select a card from remainingCards
            fillPlayerAny()
         }
      }

      fillPlayer(player.hasOneGroups.toMutableList())
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