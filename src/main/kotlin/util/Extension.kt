package util

public inline fun <T> Iterable<T>.allIndexed(predicate: (Int, T) -> Boolean): Boolean {
   if (this is Collection && isEmpty()) return true
   this.forEachIndexed { index, t ->  if (!predicate(index, t)) return false }
   return true
}