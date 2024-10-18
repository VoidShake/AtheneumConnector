package com.possible_triangle.atheneum_connector

import com.possible_triangle.atheneum.messages.ServerStatus
import com.possible_triangle.atheneum.messages.ServerStatusMessage
import com.rabbitmq.client.*
import io.ktor.util.logging.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import net.minecraftforge.fml.ModList
import org.objectweb.asm.Type
import java.lang.annotation.ElementType
import kotlin.reflect.KParameter
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

object RabbitMQ : RecoveryListener, ShutdownListener {

    private const val EXCHANGE_KEY = "exchange"

    private lateinit var channel: Channel

    fun connect() {
        val factory = Config.SERVER.configureRabbitMQ()

        AtheneumConnector.LOG.info("Connecting to RabbitMQ at amqp://${factory.host}:${factory.port}")

        val connection = factory.newConnection()

        connection.addShutdownListener(this)

        if (connection is Recoverable) {
            connection.addRecoveryListener(this)
        }

        channel = connection.createChannel()
        declareExchanges()

        val queue = channel.queueDeclare().queue

        val consumers = hashMapOf<String, MutableList<(ByteArray) -> Unit>>()
        loadConsumers { topic, consumer ->
            channel.queueBind(queue, EXCHANGE_KEY, topic)
            consumers.getOrPut(topic, ::arrayListOf).add(consumer)
        }

        channel.basicConsume(queue, true, { _, message ->
            val routingKey = message.envelope.routingKey
            consumers[routingKey]?.forEach {
                it(message.body)
            }
        }, { _ -> })
    }

    private fun declareExchanges() {
        channel.exchangeDeclare(EXCHANGE_KEY, "direct")
    }

    override fun handleRecovery(recoverable: Recoverable) {
        AtheneumConnector.LOG.warn("RabbitMQ has recovered")
        declareExchanges()
        publish(ServerStatusMessage(ServerStatus.RECOVERED))
    }

    override fun handleRecoveryStarted(recoverable: Recoverable) {
        AtheneumConnector.LOG.warn("Trying to reconnect to RabbitMQ...")
    }

    override fun shutdownCompleted(exception: ShutdownSignalException) {
        if (exception.isInitiatedByApplication) return
        AtheneumConnector.LOG.warn("RabbitMQ has shutdown, reconnecting... ({})", exception.message)
    }

    private fun tryCatching(action: () -> Unit) {
        try {
            action()
        } catch (ex: ShutdownSignalException) {
            AtheneumConnector.LOG.error("Encountered exception when publishing to RabbitMQ")
            AtheneumConnector.LOG.error(ex)
        }
    }

    fun close() = tryCatching {
        channel.connection.close()
    }

    fun publish(routingKey: String, bytes: ByteArray) = tryCatching {
        channel.basicPublish(EXCHANGE_KEY, routingKey, null, bytes)
    }

    private fun loadConsumers(consume: (String, (ByteArray) -> Unit) -> Unit) {
        val annotations = ModList.get().allScanData
            .flatMap { it.annotations }
            .filter { it.annotationType == Type.getType(SubscribeMessage::class.java) }
            .filter { it.targetType == ElementType.METHOD }
            .groupBy { it.clazz }
            .mapKeys { Class.forName(it.key.className).kotlin }

        annotations.forEach { (clazz) ->
            clazz.memberFunctions
                .filter { it.hasAnnotation<SubscribeMessage>() }
                .forEach { method ->
                    val messageType = method.parameters.first { it.kind == KParameter.Kind.VALUE }.type
                    val topic = Class.forName(messageType.toString()).simpleName
                    val deserializer = serializer(messageType)

                    require(method.parameters.size == 2) { "@SubscribeMessage handlers may only accept one parameter, the message itself" }

                    consume(topic) {
                        val message = AtheneumConnector.JSON.decodeFromString(deserializer, it.decodeToString())
                        method.call(clazz.objectInstance, message)
                    }
                }
        }
    }

}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> RabbitMQ.publish(message: T, routingKey: String = T::class.simpleName!!) {
    val serializer = T::class.serializer()
    val bytes = AtheneumConnector.JSON.encodeToString(serializer, message).toByteArray()

    publish(routingKey, bytes)
}