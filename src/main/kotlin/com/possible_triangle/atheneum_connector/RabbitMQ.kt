package com.possible_triangle.atheneum_connector

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import net.minecraftforge.fml.ModList
import org.objectweb.asm.Type
import java.lang.annotation.ElementType
import kotlin.reflect.KParameter
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

object RabbitMQ {

    private const val EXCHANGE_KEY = "exchange"

    private lateinit var channel: Channel

    fun connect(url: String) {
        AtheneumConnector.LOG.info("Connecting to RabbitMQ")

        val connection = ConnectionFactory().newConnection(url)

        channel = connection.createChannel()
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

    fun close() {
        channel.connection.close()
    }

    fun publish(routingKey: String, bytes: ByteArray) {
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

                    if (method.parameters.size != 2) {
                        throw IllegalArgumentException("@SubscribeMessage handlers may only accept one parameter, the message itself")
                    }

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