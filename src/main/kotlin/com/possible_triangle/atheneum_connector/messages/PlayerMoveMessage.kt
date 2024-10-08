package com.possible_triangle.atheneum_connector.messages

import com.possible_triangle.atheneum_connector.generated.placesquery.Point
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.player.Player

@Serializable
data class PlayerMoveMessage(
    val player: String,
    val pos: Point,
) {
    companion object {
        fun from(player: Player) = PlayerMoveMessage(
            player.uuid.toString(),
            Point(
                player.blockX, player.blockY, player.blockZ,
                player.level().dimension().location().path,
            )
        )
    }
}