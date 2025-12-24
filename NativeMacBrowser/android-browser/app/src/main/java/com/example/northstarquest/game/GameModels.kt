package com.example.northstarquest.game

import androidx.annotation.ColorInt

enum class EntityType {
    PLATFORM,
    COIN,
    INFO,
    ENEMY,
    FLAG
}

data class Entity(
    val id: String,
    val type: EntityType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val label: String? = null,
    val contentId: String? = null,
    val patrolStart: Float? = null,
    val patrolEnd: Float? = null,
    val direction: Int = 1
)

data class LevelContent(
    val id: String,
    val title: String,
    val shortText: String,
    val fullText: String
)

data class LevelTheme(
    @ColorInt val background: Int,
    @ColorInt val platformColor: Int,
    @ColorInt val accentColor: Int
)

data class LevelConfig(
    val id: Int,
    val name: String,
    val description: String,
    val theme: LevelTheme,
    val entities: List<Entity>,
    val content: Map<String, LevelContent>
)

data class PlayerState(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val velocityX: Float,
    val velocityY: Float,
    val onGround: Boolean
)

data class GameHudState(
    val score: Int,
    val levelName: String,
    val storyMessage: String?
)

data class GameConfig(
    val gravity: Float = 0.5f,
    val friction: Float = 0.85f,
    val moveSpeed: Float = 0.35f,
    val maxSpeed: Float = 4.0f,
    val jumpForce: Float = -14.5f,
    val bounceForce: Float = -9f,
    val terminalVelocity: Float = 12f,
    val tileSize: Float = 40f
)

enum class GameStatus {
    MENU,
    PLAYING,
    READING,
    LEVEL_COMPLETE,
    PAUSED
}

