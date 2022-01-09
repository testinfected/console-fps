import kotlin.math.acos
import kotlin.math.sqrt

typealias Angle = Double

typealias Point = Vector2D

typealias Edge = Pair<Point, Point>

fun Edge.toVector(): Vector2D = second - first

operator fun Edge.not(): Vector2D = !toVector()

data class Vector2D(val x: Double, val y: Double) {
    val magnitude: Double
        get() = sqrt(x * x + y * y)

    operator fun plus(other: Vector2D): Vector2D {
        return Vector2D(x + other.x, y + other.y)
    }

    operator fun minus(other: Vector2D): Vector2D {
        return plus(-other)
    }

    operator fun times(multiplier: Double): Vector2D {
        return Vector2D(x * multiplier, y * multiplier)
    }

    operator fun unaryMinus(): Vector2D {
        return v(-x, -y)
    }

    infix fun dot(other: Vector2D): Double {
        return x * other.x + y * other.y
    }

    operator fun not(): Vector2D {
        return v(y, -x)
    }
}

infix fun Vector2D.angle(other: Vector2D) = acos(dot(other) / (magnitude * other.magnitude) )

fun v(x: Double, y: Double) = Vector2D(x, y)

fun v(x: Int, y: Int) = v(x.toDouble(), y.toDouble())
