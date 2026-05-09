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

   override fun hashCode(): Int {
      var result = height
      result = 37 * result + width
      result = 43 * result + originY
      result = 47 * result + originX
      return result
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
      return moveWithProjector(window, byX, projector)
   }

   fun moveY(atX: Int, atY: Int, byY: Int): MoveResult {
      val window = findWindow(atX, atY)
      if (window == null)
         return MoveResult(MoveResult.NoWindowFound)
      if (byY == 0)
         return MoveResult(MoveResult.OK)

      // get the projector
      val projector = if (byY > 0) dimensionProjectorYPositive else dimensionProjectorYNegative
      return moveWithProjector(window, byY, projector)
   }

   fun windowIterator(): Iterator<Window> {
      return windowList.asSequence().filterNot { it.killed }.iterator()
   }

   private fun findWindow(atX: Int, atY: Int): Window? {
      return windowList.firstOrNull { !it.killed && it.xRange.contains(atX) && it.yRange.contains(atY) }
   }

   private fun moveWithProjector(window: Window, by: Int, projector: DimensionProjector): MoveResult {
      val pushList = ArrayList<PushInfo>()
      var remainder = abs(by)
      var minTrailingSpaceToBound = projector.trailingSpaceToBound(window)
      val collisionWindows = projector.findCollisions(window, remainder, windowList)
      var movedAmount = 0
      val movedWindowIDs = BitSet()
      movedWindowIDs.set(window.ID)

      // QUICK PATH: if there are no windows to collide with, skip the push logic
      if (collisionWindows.isNotEmpty()) {
         // The collisionSet holds windows that would definitely be in collision path, IF window is moved by 'by'.
         val collisionSet = HashSet(collisionWindows)
         // This interval tree is maintained so that there are no overlapping intervals. The value stored is
         // the window's "reference coordinate" of its trailing edge on the main axis - i.e. its trailing edge minus the amount
         // the original window has moved so far
         val pushedWindowCrossAxisRanges = IntervalTreeMap<Int>()
         pushedWindowCrossAxisRanges.insert(projector.crossAxisRange(window), projector.beforeTrailingEdge(window, 0))
         val overlappersToRemove = mutableListOf<IntervalTreeMap.OverlapResult<Int>>()
         var pendingWindows = mutableListOf(window)
         var hitWindows = mutableListOf<Window>()

         while (remainder > 0 && minTrailingSpaceToBound > 0) {
            if (collisionSet.isEmpty()) {
               val space = min(remainder, minTrailingSpaceToBound)
               if (space > 0) {
                  pushList.add(PushInfo(space, pendingWindows))
                  remainder -= space
               }
               break
            }

            // find all windows whose leading space to the pushed clump is minimal
            var minimumSpace = remainder
            for (collisionWindow in collisionSet) {
               val pushedWindowTrailingEdge =
                  projector.greatestMainAxisOffsetInSequenceAdding(
                     pushedWindowCrossAxisRanges
                        .overlappers(projector.crossAxisRange(collisionWindow))
                        .asSequence().map { it.ancillary },
                     movedAmount
                  )
               val leadingSpace = projector.beforeLeadingEdge(collisionWindow, pushedWindowTrailingEdge)
               assert(leadingSpace >= 0)
               if (leadingSpace < minimumSpace) {
                  minimumSpace = leadingSpace
                  hitWindows.clear()
                  hitWindows.add(collisionWindow)
               } else if (leadingSpace == minimumSpace) {
                  hitWindows.add(collisionWindow)
               }
            }

            @Suppress("ConvertArgumentToSet")
            collisionSet.removeAll(hitWindows)

            // hit window calculations use updated data here
            minTrailingSpaceToBound -= minimumSpace
            remainder -= minimumSpace
            movedAmount += minimumSpace

            for (hitWindow in hitWindows) {
               movedWindowIDs.set(hitWindow.ID)
               val hitWindowCrossRange = projector.crossAxisRange(hitWindow)
               val overlappers = pushedWindowCrossAxisRanges.overlappers(hitWindowCrossRange)
               var leadingOverlapperReplacement: IntervalTreeMap.OverlapResult<Int>? = null
               var trailingOverlapperReplacement: IntervalTreeMap.OverlapResult<Int>? = null

               // for each hit window, more windows may become collision candidates
               val newCollisions = projector.findCollisions(hitWindow, remainder, windowList)
               collisionSet.addAll(newCollisions)

               // We need to set the hitWindow's cross-axis range in the interval tree and split
               // overlappers that intersect it, subtracting out the part of pushedWindow.
               // we know that overlappers are sorted from min to max and are mutually non-intersecting
               for (overlapper in overlappers) {
                  // all of the overlappers will be removed because they change
                  overlappersToRemove.add(overlapper)
                  val overlapperInterval = overlapper.interval
                  if (overlapperInterval.first < hitWindowCrossRange.first) {
                     // case 1: the overlapper starts before window (it must be first overlapper) and ends before it,
                     // we mark the overlapper for replacement by its prefix before window
                     val leadingInterval = IntRange(overlapperInterval.first, hitWindowCrossRange.first - 1)
                     if (overlapperInterval.last < hitWindowCrossRange.last) {
                        leadingOverlapperReplacement = IntervalTreeMap.OverlapResult(leadingInterval, overlapper.ancillary)
                     } else {
                        // case 2: the overlapper subsumes the window, but window interval must replace it, splitting it.
                        leadingOverlapperReplacement = IntervalTreeMap.OverlapResult(leadingInterval, overlapper.ancillary)
                        val trailingInterval = IntRange(hitWindowCrossRange.last + 1, overlapperInterval.last)
                        trailingOverlapperReplacement = IntervalTreeMap.OverlapResult(trailingInterval, overlapper.ancillary)
                     }
                  }
                  // case 3: the overlapper starts after the window starts, and extends past it
                  else if (overlapperInterval.last > hitWindowCrossRange.last) {
                     val trailingInterval = IntRange(hitWindowCrossRange.last + 1, overlapperInterval.last)
                     trailingOverlapperReplacement = IntervalTreeMap.OverlapResult(trailingInterval, overlapper.ancillary)
                  }
                  // in case 4, the window subsumes the overlapper, we do nothing since we remove overlapper anyway
               }
               for (overlapper in overlappersToRemove) {
                  pushedWindowCrossAxisRanges.delete(overlapper.interval)
               }
               // important! must insert after removing overlappers
               pushedWindowCrossAxisRanges.insert(hitWindowCrossRange, projector.beforeTrailingEdge(hitWindow, movedAmount))
               leadingOverlapperReplacement?.let {
                  pushedWindowCrossAxisRanges.insert(it.interval, it.ancillary) // keep the reference coordinate
               }
               trailingOverlapperReplacement?.let {
                  pushedWindowCrossAxisRanges.insert(it.interval, it.ancillary) // keep the reference coordinate
               }
               overlappersToRemove.clear()
               minTrailingSpaceToBound = min(minTrailingSpaceToBound, projector.trailingSpaceToBound(hitWindow))
            }

            // minimumSpace might be zero after a)first pass b) hitWindows contains a wide window of width larger than
            // the search width ('remainder')
            if (minimumSpace > 0) {
               pushList.add(PushInfo(minimumSpace, pendingWindows))
               pendingWindows = hitWindows
               hitWindows = mutableListOf()
            } else {
               pendingWindows.addAll(hitWindows)
               hitWindows.clear()
            }
         }
      } else {
         val space = min(remainder, minTrailingSpaceToBound)
         if (space > 0) {
            pushList.add(PushInfo(space, listOf(window)))
            remainder -= space
         }
      }

      // update the windows and trees
      // The windows have to be all removed, update their coordinates and then re-added
      var windowID = movedWindowIDs.nextSetBit(0)
      while (windowID >= 0) {
         removeFromUpperLeftTree(windowList[windowID])
         removeFromLowerRightTree(windowList[windowID])
         windowID = movedWindowIDs.nextSetBit(windowID + 1)
      }

      movedAmount = 0
      for (info in pushList.asReversed()) {
         movedAmount += info.distance
         for (movedWindow in info.windows) {
            projector.moveWindowBy(movedWindow, movedAmount)
         }
      }

      windowID = movedWindowIDs.nextSetBit(0)
      while (windowID >= 0) {
         addToUpperLeftTree(windowList[windowID])
         addToLowerRightTree(windowList[windowID])
         windowID = movedWindowIDs.nextSetBit(windowID + 1)
      }

      assert(upperLeft2DTree.size() == lowerRight2DTree.size())
      val code = if (remainder == 0) MoveResult.OK else MoveResult.MovedLess
      return MoveResult(code, movedAmount)
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

private class PushInfo(val distance: Int, val windows: List<Window>)

private abstract class DimensionProjector protected constructor(
   val dimension: Dimension,
) {
   abstract fun moveWindowBy(movedWindow: Window, movedAmount: Int)
   abstract fun crossAxisRange(window: Window): IntRange
   abstract fun mainAxisRange(window: Window): IntRange
   abstract fun trailingSpaceToBound(window: Window): Int
   abstract fun findCollisions(window: Window, width: Int, windowList: List<Window>): List<Window>
   abstract fun beforeTrailingEdge(window: Window, amount: Int): Int
   abstract fun beforeLeadingEdge(window: Window, amount: Int): Int // TODO : rename
   abstract fun greatestMainAxisOffsetInSequenceAdding(seq: Sequence<Int>, amount: Int): Int

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
               object : DimensionProjector(Dimension.X) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveX(movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.yRange

                  override fun mainAxisRange(window: Window): IntRange =
                     window.xRange

                  override fun trailingSpaceToBound(window: Window): Int =
                     spaceBounds.width - window.xRange.last

                  override fun findCollisions(window: Window, width: Int, windowList: List<Window>): List<Window> {
                     return upperLeftTree
                        .findAll(window.xRange.last + 1..window.xRange.last + width, spaceBounds.y..window.yRange.last)
                        .mapNotNull { if (windowList[it].yRange.last >= window.yRange.first) windowList[it] else null }
                  }

                  override fun beforeTrailingEdge(window: Window, amount: Int): Int =
                     window.xRange.last - amount

                  override fun beforeLeadingEdge(window: Window, amount: Int): Int =
                     window.xRange.first - (amount + 1)

                  override fun greatestMainAxisOffsetInSequenceAdding(seq: Sequence<Int>, amount: Int): Int =
                     seq.max() + amount

               }
            } else {
               object : DimensionProjector(Dimension.X) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveX(-movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.yRange

                  override fun mainAxisRange(window: Window): IntRange =
                     window.xRange

                  override fun trailingSpaceToBound(window: Window): Int =
                     window.xRange.first - spaceBounds.x

                  override fun findCollisions(window: Window, width: Int, windowList: List<Window>): List<Window> {
                     return lowerRightTree
                        .findAll(window.xRange.first - width..< window.xRange.first, window.yRange.first .. spaceBounds.height)
                        .mapNotNull { if (windowList[it].yRange.first <= window.yRange.last) windowList[it] else null }
                  }

                  override fun beforeTrailingEdge(window: Window, amount: Int): Int =
                     window.xRange.first + amount

                  override fun beforeLeadingEdge(window: Window, amount: Int): Int =
                     amount - 1 - window.xRange.last

                  override fun greatestMainAxisOffsetInSequenceAdding(seq: Sequence<Int>, amount: Int): Int =
                     seq.min() - amount
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

                  override fun mainAxisRange(window: Window): IntRange =
                     window.yRange

                  override fun trailingSpaceToBound(window: Window): Int =
                     spaceBounds.height - window.yRange.last

                  override fun findCollisions(window: Window, width: Int, windowList: List<Window>): List<Window> {
                     return upperLeftTree
                        .findAll(spaceBounds.x .. window.xRange.last, window.yRange.last + 1 .. window.yRange.last + width)
                        .mapNotNull { if (windowList[it].xRange.last >= window.xRange.first) windowList[it] else null }
                  }

                  override fun beforeTrailingEdge(window: Window, amount: Int): Int =
                     window.yRange.last - amount

                  override fun beforeLeadingEdge(window: Window, amount: Int): Int =
                     window.yRange.first - (amount + 1)

                  override fun greatestMainAxisOffsetInSequenceAdding(seq: Sequence<Int>, amount: Int): Int =
                     seq.max() + amount

               }
            } else {
               object : DimensionProjector(Dimension.Y) {
                  override fun moveWindowBy(movedWindow: Window, movedAmount: Int) {
                     movedWindow.moveY(-movedAmount)
                  }

                  override fun crossAxisRange(window: Window): IntRange =
                     window.xRange

                  override fun mainAxisRange(window: Window): IntRange =
                     window.yRange

                  override fun trailingSpaceToBound(window: Window): Int =
                     window.yRange.first - spaceBounds.y

                  override fun findCollisions(window: Window, width: Int, windowList: List<Window>): List<Window> {
                     return lowerRightTree
                        .findAll(window.xRange.first .. spaceBounds.width, window.yRange.first - width..< window.yRange.first)
                        .mapNotNull { if (windowList[it].xRange.first <= window.xRange.last) windowList[it] else null }
                  }

                  override fun beforeTrailingEdge(window: Window, amount: Int): Int =
                     window.yRange.first + amount

                  override fun beforeLeadingEdge(window: Window, amount: Int): Int =
                     amount - 1 - window.yRange.last

                  override fun greatestMainAxisOffsetInSequenceAdding(seq: Sequence<Int>, amount: Int): Int =
                     seq.min() - amount

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
            if (!wm.open(params[0], params[1], params[2], params[3])) {
               commandOutputs += "Command $number: OPEN - $doesNotFit"
            }
         }
         WindowManager.Command.RESIZE -> {
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