package com.example.northstarquest.game

import android.graphics.Color

object GameLevels {

    val config = GameConfig()

    private val contentDb: Map<String, LevelContent> = mapOf(
        "vision" to LevelContent(
            id = "vision",
            title = "Cognitive Architecture",
            shortText = "The Shift: Deterministic → Cognitive",
            fullText = """
                The North Star describes the evolution from deterministic, rule-based systems to cognitive, agentic systems.
                
                Key Concept: The Cognitive Core combines business data, process knowledge, and reasoning models.
                
                It creates an Apps → Data → AI → Apps flywheel where each interaction improves future outcomes.
            """.trimIndent()
        ),
        "ux" to LevelContent(
            id = "ux",
            title = "Experience Layer",
            shortText = "Joule & Generative UX",
            fullText = """
                The User Experience Layer becomes the cognitive interface. Joule interprets user intent and orchestrates agents.
                
                Interfaces shift from static screens to adaptive, multimodal experiences with human-in-the-loop controls.
            """.trimIndent()
        ),
        "process" to LevelContent(
            id = "process",
            title = "Process Layer",
            shortText = "Agents & Orchestration",
            fullText = """
                Workflows shift from fixed logic to dynamic coordination. Joule Agents plan, reason, and act across systems.
                
                This bridges the Deterministic Path (Golden Path apps) with the Agentic Path (adaptive agents).
            """.trimIndent()
        ),
        "foundation" to LevelContent(
            id = "foundation",
            title = "Foundation Layer",
            shortText = "Data & AI Core",
            fullText = """
                The intelligent core of process and data. AI Foundation manages agents and models.
                
                SAP Business Data Cloud and Knowledge Graph provide the semantic grounding for trusted AI.
            """.trimIndent()
        )
    )

    private fun storyCoin(
        x: Int,
        y: Int,
        label: String,
        idSuffix: String
    ): Entity {
        val size = config.tileSize * 0.8f
        return Entity(
            id = "coin-$idSuffix",
            type = EntityType.COIN,
            x = x * config.tileSize,
            y = y * config.tileSize,
            width = size,
            height = size,
            label = label
        )
    }

    private fun infoBlock(
        x: Int,
        y: Int,
        label: String,
        contentId: String,
        index: Int
    ): Entity {
        return Entity(
            id = "info-$index",
            type = EntityType.INFO,
            x = x * config.tileSize,
            y = y * config.tileSize,
            width = config.tileSize,
            height = config.tileSize,
            label = label,
            contentId = contentId
        )
    }

    private fun platform(
        id: String,
        x: Int,
        y: Int,
        wTiles: Int,
        hTiles: Int
    ): Entity {
        return Entity(
            id = id,
            type = EntityType.PLATFORM,
            x = x * config.tileSize,
            y = y * config.tileSize,
            width = wTiles * config.tileSize,
            height = hTiles * config.tileSize
        )
    }

    private fun enemy(
        id: String,
        x: Int,
        y: Int,
        patrolStart: Int,
        patrolEnd: Int
    ): Entity {
        return Entity(
            id = id,
            type = EntityType.ENEMY,
            x = x * config.tileSize,
            y = y * config.tileSize,
            width = config.tileSize,
            height = config.tileSize,
            label = "SILO",
            patrolStart = patrolStart * config.tileSize,
            patrolEnd = patrolEnd * config.tileSize,
            direction = 1
        )
    }

    private fun flag(
        x: Int,
        y: Int
    ): Entity {
        return Entity(
            id = "flag",
            type = EntityType.FLAG,
            x = x * config.tileSize,
            y = y * config.tileSize,
            width = config.tileSize,
            height = config.tileSize * 4,
            label = "LIVE"
        )
    }

    private fun baseLevel(
        id: Int,
        name: String,
        description: String
    ): LevelConfig {
        val entities = mutableListOf<Entity>()

        // Floor
        for (i in -5..150) {
            entities.add(
                Entity(
                    id = "floor-$i",
                    type = EntityType.PLATFORM,
                    x = i * config.tileSize,
                    y = 13 * config.tileSize,
                    width = config.tileSize,
                    height = config.tileSize
                )
            )
        }

        // Section 1: Vision
        entities.add(storyCoin(10, 9, "DATA", "1"))
        entities.add(storyCoin(14, 7, "CTX", "2"))
        entities.add(infoBlock(20, 9, "VIS", "vision", 1))

        // Section 2: UX
        entities.add(platform("p1", 26, 10, 3, 1))
        entities.add(storyCoin(27, 8, "UX", "3"))
        entities.add(infoBlock(32, 9, "JOULE", "ux", 2))

        // Section 3: Process (Enemies as Silos)
        entities.add(enemy("silo-1", 40, 12, 38, 45))
        entities.add(platform("p2", 48, 8, 3, 1))
        entities.add(storyCoin(49, 6, "PROC", "4"))
        entities.add(infoBlock(55, 9, "AGENT", "process", 3))

        // Section 4: Foundation
        entities.add(platform("p3", 62, 7, 4, 1))
        entities.add(enemy("silo-2", 70, 12, 68, 75))
        entities.add(storyCoin(63, 5, "AI", "5"))
        entities.add(infoBlock(78, 9, "FND", "foundation", 4))

        // End
        entities.add(flag(85, 9))

        // Walls
        entities.add(platform("wall-l", -1, 0, 1, 20))
        entities.add(platform("wall-r", 90, 0, 1, 20))

        return LevelConfig(
            id = id,
            name = name,
            description = description,
            theme = LevelTheme(
                background = Color.parseColor("#0F172A"), // Slate 900
                platformColor = Color.parseColor("#334155"), // Slate 700
                accentColor = Color.parseColor("#3B82F6") // Blue 500
            ),
            entities = entities,
            content = contentDb
        )
    }

    val levels: List<LevelConfig> = listOf(
        baseLevel(
            id = 1,
            name = "North Star Quest",
            description = "Navigate the layers of the AI-Native Architecture."
        )
    )
}

