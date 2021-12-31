class Screen {
    val width = 120
    val height = 40

    private val console = System.console()
    private val writer get() = console.writer()

    private val buffer = CharArray(width * height)

    operator fun set(x: Int, y: Int, value: Char) {
        buffer[y * width + x] = value
    }

    fun redraw() {
        clear()
        draw()
    }

    fun draw() {
        writer.print(buffer)
        writer.flush()
    }

    fun clear() {
        writer.print(ANSI.HIDE_CURSOR)
        writer.print(ANSI.CLS)
        writer.print(ANSI.HOME)
        writer.flush()
    }
}


/** See https://en.wikipedia.org/wiki/ANSI_escape_code */
object ANSI {
    // Starts most of the useful sequences
    const val ESC = "\u001b"

    // CSI sequences

    // Clear screen and delete lines in scroll back buffer
    const val CLS = "$ESC[3J"
    const val HOME = "$ESC[H"
    const val HIDE_CURSOR = "$ESC[?25l"
}

fun main() {
    val screen = Screen()

    while (true) {
        (0 until screen.width ).forEach { x ->
            (0.until(screen.height)).forEach { y ->
                screen[x, y] = "${x % 10}"[0]
            }
        }

        screen.redraw()
    }
}