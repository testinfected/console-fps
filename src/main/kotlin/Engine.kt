import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.*

class World(private val map: CharArray) {
    val width = 16
    val height = 16
    val depth = 16.0

    operator fun get(x: Int, y: Int) = map[y * width + x]

    fun isOutOfBounds(p: Point) =
        p.x < 0 || p.x >= width || p.y < 0 || p.y >= height

    fun isWallAt(step: Point) = this[step.x.toInt(), step.y.toInt()] == WALL

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

    private val frameDelay: Long
        get() {
            val now = nanos()
            val timeUntilPulse = (nextPulse - now) / 1_000_000
            return max(0, timeUntilPulse)
        }

    fun pulse(): Long {
        Thread.sleep(frameDelay)
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

    fun moveTo(pos: Point) {
        this.pos = pos
    }

    fun moveBy(dx: Double, dy: Double) {
        moveTo(pos + v(dx, dy))
    }

    fun moveForward(nanos: Long) {
        moveBy(sin(pov) * speed * nanos, cos(pov) * speed * nanos)
    }

    fun moveBackward(nanos: Long) {
        moveBy(-sin(pov) * speed * nanos, -cos(pov) * speed * nanos)
    }

    fun turnLeft(nanos: Long) {
        turnBy(-rotationSpeed * nanos)
    }

    fun turnTo(pov: Angle) {
        this.pov = pov
    }

    fun turnBy(angle: Angle) {
        turnTo(this.pov + angle)
    }

    fun turnRight(nanos: Long) {
        turnBy(+rotationSpeed * nanos)
    }
}

