
package icpc.twothousandtwentyone

import java.io.InputStream
import java.io.OutputStream

/**
 * This file contains a solution to the ICPC problem: https://icpc.kattis.com/problems/splitstream
 */

/**
 * The Splitstream is initialized with nodes, then allows one to query outputs. I refer to the inputs and
 * outputs as "lines", similar to lines of a circuit.
 */
class Splitstream(inputLength: Int, nodes: List<Node>) {
   /**
    * The node is used in construction of the network. Lines are always passed inputs before outputs.
    * Nodes are used primarily to construct the Lines.
    * @param typeChar 'S' or 'M'
    * @param line1 The identifier of line 1. It is the always an input.
    * @param line2 The identifier of line 2. It is the second input for a merge node; the first output
    * of a split node.
    * @param line3 The identifier of line 3. It is the first output for a merge node; the second output
    * of a merge node.
    */
   class Node(typeChar: Char, val line1: Int, val line2: Int, val line3: Int) {
      enum class Type { SPLIT, MERGE }

      val type: Type = if (typeChar == 'S') Type.SPLIT else Type.MERGE

      inline val firstInput: Int
         get() = line1
      inline val secondInput: Int
         get() = if (type == Type.SPLIT) NONEXISTENT_LINE else line2
      inline val firstOutput: Int
         get() = if (type == Type.SPLIT) line2 else line3
      inline val secondOutput: Int
         get() = if (type == Type.SPLIT) line3 else NONEXISTENT_LINE
   }

   /**
    * Line is abstract, and separates the logic of splits and merges. Also, a Root line is not associated
    * with a Node.
    * Note about UInt: I decided to use UInt for sequence lengths and output values/positions. The reason is
    * that unsigned math results in faster bitshift operations. Split and merge logic results in dividing
    * and multiplying by 2, which should result in shift instructions, which is actually faster on unsigned types.
    * @param id The line's identity, a nonnegative integer
    * @param source The Node from which the line originates. There is no Node for a Root line.
    */
   private abstract class AbstractLine(val id: Int, open val source: Node?) {
      //
      abstract val outputLength: UInt
      abstract fun output(offset: UInt): UInt

      override fun toString(): String = "Line(id = $id)"
   }

   // The root line is the identity mapping with a given length. It has no node.
   private class RootLine(length: Int) : AbstractLine(1, null) {
      override val outputLength: UInt = length.toUInt()
      override fun output(offset: UInt): UInt = if (offset <= outputLength) offset else NONEXISTENT
   }

   /**
    * A SplitLine is the output of a split node. I use an optimization trick to "compress" a bunch of adjacent
    * split nodes. This computation is done lazily, because per the problem statement,
    * the number of queries may be much less than the number of nodes.
    * There can be more split nodes than merge nodes (a network can consist of only splits), but there
    * must be fewer merges than splits.
    */
   private inner class SplitLine(id: Int, override var source: Node) : AbstractLine(id, source) {
      // implement split compression for Split outputs:
      // If a line L has more than 1 split node ancestors before a merge, we can jump past the splits
      // up to the merge above them, in one step: the kth output of L, relative to the highest split S*,
      // is 2^d*(k - 1) + offset, where d is the depth of L from S*, and offset is the 1-indexed
      // offset of this line relative to S*, calculated by the number of right children there are in the path from S* to L.
      // This formula is broken into `frequency`, which is the 2^d, and the pseudoRelativeOffset.
      // S* is the pseudoSource.
      private var doneCompression = false
      private var pseudoSource = source
      private var pseudoRelativeOffset = 1U
      private var frequency = 1U // frequency = 2^d in the split compression discussion below

      // The output length is half of the Node's input length. If that length is odd, then the first output
      // is one shorter than the right.
      override val outputLength: UInt by lazy {
         val sourceLength = lines[source.firstInput].outputLength
         if (source.firstOutput == id)
            (sourceLength + 1U) / 2U
         else sourceLength / 2U
      }

      override fun output(offset: UInt): UInt {
         if (offset > outputLength)
            return NONEXISTENT
         // lazily compress
         compress()

         // Compute relative to the pseudo source
         val splitRelativeOffset = frequency * (offset - 1U) + pseudoRelativeOffset
         // can recurse to the pseudoSource and skip intermediates
         return lines[pseudoSource.firstInput].output(splitRelativeOffset)
      }

      private fun compress() {
         if (!doneCompression) {
            (lines[source.firstInput] as? SplitLine)?.let { splitParent ->
               splitParent.compress()
               pseudoSource = splitParent.pseudoSource
               frequency = 2U * splitParent.frequency
               // The offset relative to pseudoSource is equal to the parent (source) if this line is the first (left)
               // output; otherwise, you add the parent's frequency to get the correct offset
               pseudoRelativeOffset = splitParent.pseudoRelativeOffset
               if (source.secondOutput == id)
                  pseudoRelativeOffset += splitParent.frequency
            } ?: run {
               frequency = 2U
               if (source.secondOutput == id)
                  pseudoRelativeOffset = 2U
            }

            doneCompression = true
         }
      }
   }

