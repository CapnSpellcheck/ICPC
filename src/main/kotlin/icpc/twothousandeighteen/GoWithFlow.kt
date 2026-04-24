package icpc.twothousandeighteen

import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.max

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/gowithflow
 */

class RiverResult(val length: Int, val lineWidth: Int)

private class RiverInfo(var lastOffset: Int, var length: Int = 1)

private const val ONE = 1.toByte()

// The main solution to the problem. There is likely no happy path to the right answer. I simulate
// the text placement starting with the narrowest valid line width. My approach to scan the rivers
// for a given width involves using 2 queues, one for the last line and one for the current.
// The important thing is to quickly find rivers that are growing in length, and a deque is just great
// for that. Upon placing each word we can scan the rivers of last line up to current position, and
// discard them if they end, or renew them and grow length by 1 when they continue. Note that the range
// to check for a continued river is 3 positions, as the problem says +/- 1.
// The only part I consider to be 'hairy' is the discovery that a word of length 1 can cause a river
// to split into 2, or 2 rivers to merge. I found these cases out via code rejection and had to
// figure out test cases for that :)
// Note that I originally wrote the algo with one queue, and had some complicated conditions and
// loop breaks in there. I prefer the way I changed it to, with 2 queues, although it takes a bit
// longer to execute.
fun longestTypographicRiver(words: ByteArray, longestWord: Int): RiverResult {
   var longestLength = 0
   var longestLineWidth = -1

   var lineWidth = longestWord
   var stop = false

   fun expireRiver(river: RiverInfo) {
      if (river.length > longestLength) {
         longestLength = river.length
         longestLineWidth = lineWidth
      }
   }

   while (!stop) {
      var i = 0
      var lineOffset = 0
      var lastLineRivers = ArrayDeque<RiverInfo>()
      var curLineRivers = ArrayDeque<RiverInfo>()
      var linebreakCount = 0

      while (i < words.size) {
         val length = words[i]

         if (lineOffset + length > lineWidth) {
            // need to line break before placing this word
            // first expire any rivers from the last line that ended
            lastLineRivers.forEach { riverInfo ->
               expireRiver(riverInfo)
            }
            // swap queues
            lastLineRivers = curLineRivers
            curLineRivers = ArrayDeque()

            lineOffset = 0
            linebreakCount += 1
         } else {
            var continuedRiver = false
            // Check rivers from last line up to this offset
            while (lastLineRivers.isNotEmpty() && lastLineRivers.peek().lastOffset <= lineOffset) {
               val river = lastLineRivers.pop()
               if (river.lastOffset >= lineOffset - 2) {
                  // extend the river to this line
                  val riverLastOffset = river.lastOffset
                  river.lastOffset = lineOffset - 1
                  river.length += 1
                  curLineRivers.offer(river)
                  continuedRiver = true

                  // Handle the annoying case of river splitting and merging -- it only happens
                  // when there is a length-1 word
                  if (length == ONE && riverLastOffset == lineOffset) {
                     // The river from above has split into 2 rivers, because this is a length-1 word
                     // and the river was directly above the char of this word
                     val splitRiver = RiverInfo(lineOffset + 1, river.length)
                     curLineRivers.offer(splitRiver)
                  } else if (lastLineRivers.peek()?.lastOffset == lineOffset) {
                     // What if 2 rivers from the last line become one (the length-1 word is above)?
                     // merge them
                     val mergedRiver = lastLineRivers.pop()
                     curLineRivers.peekLast().let {
                        it.length = max(it.length, mergedRiver.length + 1)
                     }
                  }
               } else {
                  expireRiver(river)
               }
            }

            // if we didn't continue a river, we created one (remember it's at the start of the new word)
            if (!continuedRiver && lineOffset > 0)
               curLineRivers.offer(RiverInfo(lineOffset - 1))
         }

         // whether we line broke or are continuing the line, place the word
         lineOffset += length + 1
         i += 1
      }

      // done with final line, so check extant rivers to see if any are the longest
      lastLineRivers.forEach { riverInfo ->
         expireRiver(riverInfo)
      }
      curLineRivers.forEach { riverInfo ->
         expireRiver(riverInfo)
      }

      // try a longer width
      lineWidth += 1

      // we can stop when there are so few lines that there could not be a longer river
      if (linebreakCount + 1 <= longestLength)
         stop = true
   }

   return RiverResult(longestLength, longestLineWidth)
}

/**
 * In the I/O wrapper, which is required for ICPC, I take great effort not to read the word strings
 * since they are not needed. My goal was to try to have the fastest executing JVM solution.
 */
fun longestTypographicRiverIO(inputStream: InputStream, outputStream: OutputStream) {
   val wordCount: Int
   val bytes = ByteArray(4)
   var used = 0
   // read word count
   while (true) {
      val charCode = inputStream.read()
      if (!Character.isDigit(charCode)) {
         wordCount = String(bytes, 0, used).toInt()
         break
      } else {
         bytes[used] = charCode.toByte()
         used += 1
      }
   }

   // scan text and determine word lengths only
   var length = 0
   var readWords = 0
   val wordLengths = ByteArray(wordCount)
   var maxLength = 0
   while (true) {
      val charCode = inputStream.read()
      if (charCode == -1)
         break
      if (Character.isLetter(charCode)) {
         length += 1
      } else {
         wordLengths[readWords] = length.toByte()
         maxLength = max(length, maxLength)
         length = 0
         readWords += 1
      }
   }
   assert(readWords == wordCount)

   // done with parsing, run it
   val result = longestTypographicRiver(wordLengths, maxLength)
   // output
   outputStream.writer().use {
      it.write("${result.lineWidth} ${result.length}")
   }
}

fun main() {
   longestTypographicRiverIO(System.`in`, System.out)
}