package util

/**
 * A pair of ints that uses the primitive integers. Using a generic Pair<Int,Int> would use boxed objects.
 */
class IntPair(val first: Int, val second: Int) {
   override fun toString(): String {
      return "IntPair(first=$first, second=$second)"
   }
}