   /**
    * A MergeLine is the output of a merge node. Because the number of merge nodes is bounded, I didn't
    * consider a compression optimization. I felt it was much less likely to produce a benefit.
    */
   private inner class MergeLine(id: Int, override val source: Node) : AbstractLine(id, source) {
      // The output length is the sum of the lengths of the inputs
      override val outputLength: UInt by lazy {
         lines[source.firstInput].outputLength + lines[source.secondInput].outputLength
      }

      override fun output(offset: UInt): UInt {
         if (offset > outputLength)
            return NONEXISTENT
         // Some math to determine which input this offset comes from, because they may have unequal lengths,
         // so can't just say "odd is first and even is second". Check to see if the requested offset is
         // past the exhaustion point of one of the inputs.
         val firstInputLength = lines[source.firstInput].outputLength
         val secondInputLength = lines[source.secondInput].outputLength
         if (offset > 2U * firstInputLength)
            return lines[source.secondInput].output(offset - firstInputLength)
         if (offset > 2U * secondInputLength)
            return lines[source.firstInput].output(offset - secondInputLength)
         // Both inputs are active so "odd is first and even is second" ;)
         val sourceInput = if (offset % 2U == 0U) source.secondInput else source.firstInput
         return lines[sourceInput].output((offset + 1U) / 2U)
      }
   }

   private val lines: Array<AbstractLine>

   // Assemble the Lines from the nodes provided in the constructor.
   init {
      // I prefer avoiding optionals when unnecessary, but also wanted to use an array to hold the lines
      // for performance reasons. Ordinarily, I don't worry about using arrays with classes too much in production.
      // So dummyLine is a throwaway line for array initialization
      val dummyLine = RootLine(0)
      // The maximum number of output lines equals 2 x the number of nodes
      lines = Array(2 * nodes.size + 2) { dummyLine } // [0] is unused, and [1] is the root input.
      lines[1] = RootLine(inputLength)
      for (node in nodes) {
         var id = node.firstOutput
         lines[id] = if (node.type == Node.Type.SPLIT) SplitLine(id, node) else MergeLine(id, node)
         if (node.type == Node.Type.SPLIT) {
            id = node.secondOutput
            lines[id] = SplitLine(id, node)
         }
      }
   }

   fun query(line: Int, offset: UInt): UInt = lines[line].output(offset)

   companion object {
      const val NONEXISTENT = 0U
      const val NONEXISTENT_LINE = 0
   }
}

fun splitstreamIO(inputStream: InputStream, outputStream: OutputStream) {
   inputStream.bufferedReader().use { reader ->
      val nums = reader.readLine().split(' ')
      val inputLength = nums[0].toInt()
      val queries = nums[2].toInt()

      // read nodes
      val nodes = nums[1].toInt()
      val nodeList = ArrayList<Splitstream.Node>(nodes)
      repeat(nodes) {
         val typeCharCode = reader.read()
         reader.read() // space
         val lines = reader.readLine().split(' ')
         val node = Splitstream.Node(typeCharCode.toChar(), lines[0].toInt(), lines[1].toInt(), lines[2].toInt())
         nodeList += node
      }

      // create network
      val splitstream = Splitstream(inputLength, nodeList)

      // read queries one by one, execute
      outputStream.bufferedWriter().use { writer ->
         repeat(queries) {
            val query = reader.readLine().split(' ')
            val output = splitstream.query(query[0].toInt(), query[1].toUInt())
            writer.write(if (output.isNonexistent) "none\n" else "$output\n")
         }
      }
   }
}

fun main() {
   splitstreamIO(System.`in`, System.out)
}

inline val UInt.isNonexistent: Boolean
   get() = this == Splitstream.NONEXISTENT