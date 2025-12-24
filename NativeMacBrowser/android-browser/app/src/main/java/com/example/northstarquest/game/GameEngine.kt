package com.example.northstarquest.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class InputState(
    val moveDirection: Int = 0,
    val jumpRequested: Boolean = false
)

data class GameState(
    val status: GameStatus = GameStatus.MENU,
    val levelIndex: Int = 0,
    val player: PlayerState,
    val score: Int = 0,
    val storyMessage: String? = null
)

class GameEngine(
    private val config: GameConfig,
    private val level: LevelConfig
) {
    fun initialPlayerState(): PlayerState {
        return PlayerState(
            x = 2f * config.tileSize,
            y = 10f * config.tileSize,
            width = config.tileSize * 0.8f,
            height = config.tileSize * 0.9f,
            velocityX = 0f,
            velocityY = 0f,
            onGround = false
        )
    }

    fun update(
        player: PlayerState,
        input: InputState,
        deltaTime: Float,
        entities: MutableList<Entity>,
        onCoinCollected: (String?) -> Unit,
        onInfoHit: (String) -> Unit,
        onLevelComplete: () -> Unit,
        onDeath: () -> Unit
    ): PlayerState {
        var vx = player.velocityX
        var vy = player.velocityY
        var x = player.x
        var y = player.y
        var onGround = player.onGround

        vx += input.moveDirection * config.moveSpeed * config.tileSize * deltaTime
        vx *= config.friction
        vx = max(-config.maxSpeed * config.tileSize, min(config.maxSpeed * config.tileSize, vx))

        vy += config.gravity * config.tileSize * deltaTime
        vy = min(config.terminalVelocity * config.tileSize, vy)

        if (input.jumpRequested && onGround) {
            vy = config.jumpForce * config.tileSize * deltaTime
            onGround = false
        }

        val futureX = x + vx * deltaTime
        val futureY = y + vy * deltaTime

        var resolvedX = futureX
        var resolvedY = futureY
        var grounded = false

        for (entity in entities) {
            if (entity.type == EntityType.PLATFORM) {
                val collisionX = checkCollision(
                    futureX,
                    y,
                    player.width,
                    player.height,
                    entity
                )
                if (collisionX) {
                    if (vx > 0) {
                        resolvedX = entity.x - player.width
                    } else if (vx < 0) {
                        resolvedX = entity.x + entity.width
                    }
                    vx = 0f
                }
            }
        }

        for (entity in entities) {
            if (entity.type == EntityType.PLATFORM) {
                val collisionY = checkCollision(
                    resolvedX,
                    futureY,
                    player.width,
                    player.height,
                    entity
                )
                if (collisionY) {
                    if (vy > 0) {
                        resolvedY = entity.y - player.height
                        grounded = true
                    } else if (vy < 0) {
                        resolvedY = entity.y + entity.height
                    }
                    vy = 0f
                }
            }
        }

        val iterator = entities.listIterator()
        while (iterator.hasNext()) {
            val entity = iterator.next()
            val collides = checkCollision(
                resolvedX,
                resolvedY,
                player.width,
                player.height,
                entity
            )
            if (!collides) continue

            when (entity.type) {
                EntityType.COIN -> {
                    iterator.remove()
                    onCoinCollected(entity.label)
                }

                EntityType.INFO -> {
                    entity.contentId?.let { onInfoHit(it) }
                }

                EntityType.ENEMY -> {
                    if (vy > 0 && resolvedY + player.height <= entity.y + entity.height / 2f) {
                        vy = config.bounceForce * config.tileSize * deltaTime
                        iterator.remove()
                    } else {
                        onDeath()
                        return initialPlayerState()
                    }
                }

                EntityType.FLAG -> {
                    onLevelComplete()
                }

                else -> Unit
            }
        }

        if (resolvedY > 20f * config.tileSize) {
            onDeath()
            return initialPlayerState()
        }

        return PlayerState(
            x = resolvedX,
            y = resolvedY,
            width = player.width,
            height = player.height,
            velocityX = vx,
            velocityY = vy,
            onGround = grounded
        )
    }

    private fun checkCollision(
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        entity: Entity
    ): Boolean {
        val ex = entity.x
        val ey = entity.y
        val ew = entity.width
        val eh = entity.height
        val overlapX = px < ex + ew && px + pw > ex
        val overlapY = py < ey + eh && py + ph > ey
        return overlapX && overlapY
    }
}

