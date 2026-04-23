package util

/**
 * A number scanner that works if the input has only single spaces between words and single punctuation characters.
 */
class NumberScanner(val text: String) {
   private var offset = 0

   fun nextInt(): Int? {
      val start = offset
      while (offset < text.length && text[offset].isDigit()) {
         offset += 1
      }
      val number = if (offset > start) {
         text.substring(start, offset)
      } else {
         null
      }
      offset += 1
      return number?.toInt()
   }

   fun reset() {
      offset = 0
   }
}