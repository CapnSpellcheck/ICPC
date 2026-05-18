package icpc.twothousandfifteen

import util.IntervalTreeMap
import util.hardAssert
import util.intersects
import java.awt.Point
import java.awt.Rectangle
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.abs
import kotlin.math.min

enum class Dimension { X, Y }

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/windows
 */

class Window(val ID: Int, origin: Point, width: Int, height: Int) {

   constructor(ID: Int, originX: Int, originY: Int, width: Int, height: Int) : this(ID, Point(originX, originY), width, height)

   var height = height; private set
   var width = width; private set
   var xRange: IntRange = IntRange.EMPTY; private set
   var yRange: IntRange = IntRange.EMPTY; private set
   var origin = origin; private set

   init {
      setRanges()
   }

   private inline fun setRanges() {
      setXRange()
      setYRange()
   }

   private inline fun setXRange() {
      xRange = IntRange(origin.x, origin.x + width - 1)
   }

   private inline fun setYRange() {
      yRange = IntRange(origin.y, origin.y + height - 1)
   }

   fun resize(newWidth: Int, newHeight: Int) {
      width = newWidth
      height = newHeight
      setRanges()
   }

   fun moveX(amount: Int) {
      origin.x += amount
      setXRange()
   }

   fun moveY(amount: Int) {
      origin.y += amount
      setYRange()
   }

   override fun toString(): String {
      return "Window(x=$origin.x, y=$origin.y, w=$width, h=$height)"
   }

   // Implemented primarily for comparison to Rectangles in unit tests
   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other is Rectangle) {
         return other.x == origin.x && other.y == origin.y && other.width == width && other.height == height
      }
      if (javaClass != other?.javaClass) return false

      other as Window

      if (height != other.height) return false
      if (width != other.width) return false
      if (origin.y != other.origin.y) return false
      if (origin.x != other.origin.x) return false

      return true
   }
}

/**
 * The heart of the solution
 */
class WindowManager(val screenWidth: Int, val screenHeight: Int) {
   enum class ResizeResult {
      OK,
      NoWindowFound,
      DoesNotFit
   }

   enum class Command {
      OPEN,
      RESIZE,
      MOVE,
      CLOSE
   }

   data class MoveResult(val code: Int, val movedAmount: Int = 0) {
      companion object {
         const val OK = 0
         const val NoWindowFound = 1
         const val MovedLess = 2
      }
   }

   // Windows aren't stored in a list, but a tree with the origin as key. This speeds up some operations by
   // a bit.
   val originTreeSet: TreeMap<Point, Window> = TreeMap(PointComparator)

   private val screenBounds = Rectangle(0, 0, screenWidth - 1, screenHeight - 1)
   // The DimensionProjector abstracts the move operation across 2 axes and positive/negative.
   private val dimensionProjectorXPositive = DimensionProjector.create(
      Dimension.X,
      screenBounds,
      true,
      originTreeSet,
   )
   private val dimensionProjectorXNegative = DimensionProjector.create(
      Dimension.X,
      screenBounds,
      false,
      originTreeSet,
   )
   private val dimensionProjectorYPositive = DimensionProjector.create(
      Dimension.Y,
      screenBounds,
      true,
      originTreeSet,
   )
   private val dimensionProjectorYNegative = DimensionProjector.create(
      Dimension.Y,
      screenBounds,
      false,
      originTreeSet,
   )

   private var nextID: Int = 0

   val windowCount: Int
      get() = originTreeSet.size

   fun open(originX: Int, originY: Int, width: Int, height: Int): Boolean {
      val xRange = originX ..< originX + width
      val yRange = originY ..< originY + height
      if (xRange.last >= screenWidth || originX < 0 || yRange.last >= screenHeight || originY < 0)
         return false

      findAny(xRange, yRange)?.let {
         return false
      }

      val window = Window(nextID(), originX, originY, width, height)
      addToUpperLeftTree(window)
      return true
   }

   fun close(atX: Int, atY: Int): Boolean {
      val window = findWindow(atX, atY)
      window?.let {
         removeFromUpperLeftTree(window)
         return true
      }
      return false
   }

