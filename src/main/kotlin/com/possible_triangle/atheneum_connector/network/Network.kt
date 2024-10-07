package com.possible_triangle.atheneum_connector.network

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor

@Suppress("INACCESSIBLE_TYPE")
@EventBusSubscriber
object Network {

    private const val VERSION = "1.0"

    private val CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation("arva", "atheneum_connector"),
        { VERSION },
        VERSION::equals,
        VERSION::equals,
    )

    fun sendTo(player: ServerPlayer, packet: Any) {
        CHANNEL.send(PacketDistributor.PLAYER.with { player }, packet)
    }

    init {
        CHANNEL.registerMessage(0, DisplayTitlePaket::class.java, DisplayTitlePaket::encode, DisplayTitlePaket::decode, DisplayTitlePaket::handle)
    }

}