package icpc.twothousandeighteen

import util.*
import java.io.StringBufferInputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals

class CompleteStaggeredVertexGrid(numRows: Int, numVerticesPerRow: Int) : StaggeredVertexGrid(numRows, numVerticesPerRow) {
   override fun horizontalSegmentFrom(row: Int, startColumn: Int): Boolean = startColumn in (0 ..< verticesPerRow - 1)
   override fun backslashSegmentBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean = true

   override fun forwardSlashSegmentBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean = true
}

class RandomStaggeredVertexGrid(
   val horizontalP: Float,
   val forwardP: Float,
   val backP: Float,
   numRows: Int,
   numVerticesPerRow: Int
) : StaggeredVertexGrid(numRows, numVerticesPerRow) {
   val horizontalTable = mutableMapOf<IntPair, Boolean>()
   val forwardTable = mutableMapOf<IntPair, Boolean>()
   val backTable = mutableMapOf<IntPair, Boolean>()
   init {
      // warm the tables to get accurate performance timings
      for (row in 0 ..< numRows) {
         for (column in 0 ..< numVerticesPerRow) {
            horizontalSegmentFrom(row, column)
            backslashSegmentBelow(row, column, true)
            forwardSlashSegmentBelow(row, column, true)
         }
      }
   }
   override fun horizontalSegmentFrom(row: Int, startColumn: Int): Boolean {
      if (startColumn >= verticesPerRow - 1) return false
      val answer = horizontalTable.getOrPut(IntPair(row, startColumn)) {
         Random.nextFloat() < horizontalP
      }
      return answer
   }
   override fun backslashSegmentBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean {
      val answer = backTable.getOrPut(IntPair(row, column)) {
         Random.nextFloat() < backP
      }
      return answer
   }

   override fun forwardSlashSegmentBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean {
      val answer = forwardTable.getOrPut(IntPair(row, column)) {
         Random.nextFloat() < forwardP
      }
      return answer
   }

}

