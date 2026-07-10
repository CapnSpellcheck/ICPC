package util

/**
 * Taken from Google search AI output. MInor changes.
 */
class FenwickTree(val size: Int) {
   private val tree = IntArray(size + 1)

   // Adds 'value' to the element at 'index'
   fun add(index: Int, value: Int) {
      var idx = index + 1
      while (idx <= size) {
         tree[idx] += value
         idx += idx and -idx // Move to the parent node using the least significant bit
      }
   }

   // Computes the prefix sum from 0 to 'index'
   fun query(index: Int): Int {
      var sum = 0
      var idx = index + 1
      while (idx > 0) {
         sum += tree[idx]
         idx -= idx and -idx // Move to the previous cumulative sum node
      }
      return sum
   }

   // Computes the range sum between 'fromIndex' and 'toIndex' (inclusive)
   fun rangeQuery(fromIndex: Int, toIndex: Int): Int {
      return query(toIndex) - query(fromIndex - 1)
   }
}
