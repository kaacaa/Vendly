package com.katarina.vendly.domain.gamification

enum class Badge(val label: String, val minPoints: Long) {
    NEWBIE   ("Rookie Scout",             0),
    BRONZE   ("Street Spotter",          10),
    SILVER   ("Neighborhood Explorer",   25),
    GOLD     ("City Explorer",          50),
    PLATINUM ("Curator",                100),
    DIAMOND  ("Vending Legend",           200);

    companion object {
        fun fromPoints(points: Long): Badge =
            values().last { points >= it.minPoints }

        fun nextTarget(points: Long): Long? =
            values().firstOrNull { points < it.minPoints }?.minPoints
    }
}