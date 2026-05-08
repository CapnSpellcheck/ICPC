package util

import java.util.*
import kotlin.math.max

class IntervalTreeMap<V> {
   private abstract class NodeOrProxy<T> {
      abstract val parent: Node<T>?
      abstract fun isLeftChild(): Boolean
      abstract fun isRightChild(): Boolean
      abstract var isBlack: Boolean
   }

   private class NilProxyNode<T>(override val parent: Node<T>?, val isLeft: Boolean) : NodeOrProxy<T>() {
      override fun isLeftChild(): Boolean = isLeft
      override fun isRightChild(): Boolean = !isLeft
      override var isBlack: Boolean
         get() = true
         set(_) {}
   }

   private class Node<V>(interval: IntRange, ancillary: V) : NodeOrProxy<V>() {
      // interval and primary data
      var interval = interval; private set
      var ancillary = ancillary; private set
      var maxEnd = interval.last

      // node references and algorithm data
      override var parent: Node<V>? = null
      var leftChild: Node<V>? = null
      var rightChild: Node<V>? = null
      override var isBlack = false

      inline val start
         get() = interval.first
      inline val end
         get() = interval.last

      ///////////////////////////////////
      // Node -- General query methods //
      ///////////////////////////////////

      /**
       * Searches the subtree rooted at this Node for the given Interval.
       * @param int - the Interval to search for
       * @return the Node with the given Interval, if it exists; otherwise,
       * the sentinel Node
       */
      fun search(int: IntRange): Node<V>? {
         tailrec fun <T> search(cur: Node<T>?): Node<T>? {
            if (cur == null)
               return null
            var comp = cur.interval.compareTo(int)
            if (comp > 0)
               return search(cur.leftChild)
            if (comp < 0)
               return search(cur.rightChild)
            return cur
         }
         return search(this)
      }

      /**
       * Searches the subtree rooted at this Node for its minimum Interval.
       * @return the Node with the minimum Interval
       */
      fun minimumNode(): Node<V> {
         var cur = this
         while (true) {
            cur.leftChild?.let { cur = it } ?: break
         }
         return cur
      }

      ///////////////////////////////////////
      // Node -- Overlapping query methods //
      ///////////////////////////////////////
      /**
       * Returns a Node from this Node's subtree that overlaps the given
       * Interval.
       *
       * The only guarantee of this method is that the returned Node overlaps
       * the Interval t. This method is meant to be a quick helper method to
       * determine if any overlap exists between an Interval and any of an
       * IntervalTree's Intervals. The returned Node will be the first
       * overlapping one found.
       * @param int - the given Interval
       * @return an overlapping Node from this Node's subtree, if one exists;
       * otherwise the sentinel Node
       */
      fun anyOverlappingNode(int: IntRange): Node<*>? {
         tailrec fun <T> anyOverlappingNode(t: IntRange, cur: Node<T>?): Node<T>? {
            if (cur == null)
               return null
            if (!t.intersects(cur.interval)) {
               return if ((cur.leftChild?.maxEnd ?: Int.MIN_VALUE) >= t.first) {
                  anyOverlappingNode(t, cur.leftChild)
               } else {
                  anyOverlappingNode(t, cur.rightChild)
               }
            }
            return cur
         }
         return anyOverlappingNode(int, this)
      }

      /**
       * Returns the minimum Node from this Node's subtree that overlaps the
       * given Interval.
       * @param int - the given Interval
       * @return the minimum Node from this Node's subtree that overlaps the
       * Interval t, if one exists; otherwise, the sentinel Node
       */
      fun minimumOverlappingNode(int: IntRange): Node<V>? {
         var result: Node<V>? = null
         var cur = this

         if (cur.maxEnd >= int.first) {
            while (true) {
               if (cur.interval.intersects(int)) {

                  // This node overlaps. There may be a lesser overlapper
                  // down the left subtree. No need to consider the right
                  // as all overlappers there will be greater.
                  result = cur
                  val left = cur.leftChild
                  if (left == null || left.maxEnd < int.first)
                  // Either no left subtree, or nodes can't overlap.
                     break
                  cur = left
               } else {

                  // This node doesn't overlap.
                  // Check the left subtree if an overlapper may be there
                  val left = cur.leftChild
                  if (left != null && left.maxEnd >= int.first)
                     cur = left
                  else {
                     // Left subtree cannot contain an overlapper. Check the
                     // right sub-tree.
                     if (cur.start > int.last)
                     // Nothing in the right subtree can overlap
                        break
                     val right = cur.rightChild
                     if (right == null || right.maxEnd < int.first)
                        break
                     cur = right
                  }
               }
            }
         }

         return result
      }

