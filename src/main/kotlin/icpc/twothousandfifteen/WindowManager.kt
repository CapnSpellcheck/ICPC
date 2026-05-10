package icpc.twothousandfifteen

import util.IntervalTreeMap
import util.TwoDTreeMap
import java.awt.Rectangle
import java.io.InputStream
import java.io.OutputStream
import java.util.BitSet
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

enum class Dimension { X, Y }

class Window(val ID: Int, originX: Int, originY: Int, width: Int, height: Int) {
   var height = height; private set
   var width = width; private set
   var originY = originY; private set
   var originX = originX; private set
   var xRange: IntRange = IntRange.EMPTY; private set
   var yRange: IntRange = IntRange.EMPTY; private set
   var killed = false; private set

   init {
      setRanges()
   }

   private inline fun setRanges() {
      setXRange()
      setYRange()
   }

   private inline fun setXRange() {
      xRange = IntRange(originX, originX + width - 1)
   }

   private inline fun setYRange() {
      yRange = IntRange(originY, originY + height - 1)
   }

   fun resize(newWidth: Int, newHeight: Int) {
      width = newWidth
      height = newHeight
      setRanges()
   }

   fun moveBy(dimension: Dimension, amount: Int) {
      if (dimension == Dimension.X) {
         originX += amount
         setXRange()
      } else {
         originY += amount
         setYRange()
      }
   }

   fun moveX(amount: Int) {
      originX += amount
      setXRange()
   }

   fun moveY(amount: Int) {
      originY += amount
      setYRange()
   }

   fun kill() {
      this.killed = true
   }

   override fun toString(): String {
      return "Window(x=$originX, y=$originY, w=$width, h=$height)"
   }

