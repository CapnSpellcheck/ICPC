package util

import java.awt.Point
import java.awt.Rectangle
import kotlin.random.Random
import kotlin.test.*

class TwoDTreeMapTest {
   @Test fun testRandomInsertsAndDeletes() {
      val tree = TwoDTreeMap<String>()
      val contentMap = HashMap<Point, String>()
      fun doInsert() {
         val value = Random.nextString(50)
         val point = Point(Random.nextInt(-100, 101), Random.nextInt(-100, 101))
         val result = tree.insert(point.x, point.y, value)
         assertEquals(contentMap.contains(point), !result, "insert item")
         contentMap[point] = value
         println("inserted $point")
      }
      fun doDelete() {
         val key = contentMap.keys.random()
         assertTrue(tree.delete(key.x, key.y), "delete item")
         contentMap.remove(key)
      }

      repeat(100) {
         doInsert()
      }

      repeat(10000) {
         val doInsert = Random.nextBoolean() || contentMap.isEmpty()
         if (doInsert) {
            if (Random.nextBoolean()) {
               // insert duplicate
               val point = contentMap.keys.random()
               val newValue = Random.nextString(10)
               assertFalse(tree.insert(point.x, point.y, newValue), "insert duplicate key - return false")
               assertEquals(newValue, tree.findAny(point.x .. point.x, point.y .. point.y), "insert duplicate key - updated value")
            } else
               doInsert()
         } else if (Random.nextBoolean()) {
            // delete real
            doDelete()
         } else {
            // delete fake
            val random = Point(Random.nextInt(-100, 101), Random.nextInt(-100, 101))
            if (contentMap.contains(random))
               random.x = 1000
            assertFalse(tree.delete(random.x, random.y),"delete item not in tree")
         }
      }
   }

   @Test fun testFindAny() {
      val tree = TwoDTreeMap<String>()
      val contentMap = HashMap<Point, String>()
      fun doInsert() {
         val value = Random.nextString(50)
         val point = Point(Random.nextInt(-100, 101), Random.nextInt(-100, 101))
         contentMap[point] = value
         tree.insert(point.x, point.y, value)
         println("tree.insert(${point.x}, ${point.y}")
      }

      repeat(100) {
         doInsert()
      }

      repeat(1000) {
         var start = Random.nextInt(-100, 101)
         val width = Random.nextInt(1, 102 - start)
         val xRange = IntRange(start, start + width - 1)
         start = Random.nextInt(-100, 101)
         val height = Random.nextInt(1, 102 - start)
         val yRange = IntRange(start, start + height - 1)
         val result = tree.findAny(xRange, yRange)
         println("FIND $xRange $yRange")
         val rect = Rectangle(xRange.start, yRange.start, width, height)
         val validValues = contentMap.entries.filter { rect.contains(it.key.x, it.key.y) }.map { it.value }
         if (validValues.isEmpty())
            assertNull(result, "tree should not have found anything")
         else
            assertContains(validValues, result, "got a valid entry")
      }

   }

   @Test fun testFindAll() {
      val tree = TwoDTreeMap<String>()
      val contentMap = HashMap<Point, String>()
      fun doInsert() {
         val value = Random.nextString(50)
         val point = Point(Random.nextInt(-100, 101), Random.nextInt(-100, 101))
         contentMap[point] = value
         tree.insert(point.x, point.y, value)
         println("tree.insert(${point.x}, ${point.y}, \"$value\")")
      }

      repeat(100) {
         doInsert()
      }

      repeat(1000) {
         var start = Random.nextInt(-100, 101)
         val width = Random.nextInt(1, 102 - start)
         val xRange = IntRange(start, start + width - 1)
         start = Random.nextInt(-100, 101)
         val height = Random.nextInt(1, 102 - start)
         val yRange = IntRange(start, start + height - 1)
         val result = tree.findAll(xRange, yRange)
         println("FIND $xRange $yRange")
         val rect = Rectangle(xRange.start, yRange.start, width, height)
         val validValues = contentMap.entries.filter { rect.contains(it.key.x, it.key.y) }.map { it.value }
         assertEquals(validValues.toSet(), result.toSet(), "didn't have set of expected values")
      }
   }

}