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


class Camera(val fov: Angle)


class Player(private val pos: Vector2D, val pov: Angle = 0.0) {
    val x by pos::x
    val y by pos::y
}
