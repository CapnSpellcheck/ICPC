package icpc.twothousandsixteen

import icpc.twothousandsixteen.PCPInstruction.Opcode
import util.WordScanner
import util.hardAssert
import util.indexBefore
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

/**
 * This file contains a solution of the ICPC problem: https://icpc.kattis.com/problems/mars
 */
private val WANTS_NO_RESOURCE = 0.toShort()

/**
 * An instruction per the problem. From the string in the constructor, I parse it into its more
 * meaning ful parts - the opcode and the subject (I'm sure there's a technical term for what I'm calling subject,
 * but I don't know what that is.)
 * @param token The problem-specified string for the instruction
 */
class PCPInstruction(token: String) {
   enum class Opcode {
      LOCK,
      UNLOCK,
      COMPUTE
   }

   val op: Opcode
   // The subject of LOCK/UNLOCK is the resource ID; for COMPUTE, it's the number of cycles needed. Mutable:
   // for COMPUTE, the task may write to it to indicate the cycles remaining.
   var subject: Int

   init {
      op = when (token[0]) {
         'L' -> Opcode.LOCK
         'C' -> Opcode.COMPUTE
         'U' -> Opcode.UNLOCK
         else -> error(Unit)
      }
      subject = token.substring(1).toInt()
   }

   override fun toString(): String {
      return "PCPInstruction(op=$op, subject=$subject)"
   }
}

/**
 * A task per the problem.
 * @param id The unique id. A given task group should have id's in the range 1-k.
 * @param startTime The clock value of the CPU when the task is eligible to run
 * @param basePriority The base priority of the task per problem statement
 * @param instructionList A single string containing tokens for instructions separated by a space.
 */
class PCPTask(val id: Int, val startTime: Int, val basePriority: Short, val instructionList: String) {
   var isStarted = false; private set // set by the `start` method
   var currentInstruction: PCPInstruction? = null; private set
   var isBlocked = false // set by outside

   private val instructionScanner = WordScanner(instructionList)

   // When the task is finished, `currentInstruction` will be null
   val isFinished: Boolean
      get() = currentInstruction == null

   /**
    * The ID of a resource this task is waiting for. 0 if not waiting. If ≠ 0, `currentInstruction.op` is LOCK.
    */
   var wantsResourceID: Short = WANTS_NO_RESOURCE; private set

   // Must be called before executing an instruction
   fun start() {
      isStarted = true
      advance()
   }

   // Move to the next instruction. Assumes current instruction has been executed. If the next instruction is a
   // LOCK, `wantsResourceID` is set to the ID of the resource it wants to lock; otherwise, `wantsResourceID` is
   // cleared to 0.
   fun advance() {
      currentInstruction = instructionScanner.nextAlphanumeric()?.let { PCPInstruction(it) }
      wantsResourceID = if (currentInstruction?.op == Opcode.LOCK) {
         currentInstruction!!.subject.toShort()
      } else {
         WANTS_NO_RESOURCE
      }
   }

   // Execute a COMPUTE instruction. Performs batch execution when the current instruction has multiple cycles
   // and we know that task priorities won't change.
   fun execute(cycles: Int = 1) {
      hardAssert(currentInstruction?.op == Opcode.COMPUTE && currentInstruction!!.subject >= cycles)
      currentInstruction!!.subject -= cycles
      if (currentInstruction!!.subject == 0) {
         advance()
      }
   }

}

/**
 * A simple representation of the resources described in the problem. The pc needs to be calculated beforehand,
 * and I keep track of the current owner with a property.
 */
class PCPResource(val id: Short, val priorityCeiling: Short, var owner: PCPTask? = null)

/**
 * Perform the simulation asked by the problem, and return the finish times.
 * @param tasks The tasks that have been parsed. Task IDs should be set in the order provided in the input starting
 * with 1.
 * @param numResources The number of resources needed by the tasks
 * @return The finish times, in task ID order.
 */
