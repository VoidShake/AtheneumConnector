package com.possible_triangle.atheneum_connector.events

import com.possible_triangle.atheneum.messages.PlayerDisconnectMessage
import com.possible_triangle.atheneum.messages.PlayerMoveMessage
import com.possible_triangle.atheneum.models.Point
import com.possible_triangle.atheneum_connector.LocationCache
import com.possible_triangle.atheneum_connector.RabbitMQ
import com.possible_triangle.atheneum_connector.network.DisplayTitlePaket
import com.possible_triangle.atheneum_connector.network.Network
import com.possible_triangle.atheneum_connector.publish
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.TickEvent.PlayerTickEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

@EventBusSubscriber(Dist.DEDICATED_SERVER)
object PlayerEvents {

    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent) {
        val player = event.player as ServerPlayer
        val level = player.level()

        if (level.gameTime % 20 != 0L) return

        RabbitMQ.publish(
            PlayerMoveMessage(
                player.uuid,
                Point(
                    world = player.level().dimension().location().path,
                    x = player.blockX,
                    y = player.blockY,
                    z = player.blockZ,
                )
            )
        )

        val location = LocationCache.containing(level.dimension(), player.onPos) ?: return

        location.ifLeft {
            Network.sendTo(player, DisplayTitlePaket(Component.translatable("travelerstitles.entered_area", it.name), "area-${it.id}"))
        }

        location.ifRight {
            Network.sendTo(
                player,
                DisplayTitlePaket(Component.translatable("travelerstitles.at_place", it.name), "place-${it.id}")
            )
        }
    }

    @SubscribeEvent
    fun onPlayerDisconnect(event: PlayerLoggedOutEvent) {
        RabbitMQ.publish(PlayerDisconnectMessage(event.entity.uuid))
    }

}