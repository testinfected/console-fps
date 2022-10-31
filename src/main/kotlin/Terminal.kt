import java.io.*
import java.lang.ProcessBuilder.Redirect
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit


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

    fun keysIn(input: Queue<Char>): List<Key> {
        return when (val key = input.poll() ?: return listOf()) {
            ESC -> {
                when ("${ESC}${input.poll() ?: return listOf(Key.ESC)}") {
                    CSI -> {
                        val control = when ("${ANSI.CSI}${input.poll()}") {
                            UP -> listOf(Key.UP)
                            DOWN -> listOf(Key.DOWN)
                            RIGHT -> listOf(Key.RIGHT)
                            LEFT -> listOf(Key.LEFT)
                            // We don't support any other control sequence
                            else -> listOf()
                        }
                        return control + keysIn(input)
                    }
                    // We don't support any other escape sequence
                    else -> listOf(Key.ESC) + keysIn(input)
                }
            }
            else -> Key.forStroke(key) + keysIn(input)
        }
    }
}


object Stty {
    fun configure(vararg options: String) {
        val stty = ProcessBuilder("/bin/sh", "-c", "stty ${options.joinToString(separator = " ")} < /dev/tty")
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


val TRUE_COLOR: (Color) -> CharArray = { color -> ANSI.fg(color.red, color.green, color.blue).toCharArray() }


class Terminal(charset: Charset) {
    val screen = Screen(OutputStreamWriter(System.out, charset), palette = TRUE_COLOR)
    val keyboard = Keyboard(InputStreamReader(System.`in`, charset))

    fun activateSingleCharacterMode() {
        Stty.configure("raw")
        Stty.configure("-echo")
    }

    fun backToInteractiveMode() {
        Stty.configure("sane")
    }

    companion object {
        fun connect(charset: Charset = StandardCharsets.UTF_8): Terminal {
            if (System.console() == null) throw IOException("Please launch from a tty")
            return Terminal(charset)
        }
    }
}