      /**
       * An Iterator over all values in this Node's subtree that overlap the
       * given Interval t.
       * @param int - the overlapping Interval
       */
      fun overlappers(int: IntRange): Iterator<OverlapResult<V>> = OverlapperIterator(this, int)

      /**
       * The next Node (relative to this Node) which overlaps the given
       * Interval t
       * @param int - the overlapping Interval
       * @return the next Node that overlaps the Interval t, if one exists;
       * otherwise, the sentinel Node
       */
      fun nextOverlappingNode(int: IntRange): Node<V>? {
         var n = this
         var retval: Node<V>? = rightChild?.minimumOverlappingNode(int)
         var parent = n.parent
         while (parent != null && retval == null) {
            if (n.isLeftChild()) {
               retval = if (parent.interval.intersects(int))
                  parent else parent.rightChild?.minimumOverlappingNode(int)
            }
            n = parent
            parent = n.parent
         }

         return retval
      }

      /**
       * Whether or not this Node is the left child of its parent.
       */
      override inline fun isLeftChild(): Boolean {
         return this == parent?.leftChild
      }

      /**
       * Whether or not this Node is the right child of its parent.
       */
      override inline fun isRightChild(): Boolean {
         return this == parent?.rightChild
      }

      /**
       * Sets this Node's color to black.
       */
      inline fun blacken() {
         isBlack = true
      }

      /**
       * Sets this Node's color to red.
       */
      inline fun redden() {
         isBlack = false
      }

      /**
       * Sets the maxEnd value for this Node.
       *
       *
       * The maxEnd value should be the highest of:
       *
       *  * the end value of this node's data
       *  * the maxEnd value of this node's left child, if not null
       *  * the maxEnd value of this node's right child, if not null
       *
       *
       * This method will be correct only if the left and right children have
       * correct maxEnd values.
       */
      fun resetMaxEnd() {
         maxEnd = maxOf(
            end,
            leftChild?.maxEnd ?: Int.MIN_VALUE,
            rightChild?.maxEnd ?: Int.MIN_VALUE
         )
      }

      /**
       * Sets the maxEnd value for this Node, and all Nodes up to the root of
       * the tree.
       */
      fun maxEndFixup() {
         tailrec fun maxEndFixup(node: Node<*>?) {
            if (node == null)
               return
            node.resetMaxEnd()
            maxEndFixup(node.parent)
         }
         maxEndFixup(this)
      }

      /**
       * Performs a left-rotation on this Node.
       * @see - Cormen et al. "Introduction to Algorithms", 2nd ed, pp. 277-279.
       */
      fun leftRotate(assignRoot: (Node<V>?) -> Unit) {
         val oldRight: Node<V>? = rightChild
         this.rightChild = oldRight?.leftChild

         oldRight?.leftChild?.let {
            it.parent = this
         }
         oldRight?.parent = parent

         if (parent == null)
            assignRoot(oldRight)
         else if (isLeftChild())
            parent?.leftChild = oldRight
         else
            parent?.rightChild = oldRight

         oldRight?.leftChild = this
         this.parent = oldRight

         resetMaxEnd()
         oldRight?.resetMaxEnd()
      }

