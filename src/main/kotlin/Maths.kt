typealias Angle = Double

typealias Point = Vector2D

data class Vector2D(val x: Double, val y: Double) {
    operator fun plus(other: Vector2D): Vector2D {
        return Vector2D(x + other.x, y + other.y)
    }

    operator fun times(multiplier: Double): Vector2D {
        return Vector2D(x * multiplier, y + multiplier)
    }
}

fun v(x: Double, y: Double) = Vector2D(x, y)


