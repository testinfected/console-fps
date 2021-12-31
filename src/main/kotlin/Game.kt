import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

fun main() {
    val screen = Screen()
    val world = World()
    val camera = Camera()
    val player = Player()

    while (true) {
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

        screen.redraw()
    }
}