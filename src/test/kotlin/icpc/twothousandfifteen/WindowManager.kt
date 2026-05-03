package icpc.twothousandfifteen

import icpc.twothousandfifteen.WindowManager.MoveResult
import icpc.twothousandfifteen.WindowManager.ResizeResult
import org.junit.jupiter.api.Assertions.assertEquals
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse

class WindowManagerTest {
   @Test fun testOpen() {
      for (dim in listOf(1000000000, 1000000, 1000, 100)) {
         val manager = WindowManager(dim, dim)
         val windowRects = HashSet<Rectangle>()
         repeat(100) {
            println("${windowRects.size} windows onscreen")
            val origin = Point(Random.nextInt(dim), Random.nextInt(dim))
            // dimension is at most 1/10th of screen
            val size = Dimension(
               if (origin.x + 1 == dim) 1 else Random.nextInt(1, min(dim / 10, dim - origin.x)),
               if (origin.y + 1 == dim) 1 else Random.nextInt(1, min(dim / 10, dim - origin.y))
            )
            val rect = Rectangle(origin, size)
            val retval = manager.open(origin.x, origin.y, size.width, size.height)
            val expected = windowRects.none { rect.intersects(it) }
            assertEquals(expected, retval, "window open result")
            if (expected) {
               windowRects.add(rect)
               println("window opened $rect")
            }
            // test above, below, right of & left of screen
            assertFalse(manager.open(-1, 0, 10, 10))
            assertFalse(manager.open(0, -5, 10, 10))
            assertFalse(manager.open(dim, 0, 10, 10))
            assertFalse(manager.open(0, dim, 10, 10))
         }
         println("******* RESET")
      }
   }

   @Test fun testClose() {
      val manager = WindowManager(1000, 2000)
      val windowRects = HashSet<Rectangle>()

      // open 100 first
      repeat(100) {
         val origin = Point(Random.nextInt(980), Random.nextInt(1960))
         // dimension is at most 1/10th of screen
         val size = Dimension(Random.nextInt(20), Random.nextInt(40))
         val rect = Rectangle(origin, size)
         if (manager.open(origin.x, origin.y, size.width, size.height))
            windowRects.add(rect)
      }

      println("opened ${windowRects.size} windows")
      // close a random point
      repeat(1000) {
         val x = Random.nextInt(1000)
         val y = Random.nextInt(2000)
         val closed = manager.close(x, y)
         val match = windowRects.firstOrNull { it.intersects(Rectangle(x, y, 1, 1))  }
         match?.let { match ->
            println("got a close")
            windowRects.remove(match)
         }
         assertEquals(match != null, closed, "window close result")
      }
   }

   @Test fun testResize() {
      val manager = WindowManager(300, 300)
      manager.open(100, 0, 100, 100)
      manager.open(0, 100, 100, 100)
      manager.open(200, 100, 100, 100)
      manager.open(100, 200, 100, 100)

      manager.open(100, 100, 100, 100)

      for (x in 0 ..< 100) {
         for (y in 200 ..< 300) {
            assertEquals(ResizeResult.NoWindowFound, manager.resize(x, y, 1, 1))
         }
      }
      assertEquals(ResizeResult.DoesNotFit, manager.resize(150, 150, 50, 101))
      assertEquals(ResizeResult.DoesNotFit, manager.resize(150, 150, 101, 10))
      assertEquals(ResizeResult.OK, manager.resize(150, 150, 99, 99))

      assertEquals(ResizeResult.DoesNotFit, manager.resize(299, 101, 101, 100))
      assertEquals(ResizeResult.OK, manager.resize(299, 101, 100, 200))

      assertEquals(ResizeResult.DoesNotFit, manager.resize(199, 201, 101, 50))
   }

   @Test fun testMoveRight() {
      val manager = WindowManager(100, 1)
      manager.open(25, 0, 20, 1)
      manager.open(60, 0, 10, 1)
      manager.open(70, 0, 12, 1)
      manager.open(83, 0, 7, 1)

      // window to move
      manager.open(0, 0, 20, 1)

      assertEquals(MoveResult(MoveResult.NoWindowFound), manager.moveX(21, 0, 10))
      assertEquals(MoveResult(MoveResult.NoWindowFound), manager.moveX(95, 0, 20))

      assertEquals(MoveResult(MoveResult.OK, 5), manager.moveX(15, 0, 5))
      // x coords now 5, 25, 60, 70, 83
      assertEquals(MoveResult(MoveResult.OK, 10), manager.moveX(15, 0, 10))
      // x coords now 15, 35, 60, 70, 83
      assertEquals(MoveResult(MoveResult.OK, 10), manager.moveX(20, 0, 10))
      // x coords now 25, 45, 65, 75, 87
      assertEquals(MoveResult(MoveResult.OK, 5), manager.moveX(30, 0, 5))
      // x coords now 30, 50, 70, 80, 92
      // can only move 1
      assertEquals(MoveResult(MoveResult.MovedLess, 1), manager.moveX(30, 0, 5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0), manager.moveX(48, 0, 5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0), manager.moveX(79, 0, 5))
   }

