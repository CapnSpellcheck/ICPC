package util

/**
 * A word scanner that works if the input has only single spaces between words and single punctuation characters.
 */
class WordScanner(val text: String) {
   var lastPunctuation: Char? = null; private set

   private var offset = 0

   fun nextWord(): String? {
      val start = offset
      while (offset < text.length && text[offset].isLetter()) {
         offset += 1
      }
      val word = if (offset > start) {
         text.substring(start, offset)
      } else {
         null
      }
      lastPunctuation = if (offset > start && !text[offset].isWhitespace()) {
         offset += 1
         text[offset - 1]
      } else {
         null
      }
      offset += 1
      return word
   }

   fun reset() {
      offset = 0
      lastPunctuation = null
   }
}