import Color.Companion.BLACK
import Color.Companion.WHITE
import java.io.CharArrayWriter
import java.io.PrintWriter
import java.io.Reader
import java.io.Writer
import java.util.*


data class Color(val rgb: Int) {
    val red: Int get() = rgb and 0xFF0000 shr 16
    val green: Int get() = rgb and 0xFF00 shr 8
    val blue: Int get() = rgb and 0xFF

    companion object {
        val BLACK = Color(0x000000)
        val WHITE = Color(0xFFFFFF)

        operator fun invoke(red: Int, green: Int, blue: Int): Color {
            return Color(((red and 0xFF) shl 16) + ((green and 0xFF) shl 8) + (blue and 0xFF))
        }

        fun grey(light: Double): Color = shade(light).let { Color(it, it, it) }

        fun red(light: Double): Color = Color(shade(light), 0, 0)

        fun green(light: Double): Color = Color(0, shade(light), 0)

        private fun shade(light: Double) = (255 * light).toInt()
    }
}

typealias Glyph = Pair<Char, Color>

val Glyph.symbol get() = first
val Glyph.color get() = second


typealias ColorPalette = (Color) -> CharArray

data class TextColor(val fg: Color, val bg: Color)

val WHITE_ON_BLACK = TextColor(WHITE, BLACK)

class Screen(out: Writer, private val size: Size, private val color: TextColor, private val palette: ColorPalette) {
    val width by size::width
    val height by size::height

    private val out = PrintWriter(out)
    private val chars = CharArray(width * height)
    private val colors = IntArray(width * height)
    private val buffer = CharArrayWriter(width * height * 24)

    operator fun set(x: Int, y: Int, value: Char) {
        set(x, y, value to color.fg)
    }

    operator fun set(x: Int, y: Int, glyph: Glyph) {
        offset(y, x).let {
            chars[it] = glyph.symbol
            colors[it] = glyph.color.rgb
        }
    }

    private fun offset(y: Int, x: Int) = y * width + x

    fun render() {
        renderOffscreen()
        swapBuffers()
    }

    private fun swapBuffers() {
        out.print(ANSI.HOME)
        out.print(buffer.toCharArray())
        out.flush()
        buffer.reset()
    }

    private fun renderOffscreen() {
        (0 until width * height).fold(color.bg) { currentColor, offset ->
            val color = Color(colors[offset])
            if (color != currentColor) buffer.write(palette(color))
            buffer.append(chars[offset])
            color
        }
    }
}


enum class Key(val symbol: String) {
    M("m"),
    Q("q"),
    LEFT("\u2190"),
    UP("\u2191"),
    RIGHT("\u2192"),
    DOWN("\u2193"),
    SHIFT("\u21E7"),
    SPACE("\u2420"),
    ESC("ESC");

    companion object {
        fun forStroke(character: Char): Set<Key> {
            when (character) {
                'm' -> return setOf(M)
                'q' -> return setOf(Q)
                'M' -> return setOf(SHIFT, M)
                'Q' -> return setOf(SHIFT, Q)
                ' ' -> return setOf(SPACE)
            }
            return setOf()
        }
    }

    override fun toString() = symbol
}

interface InputDevice {
    fun poll(): Set<Key>
}

class Keyboard(private val reader: Reader): InputDevice {
    private val noInput = charArrayOf()

    // Assume this is enough for now
    private val buffer = CharArray(128)

    override fun poll(): Set<Key> {
        return ANSI.keysIn(ArrayDeque(readInput().toList()))
    }

    private fun readInput(): CharArray {
        if (!reader.ready()) return noInput
        val count = reader.read(buffer)
        if (count <= 0) return noInput
        return buffer.copyOfRange(0, count)
    }
}