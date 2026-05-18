package util


fun <T> countingSort(theArray: List<T>, `class`: Class<T>, selector: (T) -> Int, maxValue: Int): Array<T> {

   // count the number of times each value appears.
   // counts[0] stores the number of 0's in the input
   // counts[4] stores the number of 4's in the input
   // etc.
   val counts = IntArray(maxValue + 1)
   for (item in theArray) {
      counts[selector(item)] += 1
   }

   // overwrite counts to hold the next index an item with
   // a given value goes. so, counts[4] will now store the index
   // where the next 4 goes, not the number of 4's our
   // array has.
   var numItemsBefore = 0
   var i = 0
   while (i < counts.size) {
      val count = counts[i]
      counts[i] = numItemsBefore
      numItemsBefore += count
      i += 1
   }

   // output array to be filled in
   val sortedArray = java.lang.reflect.Array.newInstance(`class`, theArray.size) as Array<T>

   // run through the input array
   for (item in theArray) {
      val value = selector(item)

      // place the item in the sorted array
      sortedArray[counts[value]] = item

      // and, make sure the next item we see with the same value
      // goes after the one we just placed
      counts[value] += 1
   }
   return sortedArray
}