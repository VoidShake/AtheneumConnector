package com.possible_triangle.atheneum_connector

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.fml.common.Mod
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

@Mod("atheneum_connector")
object AtheneumConnector {

    fun JsonBuilder.configure() {
        // Nothing for now
    }

    val JSON = Json { configure() }

    val LOG: Logger = LoggerFactory.getLogger("Atheneum Connector")

    init {
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER) {
            Runnable(::serverInit)
        }
    }

    private fun serverInit() {
        RabbitMQ.connect("amqp://guest:guest@localhost:5672/")
        LocationCache.initialize()

        FORGE_BUS.addListener { _: ServerStoppingEvent ->
            RabbitMQ.close()
        }
    }

}