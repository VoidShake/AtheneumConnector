package com.possible_triangle.atheneum_connector

import com.possible_triangle.atheneum_connector.messages.ServerStatus
import com.possible_triangle.atheneum_connector.messages.ServerStatusMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

@Mod("atheneum_connector")
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object AtheneumConnector {

    fun JsonBuilder.configure() {
        // Nothing for now
    }

    val JSON = Json { configure() }

    val LOG: Logger = LoggerFactory.getLogger("Atheneum Connector")

    init {
        Config.register()
    }

    @SubscribeEvent
    fun serverInit(event: FMLDedicatedServerSetupEvent) {
        RabbitMQ.connect()
        LocationCache.initialize()

        FORGE_BUS.addListener { _: ServerStartedEvent ->
            RabbitMQ.publish(ServerStatusMessage(ServerStatus.STARTED))
        }

        FORGE_BUS.addListener { _: ServerStoppingEvent ->
            RabbitMQ.publish(ServerStatusMessage(ServerStatus.STOPPED))
            RabbitMQ.close()
        }
    }

}