   fun resize(atX: Int, atY: Int, newWidth: Int, newHeight: Int): ResizeResult {
      val window = findWindow(atX, atY)
         ?: return ResizeResult.NoWindowFound
      if (window.origin.x + newWidth > screenWidth || window.origin.y + newHeight > screenHeight)
         return ResizeResult.DoesNotFit

      // check rectangles of new area
      if (newWidth > window.width) {
         findAnyNotIn(window.xRange.last + 1 ..< window.origin.x + newWidth, window.origin.y ..< window.origin.y + newHeight, window)
            ?.let { return ResizeResult.DoesNotFit }
      }
      if (newHeight > window.height) {
         findAnyNotIn(window.origin.x ..< window.origin.x + newWidth, window.yRange.last + 1 ..< window.origin.y + newHeight, window)
            ?.let { return ResizeResult.DoesNotFit }
      }

      // update the window and trees
      removeFromUpperLeftTree(window)
      window.resize(newWidth, newHeight)
      addToUpperLeftTree(window)

      return ResizeResult.OK
   }

   fun moveX(atX: Int, atY: Int, byX: Int): MoveResult {
      val window = findWindow(atX, atY)
      if (window == null)
         return MoveResult(MoveResult.NoWindowFound)
      if (byX == 0)
         return MoveResult(MoveResult.OK)

      // get the projector
      val projector = if (byX > 0) dimensionProjectorXPositive else dimensionProjectorXNegative
      return moveWithProjector(window, abs(byX), projector)
   }

   fun moveY(atX: Int, atY: Int, byY: Int): MoveResult {
      val window = findWindow(atX, atY)
      if (window == null)
         return MoveResult(MoveResult.NoWindowFound)
      if (byY == 0)
         return MoveResult(MoveResult.OK)

      // get the projector
      val projector = if (byY > 0) dimensionProjectorYPositive else dimensionProjectorYNegative
      return moveWithProjector(window, abs(byY), projector)
   }

   fun windows(): List<Window> {
      return originTreeSet.values.sortedBy { it.ID }
   }

   private fun findWindow(atX: Int, atY: Int): Window? {
      return originTreeSet.values.firstOrNull {
         it.xRange.contains(atX) && it.yRange.contains(atY)
      }
   }

   private fun findAny(xRange: IntRange, yRange: IntRange): Window? {
      return originTreeSet.values.firstOrNull {
         it.xRange.intersects(xRange) && it.yRange.intersects(yRange)
      }
   }

   private fun findAnyNotIn(xRange: IntRange, yRange: IntRange, window: Window): Window? {
      return originTreeSet.values.firstOrNull {
         it !== window && it.xRange.intersects(xRange) && it.yRange.intersects(yRange)
      }
   }

