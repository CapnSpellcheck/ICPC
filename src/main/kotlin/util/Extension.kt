package util

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


inline fun <T> Iterable<T>.allIndexed(predicate: (Int, T) -> Boolean): Boolean {
   if (this is Collection && isEmpty()) return true
   this.forEachIndexed { index, t ->  if (!predicate(index, t)) return false }
   return true
}

/**
 * [x1, x2] is ordered with [y1, y2] as thus:
 * if x1 != y1, the smaller of x1, y1 identifies the lesser interval.
 * Otherwise, the smaller of x2, y2 determines the lesser interval.
 * If x1 == y1 and x2 == y2, the intervals compare as 0.
 */
fun IntRange.compareTo(other: IntRange): Int {
   val startComparison = this.first.compareTo(other.first)
   if (startComparison != 0)
      return startComparison
   return this.last.compareTo(other.last)
}

inline fun IntRange.intersects(other: IntRange): Boolean {
   return this.first <= other.last && this.last >= other.first
}

inline fun IntRange.intersection(other: IntRange): IntRange =
   IntRange(max(this.first, other.first), min(this.last, other.last))

inline fun IntRange.subtracting(other: IntRange): Pair<IntRange?, IntRange?> {
   val intersection = intersection(other)
   if (intersection.isEmpty() || intersection == this)
      return Pair<IntRange?, IntRange?>(null, null)
   val first = IntRange(this.first, intersection.first - 1)
   val second = IntRange(intersection.last + 1, this.last)
   return Pair(if (first.isEmpty()) second else first, if (first.isEmpty()) null else second)
}

fun Random.nextString(length: Int): String {
   val leftLimit = 97 // letter 'a'
   val rightLimit = 122 // letter 'z'
   val buffer = StringBuilder(length)
   for (i in 0 until length) {
      val randomLimitedInt = leftLimit + (this.nextFloat() * (rightLimit - leftLimit + 1)).toInt()
      buffer.append(randomLimitedInt.toChar())
   }
   return buffer.toString()
}