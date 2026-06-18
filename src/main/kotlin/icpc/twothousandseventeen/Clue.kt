package icpc.twothousandseventeen

import util.hardAssert
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.max

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
   }
}

typealias Suggestion = Triple<Card, Card, Card>
typealias Deduction = Triple<Card?, Card?, Card?>

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
   open fun holding(card: Card) {}
}

class MyHand(cards: Array<Card>) : Hand() {
   val cardsHas = EnumSet.of(cards[0], cards[1], cards[2], cards[3], cards[4])
}

class CompetitorHand(val numberOfCards: Int, val game: ClueGame, val playerNo: Int) : Hand() {
   private val cardsHas = EnumSet.noneOf(Card::class.java)
   private var cardsHasInMultiHand = 0
   private val hasOneOf = mutableListOf<EnumSet<Card>>()
   private val cardsDoesntHave = EnumSet.noneOf(Card::class.java)

   val isDeduced: Boolean
      get() = cardsHas.size == numberOfCards

   val hasOneGroups: List<EnumSet<Card>>
      get() = hasOneOf

   val numberOfSlots: Int
      get() = numberOfCards - cardsHas.size - cardsHasInMultiHand

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
      // if the hand already holds one of the cards, the suggestion is useless
      if (cardsHas.contains(suggestion.first) || cardsHas.contains(suggestion.first) || cardsHas.contains(suggestion.first)) {
         printDebug("presentedForSuggestion: player $playerNo: suggestion useless")
         return
      }
      val hasOneOf_ = EnumSet.of(suggestion.first, suggestion.second, suggestion.third)
      // remove the card if we know the holder, or we know this hand doesn't hold it
      if (cardHolders[suggestion.first] != null || cardsDoesntHave.contains(suggestion.first))
         hasOneOf_.remove(suggestion.first)
      if (cardHolders[suggestion.second] != null  || cardsDoesntHave.contains(suggestion.second))
         hasOneOf_.remove(suggestion.second)
      if (cardHolders[suggestion.third] != null  || cardsDoesntHave.contains(suggestion.third))
         hasOneOf_.remove(suggestion.third)
      hardAssert(hasOneOf_.isNotEmpty())

      printDebug("presentedForSuggestion: hand $this, hasOneOf_ = $hasOneOf_")
      if (hasOneOf_.size == 1) {
         holding(hasOneOf_.first())
      } else if (!isDeduced) {
         hasOneOf.add(hasOneOf_)
      }
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
      val hasOfSet = hasOneOf.toHashSet()
      hasOfSet.remove(EMPTY_CARD_SET)

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
         printDebug("isolatedPigeonhole: player $playerNo: maxOccurences = $maxOccurrences")
         if (maxOccurrences < 2)
            return false

         for (entry in groupOccurrences.entries.filter { it.value == maxOccurrences }) {
            // remove groups containing this card, and check the cover size must be = numberOfSlots - 1
            val card = entry.key
            printDebug("isolatedPigeonhole: Trying $card")
            val groupsWithoutCard = hasOfSet.filter { !it.contains(card) }
            if (doesCoverRequire(groupsWithoutCard.toHashSet(), numberOfSlots - 1)) {
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

   fun negativeIsolatedPigeonhole(cardHolders: Map<Card, Hand>) {
      val hasOfSet = hasOneOf.toHashSet()
      hasOfSet.remove(EMPTY_CARD_SET)

      if (isSaturated(hasOfSet)) {
         printDebug("negativeIsolatedPigeonhole: player $playerNo: cover requires all slots")
         // add all cards not owned and not in hasOneOf's to cardsDoesntHave
         val flatHasOfSet = EnumSet.noneOf(Card::class.java)
         for (hasOf in hasOfSet) {
            flatHasOfSet.addAll(hasOf)
         }
         for (card in Card.entries) {
            if (cardHolders[card] == null && !flatHasOfSet.contains(card)) {
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
                  val groupsWithoutCard = hasOfSet.filter { !it.contains(card) }
                  if (!doesCoverExist(groupsWithoutCard.toHashSet(), numberOfSlots - 1)) {
                     printDebug("negativeIsolatedPigeonhole doesn't have $card [2]")
                     newDoesntHave.add(card)
                     cardsDoesntHave.add(card)
                  }
               }
            }
         }
         if (newDoesntHave.isNotEmpty()) {
            for (group in hasOneOf) {
               group.removeAll(newDoesntHave)
            }
         }

         // we've done work pigeonholing, so might as well write it back to `hasOneOf`
         hasOneOf.clear()
         hasOneOf.addAll(hasOfSet)
         hardAssert(hasOneOf.size == hasOfSet.size)
      }
   }

   fun isSaturated(): Boolean {
      val hasOneOfSet = hasOneOf.toHashSet()
      hasOneOfSet.remove(EMPTY_CARD_SET)
      return isSaturated(hasOneOfSet)
   }

   fun isSaturated(hasOneOfSet: Set<EnumSet<Card>>): Boolean =
      doesCoverRequire(hasOneOfSet, numberOfSlots)

   fun doesntHave(card: Card): Boolean {
      return if (isDeduced) !cardsHas.contains(card) else cardsDoesntHave.contains(card)
   }

   override fun holding(card: Card) {
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

   fun addMultiHandGroup(group: EnumSet<Card>) {
      cardsHasInMultiHand += 1
      hasOneOf.remove(group)
   }

   override fun toString(): String {
      return "CompetitorHand(playerNo=$playerNo)"
   }

   companion object {
      val EMPTY_CARD_SET = EnumSet.noneOf(Card::class.java)
   }

}

class MultiCompetitorHand : Hand()

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
         printDebug("examination $examination")
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
      do {} while (positiveElimination())

      // apply negative elimination for all cards
      negativeElimination()

      return Deduction(deducedPerson, deducedWeapon, deducedRoom)
   }

