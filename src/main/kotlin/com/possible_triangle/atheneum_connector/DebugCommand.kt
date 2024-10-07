package com.possible_triangle.atheneum_connector

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

@EventBusSubscriber(Dist.DEDICATED_SERVER)
object DebugCommand {

    @SubscribeEvent
    fun register(event: RegisterCommandsEvent) {
        event.dispatcher.register(
            literal("atheneum")
                .then(literal("reload")
                    .executes(::reload)
                )
        )
    }

    private fun reload(ctx: CommandContext<CommandSourceStack>): Int {
        LocationCache.reload()

        ctx.source.sendSuccess({ Component.literal("Reloaded cache!") }, true)

        return 1
    }

}