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

    fun isWallAt(pos: Point) = this[pos.x.toInt(), pos.y.toInt()] == WALL

    fun locateWall(from: Point, towards: Direction): Pair<Polyhedron, Distance> {
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

fun World.depth(position: Point, direction: Direction): Distance {
    return RayCasting(depth, outOfBounds = ::isOutOfBounds).distanceFrom(position, ::isWallAt, direction)
}

// Simple ray casting to find distance to object
class RayCasting(private val depth: Distance, private val step: Distance = 0.1, private val outOfBounds: (Point) -> Boolean) {
    fun distanceFrom(pos: Point, toPos: (Point) -> Boolean, inDirection: Direction): Distance {
        return distance(pos, inDirection, toPos, 0.0)
    }

    private tailrec fun distance(from: Point, direction: Direction, objectAt: (Point) -> Boolean, delta: Distance): Distance {
        if (delta >= depth) return depth
        if (outOfBounds(from)) return depth // There is nothing at all in that direction
        if (objectAt(from)) return delta
        return distance(from + direction * step, direction, objectAt, delta + step)
    }
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

