package com.possible_triangle.atheneum_connector.messages

import kotlinx.serialization.Serializable

@Serializable
data class CreatedPlaceMessage(val placeId: Int)