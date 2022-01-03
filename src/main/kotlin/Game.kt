import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
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

    // Set frame duration to account for terminal key repeat delay in order to get a smooth animation
    private val timer = AnimationTimer(frameDuration = MILLISECONDS.toNanos(15))

    // Camera has field of view of 90º (which is rather small)
    private val camera = Camera(fov = Math.PI / 4)

    // Player starts at the middle of the map
    private val player = Player(v(8.0, 8.0))

    private val keystrokes = mutableListOf<Key>()
    private var enableStats = false

    fun launch() {
        init()

        while (true) {
            val elapsedTime = timer.pulse()
            processInput()
            updateFrame(elapsedTime)
            displayStats(elapsedTime)
            screen.renderFrame()
        }
    }

    private fun displayStats(elapsedTime: Long) {
        if (!enableStats) return
        val info = "X = %3.2f Y = %3.2f, POV = %3.2f, FPS = %4.2f".format(
            player.x,
            player.y,
            player.pov,
            SECONDS.toNanos(1).toDouble() / elapsedTime
        )
        info.toCharArray().forEachIndexed { i, char ->
            screen[i, 0] = char
        }
    }

    private fun processInput() {
        keystrokes.clear()
        keystrokes += keyboard.readKeys()
        if (Key.Q in keystrokes) quit()
        if (Key.SPACE in keystrokes) enableStats = !enableStats
    }

    private fun quit() {
        terminal.backToInteractiveMode()
        exitProcess(0)
    }

    private fun init() {
        terminal.activateSingleCharacterMode()
    }

    private fun updateFrame(elapsedTime: Long) {
        if (Key.LEFT in keystrokes) {
            player.pov -= player.rotationSpeed * elapsedTime
        }
        if (Key.RIGHT in keystrokes) {
            player.pov += player.rotationSpeed * elapsedTime
        }
        if (Key.UP in keystrokes) {
            player.x += sin(player.pov) * player.speed * elapsedTime
            player.y += cos(player.pov) * player.speed * elapsedTime
        }
        if (Key.DOWN in keystrokes) {
            player.x -= sin(player.pov) * player.speed * elapsedTime
            player.y -= cos(player.pov) * player.speed * elapsedTime
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

            val wallShade =
                // full shade
                if (distanceToWall < world.depth / 4.0) '\u2588'
                // dark shade
                else if (distanceToWall < world.depth / 3.0) '\u2593'
                // medium shade
                else if (distanceToWall < world.depth / 2.0) '\u2592'
                // light shade
                else if (distanceToWall < world.depth) '\u2591'
                // no shade at all
                else ' '

            (0 until screen.height).forEach { y ->
                if (y < ceilingHeight) {
                    // Ceiling
                    screen[x, y] = ' '
                } else if (y < floorHeight) {
                    // Wall
                    screen[x, y] = wallShade
                } else {
                    // Floor
                    screen[x, y] = ' '
                }
            }
        }
    }
}


fun main() {
    Game.launch()
}