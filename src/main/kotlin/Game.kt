import WallShades.DARK
import WallShades.FULL
import WallShades.MEDIUM
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.math.cos
import kotlin.math.sin


// World has fixed size 32x32 size
val map = """
        ################################
        #..............##..............#
        #..............##..............#
        #......##########......#########
        #......##..............##......#
        #......##..............##......#
        #..............................#
        #..#...........................#
        #..#...........##..............#
        #..#...........##..............#
        #..#....##..#####.......###..###
        #.......#......##.......#......#
        #.......#......##.......#......#
        #.......####..#############..###
        #..............##..............#
        #..............##..............#
        #..............##..............#
        #..............##..............#
        #..............##..............#
        #......##########......##......#
        #......##..............##......#
        #......##..............##......#
        #..............................#
        #..............................#
        #..............##..............#
        #..............##..............#
        #.......##..#####.......##..####
        #.......#......##.......#......#
        #.......#......##.......#......#
        #.......#####.#############...##
        #..............................#
        ################################
""".trimIndent()


object WallShades {
    const val FULL = '\u2588'
    const val DARK = '\u2593'
    const val MEDIUM = '\u2592'
    const val LIGHT = '\u2591'
    const val NONE = ' '
}

private fun wallShade(distance: Double) = Color.grey(1.0 - distance)

private fun floorColor(distance: Double): Color = Color.green(1.0 - distance)

private fun ceilingColor(distance: Double) = Color.red(distance)


private const val TINY_ANGLE = 0.001

class Game(private val screen: Screen, private val keyboard: Keyboard) {
    private val world = World.load(map)

    // Set frame duration to account for terminal key repeat delay in order to get a smooth animation
    private val timer = AnimationTimer(frameDuration = MILLISECONDS.toNanos(15))

    // Camera has field of view of 90º (which is rather small)
    private val camera = Camera(fov = Math.PI / 4)

    // Player starts at the middle of the map
    private val player = Player(v(5.0, 8.0))

    private var stopped = false
    private var keys = setOf<Key>()
    private var showStats = false
    private var showMap = false

    fun start() {
        while (!stopped) {
            val elapsedTime = timer.pulse()
            processInput()
            updateFrame(elapsedTime)
            if (showStats) displayStats(timer.frameRate(elapsedTime))
            if (showMap) displayMap()
            screen.render()
        }
    }

    private fun displayStats(frameRate: Double) {
        val info = "X = %3.2f Y = %3.2f, POV = %3.2f, FPS = %4.2f, KEYS = %s".format(
            player.position.x,
            player.position.y,
            player.pointOfView,
            frameRate,
            keys.joinToString { it.symbol }
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
        keys = keyboard.poll()
        if (Key.Q in keys) quit()
        if (Key.SPACE in keys) showStats = !showStats
        if (Key.M in keys) showMap = !showMap
    }

    private fun quit() {
        stopped = true
    }

    private fun updateFrame(elapsedTime: Long) {
        handleInput(elapsedTime)
        drawMap()
    }

    private fun drawMap() {
        (0 until screen.width).forEach { x ->
            val eye = lookingDirection(x)
            val (wall, distance) = world.locateWall(from = player.position, towards = eye)
            val visibleEdges = wall.visibleEdges(fromPointOfView = eye)

            val wallShade = wallShade((distance / world.depth))
            val wallGlyph = when {
                visibleEdges.any { eye.isParallelTo(it - player.position, margin = TINY_ANGLE) } -> MEDIUM to wallShade
                else -> FULL to wallShade
            }

            val ceilingHeight = ceilingHeight(distance = distance)
            val floorHeight = floorHeight(distance = distance)

            (0 until screen.height).forEach { y ->
                when {
                    y <= ceilingHeight -> screen.set(x, y, DARK to ceilingColor(distanceToFloor(y)))
                    y <= floorHeight -> screen.set(x, y, wallGlyph)
                    else -> screen.set(x, y, DARK to floorColor(distanceToFloor(y)))
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

    private fun distanceToFloor(y: Int) = 1.0 - (y - screen.height / 2.0) / (screen.height / 2.0)

    private fun handleInput(elapsedTime: Long) {
        if (Key.LEFT in keys) {
            player.turnLeft(elapsedTime)
        }
        if (Key.RIGHT in keys) {
            player.turnRight(elapsedTime)
        }
        if (Key.UP in keys) {
            player.moveForward(elapsedTime)
            if (player.hasHitWallIn(world)) player.moveBackward(elapsedTime)
        }
        if (Key.DOWN in keys) {
            player.moveBackward(elapsedTime)
            if (player.hasHitWallIn(world)) player.moveForward(elapsedTime)
        }
    }

    companion object {
        fun launchFrom(terminal: Terminal) {
            terminal.raw()
            terminal.hideCursor()
            val game = Game(terminal.screen, terminal.keyboard)
            try {
                game.start()
            } finally {
                terminal.sane()
            }
        }
    }
}

fun main() {
    val terminal = Terminal.connect(Size(120, 40), WHITE_ON_BLACK, TRUE_COLOR, StandardCharsets.UTF_8)
    Game.launchFrom(terminal = terminal)
}
