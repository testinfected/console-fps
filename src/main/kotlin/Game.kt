import WallShades.DARK
import WallShades.FULL
import WallShades.LIGHT
import WallShades.MEDIUM
import WallShades.NONE
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.exitProcess


// World has fixed size 16x16 size
val map = """
        ################
        #..............#
        #..............#
        #......#########
        #......##......#
        #......##......#
        #..............#
        #..#...........#
        #..#...........#
        #..#...........#
        #..#....##..####
        #.......#......#
        #.......#......#
        #.......#####.##
        #..............#
        ################
""".trimIndent()


object WallShades {
    const val FULL = '\u2588'
    const val DARK = '\u2593'
    const val MEDIUM = '\u2592'
    const val LIGHT = '\u2591'
    const val NONE = ' '
}

private fun World.wallShade(distance: Double): Char {
    return if (distance < depth / 4.0) FULL
    else if (distance < depth / 3.0) DARK
    else if (distance < depth / 2.0) MEDIUM
    else if (distance < depth) LIGHT
    else NONE
}

private fun World.floorShade(distanceToFloor: Double): Char {
    return if (distanceToFloor < 0.25) '#'
    else if (distanceToFloor < 0.5) 'x'
    else if (distanceToFloor < 0.75) '-'
    else if (distanceToFloor < 0.9) '.'
    else ' '
}


private val terminal = Terminal.connect(StandardCharsets.UTF_8)


private const val TINY_ANGLE = 0.004

object Game {
    private val screen = terminal.screen
    private val keyboard = terminal.keyboard
    private val world = World.load(map)

    // Set frame duration to account for terminal key repeat delay in order to get a smooth animation
    private val timer = AnimationTimer(frameDuration = MILLISECONDS.toNanos(15))

    // Camera has field of view of 90º (which is rather small)
    private val camera = Camera(fov = Math.PI / 4)

    // Player starts at the middle of the map
    private val player = Player(v(5.0, 8.0))

    private val keystrokes = mutableListOf<Key>()
    private var showStats = false
    private var showMap = false

    fun launch() {
        init()

        while (true) {
            val elapsedTime = timer.pulse()
            processInput()
            updateFrame(elapsedTime)
            if (showStats) displayStats(timer.frameRate(elapsedTime))
            if (showMap) displayMap()
            screen.renderFrame()
        }
    }

    private fun displayStats(frameRate: Double) {
        val info = "X = %3.2f Y = %3.2f, POV = %3.2f, FPS = %4.2f, KEYS = %s".format(
            player.position.x,
            player.position.y,
            player.pointOfView,
            frameRate,
            keystrokes.joinToString { it.symbol }
        )
        info.toCharArray().forEachIndexed { i, char ->
            screen[i, 0] = char
        }
    }

    private fun displayMap() {
        (0 until world.width).forEach { x ->
            (0 until world.height).forEach { y ->
                screen[x, y + 1] = world[x, y]
            }
        }
        screen[player.position.x.toInt(), player.position.y.toInt() + 1] = 'P'
    }


    private fun processInput() {
        keystrokes.clear()
        keystrokes += keyboard.readKeys()
        if (Key.Q in keystrokes) quit()
        if (Key.SPACE in keystrokes) showStats = !showStats
        if (Key.M in keystrokes) showMap = !showMap
    }

    private fun init() {
        terminal.activateSingleCharacterMode()
    }

    private fun quit() {
        terminal.backToInteractiveMode()
        exitProcess(0)
    }

    private fun updateFrame(elapsedTime: Long) {
        handleInput(elapsedTime)
        drawMap()
    }

    private fun drawMap() {
        (0 until screen.width).forEach { x ->
            val eye = lookingDirection(x)
            val wall = world.locateWall(from = player.position, towards = eye)
            val visibleEdges = wall.visibleEdges(fromPointOfView = eye)

            val wallShade = when {
                visibleEdges.any { eye.isParallelTo(it - player.position, margin = TINY_ANGLE) } -> NONE
                else -> world.wallShade(distance = wall.distance)
            }

            val ceilingHeight = ceilingHeight(distance = wall.distance)
            val floorHeight = floorHeight(distance = wall.distance)

            (0 until screen.height).forEach { y ->
                when {
                    y <= ceilingHeight -> screen[x, y] = ' '
                    y <= floorHeight -> screen[x, y] = wallShade
                    else -> screen[x, y] = world.floorShade(distanceToFloor(y))
                }
            }
        }
    }

    private fun lookingDirection(x: Int): Vector2D {
        val rayAngle = player.pointOfView - camera.fov / 2 + camera.fov * x / screen.width
        // Unit vector looking direction
        return v(sin(rayAngle), -cos(rayAngle))
    }

    private fun ceilingHeight(distance: Double) = screen.height / 2.0 - screen.height / distance

    private fun floorHeight(distance: Double) = screen.height - ceilingHeight(distance)

    private fun distanceToFloor(y: Int) = 1.0 - (y - screen.height / 2.0) / screen.height

    private fun handleInput(elapsedTime: Long) {
        if (Key.LEFT in keystrokes) {
            player.turnLeft(elapsedTime)
        }
        if (Key.RIGHT in keystrokes) {
            player.turnRight(elapsedTime)
        }
        if (Key.UP in keystrokes) {
            player.moveForward(elapsedTime)
            if (world.isWallAt(player.position)) player.moveBackward(elapsedTime)
        }
        if (Key.DOWN in keystrokes) {
            player.moveBackward(elapsedTime)
            if (world.isWallAt(player.position)) player.moveForward(elapsedTime)
        }
    }
}


fun main() {
    Game.launch()
}
