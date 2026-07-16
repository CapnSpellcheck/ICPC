package icpc.twothousandeighteen

import util.*
import java.io.InputStream
import java.io.OutputStream
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/triangles3
 */

/**
 * An important construct in my solution is the "slash line". The slash line is a particular angled
 * line that runs between some set of rows. All forward slash lines are parallel, and all backslash lines are parallel.
 * The lines are assigned an index. For the forward slash, the line going through the upper left corner is index 0,
 * and each line to the right is the next index. For the backslash, it's the mirror image: the line going through the
 * upper right corner is 0, and each line to the left is the next one.
 * This converter helps by calculating the index number for a given row and column.
 */
class SlashIndexConverter(val numCols: Int) {
   inline fun forwardSlashIndex(row: Int, column: Int): Int = column + (row + 1)/2
   inline fun backslashIndex(row: Int, column: Int): Int = (numCols - 1 - column) + row/2
}

/**
 * An abstract type for a staggered vertex grid. This allows for unit tests to easily build grids.
 */
abstract class StaggeredVertexGrid(val numRows: Int, val verticesPerRow: Int) {
   abstract fun horizontalSegmentFrom(row: Int, startColumn: Int): Boolean
   abstract fun backslashSegmentBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean
   abstract fun forwardSlashSegmentBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean
}

// closed interval
data class Interval(val start: Int, val end: Int) {
   val width = end - start + 1

   inline fun contains(first: Int, last: Int): Boolean {
      return first >= this.start && last <= this.end
   }
}

/**
 * The grid with the text format given in the problem statement.
 */
class TextStaggeredVertexGrid(val text: Array<DefaultString>, numRows: Int)
   : StaggeredVertexGrid(numRows, 1 + text[0].lastIndexOf('x')/4)
{
   override fun horizontalSegmentFrom(row: Int, startColumn: Int): Boolean {
      val shift = if (row.and(1) == 0) 2 else 4
      return text[2*row][4*startColumn + shift] == '-'
   }

   override fun backslashSegmentBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean {
      val shift = if (rowIsOdd) 1 else 3
      return text[2*row + 1][4*column + shift] == '\\'
   }

   override fun forwardSlashSegmentBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean {
      val shift = if (rowIsOdd) -1 else 1
      return text[2*row + 1][4*column + shift] == '/'
   }
}

/**
 * The SegmentInfoBuilder is used to produce the horizontal segments in rows in the grid.
 */
private class SegmentInfoBuilder() {
   private var cur = 0
   private var start = cur
   private var isOn = false
   private var outList: MutableList<Interval> = mutableListOf()

   fun start(baseOffset: Int, outList: MutableList<Interval>) {
      cur = baseOffset
      this.outList = outList
   }

   inline fun next(segmentIsOn: Boolean) {
      if (segmentIsOn)
         on()
      else
         off()
   }

   inline fun on() {
      if (!isOn) {
         start = cur
         isOn = true
      }
      cur += 1
   }

   inline fun off() {
      finish()
      cur += 1
      isOn = false
   }

   inline fun finish() {
      if (isOn) {
         outList.add(Interval(start, cur - 1))
         isOn = false
      }
   }
}

/**
 * Build the horizontal segments. This is passed in to the main algorithm.
 * Why calculate these separately, differently from the way I handle slash lines? While it was an accidental
 * decision, I found that it results in a significant latency improvement.
 * @return a list of the locations of horizontal line segments indexed by row
 */
fun buildHorizontalSegmentsInfo(grid: StaggeredVertexGrid): Array<out List<Interval>> {
   val rowSegmentsInfo = Array<MutableList<Interval>>(grid.numRows) { ArrayList() }
   val builder = SegmentInfoBuilder()
   for (row in 0 ..< grid.numRows) {
      val rowSegments = rowSegmentsInfo[row]
      builder.start(0, rowSegments)
      for (offset in 0 .. grid.verticesPerRow - 2) {
         builder.next(grid.horizontalSegmentFrom(row, offset))
      }
      builder.finish()
   }
   return rowSegmentsInfo
}

/**
 * The abstraction for a slash line. All slash lines have an initialization of the properties: curRow, curColumn, rowOdd;
 * and a stub function that determines whether the line continues below a certain coordinate.
 */
abstract class SlashLine(val grid: StaggeredVertexGrid, val lineIndex: Int, defaultLastRow: Int) {
   var currentSegment: Interval? = null; private set
   protected var curRow: Int = 0
   protected var curColumn: Int = 0
   protected var rowOdd: Boolean = false
   private val lastRow = min(defaultLastRow, grid.numRows - 1)

   abstract fun init()
   abstract fun lineContinuesBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean

   init {
      init()
      rowOdd = curRow.and(1) == 0
   }

