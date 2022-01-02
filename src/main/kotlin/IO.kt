import java.io.PrintWriter
import java.io.Reader
import java.io.Writer
import java.util.ArrayDeque

class Screen(output: Writer) {
    val width = 120
    val height = 40

    private val output = PrintWriter(output)
    private val buffer = CharArray(width * height)

    operator fun set(x: Int, y: Int, value: Char) {
        buffer[y * width + x] = value
    }

    fun render() {
        clear()
        swapBuffers()
    }

    private fun swapBuffers() {
        output.print(buffer)
        output.flush()
    }

    private fun clear() {
        output.print(ANSI.HIDE_CURSOR)
        output.print(ANSI.CLS)
        output.print(ANSI.HOME)
        output.flush()
    }
}


enum class Key(val symbol: String) {
    Q("q"),
    LEFT("\u2190"),
    UP("\u2191"),
    RIGHT("\u2192"),
    DOWN("\u2193"),
    SHIFT("\u21E7"),
    SPACE("\u2420"),
    ESC("ESC");

    companion object {
        fun forStroke(character: Char): List<Key> {
            when (character) {
                'q' -> return listOf(Q)
                'Q' -> return listOf(SHIFT, Q)
                ' ' -> return listOf(SPACE)
            }
            return listOf()
        }
    }

    override fun toString() = symbol
}


class Keyboard(private val reader: Reader) {
    private val noInput = charArrayOf()

    // Assume this is enough for now
    private val buffer = CharArray(128)

    fun readKeys(): List<Key> {
        return ANSI.keysIn(ArrayDeque(readInput().toList()))
    }

    private fun readInput(): CharArray {
        if (!reader.ready()) return noInput
        val count = reader.read(buffer)
        if (count <= 0) return noInput
        return buffer.copyOfRange(0, count)
    }
}