   /**
    * The main part of the move algorithm, which is the most complicated part. Moving boils down to
    * calculating which windows move, and when, and if the screen bounds is hit before moving the requested
    * amount.
    */
   private fun moveWithProjector(window: Window, amount: Int, projector: DimensionProjector): MoveResult {
      val initialMovedWindows = projector.findCollisions(window, amount)

      // Keep a set of moved windows for quick filtering of duplicate collisions
      val movedWindowIDs = BitSet()
      movedWindowIDs.set(window.ID)
      for (window in initialMovedWindows) {
         movedWindowIDs.set(window.ID)
      }

      // Calculate the free space in front of windows that are going to be hit.
      // avoid hashing and autoboxing by using a primitive array, will probably waste some slots, but it's fast
      val freeSpaceBefore = IntArray(nextID)
      val windowsByID = arrayOfNulls<Window>(nextID)
      freeSpaceBefore[window.ID] = 0 // initial window has 0 free space before
      windowsByID[window.ID] = window

      // The furthest window ranges tells which window is the furthest so far from the requested window
      // (that will be hit) for a given interval on the cross axis. It helps me calculate the freeSpaceBefore.
      val furthestWindowRanges = IntervalTreeMap<Int>()
      furthestWindowRanges.insert(projector.crossAxisRange(window), window.ID)

      // The algorithm needs to check windows in order from their distance from initial window in order
      // for the freeSpaceBefore info to be correct. But when searching for collisions it's not easy
      // to be guaranteed to get them in sorted order without explicitly sorting them.
      val unprocessedWindowsByLeadingEdge = PriorityQueue(projector)
      unprocessedWindowsByLeadingEdge.addAll(initialMovedWindows)

      var maxPushAmount = min(amount, projector.trailingSpaceToBound(window))

      while (!unprocessedWindowsByLeadingEdge.isEmpty()) {
         // We 'process' a window by calculating its freeSpaceBefore and finding windows it will
         // collide with. Once we know its free space before, we know how much it will move, so
         // we can know the area of its collisions.
         val movedWindow = unprocessedWindowsByLeadingEdge.poll()
         windowsByID[movedWindow.ID] = movedWindow
         var freeSpace = amount // maximum
         val crossAxisRange = projector.crossAxisRange(movedWindow)
         val overlappers = furthestWindowRanges.successorIterator(crossAxisRange)
         var leadingOverlapperReplacement: IntervalTreeMap.OverlapResult<Int>? = null
         var trailingOverlapperReplacement: IntervalTreeMap.OverlapResult<Int>? = null

         // We need to put current window's cross-axis range in the interval tree and split
         // overlappers that intersect it, subtracting out the part of current window;
         // we know that overlappers are sorted from min to max and are mutually non-intersecting
         for (overlapper in overlappers) {
            if (overlapper.interval.first > crossAxisRange.last)
               break
            val spaceBetween = projector.spaceBetween(windowsByID[overlapper.ancillary]!!, movedWindow)
            freeSpace = min(freeSpace, spaceBetween + freeSpaceBefore[overlapper.ancillary])

            val overlapperInterval = overlapper.interval
            if (overlapperInterval.first < crossAxisRange.first) {
               // case 1: the overlapper starts before window (it must be first overlapper) and ends before it,
               // we mark the overlapper for replacement by its prefix before window
               val leadingInterval = IntRange(overlapperInterval.first, crossAxisRange.first - 1)
               leadingOverlapperReplacement = IntervalTreeMap.OverlapResult(leadingInterval, overlapper.ancillary)
               if (overlapperInterval.last <= crossAxisRange.last) {
                  Unit
               } else {
                  // case 2: the overlapper subsumes the window, but window interval must replace it, splitting it.
                  val trailingInterval = IntRange(crossAxisRange.last + 1, overlapperInterval.last)
                  trailingOverlapperReplacement = IntervalTreeMap.OverlapResult(trailingInterval, overlapper.ancillary)
               }
            }
            // case 3: the overlapper starts after the window starts, and extends past it
            else if (overlapperInterval.last > crossAxisRange.last) {
               val trailingInterval = IntRange(crossAxisRange.last + 1, overlapperInterval.last)
               trailingOverlapperReplacement = IntervalTreeMap.OverlapResult(trailingInterval, overlapper.ancillary)
            }
            // in case 4, the window subsumes the overlapper, we do nothing since we remove overlapper anyway

            overlappers.remove() // all overlaps are deleted
         }
         leadingOverlapperReplacement?.let {
            furthestWindowRanges.insert(it.interval, it.ancillary)
         }
         trailingOverlapperReplacement?.let {
            furthestWindowRanges.insert(it.interval, it.ancillary)
         }
         furthestWindowRanges.insert(crossAxisRange, movedWindow.ID)

         hardAssert(freeSpace >= 0)
         freeSpaceBefore[movedWindow.ID] = freeSpace
         // update the maximum push amount by considering the trailing distance to edge for this moved window
         maxPushAmount = min(maxPushAmount, projector.trailingSpaceToBound(movedWindow) + freeSpace)

         val newCollisions = projector.findCollisions(movedWindow, amount - freeSpace)
         for (newCollision in newCollisions) {
            if (!movedWindowIDs.get(newCollision.ID)) {
               movedWindowIDs.set(newCollision.ID)
               unprocessedWindowsByLeadingEdge.add(newCollision)
            }
         }
      }

      // We have the free space before all hit windows, we have the amount to push maxPushAMount
      // we can calculate the amount each window moved as: maxPushAmount - freeSpace
      // The windows have to be all removed, update their coordinates and then re-added because a window
      // can move to another window's prior position, and the tree doesn't allow duplicates.
      var movedWindowID = movedWindowIDs.nextSetBit(0)
      while (movedWindowID > -1) {
         val movedAmount = maxPushAmount - freeSpaceBefore[movedWindowID]
         if (movedAmount > 0) {
            val movedWindow = windowsByID[movedWindowID]!!
            removeFromUpperLeftTree(movedWindow)
            projector.moveWindowBy(movedWindow, movedAmount)
         }
         movedWindowID = movedWindowIDs.nextSetBit(movedWindowID + 1)
      }
      movedWindowID = movedWindowIDs.nextSetBit(0)
      while (movedWindowID > -1) {
         val movedWindow = windowsByID[movedWindowID]!!
         addToUpperLeftTree(movedWindow)
         movedWindowID = movedWindowIDs.nextSetBit(movedWindowID + 1)
      }

      val code = if (maxPushAmount == amount) MoveResult.OK else MoveResult.MovedLess
      return MoveResult(code, maxPushAmount)
   }

   private fun nextID(): Int {
      val next = nextID
      nextID += 1
      return next
   }

   private inline fun addToUpperLeftTree(window: Window) {
      originTreeSet[window.origin] = window
   }

   private inline fun removeFromUpperLeftTree(window: Window) {
      originTreeSet.remove(window.origin)
   }

