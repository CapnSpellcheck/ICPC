package icpc.twothousandsixteen

import util.Interval
import util.printDebug
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.atan
import kotlin.math.max

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/oil2
 */

/**
 * An oil deposit per the problem specification.
 * @param xRange A semi-closed interval
 */
data class Deposit(val depth: Int, val xRange: Interval) {
   inline val width: Int
      get() = xRange.width - 1
}

data class IntAndDouble(val int: Int, val double: Double)

/**
 * Find the maximal sum of deposit widths that intersect a line from the "surface" through a specified
 * deposit. A single point is chosen along the specified deposit by a point selector.
 *
 * How do we find the maximal sum through a specific point? Simple, we use a 'sweep line' but we rotate
 * it, not translate! We will start at theta = ε, i.e. the line goes towards x=+∞ above and -∞ below.
 * We figure out when each other deposit will start to be struck by this line, call that the "entrance angle".
 * We figure out when each other deposit will stop being struck by this line, call that the "exit angle" which
 * is > entrance angle. So, at any given angle, the sum of deposit widths of deposits that are intersected
 * by the line, equals the sum of the widths of deposits whose entrance angle <= theta and exit angle >= theta.
 *
 * @param deposit The lines from the surface are constrained to pass through this deposit at a point
 *   identified by `depositPointSelector`
 * @param otherDeposits The other deposits which we would like the line to maximize the sum of widths
 * that the line passes through.
 */
fun maximalSumOfDepositWidthsHitByLineThru(
   deposit: Deposit,
   otherDeposits: List<Deposit>,
   depositPointSelector: (Interval) -> Int
): Int {
   // We are going to sort the angles, but we need to know the deposit from `otherDeposits` that each angle
   // is for. So I use the very general `IntAndDouble` class defined above. It's like kotlin.Pair but as we
   // know, pairs cause primitive boxing. I use the int for the index into `otherDeposits` and the double for
   // the angle.
   val entranceAngles = ArrayList<IntAndDouble>(otherDeposits.size)
   val exitAngles = ArrayList<IntAndDouble>(otherDeposits.size)
   val depositProjection = depositPointSelector(deposit.xRange)
   otherDeposits.forEachIndexed { i, otherDeposit ->
      val dy = deposit.depth - otherDeposit.depth
      if (dy == 0) {
         // skip; a horizontal line does not go through the surface
         return@forEachIndexed
      }

      // Remember, as we sweep from min theta, for any deposit above (lesser y), it enters at its right endpoint
      // and exits at its left; but for a deposit below, it enters at its left and exits at its right
      val dxEnd = (otherDeposit.xRange.end - depositProjection).toDouble()
      val dxStart = (otherDeposit.xRange.start - depositProjection).toDouble()
      if (dy > 0) {
         entranceAngles.add(IntAndDouble(i, atan(-dxEnd / dy)))
         exitAngles.add(IntAndDouble(i, atan(-dxStart / dy)))
      } else {
         entranceAngles.add(IntAndDouble(i, atan(-dxStart / dy)))
         exitAngles.add(IntAndDouble(i, atan(-dxEnd / dy)))
      }
   }
//   printDebug("ENTRANCE: $entranceAngles")
//   printDebug("EXIT: $exitAngles")

   // In order to sweep by angle, we must sort them. I don't merge them, I will just do a single loop
   // through both.
   val compareDouble = compareBy<IntAndDouble> { it.double }
   entranceAngles.sortWith(compareDouble)
   exitAngles.sortWith(compareDouble)

   var currentSum = deposit.width // We always get this one!
   var maximumSum = currentSum
   var sortedEntranceIndex = 0
   var sortedExitIndex = 0
   // Loop through entrances and exits, finding the smallest unprocessed one.
   // The last entrance must occur before last exit, and once we've reached the last entrance, the maximum can't
   // go any higher - we can stop
   while (sortedEntranceIndex < entranceAngles.size) {
      printDebug("LOOP: entrance=$sortedEntranceIndex exit=$sortedExitIndex")
      // ties go to entering: we should always count entrances first
      if (entranceAngles[sortedEntranceIndex].double <= exitAngles[sortedExitIndex].double) {
         // Upon entrance, the deposit starts intersecting the sweep line, so we can add its width to current sum.
         printDebug("LOOP: entrance")
         currentSum += otherDeposits[entranceAngles[sortedEntranceIndex].int].width
         maximumSum = max(currentSum, maximumSum)
         printDebug("maximum = $maximumSum")
         sortedEntranceIndex += 1
      } else {
         // Upon exit, the deposit stops intersecting the sweep line, so we can subtract its width from current sum.
         printDebug("LOOP: exit")
         currentSum -= otherDeposits[exitAngles[sortedExitIndex].int].width
         sortedExitIndex += 1
      }
   }

   return maximumSum
}

/**
 * In order to find the maximal sum of ANY line that extends from the surface, we run the prior algorithm with
 * every deposit as the key deposit, and do it twice actually, selecting the left endpoint and right endpoint
 * from that deposit as the 'anchor point' that the sweep line always crosses. As for why we should do it twice
 * with those points, I leave it as an exercise to the reader.
 */
fun maximalSumOfDepositWidthsHitByAnyLineFromSurface(deposits: List<Deposit>): Int {
   var maximumSum = 0
   val startProjection: (Interval) -> Int = { it.start }
   val endProjection: (Interval) -> Int = { it.end }

   deposits.forEachIndexed { i, deposit ->
      printDebug("DEPOSIT LOOP $i $deposit")
      maximumSum = max(maximalSumOfDepositWidthsHitByLineThru(deposit, deposits, startProjection), maximumSum)
      printDebug("maximum sum after deposit $i START: $maximumSum")
      maximumSum = max(maximalSumOfDepositWidthsHitByLineThru(deposit, deposits, endProjection), maximumSum)
      printDebug("maximum sum after deposit $i END: $maximumSum")
   }
   return maximumSum
}

fun maximalSumOfDepositWidthsHitByAnyLineFromSurfaceIO(inputStream: InputStream, outputStream: OutputStream) {
   inputStream.bufferedReader().use { reader ->
      val count = reader.readLine().toInt()
      val deposits = ArrayList<Deposit>(count)
      repeat(count) {
         val ints = reader.readLine().split(' ')
         // Ignore zero width deposits
         if (ints[0] != ints[1]) {
            // As shown in Sample 2, the x-coordinates can be given in any order
            deposits.add(Deposit(ints[2].toInt(), Interval.fromUnordered(ints[0].toInt(), ints[1].toInt())))
         }
      }
      val answer = maximalSumOfDepositWidthsHitByAnyLineFromSurface(deposits)
      outputStream.write(answer.toString().toByteArray())
      outputStream.flush()
   }
}

fun main() {
   maximalSumOfDepositWidthsHitByAnyLineFromSurfaceIO(System.`in`, System.out)
}