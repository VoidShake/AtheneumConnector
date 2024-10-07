package com.possible_triangle.atheneum_connector.consumers

import com.possible_triangle.atheneum_connector.AtheneumConnector
import com.possible_triangle.atheneum_connector.SubscribeMessage
import com.possible_triangle.atheneum_connector.messages.CreatedPlaceMessage

object LocationsConsumer {

    @SubscribeMessage
    fun onPlaceCreate(message: CreatedPlaceMessage) {
        AtheneumConnector.LOG.info("new place with id {} has been created", message.placeId)
    }

}