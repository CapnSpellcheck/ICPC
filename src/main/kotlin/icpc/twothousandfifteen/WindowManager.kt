package icpc.twothousandfifteen

import icpc.twothousandfifteen.DimensionHeuristic.Advice
import util.IntervalTree
import util.intersects
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class Dimension { X, Y }

class Window(originX: Int, originY: Int, private var width: Int, private var height: Int) {
   var originY = originY; private set
   var originX = originX; private set
   var xRange: IntRange = IntRange.EMPTY; private set
   var yRange: IntRange = IntRange.EMPTY; private set

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

   override fun toString(): String {
      return "Window(x=$originX, y=$originY, w=$width, h=$height)"
   }

}

class WindowManager(val screenWidth: Int, val screenHeight: Int) {
   private val intervalXTree = IntervalTree<Window>()
   private val intervalYTree = IntervalTree<Window>()
   private val dimensionHeuristic = DimensionHeuristic()
   private val dimensionProjectorXPositive = DimensionProjector.create(
      Dimension.X,
      screenWidth - 1,
      true,
      intervalXTree,
      intervalYTree,
      ::removeFromXTree,
      ::addToXTree,
      ::removeFromYTree,
      ::addToYTree
   )
   private val dimensionProjectorXNegative = DimensionProjector.create(
      Dimension.X,
      0,
      false,
      intervalXTree,
      intervalYTree,
      ::removeFromXTree,
      ::addToXTree,
      ::removeFromYTree,
      ::addToYTree
   )
   private val dimensionProjectorYPositive = DimensionProjector.create(
      Dimension.Y,
      screenHeight - 1,
      true,
      intervalXTree,
      intervalYTree,
      ::removeFromXTree,
      ::addToXTree,
      ::removeFromYTree,
      ::addToYTree
   )
   private val dimensionProjectorYNegative = DimensionProjector.create(
      Dimension.Y,
      0,
      false,
      intervalXTree,
      intervalYTree,
      ::removeFromXTree,
      ::addToXTree,
      ::removeFromYTree,
      ::addToYTree
   )

   enum class ResizeResult {
      OK,
      NoWindowFound,
      DoesNotFit
   }

   data class MoveResult(val code: Int, val movedAmount: Int = 0) {
      companion object {
         const val OK = 0
         const val NoWindowFound = 1
         const val MovedLess = 2
      }
   }

   fun open(originX: Int, originY: Int, width: Int, height: Int): Boolean {
      val xRange = IntRange(originX, originX + width - 1)
      val yRange = IntRange(originY, originY + height - 1)
      if (xRange.last >= screenWidth || originX < 0 || yRange.last >= screenHeight || originY < 0)
         return false

      val xOverlaps = intervalXTree.overlappers(xRange)
      val yOverlaps = intervalYTree.overlappers(yRange)
      if (xOverlaps.hasNext() && yOverlaps.hasNext()) {
         if (dimensionHeuristic.getAdvice() == Advice.Y) {
            val yOverlaps = yOverlaps.asSequence().toList()
            dimensionHeuristic.addYObservation(yOverlaps.size)
            for (xOverlap in xOverlaps) {
               val collision = yOverlaps.firstOrNull { it.ancillary == xOverlap.ancillary }
               if (collision != null) {
                  return false
               }
            }
         } else {
            val xOverlaps = xOverlaps.asSequence().toList()
            dimensionHeuristic.addXObservation(xOverlaps.size)
            for (yOverlap in yOverlaps) {
               val collision = xOverlaps.firstOrNull { it.ancillary == yOverlap.ancillary }
               if (collision != null) {
                  return false
               }
            }
         }
      }

      val window = Window(originX, originY, width, height)
      addToXTree(window)
      addToYTree(window)
      return true
   }

   fun close(atX: Int, atY: Int): Boolean {
      val window = findWindow(atX, atY)
      window?.let {
         removeFromXTree(window)
         removeFromYTree(window)
         return true
      }
      return false
   }

