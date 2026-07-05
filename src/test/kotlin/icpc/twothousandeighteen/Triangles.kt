package icpc.twothousandeighteen

import util.*
import java.io.StringBufferInputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals

class CompleteStaggeredVertexGrid(numRows: Int, numVerticesPerRow: Int) : StaggeredVertexGrid(numRows, numVerticesPerRow) {
   override fun horizontalSegmentFrom(row: Int, startColumn: Int): Boolean = true
   override fun backslashSegmentBelow(row: Int, column: Int): Boolean = true
   override fun forwardSlashSegmentBelow(row: Int, column: Int): Boolean = true
}

class RandomStaggeredVertexGrid(
   val horizontalP: Float,
   val forwardP: Float,
   val backP: Float,
   numRows: Int,
   numVerticesPerRow: Int
) : StaggeredVertexGrid(numRows, numVerticesPerRow) {
   override fun horizontalSegmentFrom(row: Int, startColumn: Int): Boolean = Random.nextFloat() < horizontalP
   override fun backslashSegmentBelow(row: Int, column: Int): Boolean = Random.nextFloat() < backP
   override fun forwardSlashSegmentBelow(row: Int, column: Int): Boolean = Random.nextFloat() < forwardP
}

class TrianglesTest {
   @Test fun testBuildHorizontalSegmentsInfo() {
      val grid1 = TextStaggeredVertexGrid("""
         x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 1)
      val horizontalSegmentInfo1 = buildHorizontalSegmentsInfo(grid1)
      assertContentEquals(listOf(
         listOf(), // row 0
      ), horizontalSegmentInfo1.map { it })

      val grid2 = TextStaggeredVertexGrid("""
         x   x---x---x   x---x   x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 1)
      val horizontalSegmentInfo2 = buildHorizontalSegmentsInfo(grid2)
      assertContentEquals(listOf(
         listOf(IntRange(1, 2), IntRange(4, 4)), // row 0
      ), horizontalSegmentInfo2.map { it })

      val grid3 = TextStaggeredVertexGrid("""
         x   x---x---x   x
              \ /   / \
           x   x---x   x   x
              / \ / \   \
         x   x---x---x---x
            /   / \   \ / \
           x---x---x---x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      val horizontalSegmentInfo3 = buildHorizontalSegmentsInfo(grid3)
      assertContentEquals(listOf(
         listOf(IntRange(1, 2),), // row 0
         listOf(IntRange(1, 1),), // row 1
         listOf(IntRange(1, 3),), // row 2
         listOf(IntRange(0, 3),), // row 3
      ), horizontalSegmentInfo3.map { it })
   }

   @Test fun testBuildBackslashSegmentsInfo() {
      val grid1 = TextStaggeredVertexGrid("""
         x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 1)
      val backslashSegmentInfo1 = buildBackslashSegmentsInfo(grid1, SlashIndexConverter((grid1.numRows)))
      assertContentEquals(listOf(
         listOf(),
      ), backslashSegmentInfo1.map { it.toList() })

      val grid2 = TextStaggeredVertexGrid("""
         x   x   x
          \   \
           x   x   x
                \
         x   x   x
          \   \
           x   x   x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      val backslashSegmentInfo2 = buildBackslashSegmentsInfo(grid2, SlashIndexConverter((grid2.numRows)))
      assertContentEquals(listOf(
         listOf(IntRange(2, 2)),
         listOf(IntRange(0, 0), IntRange(2, 2)),
         listOf(IntRange(0, 1)),
         emptyList()
      ), backslashSegmentInfo2.map { it.toList() })

      val grid3 = TextStaggeredVertexGrid("""
         x   x---x---x   x
              \ /   / \
           x   x---x   x   x
              / \ / \   \
         x   x---x---x---x
            /   / \   \ / \
           x---x---x---x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      val backslashSegmentInfo3 = buildBackslashSegmentsInfo(grid3, SlashIndexConverter((grid3.numRows)))
      assertContentEquals(listOf(
         emptyList(),
         emptyList(),
         listOf(IntRange(0, 2)),
         listOf(IntRange(1, 2)),
         listOf(IntRange(0, 2)),
         emptyList(),
      ), backslashSegmentInfo3.map { it.toList() })

      val grid4 = TextStaggeredVertexGrid("""
         x   x---x
              \
           x   x---x
                \ 
         x   x---x
          \ /     \ 
           x---x---x
                \
         x   x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 5)
      val backslashSegmentInfo4 = buildBackslashSegmentsInfo(grid4, SlashIndexConverter((grid4.numRows)))
      assertContentEquals(listOf(
         emptyList(),
         listOf(IntRange(2, 2)),
         listOf(IntRange(3, 3)),
         listOf(IntRange(0, 2)),
         emptyList(),
      ), backslashSegmentInfo4.map { it.toList() })
   }

   @Test fun testBuildForwardSlashSegmentsInfo() {
      val grid2 = TextStaggeredVertexGrid("""
         x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 1)
      val forwardSlashSegmentInfo2 = buildForwardSlashSegmentsInfo(grid2, SlashIndexConverter((grid2.numRows)))
      assertContentEquals(listOf(emptyList()), forwardSlashSegmentInfo2.map { it.toList() })

      val grid3 = TextStaggeredVertexGrid("""
         x   x---x---x   x
              \ /   / \
           x   x---x   x   x
              / \ / \   \
         x   x---x---x---x
            /   / \   \ / \
           x---x---x---x---x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      val forwardSlashSegmentsInfo3 = buildForwardSlashSegmentsInfo(grid3, SlashIndexConverter((grid3.numRows)))
      assertContentEquals(listOf(
         emptyList(),
         emptyList(),
         listOf(IntRange(0, 2)),
         listOf(IntRange(0, 2)),
         emptyList(),
         listOf(IntRange(2, 2)),
         emptyList(),
      ), forwardSlashSegmentsInfo3.map { it.toList() })

      val grid4 = TextStaggeredVertexGrid("""
         x   x   x
            /
           x   x   x
              / 
         x   x   x
            /   /
           x   x   x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      val forwardSlashSegmentInfo4 = buildForwardSlashSegmentsInfo(grid4, SlashIndexConverter((grid4.numRows)))
      assertContentEquals(listOf(
         emptyList(),
         listOf(IntRange(0, 0)),
         listOf(IntRange(1, 2),),
         listOf(IntRange(2, 2)),
         emptyList(),
      ), forwardSlashSegmentInfo4.map { it.toList() })

      val grid5 = TextStaggeredVertexGrid("""
         x   x   x   x
                /   /
           x   x   x   x
                  /   /
         x   x   x   x
            /   /  
           x   x   x   x
      """.trimIndent().split("\n").map { DefaultString(it) }.toTypedArray(), 4)
      val forwardSlashSegmentInfo5 = buildForwardSlashSegmentsInfo(grid5, SlashIndexConverter((grid5.numRows)))
      assertContentEquals(listOf(
         emptyList(),
         emptyList(),
         listOf(IntRange(0, 0), IntRange(2, 2)),
         listOf(IntRange(0, 2)),
         listOf(IntRange(1, 1)),
         emptyList(),
      ), forwardSlashSegmentInfo5.map { it.toList() })
   }

   @Test fun testSample1Preprocessed() {
      val horizontalSegmentsInfo = arrayOf(
         listOf(IntRange(1, 2),), // row 0
         listOf(IntRange(1, 1),), // row 1
         listOf(IntRange(1, 3),), // row 2
         listOf(IntRange(0, 3),), // row 3
      )

      val forwardSlashSegmentsInfo = Array<MutableList<IntRange>>(7) { ArrayList() }
      forwardSlashSegmentsInfo[2].addAll(listOf(IntRange(0, 2)))
      forwardSlashSegmentsInfo[3].addAll(listOf(IntRange(0, 2)))
      forwardSlashSegmentsInfo[5].addAll(listOf(IntRange(2, 2)))

      val backslashSegmentsInfo = Array<MutableList<IntRange>>(6) { ArrayList() }
      backslashSegmentsInfo[2].addAll(listOf(IntRange(0, 2)))
      backslashSegmentsInfo[3].addAll(listOf(IntRange(1, 2)))
      backslashSegmentsInfo[4].addAll(listOf(IntRange(0, 2)))

      val result = triangles(horizontalSegmentsInfo, forwardSlashSegmentsInfo, backslashSegmentsInfo)
      assertEquals(12L, result)
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
      val complete1000x = CompleteStaggeredVertexGrid(1000, 1000)
      var start = System.currentTimeMillis()
      var count = triangles(complete1000x)
      var elapsed = System.currentTimeMillis() - start
      println("1000x1000: $elapsed ms")
      val random1000x = RandomStaggeredVertexGrid(0.6f, 0.5f, 0.2f, 1000, 1000)
      start = System.currentTimeMillis()
      count = triangles(random1000x)
      elapsed = System.currentTimeMillis() - start
      println("1000x1000: $elapsed ms")
   }
}

class SlashIndexConverterTest {
   @Test fun testBackslash() {
      assertEquals(2, SlashIndexConverter(5).backslashIndex(0, 0))
      assertEquals(2, SlashIndexConverter(5).backslashIndex(1, 0))
      assertEquals(1, SlashIndexConverter(5).backslashIndex(2, 0))
      assertEquals(1, SlashIndexConverter(5).backslashIndex(3, 0))
      assertEquals(0, SlashIndexConverter(5).backslashIndex(4, 0))

      assertEquals(3, SlashIndexConverter(5).backslashIndex(0, 1))
      assertEquals(3, SlashIndexConverter(5).backslashIndex(1, 1))
      assertEquals(2, SlashIndexConverter(5).backslashIndex(2, 1))
      assertEquals(2, SlashIndexConverter(5).backslashIndex(3, 1))
      assertEquals(1, SlashIndexConverter(5).backslashIndex(4, 1))

      assertEquals(1, SlashIndexConverter(4).backslashIndex(0, 0))
      assertEquals(1, SlashIndexConverter(4).backslashIndex(1, 0))
      assertEquals(0, SlashIndexConverter(4).backslashIndex(2, 0))
      assertEquals(0, SlashIndexConverter(4).backslashIndex(3, 0))

      assertEquals(2, SlashIndexConverter(4).backslashIndex(0, 1))
      assertEquals(2, SlashIndexConverter(4).backslashIndex(1, 1))
      assertEquals(1, SlashIndexConverter(4).backslashIndex(2, 1))
      assertEquals(1, SlashIndexConverter(4).backslashIndex(3, 1))
   }
}