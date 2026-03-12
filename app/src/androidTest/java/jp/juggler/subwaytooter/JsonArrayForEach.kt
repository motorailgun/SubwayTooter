package jp.juggler.subwaytooter

import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.juggler.util.data.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class JsonArrayForEach {

    @Test
    @Throws(Exception::class)
    fun test() {
        val array = buildJsonArray {
            add("a")
            add("b")
            add(null)
            add(null)
        }

        var count = 0

        array.forEach {
            println("JsonArray.forEach $it")
            ++count
        }

        array.forEachIndexed { i, v ->
            println("JsonArray.forEachIndexed $i $v")
            ++count
        }

        for (i in array.indices.reversed()) {
            val it = array[i]
            println("JsonArray.downForEach $it")
            ++count
        }

        for (i in array.indices.reversed()) {
            val v = array[i]
            println("JsonArray.downForEachIndexed $i $v")
            ++count
        }

        for (o in array.iterator()) {
            println("JsonArray.iterator $o")
            ++count
        }
        for (o in array.asReversed().iterator()) {
            println("JsonArray.reverseIterator $o")
            ++count
        }

        assertEquals(count, 24)
    }
}