   /**
    * Find the segment along the line that touches `row` from above.
    * @param row A monotonically increasing row number; after the function returns, `currentSegment.last` must
    * hit `row` from above, otherwise it must be null.
    */
   fun advanceToRow(row: Int) {
      if (currentSegment?.end.lt(row - 1))
         currentSegment = null
      while (curRow < row) {
         if (lineContinuesBelow(curRow, curColumn, rowOdd)) {
            val segmentStart = curRow
            // go to the segment end
            while (curRow < lastRow && (lineContinuesBelow(curRow, curColumn, rowOdd))) {
               onNextRow()
            }
            // curRow is 1 past the segment end
            currentSegment = Interval(segmentStart, curRow - 1)
         } else {
            currentSegment = null
         }
         onNextRow()
      }
   }

   open fun onNextRow() {
      curRow += 1
      rowOdd = !rowOdd
   }

}

/**
 * The model of a concrete forward slash line. Some important observations about the geometry:
 * The first N start at row 0, one per column, while following ones start at the rightmost column of an even numbered row.
 * When going from an odd to even row, a given line shifts one column left.
 * The slash line not only locates the line segments, it caches the 'current' one. This allows the main
 * algorithm to NOT need to precompute them; instead it tells this object how far to seek down the line.
 */
class ForwardSlashLine(grid: StaggeredVertexGrid, lineIndex: Int) : SlashLine(grid, lineIndex, 2*lineIndex) {
   override fun init() {
      val lastColumn = grid.verticesPerRow - 1
      curRow = if (lineIndex > lastColumn) 2*(lineIndex - lastColumn) - 1 else 0
      curColumn = min(lineIndex, lastColumn)
   }

   override fun lineContinuesBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean =
      grid.forwardSlashSegmentBelow(curRow, curColumn, rowOdd)

   override fun onNextRow() {
      if (rowOdd)
         curColumn -= 1
      super.onNextRow()
   }
}

/**
 * The model of a concrete backslash line. Some important observations about the geometry:
 * The first N start at row 0, one per column, from the rightmost, while following ones start at the leftmost
 * column of an odd numbered row.
 * When going from an even to odd row, a given line shifts one column right.
 * The slash line not only locates the line segments, it caches the 'current' one. This allows the main
 * algorithm to NOT need to precompute them; instead it tells this object how far to seek down the line.
 */
class BackslashLine(grid: StaggeredVertexGrid, lineIndex: Int) : SlashLine(grid, lineIndex, 2*lineIndex + 1){
   override fun init() {
      val lastColumn = grid.verticesPerRow - 1
      curRow = if (lineIndex > lastColumn) 2*(lineIndex - lastColumn) else 0
      curColumn = max(0, lastColumn - lineIndex)
   }

   override fun lineContinuesBelow(row: Int, column: Int, rowIsOdd: Boolean): Boolean =
      grid.backslashSegmentBelow(curRow, curColumn, rowOdd)

   override fun onNextRow() {
      if (!rowOdd)
         curColumn += 1
      super.onNextRow()
   }
}

/**
 * The main algorithm. Passed Kattis 3.65 sec. Iterates through the rows, taking the horizontal segments passed in.
 * Finds all the UP pointing triangles for each row BEFORE finding any DOWN for the row, because the 'key' row
 * (where the segment must exist in order for the triangles touching the row to exist) crosses the row for the DOWN.
 * A Fenwick tree is used to compute the number of triangles made by the LEFT side, for each possible RIGHT side.
 */
