package util

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntervalTreeTest {

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

      val tree = IntervalTree<String>()
      for (i in 0 .. intervals.lastIndex) {
         tree.insert(intervals[i], values[i])
         println("Tree size: ${tree.debugSize()}")
      }

      // check values
      for (i in 0 .. intervals.lastIndex) {
         assertEquals(values[i], tree.getAncillary(intervals[i]))
      }
   }

   @Test fun testContainsViaRandomInserts() {
      val intervals = (1 .. 100).map {
         Random.nextInt(151).let { r -> IntRange(r - 100, r - 100 + Random.nextInt(50)) }
      }
      println("--- RANDOM INTERVALS: $intervals")

      val tree = IntervalTree<Any?>()
      intervals.forEach {
         tree.insert(it, null)
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

         val tree = IntervalTree<Any?>()
         intervals.forEach { tree.insert(it, null) }
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

      val tree = IntervalTree<Unit?>()
      intervals.forEach { tree.insert(it, null) }
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

      val tree = IntervalTree<Any?>()
      intervals.forEach { tree.insert(it, null) }
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
            tree.insert(intervals.last(), null)
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

      val tree = IntervalTree<Any?>()
      intervals.forEach { tree.insert(it, null) }
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
            tree.insert(intervals.last(), null)
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

   @Test fun asdgsadg() {
      val intervals = listOf(62..151, 143..176, 17..45, 415..478, 665..751, 564..663, 203..288, 787..864, 324..358, 180..224, 82..160, 289..358, 185..229, 883..942, 786..842, 180..210, 163..224, 162..254, 829..872, 327..409, 281..303, 609..675, 303..305, 77..119, 80..166, 168..198, 130..229, 154..167, 156..247, 472..493, 849..947, 184..262, 138..196, 261..348, 735..809, 79..155, 803..833, 815..885, 418..515, 207..283, 220..318, 602..628, 749..805, 279..370, 292..332, 148..162, 384..403, 328..427, 35..129, 361..409, 746..807, 409..466, 5..100, 49..103, 71..117, 491..506, 690..747, 624..668, 813..896, 783..875, 236..292, 851..871, 241..319, 609..651, 299..344, 492..563, 192..200, 733..733, 211..292, 347..390, 622..651, 885..914, 897..986, 93..101, 35..91, 634..711, 777..783, 564..654, 843..939, 228..236, 10..47, 45..130, 888..973, 811..883, 670..678, 352..368, 97..179, 865..915, 1..82, 719..769, 723..808, 618..693, 856..912, 544..639, 109..202, 510..573, 801..882, 748..792, 891..946, 372..462)
      val tree = IntervalTree<Any?>()
      intervals.forEach { tree.insert(it, null) }
      tree.delete(130..229)
      tree.delete(261..348)
      tree.delete(733..733)
      tree.delete(786..842)
      tree.delete(415..478)
      assertTrue(tree.hasValidColoring())
      assertTrue(tree.hasConsistentMaxEnds())
   }
   
}