fun finishTimes(tasks: Array<PCPTask>, numResources: Int): IntArray {
   val tasksById = Array(tasks.size + 1) { tasks[0] } // [0] is sentinel
   for (task in tasks) {
      tasksById[task.id] = task
   }

   // create basePriorityMap: basePriorityMap[k] = id of task with basePriority k ([0] is sentinel, unused)
   val basePriorityMap = IntArray(tasks.size + 1)
   for (task in tasks) {
      basePriorityMap[task.basePriority.toInt()] = task.id
   }

   // create resources, computing priority ceilings
   // [0] is again a sentinel, never owned and has a `priorityCeiling` = 0.
   val resources = Array(numResources + 1) { i ->
      val resourceId = i.toShort()
      // The ceiling is the max base priority of any task that locks it. The instruction list will contain
      // 'L<resource id> ', note it will always have a space after it since it cannot be a final instruction.
      val pc = tasks.maxOf { if (it.instructionList.contains("L$resourceId ")) it.basePriority else 0 }
      PCPResource(resourceId, pc)
   }

   // Need to know the next task to start. Reorders the passed array.
   tasks.sortBy { it.startTime }

   var clock = 1
   var taskPos = 0
   var nextTask: PCPTask? = tasks[0]
   var unfinishedTaskCount = tasks.size
   val currentPriorities = ShortArray(tasks.size + 1) // cp[0] always = 0, 0 is sentinel task id
   val finishTimes = IntArray(tasks.size) // the return data

   while (unfinishedTaskCount != 0) {
      // start any tasks that start at current clock
      while (nextTask?.startTime == clock) {
         tasks[taskPos].start()
         taskPos += 1
         nextTask = if (tasks.size > taskPos) tasks[taskPos] else null
      }

      // reset currentPriorities
      repeat(tasks.size) { i ->
         currentPriorities[tasks[i].id] = tasks[i].basePriority
      }

      // We know what tasks are running: task.isStarted && !task.isFinished
      // Evaluate blocking among tasks that want a lock now.
      // We can do this in a single pass by starting with the running task with the highest bp, and iterating
      // down: we know the highest task's cp always = bp, so we don't have to worry about finding which tasks
      // it blocks, but we do need to analyze any tasks that block it. For those tasks their cp is raised to this
      // bp. For each next task, inductively, we know that its cp has reached its upper bound when we reach it, so
      // we can decide whether it is blocked.
      var basePriority = basePriorityMap.indexOfLast { tasksById[it].isStarted && !tasksById[it].isFinished }
      while (basePriority > 0) {
         // get the task ID from the map
         val runningTaskID = basePriorityMap[basePriority]
         val task = tasksById[runningTaskID]
         val taskPriority = currentPriorities[task.id]
         task.isBlocked = false // reset to false, will set if true
         if (task.wantsResourceID > 0) {
            // find all resources owned with priorityCeiling >= this task's cp OR the resource it wants if owned
            resources.asSequence().filter { res ->
               if (res.owner == null || res.owner == task)
                  return@filter false
               res.id == task.wantsResourceID || res.priorityCeiling >= currentPriorities[task.id]
            }
               .forEach { res ->
                  // task is blocked
                  task.isBlocked = true
                  // raise the blocker's cp if higher
                  if (taskPriority > currentPriorities[res.owner!!.id])
                     currentPriorities[res.owner!!.id] = taskPriority
               }
         }

         // find the next smaller bp among running
         basePriority = basePriorityMap.indexBefore({ tasksById[it].isStarted && !tasksById[it].isFinished }, basePriority)
      }

      // The cp's are now correct, and the task.isBlocked are correct. Find the running task with highest cp
      var winningTaskID = 0 // sentinel value
      for (i in 1 ..< currentPriorities.size) {
         val task = tasksById[i]
         if (task.isStarted && !task.isFinished && !task.isBlocked && currentPriorities[i] > currentPriorities[winningTaskID]) {
            winningTaskID = task.id
         }
      }
      val taskToExecute = if (winningTaskID > 0) tasksById[winningTaskID] else null
      val instructionToExecute = taskToExecute?.currentInstruction

      clock = when (instructionToExecute?.op) {
         Opcode.LOCK -> {
            val resource = resources[instructionToExecute.subject]
            resource.owner = taskToExecute
            taskToExecute.advance()
            clock
         }
         Opcode.UNLOCK -> {
            val resource = resources[instructionToExecute.subject]
            resource.owner = null
            taskToExecute.advance()
            clock
         }
         Opcode.COMPUTE -> {
            // The highest cp task remains so until it a) finishes, b) wants to lock, c) another task starts
            // So it can run compute instructions until another task starts
            val cyclesUntilNextStart = nextTask?.startTime?.let { it - clock } ?: 99999
            val cyclesToExecute = min(cyclesUntilNextStart, instructionToExecute.subject)
            // 'execute' will advance if appropriate
            taskToExecute.execute(cyclesToExecute)
            clock + cyclesToExecute
         }
         null -> clock + 1
      }

      if (taskToExecute?.isFinished == true) {
         finishTimes[taskToExecute.id - 1] = clock
         unfinishedTaskCount -= 1
      }
   }

   return finishTimes
}

fun finishTimesIO(inputStream: InputStream, outputStream: OutputStream) {
   inputStream.bufferedReader().use { reader ->
      val nums = reader.readLine().split(' ')
      val numResources = nums[1].toInt()
      val tasks = Array(nums[0].toInt()) { i ->
         val taskSpec = reader.readLine()
         val thirdSpacePos = taskSpec.indexOf(' ', taskSpec.indexOf(' ', taskSpec.indexOf(' ') + 1) + 1)
         val taskNums = taskSpec.substring(0, thirdSpacePos).split(' ')
         val instructions = taskSpec.substring(thirdSpacePos + 1)
         PCPTask(i + 1, taskNums[0].toInt(), taskNums[1].toShort(), instructions)
      }

      val finishTimes = finishTimes(tasks, numResources)

      outputStream.bufferedWriter().use { writer ->
         for (time in finishTimes) {
            writer.write(time.toString())
            writer.newLine()
         }
      }
   }
}

fun main() {
   finishTimesIO(System.`in`, System.out)
}