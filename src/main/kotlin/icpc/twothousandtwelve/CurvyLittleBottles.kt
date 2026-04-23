package icpc.twothousandtwelve

import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.abs

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/bottles
 */

// insufficient volume is indicated by an empty `markDistances` array.
class VolumeResult(
   val volume: Double,
   val markDistances: List<Double>
)

/**
 * Simple abstraction for a polynomial
 */
@JvmInline
value class Polynomial(val coefficients: DoubleArray) {
   fun evaluate(coordinate: Double) =
      coefficients.foldRight(.0) { coeff, accum -> accum * coordinate + coeff }

   /**
    * Perform integration of a polynomial.
    */
   fun integrate(): Polynomial {
      val integral = DoubleArray(coefficients.size + 1)
      for (i in 1 .. coefficients.size) {
         integral[i] = coefficients[i - 1] / i
      }
      return Polynomial(integral)
   }

   /**
    * Square a polynomial.
    */
   fun square(): Polynomial {
      val degree = 2*(coefficients.size - 1)
      val square = DoubleArray(degree + 1)
      for (i in 0 ..< coefficients.size) {
         for (j in 0 ..< coefficients.size)
            square[i + j] += coefficients[i] * coefficients[j]
      }
      return Polynomial(square)
   }

   /**
    * Find the x-value at which the polynomial evaluates to a target.
    * @param target The target projection of the `polynomial` function
    * @param polynomial The polynomial to find the root for.
    * @param derivative A polynomial that is a-priori known to be the derivative of `polynomial`
    * @param initialGuess Unfortunately, you need to seed Newton's method with a guess
    * @param error The tolerance of error for the return value
    * @return The determined x-value at which polynomial projects to `target`.
    */
   @Suppress("SameParameterValue")
   fun findValue(target: Double, derivative: Polynomial, initialGuess: Double, error: Double): Double {
      // subtract the target from the constant so we can look for function zeroes
      val origPolynomial0 = coefficients[0]
      coefficients[0] -= target
      // take the guess
      var guess = initialGuess
      var limit = 0
      do {
         val projection: Double
         val derivativeProjection = derivative.evaluate(guess)
         // Check for derivative safety - if too small, Newton will not find the root quickly or at all
         // if derivative is zero, then the function is roughly a horizontal line, so divide by
         if (derivativeProjection < 0.001) {
            guess += 0.01
            projection = evaluate(guess)
         } else {
            projection = evaluate(guess)
            guess -= projection / derivativeProjection
         }
         if (abs(projection) < 0.01)
            limit += 1
         else
            limit = 0
      } while (abs(projection) > error && limit < 5) // projection should be close to zero within tolerance
      coefficients[0] = origPolynomial0 // return the constant to its initial value
      return guess
   }
}

/**
 * The main solution to the problem. I use polynomial integration, evaluation with Horner's method,
 * and Newton's iterative root search.
 */
fun volumeMarks(polynomial: Polynomial, xStart: Double, xEnd: Double, volumeIncrement: Int): VolumeResult {
   val slopePolynomial = polynomial.square()
   val volumePolynomial = slopePolynomial.integrate()
   val volumeBase = volumePolynomial.evaluate(xStart)
   val volumeInPiUnits = volumePolynomial.evaluate(xEnd) - volumeBase
   val volumeIncrementInInvertedPiUnits = volumeIncrement / Math.PI

   val markDistances = ArrayList<Double>(8)
   var currentMark = 1
   var x = xStart

   // The problem dictates a maximum of 8 marks
   while (currentMark <= 8 && x < xEnd) {
      // Look for the next volume mark. The desired projection is the projection of the volume
      // function at the starting coordinate plus the desired volume through the point of the mark.
      val xFound = volumePolynomial.findValue(
         volumeBase + currentMark * volumeIncrementInInvertedPiUnits,
         slopePolynomial,
         x + 0.05,
         0.001
      )
      if (xFound < x) {
         error("don't go back!")
      }
      x = xFound
      if (x < xEnd) {
         markDistances += x - xStart
      }
      currentMark += 1
   }

   return VolumeResult(Math.PI * volumeInPiUnits, markDistances)
}

fun volumeMarksIO(inputStream: InputStream, outputStream: OutputStream) {
   inputStream.bufferedReader().use { reader ->
      var degreeLine: String?
      outputStream.bufferedWriter().use { writer ->
         var case = 1U
         while (true) {
            degreeLine = reader.readLine()
            if (degreeLine == null)
               break
            val polynomialLine = reader.readLine()
            val polynomial = DoubleArray(degreeLine!!.toInt() + 1)
            polynomialLine.split(' ').mapIndexed { index, s ->
               polynomial[index] = s.toDouble()
            }
            val lastLine = reader.readLine()
            val numbers = lastLine.split(' ')
            val xLow = numbers[0].toDouble()
            val xHigh = numbers[1].toDouble()
            val volumeIncrement = numbers[2].toInt()
            val result = volumeMarks(Polynomial(polynomial), xLow, xHigh, volumeIncrement)
            outputVolumeResult(case, result, writer)
            case += 1U
         }
      }
   }
}

fun main() {
   volumeMarksIO(System.`in`, System.out)
}

private const val FLOAT_FORMAT = "%.2f"
private fun outputVolumeResult(caseNumber: UInt, result: VolumeResult, writer: BufferedWriter) {
   writer.write("Case $caseNumber: ${FLOAT_FORMAT.format(result.volume)}\n")
   if (result.markDistances.isEmpty()) {
      writer.write("insufficient volume\n")
   } else {
      writer.write(result.markDistances.joinToString(" ", postfix = "\n") { markDistance ->
         FLOAT_FORMAT.format(markDistance)
      })
   }

}
