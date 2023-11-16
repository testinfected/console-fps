import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.*

// Top left corner is located at (0, 0)
class World(private val map: CharArray) {
    val width = 32
    val height = 32
    val depth = 24.0

    operator fun get(x: Int, y: Int) = map[y * width + x]

    fun isOutOfBounds(p: Point) =
        p.x < 0 || p.x >= width || p.y < 0 || p.y >= height

    fun isWallAt(step: Point) = this[step.x.toInt(), step.y.toInt()] == WALL

    fun locateWall(from: Point, towards: Vector2D): Pair<Polyhedron, Double> {
        val distance = depth(from, direction = towards)
        val hit = from + towards * distance
        return cube(v(hit.x.toInt(), hit.y.toInt())) to distance
    }

    companion object {
        private const val WALL = '#'

        fun load(map: String): World {
            return World(map.filterNot { it == '\n' }.toCharArray())
        }
    }
}

// Simple ray casting to find distance to wall
tailrec fun World.depth(from: Point, direction: Vector2D, step: Double = 0.1, delta: Double = 0.0): Double {
    if (delta >= depth) return depth
    if (isOutOfBounds(from)) return depth // There is no wall at all in that direction
    if (isWallAt(from)) return delta
    return depth(from + direction * step, direction, step, delta + step)
}


class AnimationTimer(private val frameDuration: Long) {
    private val start: Long = nanos()
    private var nextPulse: Long = start
    private var lastPulse: Long = start

    private fun frameDelay(): Long {
        val now = nanos()
        val timeUntilPulse = (nextPulse - now) / 1_000_000
        return max(0, timeUntilPulse)
    }

    fun pulse(): Long {
        val delay = frameDelay()
        if (delay > 0) Thread.sleep(delay)
        val now = nanos()
        val elapsedTime = now - lastPulse
        recordPulse(now)
        return elapsedTime
    }

    fun frameRate(pulse: Long) = SECONDS.toNanos(1).toDouble() / pulse

    private fun recordPulse(now: Long) {
        lastPulse = now
        nextPulse = now + frameDuration
    }

    private fun nanos() = System.nanoTime()
}


class Camera(val fov: Angle)


class Player(private var pos: Point, private var pov: Angle = 0.0) {
    val position get() = pos
    val pointOfView get() = pov

    private val speed = 5.0 / SECONDS.toNanos(1)

    // Player makes a full revolution in 3s
    private val rotationSpeed = 2.0 * PI / 3.0 / SECONDS.toNanos(1)

    private fun advance(nanos: Long): Vector2D = v(sin(pov) * speed * nanos, -cos(pov) * speed * nanos)

    fun moveForward(nanos: Long) {
        pos += advance(nanos)
    }

    fun moveBackward(nanos: Long) {
        pos -= advance(nanos)
    }

    fun turnRight(nanos: Long) {
        pov += rotationSpeed * nanos
    }

    fun turnLeft(nanos: Long) {
        pov -= rotationSpeed * nanos
    }
}

fun Player.hasHitWallIn(world: World) = world.isWallAt(position)

