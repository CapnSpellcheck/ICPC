package icpc.twothousandtwenty

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

// chunk size for output elevations
const val DEFAULT_CHUNK_SIZE = 1000

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/landscape
 * NOTE: My original idea was to use an 'output thread' to dump the data in chunks to stdout, in parallel with
 * ongoing computation of the output. In my
 * testing, elapsed time was reduced on a multicore CPU by doing this. However, the time limit calculation
 * on Kattis seems to balk at use of multiple cores -- launching a thread seems to immediately
 * trigger 'Time Limit Exceeded' result
 */

/**
 * Process a new modification into data that we can use to calculate the elevations in one pass (without sorting!)
 * How to do that? Let's break down the modifications into 2 types: A, constant elevation change on the interval, B,
 * an incline (or decline) on the interval. The 2 non-constant modifications, Hill and Valley, will be transformed
 * into these 2 types.
 * For this, we'll need multiple arrays for each position.
 * For all these input arrays, the 0th element actually goes unused, since `start` and `end` are 1-indexed.
 * I felt like keeping it that way. In addition, an extra position is needed at the end of the arrays because
 * it avoids end-of-array checks.
 *
 * @param char The letter code for the modification; see LandscapeGenerator.pdf
 * @param start The position at which the modification begins
 * @param end The position at which the modification ends, inclusive
 * @param baseElevationDeltas An array, initially filled with zero, of length points + 2, which will be filled with
 * *changes* to the base elevation.
 * @param inclineStarts An array, initially filled with zero, of length points + 2, which will be filled with the
 * number of inclines that begin at `start`
 * @param inclineEnds An array, initially filled with zero, of length points + 2, which will be filled with the
 * number of inclines that end at `end`
 * @param inclineDeltas An array, initially filled with zero, of length points + 2, which will be filled with
 * adjustments to the incline rate
 * @param declineStarts An array, initially filled with zero, of length points + 2, which will be filled with the
 * number of declines that begin at `start`
 * @param declineEnds An array, initially filled with zero, of length points + 2, which will be filled with the
 *  number of declines that end at `end`
 */
fun processModification(
   char: Char,
   start: Int,
   end: Int,
   baseElevationDeltas: LongArray,
   inclineStarts: IntArray,
   inclineEnds: IntArray,
   inclineDeltas: LongArray,
   declineStarts: IntArray,
   declineEnds: IntArray,
) {
   when (char) {
      'R' -> {
         // A Raise increases the base elevation by 1 on [start, end]. `baseElevationDeltas` is used pre-position, so
         // reverse the effect on end + 1
         baseElevationDeltas[start] += 1L
         baseElevationDeltas[end + 1] -= 1L
      }
      'D' -> {
         // A Depress decreases the base elevation by 1 on [start, end]. `baseElevationDeltas` is used pre-position, so
         // reverse the effect on end + 1
         baseElevationDeltas[start] -= 1L
         baseElevationDeltas[end + 1] += 1L
      }
      // Decomposition of Hills and Valleys
      'H' -> {
         // A Hill is split into an incline and a decline. A synthetic Raise is created, starting at the first position
         // after the peak is reached, and ending at the end of the Hill. But first, I reduce the amount of math done
         // if the Hill has length 1 or 2, because it could just be transformed to a Raise of 1.
         if (end - start <= 1) {
            baseElevationDeltas[start] += 1L
            baseElevationDeltas[end + 1] -= 1L
         } else {
            // mid is the start of the peak
            val mid = (start + end) / 2
            val peakMagnitude: Long = (mid - start + 1).toLong()
            // introduce the Raise effect, with a magnitude equal to the height of the Hill
            baseElevationDeltas[mid + 1] += peakMagnitude
            baseElevationDeltas[end + 1] -= peakMagnitude
            // The incline occurs in the first half of the Hill
            inclineStarts[start] += 1
            inclineEnds[mid] += 1
            // We need to counteract the initial bump by the Raise effect, and I do this via `inclineDeltas`
            inclineDeltas[mid + 1] -= peakMagnitude
            inclineDeltas[end + 1] += peakMagnitude - 1 // - 1 because inclineDeltas is used pre-position
            // The decline is the second half of the Hill
            declineStarts[mid + 1 + (end - start) % 2] += 1 // odd length hills - peak lasts 2 positions
            declineEnds[end] += 1
         }
      }
      'V' -> {
         // A Valley is split into an decline and an incline. A synthetic Depress is created, starting at the first position
         // after the lowpoint is reached, and ending at the end of the Valley. But first, I reduce the amount of math done
         // if the Valley has length 1 or 2, because it could just be transformed to a Depress of 1.
         if (end - start <= 1) {
            baseElevationDeltas[start] -= 1L
            baseElevationDeltas[end + 1] += 1L
         } else {
            // mid is the start of the lowpoint
            val mid = (start + end) / 2
            val peakMagnitude: Long = (mid - start + 1).toLong()
            // introduce the Depress effect, with a magnitude equal to the height of the Valley
            baseElevationDeltas[mid + 1] -= peakMagnitude
            baseElevationDeltas[end + 1] += peakMagnitude
            // The decline occurs in the first half of the Hill
            declineStarts[start] += 1
            declineEnds[mid] += 1
            // We need to counteract the initial dip by the Depress effect, and I do this via `inclineDeltas`
            inclineDeltas[mid + 1] += peakMagnitude
            inclineDeltas[end + 1] -= peakMagnitude - 1
            // The incline is the second half of the Valley
            inclineStarts[mid + 1 + (end - start) % 2] += 1
            inclineEnds[end] += 1
         }
      }
   }
}