   private fun applyPigeonholes(isRepeat: Boolean): Boolean {
      var rePigeonhole = false
      rePigeonhole = rePigeonhole || player2.isolatedPigeonhole()
      rePigeonhole = rePigeonhole || player3.isolatedPigeonhole()
      rePigeonhole = rePigeonhole || player4.isolatedPigeonhole()

      // negative pigeonholing
      player2.negativeIsolatedPigeonhole(cardHolders)
      player3.negativeIsolatedPigeonhole(cardHolders)
      player4.negativeIsolatedPigeonhole(cardHolders)

      // analyze multi-player pigeonholing.
      rePigeonhole = rePigeonhole || pairPigeonhole(player2, player3, player4)
      rePigeonhole = rePigeonhole || pairPigeonhole(player3, player4, player2)
      rePigeonhole = rePigeonhole || pairPigeonhole(player4, player2, player3)
      triadPigeonhole()
      rePigeonhole = rePigeonhole || allPlayerTypedPigeonholing(Card.Type.WEAPON)
      rePigeonhole = rePigeonhole || allPlayerTypedPigeonholing(Card.Type.ROOM)
      rePigeonhole = rePigeonhole || allPlayerTypedPigeonholing(Card.Type.PERSON)
      // curious
//      hardAssert(!(isRepeat && rePigeonhole))
      return rePigeonhole
   }

   private fun pairPigeonhole(hand1: CompetitorHand, hand2: CompetitorHand, otherHand: CompetitorHand): Boolean {
      for (group in hand1.hasOneGroups) {
         if (group.size == 2 && cardHolders[group.first()] == null && hand2.hasOneGroups.contains(group)) {
            printDebug("twoWayPigeonhole: ${hand1.playerNo} + ${hand2.playerNo} - $group")
            for (card in group) {
               cardHolders[card] = multiHand
               otherHand.reduce(card)
            }
            // remove the group from both hands
            hand1.addMultiHandGroup(group)
            hand2.addMultiHandGroup(group)
            return true
         }
      }
      return false
   }

   private fun triadPigeonhole() {
      // For a 3-way hole, all players must have a group containing the same 2 or 3 cards.
      outer@ for (group2 in player2.hasOneGroups) {
         if (cardHolders[group2.first()] != null) {
            for (group3 in player3.hasOneGroups) {
               val merge = group2 + group3
               if (merge.size <= 3) {
                  for (group4 in player4.hasOneGroups) {
                     val merge = merge + group4
                     if (merge.size <= 3) {
                        printDebug("threeWayPigeonhole: $merge")
                        for (card in merge) {
                           cardHolders[card] = multiHand
                        }
                        player2.addMultiHandGroup(group2)
                        player3.addMultiHandGroup(group3)
                        player4.addMultiHandGroup(group4)
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
         cardHolders[it] == null
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
         if (Card.cardsOfType(type).all { cardHolders[it] != null || otherHand.doesntHave(it) }) {
            val union = playerACardsOfType.union(playerBCardsOfType)
            if (union.size == numberOfUnidentifiedTypeCardsInCompetitorHands) {
               if (playerA.isSaturated() && playerB.isSaturated()) {
                  printDebug("allPlayerTypedPigeonholing: players ${playerA.playerNo + playerB.playerNo}: deduced type $type")
                  // the cards that p2 mentions but p3 doesn't are held by p2; symmetric for p3; mentioned
                  // by both are held by multiHand
                  for (card in union) {
                     if (!playerBCardsOfType.contains(card))
                        playerA.holding(card)
                     else if (!playerACardsOfType.contains(card))
                        playerB.holding(card)
                     else
                        cardHolders[card] = multiHand
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
         if (cardHolders[cardOfType] == null) {
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
}

fun doesCoverRequire(groups: Set<EnumSet<Card>>, coverSize: Int): Boolean {
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

fun doesCoverExist(groups: Set<EnumSet<Card>>, maxCoverSize: Int): Boolean {
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
         val groupsWithoutCard = groups.filter { !it.contains(card) }
         if (doesCoverExist(groupsWithoutCard.toHashSet(), maxCoverSize - 1))
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