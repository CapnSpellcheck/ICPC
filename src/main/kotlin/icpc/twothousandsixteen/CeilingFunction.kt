package icpc.twothousandsixteen

import util.NumberScanner
import java.io.InputStream
import java.io.OutputStream
import java.lang.StringBuilder

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/
 * See also `NumberScanner` in util.
 */
class BinaryTree(rootValue: Int) {
   class Node(val value: Int, var left: Node? = null, var right: Node? = null) {
      fun shapeTo(buf: StringBuilder) {
         buf.append('.')
         if (left == null)
            buf.append('@')
         else {
            left!!.shapeTo(buf)
         }
         if (right == null)
            buf.append('@')
         else
            right!!.shapeTo(buf)
      }

   }

   private val root = Node(rootValue)

   fun shape(buffer: StringBuilder) {
      buffer.clear()
      root.shapeTo(buffer)
   }

   // NOTE: I originally wrote a recursive version of this. I was curious to see whether an
   // iterative version is faster, so I rewrote it. Interestingly, there is no discernable difference
   // in the 'CPU time' calculated by Kattis.
   fun insert(value: Int) {
      var cur = root
      while (true) {
         if (value < cur.value) {
            if (cur.left == null) {
               cur.left = Node(value)
               break
            } else {
               cur = cur.left!!
            }
         } else {
            if (cur.right == null) {
               cur.right = Node(value)
               break
            } else {
               cur = cur.right!!
            }
         }
      }
   }
}

fun countShapes(treeList: List<IntArray>): Int {
   val treeShapes = HashSet<String>()
   val buffer = StringBuilder(2 * treeList[0].size + 1)

   for (treeValues in treeList) {
      val tree = BinaryTree(treeValues[0])
      for (i in 1 ..< treeValues.size) {
         tree.insert(treeValues[i])
      }
      tree.shape(buffer)
      treeShapes.add(buffer.toString())
   }
   return treeShapes.size
}

fun countShapesIO(inputStream: InputStream, outputStream: OutputStream) {
   inputStream.bufferedReader().use { reader ->
      val scanner = NumberScanner(reader.readLine())
      val treeCount = scanner.nextInt()!!
      val treeSize = scanner.nextInt()!!
      val trees = ArrayList<IntArray>(treeCount)
      repeat(treeCount) {
         val scanner = NumberScanner(reader.readLine())
         val treeValues = IntArray(treeSize) {
            scanner.nextInt()!!
         }
         trees.add(treeValues)
      }

      val count = countShapes(trees)

      outputStream.writer().use { writer ->
         writer.write(count.toString())
      }
   }
}

fun main() {
   countShapesIO(System.`in`, System.out)
}
