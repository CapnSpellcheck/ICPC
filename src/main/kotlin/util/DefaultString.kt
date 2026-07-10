package util

import kotlin.math.min

class DefaultString(val string: String, val defaultChar: Char = ' ') : CharSequence {
   override val length: Int
      get() = string.length

   override fun get(index: Int): Char {
      return if (index < string.length && index >= 0) string[index] else defaultChar
   }

   override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
      return DefaultString(
         string.substring(min(startIndex, string.length), min(endIndex, string.length)),
         defaultChar
      )
   }
}