/**
 * The main part of the solution. Using the precomputed information, calculate the elevation of position based on
 * the last position:
 * elevation[pos] = elevation[pos - 1] + baselineElevationDelta[pos] + inclineEffect[pos] + inclineStarts[pos] - declineStarts[pos]
 *  where inclineEffect[pos] = inclineEffect[pos - 1] + openInclinesRelative[pos - 1] + inclineDeltas[pos]
 *  where openInclinesRelative[pos] = openInclinesRelative[pos - 1] + inclineStarts[pos - 1] - inclineEnds[pos - 1] - declineStarts[pos - 1] + declineEnds[pos - 1]
 *
 * @param points the number of points / positions
 * @param baseElevationDeltas the `baseElevationDeltas` computed by `processModification` for all needed modifications
 * @param inclineStarts the `baseElevationDeltas` computed by `processModification` for all needed modifications
 * @param inclineEnds the `baseElevationDeltas` computed by `processModification` for all needed modifications
 * @param inclineDeltas the `baseElevationDeltas` computed by `processModification` for all needed modifications
 * @param declineStarts the `baseElevationDeltas` computed by `processModification` for all needed modifications
 * @param declineEnds the `baseElevationDeltas` computed by `processModification` for all needed modifications
 * @param blockSize the size of arrays to pass to `consumer`
 * @param consumer A function type that takes a LongArray of length blockSize as 'blockSize' number of positions
 * get calculated.
 */
fun generateLandscape(
   points: Int,
   baseElevationDeltas: LongArray,
   inclineStarts: IntArray,
   inclineEnds: IntArray,
   inclineDeltas: LongArray,
   declineStarts: IntArray,
   declineEnds: IntArray,
   blockSize: Int = DEFAULT_CHUNK_SIZE,
   consumer: (LongArray) -> Unit
) {
   var position = 1
   var baseElevation = 0L
   var openInclinesRelative = 0
   var inclineEffect = 0L

   while (position <= points) {
      val remaining = points - position + 1
      val resultChunk = LongArray(min(blockSize, remaining))
      var i = 0
      while (i < resultChunk.size) {
         baseElevation += baseElevationDeltas[position]
         inclineEffect += inclineStarts[position]
         inclineEffect -= declineStarts[position]
         inclineEffect += inclineDeltas[position]

         val elevation = baseElevation + inclineEffect
         resultChunk[i] = elevation

         openInclinesRelative += inclineStarts[position] - inclineEnds[position]
         openInclinesRelative += declineEnds[position] - declineStarts[position]
         inclineEffect += openInclinesRelative

         position += 1
         i += 1
      }

      consumer(resultChunk)
   }

}

const val NUM_POINTS_FOR_ASYNCIO = 999999 // An actual valid value was ~ 60,000

fun generateLandscapeIO(inputStream: InputStream, outputStream: OutputStream) {
   inputStream.bufferedReader().use { reader ->
      val sizes = reader.readLine().split(' ')
      val points = sizes[0].toInt()
      val mods = sizes[1].toInt()
      val baseElevationDeltas = LongArray(points + 2)
      val inclineStarts = IntArray(points + 2)
      val inclineEnds = IntArray(points + 2)
      val inclineDeltas = LongArray(points + 2)
      val declineStarts = IntArray(points + 2)
      val declineEnds = IntArray(points + 2)
      // Since `generateLandscape` is computation intensive, only run it until we've reached the last
      // modification end. Following positions will be 0.
      var modificationPoints = 0

      repeat(mods) {
         // Read the line describing the modification, and process it
         val typeChar = reader.read()
         reader.read() // space char
         val tokens = reader.readLine().split(' ')
         val end = tokens[1].toInt()
         modificationPoints = max(end, modificationPoints)
         processModification(
            typeChar.toChar(),
            tokens[0].toInt(),
            end,
            baseElevationDeltas,
            inclineStarts,
            inclineEnds,
            inclineDeltas,
            declineStarts,
            declineEnds
         )

      }

      val writer = outputStream.bufferedWriter()
      lateinit var outputQueue: BlockingQueue<LongArray>
      val writerThread: Thread?

      fun writeChunk(chunk: LongArray) {
         var i = 0
         while (i < chunk.size) {
            writer.write(chunk[i].toString())
            writer.newLine()
            i += 1
         }
      }
      fun writeChunkAsync(chunk: LongArray) {
         outputQueue.add(chunk)
      }

      if (points >= NUM_POINTS_FOR_ASYNCIO) {
         outputQueue = LinkedBlockingQueue()
         writerThread = thread {
            val numberOfChunks = points / DEFAULT_CHUNK_SIZE + if (points % DEFAULT_CHUNK_SIZE > 0) 1 else 0
            var chunksWritten = 0
            while (chunksWritten < numberOfChunks) {
               val chunk = outputQueue.take()
               writeChunk(chunk)
               chunksWritten += 1
            }
         }
      }
      else {
         writerThread = null
      }

      generateLandscape(
         modificationPoints,
         baseElevationDeltas,
         inclineStarts,
         inclineEnds,
         inclineDeltas,
         declineStarts,
         declineEnds,
         consumer = ::writeChunk
      )
      // Write remaining zeroes.
      repeat(points - modificationPoints) {
         writer.write("0\n")
      }
      writer.flush()
   }
}

fun main() {
   generateLandscapeIO(System.`in`, System.out)
}