class TrianglesTest {
   @Test fun testForwardSlashLine() {
      val grid = TextStaggeredVertexGrid("""
         x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 1)
      var converter = SlashIndexConverter(grid.verticesPerRow)
      var lastForwardSlashIndex = converter.forwardSlashIndex(grid.numRows - 1, grid.verticesPerRow - 1)
      var forwardSlashLines = Array<ForwardSlashLine>(1 + lastForwardSlashIndex) { i -> ForwardSlashLine(grid, i) }
      assertContentEquals(listOf(null), forwardSlashLines.map { it.currentSegment })

      val grid2 = TextStaggeredVertexGrid("""
         x   x---x---x   x
              \ /   / \
           x   x---x   x   x
              / \ / \   \
         x   x---x---x---x
            /   / \   \ / \
           x---x---x---x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      converter = SlashIndexConverter(grid2.verticesPerRow)
      lastForwardSlashIndex = converter.forwardSlashIndex(grid2.numRows - 1, grid2.verticesPerRow - 1)
      forwardSlashLines = Array<ForwardSlashLine>(1 + lastForwardSlashIndex) { i -> ForwardSlashLine(grid2, i) }
      forwardSlashLines.map { it.advanceToRow(1) }
      assertContentEquals(listOf(
         null,
         null,
         Interval(0, 2),
         Interval(0, 2),
         null,
         null,
         null,
      ), forwardSlashLines.map { it.currentSegment })

      forwardSlashLines.map { it.advanceToRow(2) }
      assertContentEquals(listOf(
         null,
         null,
         Interval(0, 2),
         Interval(0, 2),
         null,
         null,
         null,
      ), forwardSlashLines.map { it.currentSegment })

      forwardSlashLines.map { it.advanceToRow(3) }
      assertContentEquals(listOf(
         null,
         null,
         Interval(0, 2),
         Interval(0, 2),
         null,
         Interval(2, 2),
         null,
      ), forwardSlashLines.map { it.currentSegment })

      val grid4 = TextStaggeredVertexGrid("""
         x   x   x   x
                /   /
           x   x   x   x
                  /   /
         x   x   x   x
            /   /  
           x   x   x   x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      converter = SlashIndexConverter(grid4.verticesPerRow)
      lastForwardSlashIndex = converter.forwardSlashIndex(grid4.numRows - 1, grid4.verticesPerRow - 1)
      forwardSlashLines = Array<ForwardSlashLine>(1 + lastForwardSlashIndex) { i -> ForwardSlashLine(grid4, i) }
      forwardSlashLines.map { it.advanceToRow(1) }
      assertContentEquals(listOf(
         null,
         null,
         Interval(0, 0),
         Interval(0, 2),
         null,
         null,
      ), forwardSlashLines.map { it.currentSegment })

      forwardSlashLines.map { it.advanceToRow(2) }
      assertContentEquals(listOf(
         null,
         null,
         null,
         Interval(0, 2),
         Interval(1, 1),
         null,
      ), forwardSlashLines.map { it.currentSegment })

      forwardSlashLines.map { it.advanceToRow(3) }
      assertContentEquals(listOf(
         null,
         null,
         Interval(2, 2),
         Interval(0, 2),
         null,
         null,
      ), forwardSlashLines.map { it.currentSegment })
   }

   @Test fun testBackslashLine() {
      val grid1 = TextStaggeredVertexGrid("""
         x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 1)
      var converter = SlashIndexConverter(grid1.verticesPerRow)
      var lastBackslashIndex = converter.backslashIndex(grid1.numRows - 1, 0)
      var backslashLines = Array<BackslashLine>(1 + lastBackslashIndex) { i -> BackslashLine(grid1, i) }
      assertContentEquals(listOf(null), backslashLines.map { it.currentSegment })

      val grid2 = TextStaggeredVertexGrid("""
         x   x   x
          \   \
           x   x   x
                \
         x   x   x
          \   \
           x   x   x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      converter = SlashIndexConverter(grid2.verticesPerRow)
      lastBackslashIndex = converter.backslashIndex(grid2.numRows - 1, 0)
      backslashLines = Array<BackslashLine>(1 + lastBackslashIndex) { i -> BackslashLine(grid2, i) }
      backslashLines.map { it.advanceToRow(1) }
      assertContentEquals(listOf(
         null,
         Interval(0, 1),
         Interval(0, 0),
         null,
      ), backslashLines.map { it.currentSegment })

      backslashLines.map { it.advanceToRow(2) }
      assertContentEquals(listOf(
         null,
         Interval(0, 1),
         null,
         null,
      ), backslashLines.map { it.currentSegment })

      backslashLines.map { it.advanceToRow(3) }
      assertContentEquals(listOf(
         null,
         null,
         Interval(2, 2),
         Interval(2, 2),
      ), backslashLines.map { it.currentSegment })

      val grid3 = TextStaggeredVertexGrid("""
         x   x---x---x   x
              \ /   / \
           x   x---x   x   x
              / \ / \   \
         x   x---x---x---x
            /   / \   \ / \
           x---x---x---x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      converter = SlashIndexConverter(grid3.verticesPerRow)
      lastBackslashIndex = converter.backslashIndex(grid3.numRows - 1, 0)
      backslashLines = Array<BackslashLine>(1 + lastBackslashIndex) { i -> BackslashLine(grid3, i) }
      backslashLines.map { it.advanceToRow(1) }
      assertContentEquals(listOf(
         null,
         Interval(0, 2),
         null,
         Interval(0, 2),
         null,
         null,
      ), backslashLines.map { it.currentSegment })

      backslashLines.map { it.advanceToRow(2) }
      assertContentEquals(listOf(
         null,
         Interval(0, 2),
         Interval(1, 2),
         Interval(0, 2),
         null,
         null,
      ), backslashLines.map { it.currentSegment })

      backslashLines.map { it.advanceToRow(3) }
      assertContentEquals(listOf(
         null,
         Interval(0, 2),
         Interval(1, 2),
         Interval(0, 2),
         null,
         null,
      ), backslashLines.map { it.currentSegment })
//
//      val grid4 = TextStaggeredVertexGrid("""
//         x   x---x
//              \
//           x   x---x
//                \
//         x   x---x
//          \ /     \
//           x---x---x
//                \
//         x   x---x
//      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 5)
//      val backslashSegmentInfo4 = buildBackslashSegmentsInfo(grid4, SlashIndexConverter((grid4.verticesPerRow)))
//      assertContentEquals(listOf(
//         emptyList(),
//         listOf(Interval(0, 2)),
//         listOf(Interval(3, 3)),
//         listOf(Interval(2, 2)),
//         emptyList(),
//      ), backslashSegmentInfo4.map { it.toList() })
   }

   @Test fun testSample1() {
      val text = """
         x   x---x---x   x
              \ /   / \
           x   x---x   x   x
              / \ / \   \
         x   x---x---x---x
            /   / \   \ / \
           x---x---x---x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray()
      val result = triangles(TextStaggeredVertexGrid(text, 4) )
      assertEquals(12L, result)
   }

   @Test fun testSample2() {
      val text = """
         x---x
          \ /
           x
          / \
         x   x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray()
      val result = triangles(TextStaggeredVertexGrid(text, 3) )
      assertEquals(1L, result)
   }

   @Test fun testSample2Flipped() {
      val text = """
         x   x
          \ /
           x
          / \
         x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray()
      val result = triangles(TextStaggeredVertexGrid(text, 3) )
      assertEquals(1L, result)
   }

   @Test fun testSample1IO() {
      val text = """
         4 10
         x   x---x---x   x
              \ /   / \
           x   x---x   x   x
              / \ / \   \
         x   x---x---x---x
            /   / \   \ / \
           x---x---x---x---x
      """.trimIndent()
      val sos = StringOutputStream()
      trianglesIO(StringBufferInputStream(text), sos)
      assertEquals(12L, sos.toString().toLong())
   }

   @Test fun testOnlyDown() {
      val text = """
         x   x---x---x   x
              \   \ / \
           x   x---x   x   x
                \ /      
         x   x---x---x---x
                  \   \ /  
           x---x---x---x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray()
      val result = triangles(TextStaggeredVertexGrid(text, 4) )
      assertEquals(4L, result)
   }

   @Test fun testOnlyUp() {
      val text = """
         x   x---x---x   x
                /   / \
           x   x---x---x   x
              / \   \    
         x   x---x---x---x
            / \   \   \ 
           x---x---x---x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray()
      val result = triangles(TextStaggeredVertexGrid(text, 4) )
      assertEquals(4L, result)
   }

   @Test fun testTricky() {
      val text = """
         x   x   x   x
          \   \   \ / \
           x   x   x   x
                \        
         x   x   x   x
              \ / \   \ 
           x   x   x   x
                      /  
         x---x---x---x
          \ /     \ / \ 
           x   x   x   x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray()
      val result = triangles(TextStaggeredVertexGrid(text, 6) )
      assertEquals(2L, result)
   }

   @Test fun testTrianglesComplete() {
      val complete44 = CompleteStaggeredVertexGrid(4, 4)
      val result44 = triangles(complete44)
      assertEquals(28, result44)
      val complete45 = CompleteStaggeredVertexGrid(4, 5)
      val result45 = triangles(complete45)
      assertEquals(40, result45)
      val complete54 = CompleteStaggeredVertexGrid(5, 4)
      val result54 = triangles(complete54)
      assertEquals(40, result54)
      val complete55 = CompleteStaggeredVertexGrid(5, 5)
      val result55 = triangles(complete55)
      assertEquals(60, result55)
   }

   @Test fun testTime() {
      val random2000sq = RandomStaggeredVertexGrid(0.5f, 0.5f, 0.5f, 1000, 1000)
      var start = System.currentTimeMillis()
      var count = triangles(random2000sq)
      var elapsed = System.currentTimeMillis() - start
      println("Cubed: $elapsed ms")
   }

   @Test fun testBug() {
      val text = """
         x---x---x   x---x
          \   \ /       /
           x---x   x---x   x
          /   /   /
         x   x---x   x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray()
      val result = triangles(TextStaggeredVertexGrid(text, 3) )
      assertEquals(1L, result)
   }
}

class SlashIndexConverterTest {
   @Test fun testBackslash() {
      assertEquals(4, SlashIndexConverter(5).backslashIndex(0, 0))
      assertEquals(4, SlashIndexConverter(5).backslashIndex(1, 0))
      assertEquals(5, SlashIndexConverter(5).backslashIndex(2, 0))
      assertEquals(5, SlashIndexConverter(5).backslashIndex(3, 0))
      assertEquals(6, SlashIndexConverter(5).backslashIndex(4, 0))

      assertEquals(3, SlashIndexConverter(5).backslashIndex(0, 1))
      assertEquals(3, SlashIndexConverter(5).backslashIndex(1, 1))
      assertEquals(4, SlashIndexConverter(5).backslashIndex(2, 1))
      assertEquals(4, SlashIndexConverter(5).backslashIndex(3, 1))
      assertEquals(5, SlashIndexConverter(5).backslashIndex(4, 1))

      assertEquals(0, SlashIndexConverter(5).backslashIndex(0, 4))
      assertEquals(0, SlashIndexConverter(5).backslashIndex(1, 4))
      assertEquals(1, SlashIndexConverter(5).backslashIndex(2, 4))
      assertEquals(1, SlashIndexConverter(5).backslashIndex(3, 4))
      assertEquals(2, SlashIndexConverter(5).backslashIndex(4, 4))

      assertEquals(3, SlashIndexConverter(4).backslashIndex(0, 0))
      assertEquals(3, SlashIndexConverter(4).backslashIndex(1, 0))
      assertEquals(4, SlashIndexConverter(4).backslashIndex(2, 0))
      assertEquals(4, SlashIndexConverter(4).backslashIndex(3, 0))
      assertEquals(3, SlashIndexConverter(4).backslashIndex(3, 1))

      assertEquals(3, SlashIndexConverter(3).backslashIndex(3, 0))

   }
}