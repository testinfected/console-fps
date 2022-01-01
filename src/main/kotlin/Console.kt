import java.io.*
import java.lang.ProcessBuilder.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * See https://en.wikipedia.org/wiki/ANSI_escape_code
 */
object ANSI {
    // Starts most of the useful sequences
    const val ESC = "\u001b"

    // CSI sequences

    // Clear screen and delete lines in scroll back buffer
    const val CLS = "$ESC[3J"
    const val HOME = "$ESC[H"
    const val HIDE_CURSOR = "$ESC[?25l"
}


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


class Keyboard(private val reader: Reader) {
    private val noInput = charArrayOf()
    private val buffer = CharArray(128)

    fun readInput(): CharArray {
        if (!reader.ready()) return noInput
        val count = reader.read(buffer)
        if (count <= 0) return noInput
        return buffer.copyOfRange(0, count)
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

class Terminal(charset: Charset) {
    val screen = Screen(OutputStreamWriter(System.out, charset))
    val keyboard = Keyboard(InputStreamReader(System.`in`, charset))

    fun activateSingleCharacterMode() {
        Stty.configure("cbreak")
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
