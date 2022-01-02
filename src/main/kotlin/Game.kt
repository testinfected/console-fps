import java.nio.charset.StandardCharsets
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.exitProcess


// World has fixed size 16x16 size
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
""".trimIndent()

private val terminal = Terminal.connect(StandardCharsets.UTF_8)

object Game {
    private val screen = terminal.screen
    private val keyboard = terminal.keyboard
    private val world = World.load(map)
    private val camera = Camera(fov = Math.PI / 4)
    private val player = Player(v(8.0, 8.0))

    private val keystrokes = mutableListOf<Key>()

    fun launch() {
        init()

        while (true) {
            processInput()
            if (Key.Q in keystrokes) quit()
            updateFrame()
        }
    }

    private fun processInput() {
        keystrokes += keyboard.readKeys()
    }

    private fun quit() {
        terminal.backToInteractiveMode()
        exitProcess(0)
    }

    private fun init() {
        terminal.activateSingleCharacterMode()
    }

    private fun updateFrame() {
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

        keystrokes.map { it.symbol }.flatMap { it.toList() }.forEachIndexed { i, char ->
            screen[i % screen.width, 0] = char
        }

        screen.renderFrame()
    }
}


fun main() {
    Game.launch()
}