package be.volley

import java.time.Instant

data class Game(
    val date: Instant,
    val team1: String?,
    val team2: String?,
    val address: String?,
    val hall: String?
)
