import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.PI
import kotlin.math.max

class World(private val map: CharArray) {
    val width = 16
    val height = 16
    val depth = 16.0

    operator fun get(x: Int, y: Int) = map[y * width + x]

    companion object {
        fun load(map: String): World {
            return World(map.filterNot { it == '\n' }.toCharArray())
        }
    }
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

    private fun recordPulse(now: Long) {
        nextPulse = now + frameDuration
        lastPulse = now
    }

    private fun nanos() = System.nanoTime()
}


class Camera(val fov: Angle)


class Player(private val pos: Vector2D, var pov: Angle = 0.0) {
    var x by pos::x
    var y by pos::y

    val speed = 5.0 / SECONDS.toNanos(1)
    // Player makes a full revolution in 3s
    val rotationSpeed = 2.0 * PI / 3.0 / SECONDS.toNanos(1)
}