   fun resize(atX: Int, atY: Int, newWidth: Int, newHeight: Int): ResizeResult {
      val window = findWindow(atX, atY) ?: return ResizeResult.NoWindowFound
      if (window.originX + newWidth > screenWidth || window.originY + newHeight > screenHeight)
         return ResizeResult.DoesNotFit

      // need to do a thorough overlap check on new coords
      val newXRange = IntRange(window.originX, window.originX + newWidth - 1)
      val newYRange = IntRange(window.originY, window.originY + newHeight - 1)
      val xOverlaps = intervalXTree.overlappers(newXRange)
      val yOverlaps = intervalYTree.overlappers(newYRange)
      if (dimensionHeuristic.getAdvice() == Advice.Y) {
         val yOverlaps = yOverlaps.asSequence().toList()
         dimensionHeuristic.addYObservation(yOverlaps.size)
         for (xOverlap in xOverlaps) {
            if (yOverlaps.any { it.ancillary == xOverlap.ancillary && it.ancillary != window })
               return ResizeResult.DoesNotFit
         }
      } else {
         val xOverlaps = xOverlaps.asSequence().toList()
         dimensionHeuristic.addXObservation(xOverlaps.size)
         for (yOverlap in yOverlaps) {
            if (xOverlaps.any { it.ancillary == yOverlap.ancillary && it.ancillary != window })
               return ResizeResult.DoesNotFit
         }
      }

      // update the window and trees
      removeFromXTree(window)
      removeFromYTree(window)
      window.resize(newWidth, newHeight)
      addToXTree(window)
      addToYTree(window)

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

   private fun findWindow(atX: Int, atY: Int): Window? {
      val xOverlaps = intervalXTree.overlappers(IntRange(atX, atX))
      val yOverlaps = intervalYTree.overlappers(IntRange(atY, atY))
      if (xOverlaps.hasNext() && yOverlaps.hasNext()) {
         if (dimensionHeuristic.getAdvice() == Advice.Y) {
            val yOverlaps = yOverlaps.asSequence().toList()
            dimensionHeuristic.addYObservation(yOverlaps.size)
            for (xOverlap in xOverlaps) {
               yOverlaps.firstOrNull { it.ancillary == xOverlap.ancillary }
                  ?.let { return it.ancillary }
            }
         } else {
            val xOverlaps = xOverlaps.asSequence().toList()
            dimensionHeuristic.addXObservation(xOverlaps.size)
            for (yOverlap in yOverlaps) {
               xOverlaps.firstOrNull { it.ancillary == yOverlap.ancillary }
                  ?.let { return it.ancillary }
            }
         }
      }
      return null
   }

   private fun moveWithProjector(window: Window, by: Int, projector: DimensionProjector): MoveResult {
      // calc the collision range on x-axis
      val collisionRange = projector.intervalPastWindowToBound(window)

      // find overlaps on axis that isn't moving - Y axis
      val crossAxisOverlappers = projector.crossAxisOverlappers(window)
      val overlapWindows = crossAxisOverlappers.asSequence()
         .filter(projector.mainAxisIntersectorProvider(collisionRange))
         .map { it.ancillary }
         .toMutableList()
      overlapWindows.sortWith(projector.overlapWindowComparator)

      // the "push" algorithm: remainder = byX; pushList = empty; repeat while remainder > 0:
      // find the next window in line
      // is the space before it >= remainder? If so, you're done - compute the resting positions
      // No? Add a 'move note' for distance = space (clamped by screen edge*) and windows = pushList.
      // If space was == 0, add window to pushList without adding move note.
      // If space was > 0, clear pushList
      // If remainder != 0 after loop, check how much space there is between last window and screen.
      // To process the move notes, work backward.
      val pushList = ArrayList<PushInfo>(overlapWindows.size)
      var pendingList = mutableListOf<Window>()
      pendingList.add(window)
      var i = 0
      var lastWindowEnd = projector.trailingEdge(window)
      var remainder = by
      while (remainder != 0 && i < overlapWindows.size) {
         val overlapWindow = overlapWindows[i]
         val space = projector.spaceToStartOfWindow(remainder, lastWindowEnd, overlapWindow)

         if (space != 0) {
            pushList.add(PushInfo(space, pendingList))
            pendingList = mutableListOf()
         }
         pendingList.add(overlapWindow)

         lastWindowEnd = projector.trailingEdge(overlapWindow)
         remainder -= space
         i += 1
      }

      // try to move remainder after last overlap
      val space = projector.trailingSpaceToBound(remainder, lastWindowEnd)
      if (space != 0) {
         pushList.add(PushInfo(space, pendingList))
         remainder -= space
      }

      // update the windows and trees
      var movedAmount = 0
      pushList.asReversed().forEach { info ->
         movedAmount += info.distance
         for (movedWindow in info.windows) {
            projector.removeFromMainAxisTree(movedWindow)
            movedWindow.moveBy(projector.dimension, movedAmount)
            projector.addToMainAxisTree(movedWindow)
         }
      }

      val code = if (remainder == 0) MoveResult.OK else MoveResult.MovedLess
      return MoveResult(code, movedAmount)
   }

   private inline fun addToXTree(window: Window) {
      intervalXTree.insert(window.xRange, window, window.originY)
   }

   private inline fun addToYTree(window: Window) {
      intervalYTree.insert(window.yRange, window, window.originX)
   }

   private inline fun removeFromXTree(window: Window) {
      intervalXTree.delete(window.xRange, window.originY)
   }

   private inline fun removeFromYTree(window: Window) {
      intervalYTree.delete(window.yRange, window.originX)
   }
}

private class PushInfo(val distance: Int, val windows: List<Window>)

private abstract class DimensionProjector protected constructor(
   val dimension: Dimension,
   val overlapWindowComparator: Comparator<Window>,
   val mainAxisIntersectorProvider: (IntRange) -> (IntervalTree.OverlapResult<Window>) -> Boolean
) {
   abstract fun intervalPastWindowToBound(window: Window): IntRange
   abstract fun crossAxisOverlappers(window: Window): Iterator<IntervalTree.OverlapResult<Window>>
   abstract fun trailingEdge(window: Window): Int
   abstract fun addToMainAxisTree(window: Window)
   abstract fun removeFromMainAxisTree(window: Window)
   abstract fun trailingSpaceToBound(maximum: Int, from: Int): Int
   abstract fun spaceToStartOfWindow(maximum: Int, from: Int, window: Window): Int

   companion object {
      fun create(
         dimension: Dimension,
         dimMaximum: Int,
         towardsPositiveInfinity: Boolean,
         xTree: IntervalTree<Window>,
         yTree: IntervalTree<Window>,
         removeFromXTree: (Window) -> Unit,
         addToXTree: (Window) -> Unit,
         removeFromYTree: (Window) -> Unit,
         addToYTree: (Window) -> Unit,
      ): DimensionProjector {
         return if (dimension == Dimension.X) {
            if (towardsPositiveInfinity) {
               object : DimensionProjector(Dimension.X, windowXPositiveComparator, xOverlapIntersectorProvider) {
                  override fun intervalPastWindowToBound(window: Window): IntRange =
                     IntRange(window.xRange.last + 1, dimMaximum)

                  override fun crossAxisOverlappers(window: Window): Iterator<IntervalTree.OverlapResult<Window>> =
                     yTree.overlappers(window.yRange)

                  override fun trailingEdge(window: Window): Int =
                     window.xRange.last

                  override fun addToMainAxisTree(window: Window) {
                     addToXTree(window)
                  }

                  override fun removeFromMainAxisTree(window: Window) {
                     removeFromXTree(window)
                  }

                  override fun trailingSpaceToBound(maximum: Int, from: Int): Int {
                     if (maximum == 0)
                        return 0
                     return min(maximum, dimMaximum - from)
                  }

                  override fun spaceToStartOfWindow(maximum: Int, from: Int, window: Window): Int =
                     min(maximum, window.xRange.first - (from + 1))

               }
            } else {
               object : DimensionProjector(Dimension.X, windowXNegativeComparator, xOverlapIntersectorProvider) {
                  override fun intervalPastWindowToBound(window: Window): IntRange =
                     IntRange(dimMaximum, window.xRange.first - 1)

                  override fun crossAxisOverlappers(window: Window): Iterator<IntervalTree.OverlapResult<Window>> =
                     yTree.overlappers(window.yRange)

                  override fun trailingEdge(window: Window): Int =
                     window.xRange.first

                  override fun addToMainAxisTree(window: Window) {
                     addToXTree(window)
                  }

                  override fun removeFromMainAxisTree(window: Window) {
                     removeFromXTree(window)
                  }

                  // `maximum` is negative
                  override fun trailingSpaceToBound(maximum: Int, from: Int): Int {
                     if (maximum == 0)
                        return 0
                     return max(maximum, dimMaximum - from)
                  }

                  // `maximum` is negative
                  override fun spaceToStartOfWindow(maximum: Int, from: Int, window: Window): Int =
                     max(maximum, window.xRange.last - (from - 1))

               }
            }

         } else {
            if (towardsPositiveInfinity) {
               object : DimensionProjector(Dimension.Y, windowYPositiveComparator, yOverlapIntersectorProvider) {
                  override fun intervalPastWindowToBound(window: Window): IntRange =
                     IntRange(window.yRange.last + 1, dimMaximum)

                  override fun crossAxisOverlappers(window: Window): Iterator<IntervalTree.OverlapResult<Window>> =
                     xTree.overlappers(window.xRange)

                  override fun trailingEdge(window: Window): Int =
                     window.yRange.last

                  override fun addToMainAxisTree(window: Window) {
                     addToYTree(window)
                  }

                  override fun removeFromMainAxisTree(window: Window) {
                     removeFromYTree(window)
                  }

                  override fun trailingSpaceToBound(maximum: Int, from: Int): Int {
                     if (maximum == 0)
                        return 0
                     return min(maximum, dimMaximum - from)
                  }

                  override fun spaceToStartOfWindow(maximum: Int, from: Int, window: Window): Int =
                     min(maximum, window.yRange.first - (from + 1))

               }
            } else {
               object : DimensionProjector(Dimension.Y, windowYNegativeComparator, yOverlapIntersectorProvider) {
                  override fun intervalPastWindowToBound(window: Window): IntRange =
                     IntRange(dimMaximum, window.yRange.first - 1)

                  override fun crossAxisOverlappers(window: Window): Iterator<IntervalTree.OverlapResult<Window>> =
                     xTree.overlappers(window.xRange)

                  override fun trailingEdge(window: Window): Int =
                     window.yRange.first

                  override fun addToMainAxisTree(window: Window) {
                     addToYTree(window)
                  }

                  override fun removeFromMainAxisTree(window: Window) {
                     removeFromYTree(window)
                  }

                  // `maximum` is negative
                  override fun trailingSpaceToBound(maximum: Int, from: Int): Int {
                     if (maximum == 0)
                        return 0
                     return max(maximum, dimMaximum - from)
                  }

                  // `maximum` is negative
                  override fun spaceToStartOfWindow(maximum: Int, from: Int, window: Window): Int =
                     max(maximum, window.yRange.last - (from - 1))
               }
            }
         }
      }

      private val windowXPositiveComparator =
         Comparator<Window> { i1, i2 -> i1.xRange.last.compareTo(i2.xRange.last) }
      private val windowXNegativeComparator =
         Comparator<Window> { i1, i2 -> i2.xRange.first.compareTo(i1.xRange.first) } // descending
      private val windowYPositiveComparator =
         Comparator<Window> { i1, i2 -> i1.yRange.last.compareTo(i2.yRange.last) }
      private val windowYNegativeComparator =
         Comparator<Window> { i1, i2 -> i2.yRange.first.compareTo(i1.yRange.first) } // descending

      private val xOverlapIntersectorProvider: (IntRange) -> (IntervalTree.OverlapResult<Window>) -> Boolean =
         { range -> { it.ancillary.xRange.intersects(range) } }
      private val yOverlapIntersectorProvider: (IntRange) -> (IntervalTree.OverlapResult<Window>) -> Boolean =
         { range -> { it.ancillary.yRange.intersects(range) } }
   }
}

private class DimensionHeuristic {
   enum class Advice { X, Y }

