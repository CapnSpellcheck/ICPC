package util

import java.io.IOException
import java.io.OutputStream

class StringOutputStream : OutputStream() {
   private val buf = StringBuffer()
   @Throws(IOException::class)
   override fun write(b: ByteArray) {
      buf.append(String(b))
   }

   @Throws(IOException::class)
   override fun write(b: ByteArray, off: Int, len: Int) {
      buf.append(String(b, off, len))
   }

   @Throws(IOException::class)
   override fun write(b: Int) {
      val bytes = byteArrayOf(b.toByte())
      buf.append(String(bytes))
   }

   override fun toString(): String {
      return buf.toString()
   }
}
