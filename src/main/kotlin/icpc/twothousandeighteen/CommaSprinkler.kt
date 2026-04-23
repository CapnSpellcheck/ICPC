package icpc.twothousandeighteen

import util.WordScanner
import java.io.InputStream
import java.io.OutputStream

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/comma
 * The WordScanner I created is general, so I put it in my utilities.
 */

class DrSprinkler {
   val wordPredecessors = HashMap<String, HashSet<String>>()
   val wordSuccessors = HashMap<String, HashSet<String>>()
   val commaPredecessors = HashSet<String>()
   val commaSuccessors = HashSet<String>()

   // The fun part is here: Want to build the transitive closure of commaPredecessors/Successors
   // via the following consequences of Dr Sprinkler:
   // - if word W is a commaSuccessor, any word X that ever precedes W is a commaPredecessor
   // - if word W is a commaPredecessor, any word X that ever follows W is a commaSuccessor
   // these 2 functions perform a mutual-recursion DFS once we have the initial comma pred/succ's
   val foundPredecessors = HashSet<String>()
   val foundSuccessors = HashSet<String>()
   private fun searchSuccessor(commaSuccessor: String) {
      if (!foundSuccessors.contains(commaSuccessor)) {
         foundSuccessors += commaSuccessor
         for (wordPredecessor in wordPredecessors[commaSuccessor] ?: emptySet()) {
            commaPredecessors += wordPredecessor
            searchPredecessor(wordPredecessor)
         }
      }
   }
   private fun searchPredecessor(commaPredecessor: String) {
      if (!foundPredecessors.contains(commaPredecessor)) {
         foundPredecessors += commaPredecessor
         for (wordSuccessor in wordSuccessors[commaPredecessor] ?: emptySet()) {
            commaSuccessors += wordSuccessor
            searchSuccessor(wordSuccessor)
         }
      }
   }

   /**
    * The main solution to the problem
    * I solved it by working in three phases:
    * 1) Input processing: while reading the text for the first pass, I extract words and build 4 tables
    *   * wordPredecessors: for each word that comes after some word in a sentence, I build a set of
    *   the words that precede it
    *   * wordSuccessors: for each word that comes before some word in a sentence, I build a set of
    *   the words that follow it
    *   * commaPredecessors: for each word that comes before a comma, I insert it into this set.
    *   * commaSuccessors: for each word that comes after a comma, I insert it into this set.
    * 2) Complete the commaPredecessors, commaSuccessors: the rules of Dr Sprinkler are recursive,
    *   and at this point, these 2 sets have initial entries only, as found in the literal text.
    *   Therefore I use DFS with 2 mutually recursive functions to 'sniff out' all words that should
    *   be in the 2 sets.
    * 3) Generating the output: By scanning the text again, I place commas using the set of
    *   predecessors and successors, but only after confirming the input did not have a period
    *   between each pair of words. I copy periods where present.
    */
   fun apply(text: String, outputStream: OutputStream) {
      // Scan the input and build the word pred/succ tables. The comma pred/succ tables will be
      // initialized with the initial state from text.
      val textScanner = WordScanner(text)
      var lastWord = textScanner.nextWord() ?: return
      while (true) {
         val punctuation = textScanner.lastPunctuation
         val word = textScanner.nextWord() ?: break
         // If a period separated the 2 words, we simply don't create a predecessor-successor relationship
         // for them
         if (punctuation != '.') {
            wordPredecessors.computeIfAbsent(word) { hashSetOf() } += lastWord
            wordSuccessors.computeIfAbsent(lastWord) { hashSetOf() } += word
         }
         if (punctuation == ',') {
            commaPredecessors += lastWord
            commaSuccessors += word
         }
         lastWord = word
      }

      // Compute the transitive closure of comma predecessors/successors
      for (word in commaPredecessors.toList()) {
         searchPredecessor(word)
      }
      for (word in commaSuccessors.toList()) {
         searchSuccessor(word)
      }

      // Copy the text to output, inserting commas. The part about handling periods is done here.
      textScanner.reset()
      lastWord = textScanner.nextWord() ?: return
      outputStream.bufferedWriter().use { writer ->
         while (true) {
            writer.write(lastWord)
            var writePeriod = false
            if (textScanner.lastPunctuation == '.') {
               writePeriod = true
            }
            val nextWord = textScanner.nextWord() ?: break
            if (writePeriod) {
               writer.write(". ")
            } else if (lastWord in commaPredecessors || nextWord in commaSuccessors) {
               writer.write(", ")
            } else {
               writer.append(' ')
            }
            lastWord = nextWord
         }
         // write end
         writer.write(".\n")
      }
   }

}
fun applyDrSprinklerIO(inputStream: InputStream, outputStream: OutputStream) {
   inputStream.reader().use {
      val text = it.readLines().first()
      DrSprinkler().apply(text, outputStream)
   }
}

fun main() {
   applyDrSprinklerIO(System.`in`, System.out)
}