   @Test fun testMoveLeft() {
      val manager = WindowManager(100, 1)
      manager.open(15, 0, 20, 1)
      manager.open(40, 0, 10, 1)
      manager.open(50, 0, 12, 1)
      manager.open(65, 0, 10, 1)

      // window to move
      manager.open(80, 0, 20, 1)

      assertEquals(MoveResult(MoveResult.NoWindowFound), manager.moveX(11, 0, -10))
      assertEquals(MoveResult(MoveResult.NoWindowFound), manager.moveX(64, 0, -20))

      assertEquals(MoveResult(MoveResult.OK, -5), manager.moveX(99, 0, -5))
      // x coords now 15, 40, 50, 65, 75
      assertEquals(MoveResult(MoveResult.OK, -5), manager.moveX(90, 0, -5))
      // x coords now 15, 38, 48, 60, 70
      assertEquals(MoveResult(MoveResult.OK, -10), manager.moveX(80, 0, -10))
      // x coords now 8, 28, 38, 50, 60
      assertEquals(MoveResult(MoveResult.OK, -5), manager.moveX(65, 0, -5))
      // x coords now 3, 23, 33, 45, 55
      // can only move 3
      assertEquals(MoveResult(MoveResult.MovedLess, -3), manager.moveX(70, 0, -5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0), manager.moveX(60, 0, -5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0), manager.moveX(55, 0, -5))
   }

   @Test fun testMoveDown() {
      val manager = WindowManager(1, 100)
      manager.open(0, 25, 1, 20)
      manager.open(0, 60, 1, 10)
      manager.open(0, 70, 1, 12)
      manager.open(0, 83, 1, 7)

      // window to move
      manager.open(0, 0, 1, 20)

      assertEquals(MoveResult(MoveResult.NoWindowFound), manager.moveY(0, 21, 10))
      assertEquals(MoveResult(MoveResult.NoWindowFound), manager.moveY(0, 95, 20))

      assertEquals(MoveResult(MoveResult.OK, 5), manager.moveY(0, 15, 5))
      // y coords now 5, 25, 60, 70, 83
      assertEquals(MoveResult(MoveResult.OK, 10), manager.moveY(0, 15, 10))
      // y coords now 15, 35, 60, 70, 83
      assertEquals(MoveResult(MoveResult.OK, 10), manager.moveY(0, 20, 10))
      // y coords now 25, 45, 65, 75, 87
      assertEquals(MoveResult(MoveResult.OK, 5), manager.moveY(0, 30, 5))
      // y coords now 30, 50, 70, 80, 92
      // can only move 1
      assertEquals(MoveResult(MoveResult.MovedLess, 1), manager.moveY(0, 30, 5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0), manager.moveY(0, 48, 5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0), manager.moveY(0, 79, 5))
   }

   @Test fun testMoveUp() {
      val manager = WindowManager(1, 100)
      manager.open( 0, 15, 1, 20, )
      manager.open( 0, 40, 1, 10, )
      manager.open( 0, 50, 1, 12, )
      manager.open( 0, 65, 1, 10, )

      // window to move
      manager.open(0, 80, 1, 20)

      assertEquals(MoveResult(MoveResult.NoWindowFound), manager.moveY(0, 11, -10))
      assertEquals(MoveResult(MoveResult.NoWindowFound), manager.moveY(0, 64, -20))

      assertEquals(MoveResult(MoveResult.OK, -5),  manager.moveY(0, 99, -5))
      // x coords now 15, 40, 50, 65, 75
      assertEquals(MoveResult(MoveResult.OK, -5),  manager.moveY(0, 90, -5))
      // x coords now 15, 38, 48, 60, 70
      assertEquals(MoveResult(MoveResult.OK, -10), manager.moveY(0, 80, -10))
      // x coords now 8, 28, 38, 50, 60
      assertEquals(MoveResult(MoveResult.OK, -5),  manager.moveY(0, 65, -5))
      // x coords now 3, 23, 33, 45, 55
      // can only move 3
      assertEquals(MoveResult(MoveResult.MovedLess, -3), manager.moveY(0, 70, -5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0),  manager.moveY(0, 60, -5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0),  manager.moveY(0, 55, -5))
   }
}