   private object PointComparator : Comparator<Point> {
      override fun compare(o1: Point, o2: Point): Int {
         val xCmp = o1.x.compareTo(o2.x)
         if (xCmp != 0)
            return xCmp
         return o1.y.compareTo(o2.y)
      }
   }
}

private abstract class DimensionProjector protected constructor(
   val dimension: Dimension,
) : Comparator<Window> {
   abstract fun moveWindowBy(movedWindow: Window, movedAmount: Int)
   abstract fun crossAxisRange(window: Window): IntRange
   abstract fun mainAxisStart(window: Window): Int
   abstract fun afterTrailingEdge(window: Window, amount: Int = 0): Int
   abstract fun trailingSpaceToBound(window: Window): Int
   abstract fun findCollisions(window: Window, width: Int): Sequence<Window>
   abstract fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int
   abstract fun sortWindows(list: MutableList<Window>)

   companion object {
      fun create(
         dimension: Dimension,
         spaceBounds: Rectangle,
         towardsPositiveInfinity: Boolean,
         windowMap: TreeMap<Point, Window>,
      ): DimensionProjector {
         return if (dimension == Dimension.X) {
            if (towardsPositiveInfinity) {
               object : DimensionProjector(Dimension.X) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveX(movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.yRange

                  override fun mainAxisStart(window: Window): Int = window.xRange.first

                  override fun afterTrailingEdge(window: Window, amount: Int): Int =
                     window.xRange.last + amount

                  override fun trailingSpaceToBound(window: Window): Int =
                     spaceBounds.width - window.xRange.last

                  override fun findCollisions(window: Window, width: Int): Sequence<Window> {
                     return windowMap.tailMap(Point(afterTrailingEdge(window, 1), spaceBounds.y), true)
                        .headMap(Point(afterTrailingEdge(window, width + 1), spaceBounds.y), false)
                        .asSequence()
                        .map { it.value }
                        .filter { w -> w.yRange.last >= window.origin.y }
                  }

                  override fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int =
                     laterWindow.origin.x - (earlyWindow.xRange.last + 1)

                  override fun sortWindows(list: MutableList<Window>) {
                     list.sortBy { it.origin.x }
                  }

                  override fun compare(o1: Window, o2: Window): Int =
                     o1.origin.x.compareTo(o2.origin.x)

               }
            } else {
               object : DimensionProjector(Dimension.X) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveX(-movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.yRange
                  override fun mainAxisStart(window: Window): Int = window.xRange.last

                  override fun afterTrailingEdge(window: Window, amount: Int): Int =
                     window.origin.x - amount

                  override fun trailingSpaceToBound(window: Window): Int =
                     window.origin.x - spaceBounds.x

                  override fun findCollisions(window: Window, width: Int): Sequence<Window> {
                     return windowMap.values.asSequence().filter { w ->
                        w.xRange.intersects(afterTrailingEdge(window, width) ..< afterTrailingEdge(window, 0)) &&
                           w.yRange.intersects(window.yRange)
                     }
                  }

                  override fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int =
                     earlyWindow.origin.x - (laterWindow.xRange.last + 1)

                  override fun sortWindows(list: MutableList<Window>) {
                     list.sortByDescending { it.origin.x }
                  }

                  // Need to compare descending
                  override fun compare(o1: Window, o2: Window): Int =
                     o2.xRange.last.compareTo(o1.xRange.last)

               }
            }
         } else {
            if (towardsPositiveInfinity) {
               object : DimensionProjector(Dimension.Y) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveY(movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.xRange
                  override fun mainAxisStart(window: Window): Int = window.yRange.first

                  override fun afterTrailingEdge(window: Window, amount: Int): Int =
                     window.yRange.last + amount

                  override fun trailingSpaceToBound(window: Window): Int =
                     spaceBounds.height - window.yRange.last

                  override fun findCollisions(window: Window, width: Int): Sequence<Window> {
                     return windowMap.headMap(Point(window.xRange.last, afterTrailingEdge(window, width)), true)
                        .asSequence()
                        .map { it.value }
                        .filter { w ->
                           w.xRange.last >= window.origin.x &&
                              w.yRange.intersects(afterTrailingEdge(window, 1) .. afterTrailingEdge(window, width))
                        }
                  }

                  override fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int =
                     laterWindow.origin.y - (earlyWindow.yRange.last + 1)

                  override fun sortWindows(list: MutableList<Window>) {
                     list.sortBy { it.origin.y }
                  }

                  override fun compare(o1: Window, o2: Window): Int =
                     o1.origin.y.compareTo(o2.origin.y)

               }
            } else {
               object : DimensionProjector(Dimension.Y) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveY(-movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.xRange
                  override fun mainAxisStart(window: Window): Int = window.yRange.last

                  override fun afterTrailingEdge(window: Window, amount: Int): Int =
                     window.origin.y - amount

                  override fun trailingSpaceToBound(window: Window): Int =
                     window.origin.y - spaceBounds.y

                  override fun findCollisions(window: Window, width: Int): Sequence<Window> {
                     return windowMap.values.asSequence().filter { w ->
                        w.xRange.intersects(window.xRange) &&
                        w.yRange.intersects(afterTrailingEdge(window, width) ..< afterTrailingEdge(window, 0))
                     }
                  }

                  override fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int =
                     earlyWindow.origin.y - (laterWindow.yRange.last + 1)

                  override fun sortWindows(list: MutableList<Window>) {
                     list.sortByDescending { it.origin.y }
                  }

                  // Need to compare descending
                  override fun compare(o1: Window, o2: Window): Int =
                     o2.yRange.last.compareTo(o1.yRange.last)

               }
            }

         }
      }
   }
}

