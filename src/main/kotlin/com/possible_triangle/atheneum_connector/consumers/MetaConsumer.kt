package com.possible_triangle.atheneum_connector.consumers

import com.possible_triangle.atheneum_connector.SubscribeMessage
import com.possible_triangle.atheneum.messages.RestartMessage
import com.possible_triangle.atheneum.messages.ServerStatus
import com.possible_triangle.atheneum.messages.ServerStatusMessage
import com.possible_triangle.atheneum_connector.LocationCache
import com.possible_triangle.atheneum_connector.RabbitMQ
import com.possible_triangle.atheneum_connector.publish

object MetaConsumer {

    @SubscribeMessage
    fun onRestart(message: RestartMessage) {
        RabbitMQ.publish(ServerStatusMessage(ServerStatus.RECOVERED))

        if (message.refreshCache) {
            LocationCache.reload()
        }
    }

}