      /**
       * Performs a right-rotation on this Node.
       * @see - Cormen et al. "Introduction to Algorithms", 2nd ed, pp. 277-279.
       */
      fun rightRotate(assignRoot: (Node<V>?) -> Unit) {
         val oldLeft: Node<V>? = leftChild
         this.leftChild = oldLeft?.rightChild

         oldLeft?.rightChild?.let {
            it.parent = this
         }
         oldLeft?.parent = parent

         if (parent == null)
            assignRoot(oldLeft)
         else if (isLeftChild())
            parent?.leftChild = oldLeft
         else
            parent?.rightChild = oldLeft

         oldLeft?.rightChild = this
         this.parent = oldLeft

         resetMaxEnd()
         oldLeft?.resetMaxEnd()
      }

      /**
       * Take over another node's role, replacing this's data with other's.
       */
      fun replace(other: Node<V>) {
         this.maxEnd = other.maxEnd
         this.interval = other.interval
         this.ancillary = other.ancillary
         maxEndFixup()
      }

      override fun toString(): String {
         val color = if (isBlack) "black" else "red"
         return """
            start = $start
            end = $end
            maxEnd = $maxEnd
            color = $color
            """.trimIndent()
      }

   }

   class OverlapResult<V>(val interval: IntRange, val ancillary: V)

   private var root: Node<V>? = null
   private val ASSIGN_ROOT: (Node<V>?) -> Unit = { newRoot -> this.root = newRoot }

   ///////////////////////////////////
   // Tree -- General query methods //
   ///////////////////////////////////

   /**
    * The Node in this IntervalTree that contains the given Interval.
    * This method returns the nil Node if the Interval t cannot be found.
    * @param int - the Interval to search for.
    */
   private inline fun search(int: IntRange): Node<V>? = root?.search(int)

   /**
    * An Iterator over the Intervals in this IntervalTree that overlap the
    * given Interval
    * @param int - the overlapping Interval
    */
   fun overlappers(int: IntRange): Iterator<OverlapResult<V>> = root?.overlappers(int) ?: Collections.emptyIterator()

   /**
    * Whether or not any of the Intervals in this IntervalTree overlap the given
    * Interval
    * @param int - the potentially overlapping Interval
    */
   fun hasAnyOverlap(int: IntRange): Boolean {
      return root?.anyOverlappingNode(int) != null
   }

   /**
    * Whether or not this IntervalTree contains the given Interval.
    * @param t - the Interval to search for
    */
   fun contains(int: IntRange): Boolean = search(int) != null

   /**
    * Get the ancillary value of a given interval, or null if no such interval is found.
    */
   fun get(int: IntRange): V? = search(int)?.ancillary

   ///////////////////////////////
   // Tree -- Mutation methods //
   ///////////////////////////////

   /**
    * Inserts the given value into the IntervalTree.
    *
    * This method constructs a new Node containing the given value and places
    * it into the tree. If the value already exists within the tree, the tree
    * remains unchanged.
    * @param int - the value to place into the tree
    * @return if the value did not already exist, i.e., true if the tree was
    * changed, false if it was not
    */
   fun insert(int: IntRange, ancillaryData: V): Boolean {
      val new = Node(int, ancillaryData)

      var y: Node<V>? = null
      var x: Node<V>? = root
      var isLeft = false

      while (x != null) {                         // Traverse the tree down to a leaf.
         y = x
         x.maxEnd = max(x.maxEnd, new.maxEnd) // Update maxEnd on the way down.
         val intCompare: Int = int.compareTo(x.interval)
         x = if (intCompare < 0) {
            isLeft = true
            x.leftChild
         }
         else if (intCompare > 0) {
            isLeft = false
            x.rightChild
         }
         else
         // Interval already in tree. Do nothing.
            return false
      }

      new.parent = y

      if (y == null) {
         root = new
         new.isBlack = true
      } else {                      // Set the parent of n.
         if (isLeft) {
            y.leftChild = new
         } else {
            y.rightChild = new
         }
         insertFixup(new)
      }
      return true
   }

