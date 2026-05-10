package icpc.twothousandfifteen

import icpc.twothousandfifteen.WindowManager.*
import util.StringOutputStream
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.io.StringBufferInputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowManagerTest {
   @Test fun testOpen() {
      for (dim in listOf(1000000000, 1000000, 1000, 100)) {
         val manager = WindowManager(dim, dim)
         val windowRects = HashSet<Rectangle>()
         repeat(1000) {
            val origin = Point(Random.nextInt(dim), Random.nextInt(dim))
            // dimension is at most 1/10th of screen
            val size = Dimension(
               if (origin.x + 1 == dim) 1 else Random.nextInt(1, min(dim / 10, dim - origin.x)),
               if (origin.y + 1 == dim) 1 else Random.nextInt(1, min(dim / 10, dim - origin.y))
            )
            val rect = Rectangle(origin, size)
            val retval = manager.open(origin.x, origin.y, size.width, size.height)
            println("manager.open(${origin.x}, ${origin.y}, ${size.width}, ${size.height})")
            val expected = windowRects.none { rect.intersects(it) }
            assertEquals(expected, retval, "window open result")
            if (expected) {
               windowRects.add(rect)
               println("window opened")
            }
         }
         // test above, below, right of & left of screen
         assertFalse(manager.open(-1, 0, 10, 10))
         assertFalse(manager.open(0, -5, 10, 10))
         assertFalse(manager.open(dim, 0, 10, 10))
         assertFalse(manager.open(0, dim, 10, 10))
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
         println("manager.close($x, $y)")
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

      assertEquals(MoveResult(MoveResult.OK, 5), manager.moveX(99, 0, -5))
      // x coords now 15, 40, 50, 65, 75
      assertEquals(MoveResult(MoveResult.OK, 5), manager.moveX(90, 0, -5))
      // x coords now 15, 38, 48, 60, 70
      assertEquals(MoveResult(MoveResult.OK, 10), manager.moveX(80, 0, -10))
      // x coords now 8, 28, 38, 50, 60
      assertEquals(MoveResult(MoveResult.OK, 5), manager.moveX(65, 0, -5))
      // x coords now 3, 23, 33, 45, 55
      // can only move 3
      assertEquals(MoveResult(MoveResult.MovedLess, 3), manager.moveX(70, 0, -5))
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

      assertEquals(MoveResult(MoveResult.OK, 5),  manager.moveY(0, 99, -5))
      // y coords now 15, 40, 50, 65, 75
      assertEquals(MoveResult(MoveResult.OK, 5),  manager.moveY(0, 90, -5))
      // y coords now 15, 38, 48, 60, 70
      assertEquals(MoveResult(MoveResult.OK, 10), manager.moveY(0, 80, -10))
      // y coords now 8, 28, 38, 50, 60
      assertEquals(MoveResult(MoveResult.OK, 5),  manager.moveY(0, 65, -5))
      // y coords now 3, 23, 33, 45, 55
      // can only move 3
      assertEquals(MoveResult(MoveResult.MovedLess, 3), manager.moveY(0, 70, -5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0),  manager.moveY(0, 60, -5))
      assertEquals(MoveResult(MoveResult.MovedLess, 0),  manager.moveY(0, 55, -5))
   }

   @Test fun testMovingPushesAnotherWindowThatExtendsBeyondOriginalWindowOnCrossAxis() {
      val manager = WindowManager(10, 10)
      assertTrue(manager.open(0, 0, 1, 1))
      assertTrue(manager.open(5, 0, 1, 5))
      assertTrue(manager.open(6, 3, 4, 1))
      
      val result = manager.moveX(0, 0, 10)

      assertEquals(4, result.movedAmount)
   }

   @Test fun testSample1() {
      val commands = listOf(Command.OPEN, Command.OPEN, Command.OPEN, Command.RESIZE, Command.RESIZE, Command.MOVE, Command.CLOSE, Command.CLOSE, Command.MOVE)
      val params = listOf(intArrayOf(50, 50, 10, 10), intArrayOf(70, 55, 10, 10), intArrayOf(90, 50, 10, 10), intArrayOf(55, 55, 40, 40), intArrayOf(55, 55, 15, 15), intArrayOf(55, 55, 40, 0), intArrayOf(55, 55), intArrayOf(110, 60), intArrayOf(95, 55, 0, -100))
      val result = simulateWindowManager(320, 200, commands, params)

      val expectedOutput = listOf(
         "Command 4: RESIZE - window does not fit",
         "Command 7: CLOSE - no window at given position",
         "Command 9: MOVE - moved 50 instead of 100"
      )
      assertEquals(result.first, expectedOutput, "error messages")
      assertEquals(2, result.second, "window count")

      val window1 = result.third.next()
      assertEquals(90, window1.originX, "window 1 originX")
      assertEquals(0, window1.originY, "window 1 originY")
      assertEquals(15, window1.width, "window 1 width")
      assertEquals(15, window1.height, "window 1 height")

      val window2 = result.third.next()
      assertEquals(115, window2.originX, "window 1 originX")
      assertEquals(50, window2.originY, "window 1 originX")
      assertEquals(10, window2.width, "window 1 originX")
      assertEquals(10, window2.height, "window 1 originX")
      assertFalse(result.third.hasNext())
   }

   @Test fun testSample1IO() {
      val input = """
         320 200
         OPEN 50 50 10 10
         OPEN 70 55 10 10
         OPEN 90 50 10 10
         RESIZE 55 55 40 40
         RESIZE 55 55 15 15
         MOVE 55 55 40 0
         CLOSE 55 55
         CLOSE 110 60
         MOVE 95 55 0 -100
      """.trimIndent() + "\n"
      val sos = StringOutputStream()
      simulateWindowManagerIO(StringBufferInputStream(input), sos)

      assertEquals("""
         Command 4: RESIZE - window does not fit
         Command 7: CLOSE - no window at given position
         Command 9: MOVE - moved 50 instead of 100
         2 window(s):
         90 0 15 15
         115 50 10 10
      """.trimIndent() + "\n", sos.toString())
   }

   @Test fun testAllCommandsEnsuringCodeAssertions() {
      repeat(1000) {
         val w = 5
         val h = 5
         println("---- NEW CASE $w $h----")

         // generate 250 random commands
         val commandsWithParams = (1 .. 250).map { i ->
            val f = Random.nextFloat()
            val x = Random.nextInt(w)
            val y = Random.nextInt(h)
            val w = 1 + Random.nextInt(max(1, x / 2))
            val h = 1 + Random.nextInt(max(1, y / 2))
            if (f < 0.25) {
               Pair(Command.OPEN, intArrayOf(x, y, w, h))
            } else if (f < 0.4) {
               Pair(Command.CLOSE, intArrayOf(x, y))
            } else if (f < 0.75) {
               Pair(Command.RESIZE, intArrayOf(x, y, w, h))
            } else {
               val moveX = Random.nextBoolean()
               val moveGreater = Random.nextBoolean()
               val moveAmount = Random.nextInt(max(1, if (moveX) x / 2 else y / 2)) * (if (moveGreater) 1 else -1)
               Pair(Command.MOVE, intArrayOf(x, y, if (moveX) moveAmount else 0, if (!moveX) moveAmount else 0))
            }
         }.unzip()
         simulateWindowManager(w, h, commandsWithParams.first, commandsWithParams.second)
      }
   }

   @Test fun testOpenCloseAndResizeRandomly() {
      repeat(100) {
         val w = Random.nextInt(1, 101)
         val h = Random.nextInt(1, 101)
         val manager = WindowManager(w, h)
         val screenRect = Rectangle(0, 0, w, h)
         val windowRects = mutableListOf<Rectangle>()

         println("---- NEW CASE ----")
         println("w=$w h=$h")
         repeat(1000) {
            val f = Random.nextFloat()
            if (f < 0.333) {
               val rect = Rectangle(Random.nextInt(w), Random.nextInt(h), 1 + Random.nextInt(max(1, w / 2 - 1)), 1 + Random.nextInt(max(1, h / 2 - 1)))
               println("OPEN ${rect.x} ${rect.y} ${rect.width} ${rect.height}")
               val result = manager.open(rect.x, rect.y, rect.width, rect.height)
               var shouldBeTrue = true
               if (windowRects.any { it.intersects(rect) }) {
                  assertFalse(result, "open fails - window overlap")
                  shouldBeTrue = false
               }
               if (!screenRect.contains(rect)) {
                  assertFalse(result, "open fails - outside screen")
                  shouldBeTrue = false
               }
               if (shouldBeTrue) {
                  assertTrue(result, "open succeeds")
                  windowRects.add(rect)
               }
            } else if (f < 0.667) {
               val point = Point(Random.nextInt(w), Random.nextInt(h))
               println("CLOSE ${point.x} ${point.y}")
               val result = manager.close(point.x, point.y)
               val i = windowRects.indexOfFirst { it.contains(point) }
               assertEquals(i >= 0, result)
               if (i >= 0)
                  windowRects.removeAt(i)
            } else  {
               val point = Point(Random.nextInt(w), Random.nextInt(h))
               val size = Dimension(1 + Random.nextInt(max(1, w / 2 - 1)), 1 + Random.nextInt(max(1, h / 2 - 1)))
               println("RESIZE ${point.x} ${point.y} ${size.width} ${size.height}")
               val result = manager.resize(point.x, point.y, size.width, size.height)
               val windowRect = windowRects.firstOrNull { it.contains(point) }
               println("windowRect = $windowRect")
               if (windowRect == null) {
                  assertEquals(ResizeResult.NoWindowFound, result)
               } else {
                  val newWindowRect = Rectangle(windowRect).also { it.size = size }
                  if (!screenRect.contains(newWindowRect) ||
                     windowRects.any { it.location != windowRect.location && it.intersects(newWindowRect) })
                  {
                     println("$screenRect doesn't contain $newWindowRect")
                     assertEquals(ResizeResult.DoesNotFit, result)
                  } else {
                     assertEquals(ResizeResult.OK, result)
                     windowRect.size = size
                  }
               }
            }
         }

         assertEquals(windowRects.size, manager.windowCount, "window count")
         var i = 0
         val iter = manager.windowIterator()
         while (iter.hasNext()) {
            assertTrue(iter.next().equals(windowRects[i]), "final window rect")
            i += 1
         }

      }
   }

   @Test fun testMoveABunchOfWindowsDown() {
      val manager = WindowManager(100, 100)
      manager.open(40, 47, 20, 20)
      manager.open(66, 35, 25, 25)
      manager.open(35, 25, 7, 8)
      manager.open(55, 20, 40, 10)
      manager.open(7, 55, 20, 33)
      manager.open(40, 5, 20, 8)
      manager.open(22, 41, 16, 7)

      val result = manager.moveY(50, 8, 100)
      assertEquals(MoveResult.MovedLess, result.code)
      assertEquals(39, result.movedAmount)
      val windows = manager.windowIterator().asSequence().toList()
      assertEquals(62, windows[0].originY)
      assertEquals(62, windows[1].originY)
      assertEquals(52, windows[2].originY)
      assertEquals(52, windows[3].originY)
      assertEquals(67, windows[4].originY)
      assertEquals(44, windows[5].originY)
      assertEquals(60, windows[6].originY)
   }

   @Test fun testMoveABunchOfWindowsRight() {
      val manager = WindowManager(100, 100)
      manager.open(47, 40, 20, 20)
      manager.open(35, 66, 25, 25)
      manager.open(25, 35, 8, 7)
      manager.open(20, 55, 10, 40)
      manager.open(55, 7, 33, 20)
      manager.open(5, 40, 8, 20)
      manager.open(41, 22, 7, 16)

      val result = manager.moveX(8, 50, 100)
      assertEquals(MoveResult.MovedLess, result.code)
      assertEquals(39, result.movedAmount)
      val windows = manager.windowIterator().asSequence().toList()
      assertEquals(62, windows[0].originX)
      assertEquals(62, windows[1].originX)
      assertEquals(52, windows[2].originX)
      assertEquals(52, windows[3].originX)
      assertEquals(67, windows[4].originX)
      assertEquals(44, windows[5].originX)
      assertEquals(60, windows[6].originX)
   }

   @Test fun testMoveWithWindowHiddenInMiddle() {
      val manager = WindowManager(100, 100)
      manager.open(20, 8, 20, 80)
      manager.open(80, 25, 8, 50)
      manager.open(42, 30, 32, 10)
      manager.open(55, 68, 10, 12)

      val result = manager.moveX(85, 50, -100)
      assertEquals(28, result.movedAmount)
      val windows = manager.windowIterator().asSequence().toList()
      assertEquals(0, windows[0].originX)
      assertEquals(52, windows[1].originX)
      assertEquals(20, windows[2].originX)
      assertEquals(42, windows[3].originX)
   }

   @Test fun testBreak1() {
      val manager = WindowManager(34, 70)
      manager.open(24, 9, 4, 17)
      manager.open(28, 23, 1, 13)
      val result = manager.resize(26, 23, 5, 4)
      assertEquals(ResizeResult.OK, result)
   }

   @Test fun testBreak2() {
      val manager = WindowManager(38, 2)
      manager.open(5 ,0, 11, 1)
      manager.open(31, 0, 5, 1)
      manager.open(0 ,1 ,10, 1)
      manager.resize(10 ,1 ,1, 1)
      manager.resize(5,0,6,1)
      manager.close(22, 1)
      manager.open(15, 1, 2, 1)
      manager.close(26, 1)
      manager.open(19,1,15,1)

      val result = manager.resize(10,0,11,1)
      assertEquals(ResizeResult.OK, result)
   }

   @Test fun testBreak3() {
      val manager = WindowManager(38, 2)
      manager.open(8, 62553, 0, 10297)
      manager.open(6, 13108, 3, 44359)
      manager.close(8, 58057)
      manager.close(6, 16518)
      manager.open(6, 38841, 1, 36165)
      manager.moveX(6, 42563, 2)
   }

   @Test fun testBreak4() {
      val manager = WindowManager(5,5)
      manager.open(1, 2, 1, 1)
      manager.resize(2, 0, 1, 1)
      manager.close(4, 4)
      manager.moveX(1, 1, 0)
      manager.open(2, 4, 1, 2)
      manager.close(3, 3)
      manager.resize(1, 1, 1, 1)
      manager.open(2, 0, 1, 1)
      manager.resize(0, 0, 1, 1)
      manager.open(0, 4, 1, 2)
      manager.close(0, 0)
      manager.resize(0, 0, 1, 1)
      manager.moveY(2, 0, 0)
      manager.moveX(4, 3, 0)
      manager.close(2, 3)
      manager.open(1, 4, 1, 1)
      manager.open(0, 3, 1, 1)
      manager.resize(1, 4, 1, 2)
      manager.resize(0, 2, 1, 1)
      manager.moveX(3, 0, 0)
      manager.resize(3, 1, 1, 1)
      manager.open(2, 2, 1, 1)
      manager.open(1, 1, 1, 1)
      manager.open(1, 3, 1, 1)
      manager.moveY(1, 4, -1)
   }

}
