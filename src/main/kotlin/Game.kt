import java.lang.Math.PI
import java.nio.charset.StandardCharsets
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.exitProcess


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


class Player {
    val pos = v(8.0, 8.0)
    val pov = 0.0

    val x by pos::x
    val y by pos::y
}


object Game {
    private val terminal = Terminal.connect(StandardCharsets.UTF_8)

    private val screen = terminal.screen
    private val keyboard = terminal.keyboard
    private val world = World()
    private val camera = Camera()
    private val player = Player()

    fun launch() {
        init()

        while (true) {
            renderFrame()
        }
    }

    private fun init() {
        terminal.activateSingleCharacterMode()
    }

    private fun renderFrame() {
        val keys = keyboard.readInput()
        if (keys.isNotEmpty()) {
            println("input : ${keys.joinToString()}")
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
                    screen[x, y] = '\u2593'
                } else {
                    // Floor
                    screen[x, y] = ' '
                }
            }
        }

        screen.render()
    }
}


fun main() {
    Game.launch()
}