   private fun insertFixup(node: Node<V>) {
      var cur = node
      var curParent = cur.parent

      fun uncleIsRed(s: Node<*>): Node<V>? {
         curParent!!.blacken()
         s.blacken()
         curParent!!.parent?.redden()

         return if (curParent!!.parent != null) {
            cur = curParent!!.parent!!
            cur.parent
         } else
            null
      }

      while (curParent != null && !curParent.isBlack) {
         if (curParent.isLeftChild()) {
            val uncle = curParent.parent?.rightChild

            if (uncle?.isBlack != false) {
               if (cur.isRightChild()) {
                  cur = curParent
                  cur.leftRotate(ASSIGN_ROOT)
                  curParent = cur.parent
               }
               curParent?.blacken()
               curParent?.parent?.redden()
               curParent?.parent?.rightRotate(ASSIGN_ROOT)
            } else {
               curParent = uncleIsRed(uncle)
            }
         } else {
            val uncle = curParent.parent?.leftChild

            if (uncle?.isBlack != false) {
               if (cur.isLeftChild()) {
                  cur = curParent
                  cur.rightRotate(ASSIGN_ROOT)
                  curParent = cur.parent
               }
               curParent?.blacken()
               curParent?.parent?.redden()
               curParent?.parent?.leftRotate(ASSIGN_ROOT)
            } else {
               curParent = uncleIsRed(uncle)
            }
         }
      }

      root!!.isBlack = true
   }

   /**
    * Deletes the given value from this IntervalTree.
    *
    * If the value does not exist, this IntervalTree remains unchanged.
    * @param int - the Interval to delete from the tree
    * @return whether or not an Interval was removed from this IntervalTree
    */
   fun delete(int: IntRange) {
      val node = search(int) ?: return
      val nodeOrSuccessor =
         if (node.leftChild == null || node.rightChild == null)
            node
         else node.rightChild!!.minimumNode()

      val x = nodeOrSuccessor.leftChild ?: nodeOrSuccessor.rightChild
      val parent = nodeOrSuccessor.parent
      x?.parent = parent

      val yIsLeft: Boolean
      if (parent == null) {
         this.root = x
         yIsLeft = false
      } else if (nodeOrSuccessor.isLeftChild()) {
         parent.leftChild = x
         yIsLeft = true
         nodeOrSuccessor.maxEndFixup()
      } else {
         parent.rightChild = x
         yIsLeft = false
         nodeOrSuccessor.maxEndFixup()
      }

      // the copy step
      if (nodeOrSuccessor != node) {
         node.replace(nodeOrSuccessor)
      }

      if (nodeOrSuccessor.isBlack)
         deleteFixup(x ?: NilProxyNode(parent, yIsLeft))
   }

   /**
    * Ensures that red-black constraints and interval-tree constraints are
    * maintained after deletion.
    */
   private fun deleteFixup(node: NodeOrProxy<V>) {
      var cur = node

      while (cur != root && cur.isBlack) {
         val curParent = cur.parent ?: break
         if (cur.isLeftChild()) {
            var sibling: Node<V>? = curParent.rightChild
            if (sibling != null && !sibling.isBlack) {
               sibling.blacken()
               curParent.redden()
               curParent.leftRotate(ASSIGN_ROOT)
               sibling = curParent.rightChild
            }
            if (sibling?.leftChild?.isBlack != false && sibling?.rightChild?.isBlack != false) {
               sibling?.redden()
               cur = curParent
            } else {
               if (sibling.rightChild?.isBlack != false) {
                  sibling.leftChild?.blacken()
                  sibling.redden()
                  sibling.rightRotate(ASSIGN_ROOT)
                  sibling = curParent.rightChild
               }
               sibling?.isBlack = curParent.isBlack
               curParent.blacken()
               sibling?.rightChild?.blacken()
               curParent.leftRotate(ASSIGN_ROOT)
               // since original node had a parent, there must be a root.
               cur = root!!
            }
         } else {
            var sibling: Node<V>? = curParent.leftChild
            if (sibling != null && !sibling.isBlack) {
               sibling.blacken()
               curParent.redden()
               curParent.rightRotate(ASSIGN_ROOT)
               sibling = curParent.leftChild
            }
            if (sibling?.leftChild?.isBlack != false && sibling?.rightChild?.isBlack != false) {
               sibling?.redden()
               cur = curParent
            } else {
               if (sibling.leftChild?.isBlack != false) {
                  sibling.rightChild?.blacken()
                  sibling.redden()
                  sibling.leftRotate(ASSIGN_ROOT)
                  sibling = curParent.leftChild
               }
               sibling?.isBlack = curParent.isBlack
               curParent.blacken()
               sibling?.leftChild?.blacken()
               curParent.rightRotate(ASSIGN_ROOT)
               // since original node had a parent, there must be a root.
               cur = root!!
            }
         }
      }

      cur.isBlack = true
   }

