import java.io.Console
import java.io.IOException
import java.io.PrintWriter
import java.io.Reader
import java.lang.Math.PI
import java.lang.ProcessBuilder.*
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.exitProcess

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


class Screen(private val writer: PrintWriter) {
    val width = 120
    val height = 40

    private val buffer = CharArray(width * height)

    operator fun set(x: Int, y: Int, value: Char) {
        buffer[y * width + x] = value
    }

    fun drawFrame() {
        clear()
        render()
    }

    fun render() {
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


class Keyboard(private val reader: Reader) {
    fun key(): Int? = reader.takeIf { it.ready() }?.read()
}


class World {
    val width = 16
    val height = 16
    val depth = 16.0

    val map = """
        ################
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        #..............#
        ################
    """.trimIndent().filterNot { it == '\n' }

    operator fun get(x: Int, y: Int) = map[y * width + x]
}


class Camera {
    val fov: Double = PI / 4
}


data class Vector2D(val x: Double, val y: Double)

fun v(x: Double, y: Double) = Vector2D(x, y)


class Player {
    val pos = v(8.0, 8.0)
    val pov = 0.0

    val x by pos::x
    val y by pos::y
}

class Terminal(console: Console) {

    val screen = Screen(console.writer())
    val keyboard = Keyboard(console.reader())

    fun activateSingleCharacterMode() {
        stty("cbreak")
    }

    fun backToInteractiveMode() {
        stty("sane")
    }

    private fun stty(vararg options: String) {
        val stty = ProcessBuilder("/bin/sh", "-c", "stty ${options.joinToString(separator = " ")} < /dev/tty")
            .redirectOutput(Redirect.INHERIT)
            .redirectError(Redirect.INHERIT)
            .start()
        if (!stty.waitFor(1, TimeUnit.SECONDS)) {
            stty.destroy()
            throw IOException("Timed out setting tty options")
        }
        if (stty.exitValue() != 0) {
            throw RuntimeException("Failed to set tty options with code ${stty.exitValue()}")
        }
    }

    companion object {
        fun connect() = Terminal(System.console() ?: throw IOException("Not in a tty"))
    }
}

fun main() {
    val terminal = Terminal.connect()
    terminal.activateSingleCharacterMode()

    val screen = terminal.screen
    val keyboard = terminal.keyboard
    val world = World()
    val camera = Camera()
    val player = Player()

    while (true) {
        if (keyboard.key() != null) {
            terminal.backToInteractiveMode()
            exitProcess(0)
        }

        (0 until screen.width).forEach { x ->
            // Simple ray casting to find distance to wall
            val rayAngle = player.pov - camera.fov / 2 + camera.fov * x / screen.width
            val rayStep = 0.1

            // Unit vector looking direction
            val eyeDirection = v(sin(rayAngle), cos(rayAngle))

            var distanceToWall = 0.0
            var hitWall = false

            while (!hitWall && distanceToWall < world.depth) {
                distanceToWall += rayStep
                val step = v(player.x + eyeDirection.x * distanceToWall, player.y + eyeDirection.y * distanceToWall)

                if (step.x < 0 || step.x >= screen.width || step.y < 0 || step.y >= screen.height) {
                    // We're out of bounds, which means there is no wall at all in that direction
                    hitWall = true
                    distanceToWall = world.depth
                } else if (world[step.x.toInt(), step.y.toInt()] == '#') {
                    hitWall = true
                }
            }

            // Distances to ceiling and floor, top is 0
            val ceilingHeight = screen.height / 2 - screen.height / distanceToWall
            val floorHeight = screen.height / 2 + screen.height / distanceToWall

            (0 until screen.height).forEach { y ->
                if (y < ceilingHeight) {
                    // Ceiling
                    screen[x, y] = ' '
                } else if (y < floorHeight) {
                    // Wall
                    screen[x, y] = '#'
                } else {
                    // Floor
                    screen[x, y] = ' '
                }
            }
        }

        screen.drawFrame()
    }
}