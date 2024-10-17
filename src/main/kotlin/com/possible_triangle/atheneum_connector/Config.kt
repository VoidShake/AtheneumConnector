package com.possible_triangle.atheneum_connector

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.config.ModConfig
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import java.net.URI
import java.net.URL

object Config {

    private val SERVER_SPEC: ForgeConfigSpec
    val SERVER: Server

    init {
        with(ForgeConfigSpec.Builder().configure { Server(it) }) {
            SERVER = left
            SERVER_SPEC = right
        }
    }

    fun register() {
        LOADING_CONTEXT.registerConfig(ModConfig.Type.COMMON, SERVER_SPEC)
    }

    class Server internal constructor(builder: ForgeConfigSpec.Builder) {

        init {
            builder.push("rabbitmq")
        }

        private val rabbitmqUser = builder.define("user", "guest")
        private val rabbitmqPass = builder.define("password", "guest")
        private val rabbitmqHost = builder.define("host", "localhost")
        private val rabbitmqPort = builder.define("port", "5672")

        val rabbitMQUrl
            get() = "amqp://${rabbitmqUser.get()}:${rabbitmqPass.get()}@${rabbitmqHost.get()}:${rabbitmqPort.get()}/"

        init {
            builder.pop()
            builder.push("grapql")
        }

        private val _graphqlUrl = builder.define("url", "https://atlas.macarena.ceo/api/graphql")
        val graphqlUrl: URL get() = URI(_graphqlUrl.get()).toURL()

    }

}