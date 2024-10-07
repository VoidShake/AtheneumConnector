package com.possible_triangle.atheneum_connector.network

import com.possible_triangle.atheneum_connector.titles.TitlesCompat
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class DisplayTitlePaket(val component: Component, val id: String) {

    fun encode(buf: FriendlyByteBuf) {
        buf.writeComponent(component)
        buf.writeUtf(id)
    }

    fun handle(contextSupplier: Supplier<NetworkEvent.Context>) {
        val context = contextSupplier.get()

        context.enqueueWork {
            TitlesCompat.setTitle(this)
        }

        context.packetHandled = true
    }

    companion object {
        fun decode(buf: FriendlyByteBuf) = DisplayTitlePaket(buf.readComponent(), buf.readUtf())
    }

}