fun triangles(
   converter: SlashIndexConverter,
   rowSegmentsInfo: Array<out List<Interval>>,
   forwardSlashLines: Array<ForwardSlashLine>,
   backslashLines: Array<BackslashLine>,
): Long {
   val lastRow = rowSegmentsInfo.lastIndex
   var triangleCount = 0L
   for (row in 0 ..< rowSegmentsInfo.size) {
      val rowSegments = rowSegmentsInfo[row]

      // need to do all UP triangles before DOWN triangles in the row
      for (segmentInterval in rowSegments) {
         val startingColumnForwardSlashTree = FenwickTree(segmentInterval.width)
         var offsetInSegment = 0
         val columnPopLists = Array<ArrayList<Int>>(segmentInterval.width) { ArrayList() }
         var forwardSlashIndex = converter.forwardSlashIndex(row, segmentInterval.start)
         var backslashIndex = converter.backslashIndex(row, segmentInterval.start + 1)

         for (column in segmentInterval.start .. segmentInterval.end) {
            val length = offsetInSegment + 1
            // forward slash segment is on left side, here I put in the tree 1 at the column where
            // the forward slash 'dies' - it's `forwardSlashLengthUp` columns from `offsetInSegment`
            forwardSlashLines[forwardSlashIndex].currentSegment?.let { forwardSlashSegmentUp ->
               startingColumnForwardSlashTree.add(offsetInSegment, 1)
               val forwardSlashLengthUp = row - forwardSlashSegmentUp.start
               if (offsetInSegment + forwardSlashLengthUp < columnPopLists.size)
                  columnPopLists[offsetInSegment + forwardSlashLengthUp].add(offsetInSegment)
            }

            // I've avoided using iterators in the innermost loop.
            // popping needs to be done before counting the triangles with forward slash segment
            var popOffset = 0
            run {
               val popList = columnPopLists[offsetInSegment]
               while (popOffset < popList.size) {
                  startingColumnForwardSlashTree.add(popList[popOffset], -1)
                  popOffset += 1
               }
            }

            // backslash segment is on right side, at column `column` + 1
            val backslashLine = backslashLines[backslashIndex]
            backslashLine.currentSegment?.let { backslashSegmentUp ->
               val backslashLengthUp = row - backslashSegmentUp.start
               val maxLength = min(length, backslashLengthUp)
               val trianglesHere = startingColumnForwardSlashTree.rangeQuery(length - maxLength, length - 1)
//                println("ROW $row: UP: column $column: maxLength=$maxLength range=${length - maxLength}..${length - 1}, $trianglesHere triangles")
               triangleCount += trianglesHere
            }

            forwardSlashIndex += 1
            backslashIndex -= 1
            columnPopLists[offsetInSegment] = ArrayList<Int>()
            offsetInSegment += 1
         }
      }

      // A little awkward, but there are no triangles pointing down from the bottom row. It's simpler to put this
      // than force the slash logic to not crash on the last row. Plus, it's faster.
      if (row == lastRow)
         break

      // before counting DOWN triangles, advance all slash lines that go through `row` to `row + 1`
      for (forwardSlashIndex in converter.forwardSlashIndex(row, 0) .. converter.forwardSlashIndex(row, converter.numCols - 1)) {
         forwardSlashLines[forwardSlashIndex].advanceToRow(row + 1)
      }
      for (backslashIndex in converter.backslashIndex(row, 0) downTo converter.backslashIndex(row, converter.numCols - 1)) {
         backslashLines[backslashIndex].advanceToRow(row + 1)
      }

      // DOWN triangles
      for (segmentInterval in rowSegments) {
         val startingColumnBackslashTree = FenwickTree(segmentInterval.width)
         var offsetInSegment = 0
         val columnPopLists = Array<ArrayList<Int>>(segmentInterval.width) { ArrayList() }
         var forwardSlashIndex = converter.forwardSlashIndex(row, segmentInterval.start + 1)
         var backslashIndex = converter.backslashIndex(row, segmentInterval.start)

         for (column in segmentInterval.start .. segmentInterval.end) {
//            println("  segmentInterval=$segmentInterval column $column")
            val length = offsetInSegment + 1
            // backslash segment is on left side, at column `column`
            backslashLines[backslashIndex].currentSegment?.let { backslashSegmentDown ->
               startingColumnBackslashTree.add(offsetInSegment, 1)
               val backslashLengthDown = backslashSegmentDown.end - row + 1
               if (offsetInSegment + backslashLengthDown < columnPopLists.size)
                  columnPopLists[offsetInSegment + backslashLengthDown].add(offsetInSegment)
            }

            // I've avoided using iterators in the innermost loop.
            // popping needs to be done before counting the triangles with forward slash segment
            var popOffset = 0
            run {
               val popList = columnPopLists[offsetInSegment]
               while (popOffset < popList.size) {
                  startingColumnBackslashTree.add(popList[popOffset], -1)
                  popOffset += 1
               }
            }

            // forward slash segment is on right side, at column `column` + 1
            val forwardSlashLine = forwardSlashLines[forwardSlashIndex]
            forwardSlashLine.currentSegment?.let { forwardSlashSegmentDown ->
//               println("  forwardSlashLine currentSegment=$forwardSlashSegmentDown")
               val forwardSlashLengthDown = forwardSlashSegmentDown.end - row + 1
               val maxLength = min(length, forwardSlashLengthDown)
               val trianglesHere = startingColumnBackslashTree.rangeQuery(length - maxLength, length - 1)
//                println("  column $column: maxLength=$maxLength range=${length - maxLength}..${length - 1}, $trianglesHere triangles")
               triangleCount += trianglesHere
            }

            forwardSlashIndex += 1
            backslashIndex -= 1
            offsetInSegment += 1
         }
      }
   }
   return triangleCount
}

fun triangles(grid: StaggeredVertexGrid): Long {
   val converter = SlashIndexConverter(grid.verticesPerRow)
   val rowSegmentsInfo = buildHorizontalSegmentsInfo(grid)
   val lastForwardSlashIndex = converter.forwardSlashIndex(grid.numRows - 1, grid.verticesPerRow - 1)
   val forwardSlashLines = Array<ForwardSlashLine>(1 + lastForwardSlashIndex) { i -> ForwardSlashLine(grid, i) }
   val lastBackslashIndex = converter.backslashIndex(grid.numRows - 1, 0)
   val backslashLines = Array<BackslashLine>(1 + lastBackslashIndex) { i -> BackslashLine(grid, i) }
   return triangles(converter, rowSegmentsInfo, forwardSlashLines, backslashLines)
}

fun trianglesIO(inputStream: InputStream, outputStream: OutputStream) {
   val count: Long
   inputStream.bufferedReader().use { reader ->
      val nums = reader.readLine().split(" ")
      val rows = nums[0].toInt()
      val text = Array(2*rows - 1) { _ ->
         DefaultString(reader.readLine())
      }
      val grid = TextStaggeredVertexGrid(text, rows)
      count = triangles(grid)
   }
   outputStream.writer().use { it.write(count.toString()) }
}

fun main() {
   trianglesIO(System.`in`, System.out)
}