package util

import java.awt.Point
import kotlin.random.Random

class TwoDTreeMap<V : Comparable<V>> {
   private class Node<V>(val x: Int, val y: Int, value: V) {
      var value = value; private set
      private var deleted = false
      private var left: Node<V>? = null
      private var right: Node<V>? = null
      private var isHorizontal = true
//      private var height = 0

      constructor(x: Int, y: Int, value: V, left: Node<V>?, right: Node<V>?, isHorizontal: Boolean): this(x, y, value) {
         this.left = left
         this.right = right
         this.isHorizontal = isHorizontal
      }

      fun insert(node: Node<V>): Int {
         var cur: Node<V>? = this
         lateinit var parent: Node<V>
         var isLeft = true
         val x = node.x
         val y = node.y

         while (cur != null) {
            parent = cur
            val compare = if (cur.isHorizontal) x.compareTo(cur.x) else y.compareTo(cur.y)
            cur = if (compare < 0) {
               isLeft = true
               cur.left
            } else if (compare > 0 || if (cur.isHorizontal) cur.y != y else cur.x != x) {
               isLeft = false
               cur.right
            } else {
               cur.value = node.value
               if (cur.deleted) {
                  cur.deleted = false
                  return InsertCodes.UNDELETED
               } else {
                  return InsertCodes.NOT_INSERTED
               }
            }
         }

         node.isHorizontal = !parent.isHorizontal

         if (isLeft)
            parent.left = node
         else
            parent.right = node
         return InsertCodes.INSERTED
      }

      fun findAllTo(xRange: IntRange, yRange: IntRange, dest: MutableList<V>) {
         if (xRange.contains(x) && yRange.contains(y) && !deleted) {
            dest.add(value)
         }

         // quick paths to search one subtree
         if (isHorizontal) {
            if (xRange.first >= x) {
               right?.findAllTo(xRange, yRange, dest)
               return
            } else if (xRange.last < x) {
               left?.findAllTo(xRange, yRange, dest)
               return
            }
         } else {
            if (yRange.first >= y) {
               right?.findAllTo(xRange, yRange, dest)
               return
            } else if (yRange.last < y) {
               left?.findAllTo(xRange, yRange, dest)
               return
            }
         }

         left?.findAllTo(xRange, yRange, dest)
         right?.findAllTo(xRange, yRange, dest)
      }

      fun findAny(xRange: IntRange, yRange: IntRange): V? {
         if (xRange.contains(x) && yRange.contains(y) && !deleted) {
            return value
         }

         // quick paths to search one subtree
         if (isHorizontal) {
            if (xRange.first >= x)
               return right?.findAny(xRange, yRange)
            else if (xRange.last < x)
               return left?.findAny(xRange, yRange)
         } else {
            if (yRange.first >= y)
               return right?.findAny(xRange, yRange)
            else if (yRange.last < y) {
               return left?.findAny(xRange, yRange)
            }
         }

         if (Random.nextBoolean()) {
            left?.findAny(xRange, yRange)?.let { return it }
            right?.findAny(xRange, yRange)?.let { return it }
         } else {
            right?.findAny(xRange, yRange)?.let { return it }
            left?.findAny(xRange, yRange)?.let { return it }
         }
         return null
      }

      fun delete(parent: Node<V>? = null, x: Int, y: Int): Int {
         if (this.x == x && this.y == y) {
            if (deleted)
               return DeleteCodes.NOT_FOUND
            if (parent != null && left == null && right == null) {
               if (parent.left === this)
                  parent.left = null
               else
                  parent.right = null
               return DeleteCodes.DELETED_LEAF
            } else {
               deleted = true
               return DeleteCodes.DELETED_NONLEAF
            }
         }

         return if (isHorizontal) {
            if (x < this.x)
               left?.delete(this, x, y) ?: DeleteCodes.NOT_FOUND
            else
               right?.delete(this, x, y) ?: DeleteCodes.NOT_FOUND
         } else {
            if (y < this.y)
               left?.delete(this, x, y) ?: DeleteCodes.NOT_FOUND
            else
               right?.delete(this, x, y) ?: DeleteCodes.NOT_FOUND
         }
      }

   }

   private class Entry<V>(val p: Point, val value: V)

   private var setStore: HashMap<Point, V>? = HashMap(CREATE_TREE_THRESHOLD)
   private var root: Node<V>? = null
   private var size = 0
   private var deletedCount = 0

   fun insert(x: Int, y: Int, value: V): Boolean {
      setStore?.let { set ->
         if (size >= CREATE_TREE_THRESHOLD) {
            buildTree()
            this.setStore = null
         } else {
            return if (set.put(Point(x, y), value) == null) {
               size += 1
               true
            } else false
         }
      }
      root?.let { root ->
         val node = Node(x, y, value)
         val code = root.insert(node)
         if (code == InsertCodes.NOT_INSERTED)
            return false
         if (code == InsertCodes.UNDELETED)
            deletedCount -= 1
         else
            size += 1
         return true
      }
      root = Node(x, y, value)
      size = 1
      return true
   }

   fun delete(x: Int, y: Int): Boolean {
      setStore?.let { set ->
         return set.remove(Point(x, y)) != null
      }

      if (deletedCount >= DELETE_THRESHOLD && deletedCount >= size / 2) {
         rebuild()
      }

      if (size == 1) {
         root = null
         size = 0
         deletedCount = 0
         return true
      }
      val code = root!!.delete(null, x, y)
      if (code == DeleteCodes.NOT_FOUND)
         return false
      if (code == DeleteCodes.DELETED_NONLEAF)
         deletedCount += 1
      return true
   }

   fun findAll(xRange: IntRange, yRange: IntRange): List<V> {
      val list = ArrayList<V>()
      findAllTo(xRange, yRange, list)
      return list
   }

   fun findAny(xRange: IntRange, yRange: IntRange): V? {
      setStore?.let { set ->
         return set.entries
            .firstOrNull { xRange.contains(it.key.x) && yRange.contains(it.key.y) }
            ?.value
      }
      return root?.findAny(xRange, yRange)
   }

   fun findAllTo(xRange: IntRange, yRange: IntRange, dest: MutableList<V>) {
      setStore?.let { set ->
         set.mapNotNullTo(dest) {
            if (xRange.contains(it.key.x) && yRange.contains(it.key.y)) it.value else null
         }
      }
      root?.findAllTo(xRange, yRange, dest)
   }

   private fun buildTree() {
      root = buildTree(setStore!!.entries.map { Entry(it.key, it.value) } as ArrayList, true)
      setStore = null
   }

   private fun buildTree(entries: MutableList<Entry<V>>, isByX: Boolean): Node<V>? {
      if (entries.isEmpty()) return null
      entries.sortBy(if (isByX) { node -> node.p.x } else { node -> node.p.y } )
      val medianIndex = entries.size / 2
      val medianEntry = entries[medianIndex]
      val leftNode = buildTree(entries.subList(0, medianIndex), !isByX)
      val rightNode = buildTree(entries.subList(medianIndex + 1, entries.size), !isByX)
      return Node(medianEntry.p.x, medianEntry.p.y, medianEntry.value, leftNode, rightNode, isByX)
   }

   private fun rebuild() {

   }

   companion object {
      const val DELETE_THRESHOLD = 8
      const val CREATE_TREE_THRESHOLD = 7
   }
   object DeleteCodes {
      val NOT_FOUND = 0
      val DELETED_LEAF = 1
      val DELETED_NONLEAF = 2
   }

   object InsertCodes {
      val NOT_INSERTED = 0
      val UNDELETED = 1
      val INSERTED = 2
   }
}