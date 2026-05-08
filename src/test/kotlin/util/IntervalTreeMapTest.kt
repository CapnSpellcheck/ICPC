package util

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntervalTreeMapTest {

   @Test fun testAncillaryValues() {
      val intervals = listOf(
         intArrayOf(-20, 19),
         intArrayOf(10, 19),
         intArrayOf(19, 19),
         intArrayOf(19, 20),
         intArrayOf(2, 10),
         intArrayOf(1, 25),
         intArrayOf(-10, 50),
         intArrayOf(89, 99),
         intArrayOf(-50, -45),
         intArrayOf(0, 99),
      ).map{ it[0] .. it[1] }

      val values = listOf(
         "Lorem ipsum dolor sit amet",
         "consectetur adipiscing elit",
         "eiusmod tempor incididunt",
         "Ut enim ad minim veniam",
         "quis nostrud exercitation ullamco laboris",
         "Duis aute irure dolor",
         "velit esse cillum dolore",
         "fugiat nulla pariatur",
         "Excepteur sint occaecat cupidatat",
         "deserunt mollit anim",
      )

      val tree = IntervalTreeMap<String>()
      for (i in 0 .. intervals.lastIndex) {
         tree.insert(intervals[i], values[i])
      }

      // check values
      for (i in 0 .. intervals.lastIndex) {
         assertEquals(values[i], tree.get(intervals[i]))
      }
   }

   @Test fun testContainsViaRandomInserts() {
      val intervals = (1 .. 100).map {
         Random.nextInt(151).let { r -> IntRange(r - 100, r - 100 + Random.nextInt(50)) }
      }
      println("--- RANDOM INTERVALS: $intervals")

      val tree = IntervalTreeMap<Unit>()
      intervals.forEach {
         tree.insert(it, Unit)
      }
      assertTrue(tree.hasValidColoring())
      assertTrue(tree.hasConsistentMaxEnds())

      // test every subrange [x, y] -100 <= x <= 50, 0 <= y-x < 100
      for (start in -100 .. 50) {
         for (end in start ..< start + 100) {
            val testRange = start .. end
            val expected = intervals.contains(testRange)
            assertEquals(expected, tree.contains(testRange), "tree should ${if (expected) "" else " not "} contain '$testRange'")
         }
      }
   }

   @Test fun testHasAnyOverlapViaRandomInserts() {
      repeat(100) {
         val intervals = (1..10).map {
            Random.nextInt(0, 150).let { r -> IntRange(r, r + Random.nextInt(50)) }
         }
         println("--- RANDOM INTERVALS: $intervals")

         val tree = IntervalTreeMap<Unit>()
         intervals.forEach { tree.insert(it, Unit) }
         assertTrue(tree.hasValidColoring())
         assertTrue(tree.hasConsistentMaxEnds())

         for (start in 0 .. 150) {
            for (end in start ..< 200) {
               val testRange = start .. end
               val expected = intervals.any { it.intersects(testRange)}
               assertEquals(expected, tree.hasAnyOverlap(testRange), "tree should ${if (expected) "" else " not "} overlap '$testRange'")
            }
         }
      }
   }

   @Test fun testOverlappersViaRandomInserts() {
      val intervals = (1..100).map {
         Random.nextInt(0, 1000).let { r -> IntRange(r, r + Random.nextInt(1000)) }
      }
      println("--- RANDOM INTERVALS: $intervals")

      val tree = IntervalTreeMap<Unit?>()
      intervals.forEach { tree.insert(it, Unit) }
      assertTrue(tree.hasValidColoring())
      assertTrue(tree.hasConsistentMaxEnds())

      for (start in 0 .. 1000) {
         for (end in start .. 2000) {
            val testRange = start .. end
            val expected = intervals.filter { it.intersects(testRange) }.toSet()
            val overlappers = tree.overlappers(testRange).asSequence().map { it.interval }.toSet()
            assertEquals(expected, overlappers, "overlappers for '$testRange'")
         }
      }
   }

   @Test fun testContainsAfterDeletes() {
      val intervals = (1..100).map {
         Random.nextInt(0, 900).let { r -> IntRange(r, r + Random.nextInt(100)) }
      }.toMutableList()
      val deletedIntervals = mutableListOf<IntRange>()
      println("--- RANDOM INTERVALS: $intervals")

      val tree = IntervalTreeMap<Unit>()
      intervals.forEach { tree.insert(it, Unit) }
      // delete some randomly
      repeat(10) {
         val index = Random.nextInt(intervals.size)
         tree.delete(intervals[index])
         deletedIntervals.add(intervals[index])
         intervals.removeAt(index)
      }

      // verify that deleted ones are not present and undeleted ones are
      for (interval in intervals) {
         assertTrue(tree.contains(interval), "tree contains undeleted interval")
      }
      for (deletedInterval in deletedIntervals) {
         assertFalse(tree.contains(deletedInterval), "tree doesn't contain deleted interval")
      }
      assertTrue(tree.hasValidColoring())
      assertTrue(tree.hasConsistentMaxEnds())

      repeat(100) {
         // randomly delete or insert
         if (Random.nextBoolean() || intervals.isEmpty()) {
            intervals.add(Random.nextInt(0, 900).let { r -> IntRange(r, r + Random.nextInt(100)) })
            if (deletedIntervals.contains(intervals.last()))
               deletedIntervals.remove(intervals.last())
            tree.insert(intervals.last(), Unit)
         } else {
            val index = Random.nextInt(intervals.size)
            tree.delete(intervals[index])
            deletedIntervals.add(intervals[index])
            intervals.removeAt(index)
         }

         assertTrue(tree.hasValidColoring())
         assertTrue(tree.hasConsistentMaxEnds())

         // verify that deleted ones are not present and undeleted ones are
         for (interval in intervals) {
            assertTrue(tree.contains(interval), "tree contains undeleted interval")
         }
         for (deletedInterval in deletedIntervals) {
            assertFalse(tree.contains(deletedInterval), "tree doesn't contain deleted interval")
         }
      }
   }

   @Test fun testOverlappersAfterDeletes() {
      val intervals = (1..100).map {
         Random.nextInt(0, 900).let { r -> IntRange(r, r + Random.nextInt(100)) }
      }.toMutableList()
      println("--- RANDOM INTERVALS: $intervals")

      val tree = IntervalTreeMap<Unit>()
      intervals.forEach { tree.insert(it, Unit) }
      // delete some randomly
      repeat(10) {
         val index = Random.nextInt(intervals.size)
         tree.delete(intervals[index])
         intervals.removeAt(index)
      }

      for (start in 0 .. 900) {
         for (end in start .. 1000) {
            val testRange = start .. end
            val expected = intervals.filter { it.intersects(testRange) }.toSet()
            val overlappers = tree.overlappers(testRange).asSequence().map { it.interval }.toSet()
            assertEquals(expected, overlappers, "overlappers for '$testRange'")
         }
      }

      repeat(100) {
         // randomly delete or insert
         if (Random.nextBoolean() || intervals.isEmpty()) {
            intervals.add(Random.nextInt(0, 900).let { r -> IntRange(r, r + Random.nextInt(100)) })
            tree.insert(intervals.last(), Unit)
         } else {
            val index = Random.nextInt(intervals.size)
            tree.delete(intervals[index])
            intervals.removeAt(index)
         }

         for (start in 0 .. 900) {
            for (end in start .. 1000) {
               val testRange = start .. end
               val expected = intervals.filter { it.intersects(testRange) }.toSet()
               val overlappers = tree.overlappers(testRange).asSequence().map { it.interval }.toSet()
               assertEquals(expected, overlappers, "overlappers for '$testRange'")
            }
         }
      }
   }

}