   private var xCount1: Int? = null
   private var yCount1: Int? = null
   private var xCount2: Int? = null
   private var yCount2: Int? = null
   private var xCount3: Int? = null
   private var yCount3: Int? = null

   fun addXObservation(count: Int) {
      xCount3 = xCount2
      xCount2 = xCount1
      xCount1 = count
   }

   fun addYObservation(count: Int) {
      yCount3 = yCount2
      yCount2 = yCount1
      yCount1 = count
   }

   fun getAdvice() : Advice {
      var xSum = 0
      var xCount = 0
      xCount1?.let {
         xSum += it
         xCount += 1
      }
      xCount2?.let {
         xSum += it
         xCount += 1
      }
      xCount3?.let {
         xSum += it
         xCount += 1
      }
      var ySum = 0
      var yCount = 0
      yCount1?.let {
         ySum += it
         yCount += 1
      }
      yCount2?.let {
         ySum += it
         yCount += 1
      }
      yCount3?.let {
         ySum += it
         yCount += 1
      }
      if (xCount < 3 || yCount < 3)
         return if (Random.nextBoolean()) Advice.X else Advice.Y
      return if (xSum < ySum)
         Advice.X
      else if (xSum > ySum)
         Advice.Y
      else if (Random.nextBoolean()) Advice.X else Advice.Y
   }
}