   // Implemented primarily for comparison to Rectangles in unit tests
   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other is Rectangle) {
         return other.x == originX && other.y == originY && other.width == width && other.height == height
      }
      if (javaClass != other?.javaClass) return false

      other as Window

      if (height != other.height) return false
      if (width != other.width) return false
      if (originY != other.originY) return false
      if (originX != other.originX) return false

      return true
   }
}

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

   // Windows are referenced by their ID, which is set to equal their index in `windowList`.
   private val upperLeft2DTree = TwoDTreeMap<Int>()
   private val lowerRight2DTree = TwoDTreeMap<Int>()
   private val screenBounds = Rectangle(0, 0, screenWidth - 1, screenHeight - 1)
   private val dimensionProjectorXPositive = DimensionProjector.create(
      Dimension.X,
      screenBounds,
      true,
      upperLeft2DTree,
      lowerRight2DTree,
   )
   private val dimensionProjectorXNegative = DimensionProjector.create(
      Dimension.X,
      screenBounds,
      false,
      upperLeft2DTree,
      lowerRight2DTree,
   )
   private val dimensionProjectorYPositive = DimensionProjector.create(
      Dimension.Y,
      screenBounds,
      true,
      upperLeft2DTree,
      lowerRight2DTree,
   )
   private val dimensionProjectorYNegative = DimensionProjector.create(
      Dimension.Y,
      screenBounds,
      false,
      upperLeft2DTree,
      lowerRight2DTree,
   )
   // for efficiency, I don't remove closed windows
   private val windowList = ArrayList<Window>(100)
   private var closedWindowCount = 0
   private var nextID: Int = 0

   val windowCount: Int
      get() = windowList.size - closedWindowCount

   fun open(originX: Int, originY: Int, width: Int, height: Int): Boolean {
      val xRange = originX ..< originX + width
      val yRange = originY ..< originY + height
      if (xRange.last >= screenWidth || originX < 0 || yRange.last >= screenHeight || originY < 0)
         return false

      upperLeft2DTree.findAny(xRange, yRange)?.let {
         return false
      }
      lowerRight2DTree.findAny(xRange, yRange)?.let {
         return false
      }
      // full check for windows whose intersection with target fully spans one axis
      // TODO:  skip conditions
      if ((originX < screenWidth / 2 && originY < screenHeight / 2) || ((originX < screenWidth / 2 || originY < screenHeight / 2) && Random.nextBoolean())) {
         val candidates = ArrayList<Int>()
         upperLeft2DTree.findAllTo(0 ..< originX, 0 .. yRange.last, candidates)
         if (candidates.any { windowList[it].xRange.last >= originX && windowList[it].yRange.last >= originY })
            return false
         candidates.clear()
         upperLeft2DTree.findAllTo(xRange, 0 ..< originY, candidates)
         if (candidates.any { windowList[it].yRange.last >= originY })
            return false
      } else {
         val candidates = ArrayList<Int>()
         lowerRight2DTree.findAllTo(xRange.last + 1 ..< screenWidth, originY ..< screenHeight, candidates)
         if (candidates.any { windowList[it].originX <= xRange.last && windowList[it].originY <= yRange.last })
            return false
         candidates.clear()
         lowerRight2DTree.findAllTo(xRange, yRange.last + 1 ..< screenHeight, candidates)
         if (candidates.any { windowList[it].originY <= yRange.last })
            return false
      }

      val window = Window(nextID(), originX, originY, width, height)
      assert(window.ID == windowList.size)
      addToUpperLeftTree(window)
      addToLowerRightTree(window)
      assert(upperLeft2DTree.size() == lowerRight2DTree.size())
      windowList.add(window)
      return true
   }

   fun close(atX: Int, atY: Int): Boolean {
      val window = findWindow(atX, atY)
      window?.let {
         removeFromUpperLeftTree(window)
         removeFromLowerRightTree(window)
         assert(upperLeft2DTree.size() == lowerRight2DTree.size())
         closedWindowCount += 1
         window.kill()
         return true
      }
      return false
   }

   fun resize(atX: Int, atY: Int, newWidth: Int, newHeight: Int): ResizeResult {
      val window = findWindow(atX, atY)
         ?: return ResizeResult.NoWindowFound
      if (window.originX + newWidth > screenWidth || window.originY + newHeight > screenHeight)
         return ResizeResult.DoesNotFit

      // check rectangles of new area
      val candidates = ArrayList<Int>()
      if (newWidth > window.width) {
         upperLeft2DTree.findAllTo(window.xRange.last + 1 ..< window.originX + newWidth, 0 ..< window.originY + newHeight, candidates)
         if (candidates.any { windowList[it].yRange.last >= window.originY })
            return ResizeResult.DoesNotFit
      }
      if (newHeight > window.height) {
         candidates.clear()
         upperLeft2DTree.findAllTo(0 ..< window.originX + newWidth, window.yRange.last + 1 ..< window.originY + newHeight, candidates)
         if (candidates.any { windowList[it].xRange.last >= window.originX })
            return ResizeResult.DoesNotFit
      }

      // update the window and trees
      removeFromUpperLeftTree(window)
      removeFromLowerRightTree(window)
      window.resize(newWidth, newHeight)
      addToUpperLeftTree(window)
      addToLowerRightTree(window)
      assert(upperLeft2DTree.size() == lowerRight2DTree.size())

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

   fun windowIterator(): Iterator<Window> {
      return windowList.asSequence().filterNot { it.killed }.iterator()
   }

   private fun findWindow(atX: Int, atY: Int): Window? {
      return windowList.firstOrNull { !it.killed && it.xRange.contains(atX) && it.yRange.contains(atY) }
   }

   private fun moveWithProjector(window: Window, amount: Int, projector: DimensionProjector): MoveResult {
      // the full array of moved windows
      val movedWindows = mutableListOf(window)
      projector.findCollisionsTo(window, amount, windowList, movedWindows)

      // Keep a set of moved windows for quick filtering of duplicate collisions
      val movedWindowIDs = BitSet()
      for (window in movedWindows) {
         movedWindowIDs.set(window.ID)
      }

      val freeSpaceBefore = ArrayList<Int>(min(10, movedWindows.size))
      freeSpaceBefore.add(0) // initial window has 0 free space before
      val furthestWindowRanges = IntervalTreeMap<Int>()
      furthestWindowRanges.insert(projector.crossAxisRange(window), 0)
      var processedWindowCount = 1

      do {
         // have the projector sort the new portion of movedWindows, needed for free space calculation
         projector.sortWindows(movedWindows.subList(processedWindowCount, movedWindows.size))
         val overlappersToRemove = mutableListOf<IntervalTreeMap.OverlapResult<Int>>()

         // Process collisions: first compute their freeSpaceBefore
         for (i in processedWindowCount ..< movedWindows.size) {
            val movedWindow = movedWindows[i]
            freeSpaceBefore.add(amount)
            val crossAxisRange = projector.crossAxisRange(movedWindow)
            val overlappers = furthestWindowRanges.overlappers(crossAxisRange)
            var leadingOverlapperReplacement: IntervalTreeMap.OverlapResult<Int>? = null
            var trailingOverlapperReplacement: IntervalTreeMap.OverlapResult<Int>? = null

            // We need to put current window's cross-axis range in the interval tree and split
            // overlappers that intersect it, subtracting out the part of current window;
            // we know that overlappers are sorted from min to max and are mutually non-intersecting
            for (overlapper in overlappers) {
               val spaceBetween = projector.spaceBetween(movedWindows[overlapper.ancillary], movedWindow)
               freeSpaceBefore[i] = min(freeSpaceBefore[i], spaceBetween + freeSpaceBefore[overlapper.ancillary])
               assert(spaceBetween + freeSpaceBefore[overlapper.ancillary]>= 0)

               // all of the current overlappers will be removed because they change
               overlappersToRemove.add(overlapper)
               val overlapperInterval = overlapper.interval
               if (overlapperInterval.first < crossAxisRange.first) {
                  // case 1: the overlapper starts before window (it must be first overlapper) and ends before it,
                  // we mark the overlapper for replacement by its prefix before window
                  val leadingInterval = IntRange(overlapperInterval.first, crossAxisRange.first - 1)
                  if (overlapperInterval.last < crossAxisRange.last) {
                     leadingOverlapperReplacement = IntervalTreeMap.OverlapResult(leadingInterval, overlapper.ancillary)
                  } else {
                     // case 2: the overlapper subsumes the window, but window interval must replace it, splitting it.
                     leadingOverlapperReplacement = IntervalTreeMap.OverlapResult(leadingInterval, overlapper.ancillary)
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
            }
            for (overlapper in overlappersToRemove) {
               furthestWindowRanges.delete(overlapper.interval)
            }
            leadingOverlapperReplacement?.let {
               furthestWindowRanges.insert(it.interval, it.ancillary) // keep the reference coordinate
            }
            trailingOverlapperReplacement?.let {
               furthestWindowRanges.insert(it.interval, it.ancillary) // keep the reference coordinate
            }
            // important! must insert after removing overlappers
            furthestWindowRanges.insert(crossAxisRange, i)
         }

         // search the new collision regions for newly hit windows
         // additional collisions are possible for entries in the interval tree based on how much frees pace is before them:
         // they extend the collision space by the requested amount plus their thickness past the prior search range
         // minus the free space before
         // TODO: avoid filter
         val searchAreaSequence = furthestWindowRanges.iterator().asSequence()
            .filter { it.ancillary >= processedWindowCount }
            .map { SearchArea(movedWindows[it.ancillary], it.interval, freeSpaceBefore[it.ancillary]) }
         val newCollisions = projector.findCollisionsInSearchRegions(searchAreaSequence, amount, windowList)

         processedWindowCount = movedWindows.size

         // dedupe them and add them to the movedWindows
         for (collision in newCollisions) {
            if (!movedWindowIDs.get(collision.ID)) {
               movedWindowIDs.set(collision.ID)
               movedWindows.add(collision)
            }
         }
         // repeat if any new windows were collided
      } while (movedWindows.size != processedWindowCount)

      // the max amount we can actually push is the minimum of a) the requested amount
      // b) the minimum trailing space to boundary of any moved window plus the free space before it
      // have to write a manual min because I need an index
      var maxPushAmount = projector.mainAxisLength
      for (i in 0 ..< movedWindows.size) {
         val movedWindow = movedWindows[i]
         maxPushAmount = min(maxPushAmount, projector.trailingSpaceToBound(movedWindow) + freeSpaceBefore[i])
      }
      maxPushAmount = min(maxPushAmount, amount)
      assert(maxPushAmount >= 0)

      // we have the free space before all hit windows, we have the amount to push maxPushAMount
      // we can calculate the amount each window moved as: maxPushAmount - freeSpace
      // cut out the windows from the 2D trees, their points cant be moved
      // The windows have to be all removed, update their coordinates and then re-added because a window can move to a previous
      // window's position, and the tree doesn't allow duplicates
      movedWindows.forEachIndexed { index, movedWindow ->
         removeFromUpperLeftTree(movedWindow)
         removeFromLowerRightTree(movedWindow)
         val movedAmount = maxPushAmount - freeSpaceBefore[index]
         projector.moveWindowBy(movedWindow, movedAmount)
      }
      for (movedWindow in movedWindows) {
         addToUpperLeftTree(movedWindow)
         addToLowerRightTree(movedWindow)
      }

      assert(upperLeft2DTree.size() == lowerRight2DTree.size())
      val code = if (maxPushAmount == amount) MoveResult.OK else MoveResult.MovedLess
      return MoveResult(code, maxPushAmount)
   }

   private fun nextID(): Int {
      val next = nextID
      nextID += 1
      return next
   }

   private inline fun addToUpperLeftTree(window: Window) {
      upperLeft2DTree.insert(window.originX, window.originY, window.ID)
   }

   private inline fun addToLowerRightTree(window: Window) {
      lowerRight2DTree.insert(window.xRange.last, window.yRange.last, window.ID)
   }

   private inline fun removeFromUpperLeftTree(window: Window) {
      upperLeft2DTree.delete(window.originX, window.originY)
   }

   private inline fun removeFromLowerRightTree(window: Window) {
      lowerRight2DTree.delete(window.xRange.last, window.yRange.last)
   }
}

private class SearchArea(val window: Window, val crossAxisRange: IntRange, val freeSpaceBefore: Int)

private abstract class DimensionProjector protected constructor(
   val dimension: Dimension,
   val mainAxisLength: Int,
) {
   abstract fun moveWindowBy(movedWindow: Window, movedAmount: Int)
   abstract fun crossAxisRange(window: Window): IntRange

   //   abstract fun mainAxisRange(window: Window): IntRange
   abstract fun afterTrailingEdge(window: Window, amount: Int = 0): Int
   abstract fun trailingSpaceToBound(window: Window): Int
   abstract fun findCollisionsTo(window: Window, width: Int, windowList: List<Window>, dest: MutableList<Window>)
   abstract fun findCollisionsInSearchRegions(searchAreas: Sequence<SearchArea>, referenceAmount: Int, windowList: List<Window>): List<Window>
   abstract fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int
   abstract fun sortWindows(list: MutableList<Window>)

   companion object {
      fun create(
         dimension: Dimension,
         spaceBounds: Rectangle,
         towardsPositiveInfinity: Boolean,
         upperLeftTree: TwoDTreeMap<Int>,
         lowerRightTree: TwoDTreeMap<Int>,
      ): DimensionProjector {
         return if (dimension == Dimension.X) {
            if (towardsPositiveInfinity) {
               object : DimensionProjector(Dimension.X, spaceBounds.width) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveX(movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.yRange

                  override fun afterTrailingEdge(window: Window, amount: Int): Int =
                     window.xRange.last + amount

                  override fun trailingSpaceToBound(window: Window): Int =
                     spaceBounds.width - window.xRange.last

                  override fun findCollisionsTo(window: Window, width: Int, windowList: List<Window>, dest: MutableList<Window>) {
                     upperLeftTree
                        .findAll(afterTrailingEdge(window, 1)..afterTrailingEdge(window, width), spaceBounds.y..window.yRange.last)
                        .mapNotNullTo(dest) { if (windowList[it].yRange.last >= window.originY) windowList[it] else null }
                  }

                  override fun findCollisionsInSearchRegions(
                     searchAreas: Sequence<SearchArea>,
                     referenceAmount: Int,
                     windowList: List<Window>,
                  ): List<Window> {
                     val results = mutableListOf<Window>()
                     for (searchArea in searchAreas) {
                        val searchLeft = afterTrailingEdge(searchArea.window, 1)
                        val searchRight = afterTrailingEdge(searchArea.window, referenceAmount - searchArea.freeSpaceBefore)
                        upperLeftTree.findAll(
                           searchLeft..searchRight,
                           spaceBounds.y..searchArea.crossAxisRange.last
                        ).mapNotNullTo(results) {
                           if (windowList[it].yRange.last >= searchArea.crossAxisRange.first) windowList[it] else null
                        }
                     }
                     return results
                  }

                  override fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int =
                     laterWindow.originX - (earlyWindow.xRange.last + 1)

                  override fun sortWindows(list: MutableList<Window>) {
                     list.sortBy { it.originX }
                  }

               }
            } else {
               object : DimensionProjector(Dimension.X, spaceBounds.width) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveX(-movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.yRange

                  override fun afterTrailingEdge(window: Window, amount: Int): Int =
                     window.originX - amount

                  override fun trailingSpaceToBound(window: Window): Int =
                     window.originX - spaceBounds.x

                  override fun findCollisionsTo(window: Window, width: Int, windowList: List<Window>, dest: MutableList<Window>) {
                     lowerRightTree
                        .findAll(window.originX - width..<window.originX, window.originY..spaceBounds.height)
                        .mapNotNullTo(dest) {
                           if (windowList[it].originY <= window.yRange.last) windowList[it] else null
                        }
                  }

                  override fun findCollisionsInSearchRegions(searchAreas: Sequence<SearchArea>, referenceAmount: Int, windowList: List<Window>): List<Window> {
                     val results = mutableListOf<Window>()
                     for (searchArea in searchAreas) {
                        val searchRight = afterTrailingEdge(searchArea.window, 1)
                        val searchLeft = afterTrailingEdge(searchArea.window, referenceAmount - searchArea.freeSpaceBefore)
                        lowerRightTree.findAll(
                           searchLeft..searchRight,
                           searchArea.crossAxisRange.first..spaceBounds.height
                        ).mapNotNullTo(results) {
                           if (windowList[it].originY <= searchArea.crossAxisRange.last) windowList[it] else null
                        }
                     }
                     return results
                  }

                  override fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int =
                     earlyWindow.originX - (laterWindow.xRange.last + 1)

                  override fun sortWindows(list: MutableList<Window>) {
                     list.sortByDescending { it.originX }
                  }

               }
            }
         } else {
            if (towardsPositiveInfinity) {
               object : DimensionProjector(Dimension.Y, spaceBounds.height) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveY(movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.xRange

                  override fun afterTrailingEdge(window: Window, amount: Int): Int =
                     window.yRange.last + amount

                  override fun trailingSpaceToBound(window: Window): Int =
                     spaceBounds.height - window.yRange.last

                  override fun findCollisionsTo(window: Window, width: Int, windowList: List<Window>, dest: MutableList<Window>) {
                     upperLeftTree
                        .findAll(spaceBounds.x..window.xRange.last, window.yRange.last + 1..window.yRange.last + width)
                        .mapNotNullTo(dest) { if (windowList[it].xRange.last >= window.xRange.first) windowList[it] else null }
                  }

                  override fun findCollisionsInSearchRegions(searchAreas: Sequence<SearchArea>, referenceAmount: Int, windowList: List<Window>): List<Window> {
                     val results = mutableListOf<Window>()
                     for (searchArea in searchAreas) {
                        val searchTop = afterTrailingEdge(searchArea.window, 1)
                        val searchBottom = afterTrailingEdge(searchArea.window, referenceAmount - searchArea.freeSpaceBefore)
                        upperLeftTree.findAll(
                           spaceBounds.x..searchArea.crossAxisRange.last,
                           searchTop..searchBottom
                        ).mapNotNullTo(results) {
                           if (windowList[it].xRange.last >= searchArea.crossAxisRange.first) windowList[it] else null
                        }
                     }
                     return results
                  }

                  override fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int =
                     laterWindow.originY - (earlyWindow.yRange.last + 1)

                  override fun sortWindows(list: MutableList<Window>) {
                     list.sortBy { it.originY }
                  }

               }
            } else {
               object : DimensionProjector(Dimension.Y, spaceBounds.height) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveY(-movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.xRange

                  override fun afterTrailingEdge(window: Window, amount: Int): Int =
                     window.originY - amount

                  override fun trailingSpaceToBound(window: Window): Int =
                     window.originY - spaceBounds.y

                  override fun findCollisionsTo(window: Window, width: Int, windowList: List<Window>, dest: MutableList<Window>) {
                     lowerRightTree
                        .findAll(window.originX..spaceBounds.width, window.originY - width..<window.originY)
                        .mapNotNullTo(dest) { if (windowList[it].xRange.first <= window.xRange.last) windowList[it] else null }
                  }

                  override fun findCollisionsInSearchRegions(searchAreas: Sequence<SearchArea>, referenceAmount: Int, windowList: List<Window>): List<Window> {
                     val results = mutableListOf<Window>()
                     for (searchArea in searchAreas) {
                        val searchBottom = afterTrailingEdge(searchArea.window, 1)
                        val searchTop = afterTrailingEdge(searchArea.window, referenceAmount - searchArea.freeSpaceBefore)
                        lowerRightTree.findAll(
                           searchArea.crossAxisRange.first..spaceBounds.width,
                           searchTop..searchBottom
                        ).mapNotNullTo(results) {
                           if (windowList[it].originX <= searchArea.crossAxisRange.last) windowList[it] else null
                        }
                     }
                     return results
                  }

                  override fun spaceBetween(earlyWindow: Window, laterWindow: Window): Int =
                     earlyWindow.originY - (laterWindow.yRange.last + 1)

                  override fun sortWindows(list: MutableList<Window>) {
                     list.sortByDescending { it.originY }
                  }

               }
            }

         }
      }
   }
}

fun simulateWindowManager(width: Int, height: Int, commands: List<WindowManager.Command>, params: List<IntArray>):
   Triple<List<String>, Int, Iterator<Window>>
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
   return Triple(commandOutputs, wm.windowCount, wm.windowIterator())
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
            writer.write("${window.originX} ${window.originY} ${window.width} ${window.height}\n")
         }
      }
   }
}

fun main() {
   simulateWindowManagerIO(System.`in`, System.out)
}