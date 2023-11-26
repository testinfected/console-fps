import java.io.*
import java.lang.ProcessBuilder.Redirect
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets.UTF_8


/**
 * See https://en.wikipedia.org/wiki/ANSI_escape_code
 */
object ANSI {
    // Starts most of the useful sequences
    val ESC = 0x1B.toChar()
    val CSI = "$ESC["

    // CSI sequences
    // Clear screen and delete lines in scroll back buffer
    val CLS = "${CSI}3J"

    // Move cursor back to (1,1)
    val HOME = "${CSI}H"
    val UP = "${CSI}A"
    val DOWN = "${CSI}B"
    val RIGHT = "${CSI}C"
    val LEFT = "${CSI}D"
    val HIDE_CURSOR = "${CSI}?25l"

    fun fg(red: Int, green: Int, blue: Int): String {
        return "${CSI}38;2;$red;$green;${blue}m"
    }

    fun bg(red: Int, green: Int, blue: Int): String {
        return "${CSI}48;2;$red;$green;${blue}m"
    }

    fun keysIn(input: Queue<Char>): Set<Key> {
        return when (val key = input.poll() ?: return setOf()) {
            ESC -> {
                when ("${ESC}${input.poll() ?: return setOf(Key.ESC)}") {
                    CSI -> {
                        val control = when ("$CSI${input.poll()}") {
                            UP -> setOf(Key.UP)
                            DOWN -> setOf(Key.DOWN)
                            RIGHT -> setOf(Key.RIGHT)
                            LEFT -> setOf(Key.LEFT)
                            // We don't support any other control sequence
                            else -> setOf()
                        }
                        return control + keysIn(input)
                    }
                    // We don't support any other escape sequence
                    else -> setOf(Key.ESC) + keysIn(input)
                }
            }

            else -> Key.forStroke(key) + keysIn(input)
        }
    }
}


enum class TerminalCharacteristic(private val flag: String) {
    RAW("raw"), NO_ECHO("-echo"), SANE("sane");

    fun set() {
        val stty = ProcessBuilder("/bin/sh", "-c", "stty $flag --file /dev/tty")
            .redirectOutput(Redirect.INHERIT)
            .redirectError(Redirect.INHERIT)
            .start()

        if (!stty.waitFor(1, TimeUnit.SECONDS)) {
            stty.destroy()
            throw IOException("Timed out setting tty options")
        }

        if (stty.exitValue() != 0) {
            throw IOException("Failed to set tty options, exit code is ${stty.exitValue()}")
        }
    }
}

val TRUE_COLOR: ColorPalette = { ANSI.fg(it.red, it.green, it.blue).toCharArray() }

data class Size(val width: Int, val height: Int)

class Terminal(size: Size, color: TextColor, palette: ColorPalette, charset: Charset) {
    val screen = Screen(OutputStreamWriter(System.out, charset), size, color, palette)
    val keyboard = Keyboard(InputStreamReader(System.`in`, charset))

    fun raw(): Terminal = apply {
        TerminalCharacteristic.RAW.set()
        TerminalCharacteristic.NO_ECHO.set()
    }

    fun hideCursor() {
        print(ANSI.HIDE_CURSOR)
    }

    fun sane() = apply {
        TerminalCharacteristic.SANE.set()
    }

    companion object {
        fun connect(size: Size, color: TextColor, palette: ColorPalette, charset: Charset): Terminal {
            if (System.console() == null) throw IOException("Please launch from a TTY")
            return Terminal(size, color, palette, charset)
        }
    }
}
