package util

// closed interval
data class Interval(val start: Int, val end: Int) {
   val width = end - start + 1

   inline fun contains(first: Int, last: Int): Boolean {
      return first >= this.start && last <= this.end
   }

   companion object {
      fun fromUnordered(startOrEnd: Int, endOrStart: Int): Interval {
         return if (startOrEnd < endOrStart) Interval(startOrEnd, endOrStart) else Interval(endOrStart, startOrEnd)
      }
   }
}