fun simulateWindowManager(width: Int, height: Int, commands: List<WindowManager.Command>, params: List<IntArray>):
   Triple<List<String>, Int, List<Window>>
{
   val wm = WindowManager(width, height)
   val commandOutputs = mutableListOf<String>()
   val noWindow = "no window at given position"
   val doesNotFit = "window does not fit"
   commands.forEachIndexed { index, command ->
      val params = params[index]
      val number = index + 1
      when (command) {
         WindowManager.Command.OPEN -> {
//            println("manager.open(${params[0]}, ${params[1]}, ${params[2]}, ${params[3]})")
            if (!wm.open(params[0], params[1], params[2], params[3])) {
               commandOutputs += "Command $number: OPEN - $doesNotFit"
            }
         }
         WindowManager.Command.RESIZE -> {
//            println("manager.resize(${params[0]}, ${params[1]}, ${params[2]}, ${params[3]})")
            val result = wm.resize(params[0], params[1], params[2], params[3])
            when (result) {
               WindowManager.ResizeResult.OK -> Unit
               WindowManager.ResizeResult.NoWindowFound ->
                  commandOutputs += "Command $number: RESIZE - $noWindow"
               WindowManager.ResizeResult.DoesNotFit ->
                  commandOutputs += "Command $number: RESIZE - $doesNotFit"
            }
         }
         WindowManager.Command.MOVE -> {
//            println("manager.move(${params[0]}, ${params[1]}, ${params[2]}, ${params[3]})")
            val requestAmount: Int
            val result = if (params[2] != 0) {
               requestAmount = params[2]
               wm.moveX(params[0], params[1], requestAmount)
            } else {
               requestAmount = params[3]
               wm.moveY(params[0], params[1], requestAmount)
            }
            when (result.code) {
               WindowManager.MoveResult.NoWindowFound ->
                  commandOutputs += "Command $number: MOVE - $noWindow"
               WindowManager.MoveResult.MovedLess ->
                  commandOutputs += "Command $number: MOVE - moved ${abs(result.movedAmount)} instead of ${abs(requestAmount)}"
            }
         }
         WindowManager.Command.CLOSE -> {
//            println("manager.close(${params[0]}, ${params[1]})")
            if (!wm.close(params[0], params[1])) {
               commandOutputs += "Command $number: CLOSE - $noWindow"
            }
         }
      }
   }
   return Triple(commandOutputs, wm.windowCount, wm.windows())
}

fun simulateWindowManagerIO(inputStream: InputStream, outputStream: OutputStream) {
   inputStream.bufferedReader().use { reader ->
      val commands = mutableListOf<WindowManager.Command>()
      val params = mutableListOf<IntArray>()
      val line1 = reader.readLine().split(' ')
      while (true) {
         val commandLine = reader.readLine()
         if (commandLine == null)
            break

         val tokens = commandLine.split(' ')
         commands += WindowManager.Command.valueOf(tokens[0])
         params += IntArray(tokens.size - 1) { i -> tokens[i + 1].toInt() }
      }

      val result = simulateWindowManager(line1[0].toInt(), line1[1].toInt(), commands, params)

      outputStream.bufferedWriter().use { writer ->
         for (message in result.first) {
            writer.write(message)
            writer.write("\n")
         }
         writer.write("${result.second} window(s):\n")
         for (window in result.third) {
            writer.write("${window.origin.x} ${window.origin.y} ${window.width} ${window.height}\n")
         }
      }
   }
}

fun main() {
   simulateWindowManagerIO(System.`in`, System.out)
}