   fun debugSize(): Int {
      fun debugSize(cur: Node<*>?): Int {
         if (cur == null) return 0
         return 1 + debugSize(cur.leftChild) + debugSize(cur.rightChild)
      }
      return debugSize(root)
   }

   fun hasValidColoring(): Boolean {
      fun colorBalanced(cur: Node<*>?, numBlack: Int): Boolean {
         if (cur == null)
            return numBlack == 0
         val numBlack = if (cur.isBlack) numBlack - 1 else numBlack
         return colorBalanced(cur.leftChild, numBlack) && colorBalanced(cur.rightChild, numBlack)
      }
      fun colorBalanced(): Boolean {
         var numBlack = 0
         var cur: Node<*>? = root
         while (cur != null) {
            if (cur.isBlack)
               numBlack += 1
            cur = cur.leftChild
         }
         if (!colorBalanced(root, numBlack))
            return false
         return true
      }
      fun is234(cur: Node<*>?): Boolean {
         if (cur == null)
            return true
         if (!cur.isBlack && (cur.leftChild?.isBlack == false || cur.rightChild?.isBlack == false))
            return false
         return is234(cur.leftChild) && is234(cur.rightChild)
      }

      return is234(root) && colorBalanced()
   }

   fun hasConsistentMaxEnds(): Boolean {
      fun hasConsistentMaxEnds(cur: Node<*>): Boolean {
         if (cur.maxEnd < cur.interval.last)
            return false
         if (cur.leftChild != null) {
            if (cur.maxEnd < cur.leftChild!!.maxEnd || !hasConsistentMaxEnds(cur.leftChild!!))
               return false
         }
         if (cur.rightChild != null) {
            if (cur.maxEnd < cur.rightChild!!.maxEnd || !hasConsistentMaxEnds(cur.rightChild!!))
               return false
         }
         return true
      }
      return root?.let { hasConsistentMaxEnds(it) } ?: true
   }

   ///////////////////////
   // Tree -- Iterators //
   ///////////////////////

   /**
    * An Iterator which walks along this IntervalTree's Intervals that overlap
    * a given Interval in ascending order.
    *
    * This class just wraps an OverlappingNodeIterator and extracts each Node's
    * Interval.
    */
   private class OverlapperIterator<V>(root: Node<V>, t: IntRange) : Iterator<OverlapResult<V>> {
      private val nodeIter = OverlappingNodeIterator(root, t)

      override fun hasNext(): Boolean {
         return nodeIter.hasNext()
      }

      override fun next(): OverlapResult<V> {
         val node = nodeIter.next()
         return OverlapResult(node.interval, node.ancillary)
      }
   }

   /**
    * An Iterator which walks along this IntervalTree's Nodes that overlap
    * a given Interval in ascending order.
    */
   private class OverlappingNodeIterator<V>(root: Node<V>, t: IntRange) : Iterator<Node<V>> {
      private var next: Node<V>?
      private val interval: IntRange = t

      init {
         next = root.minimumOverlappingNode(interval)
      }

      override fun hasNext(): Boolean {
         return next != null
      }

      override fun next(): Node<V> {
         val retval = next!!
         next = retval.nextOverlappingNode(interval)
         return retval
      }
   }

}
