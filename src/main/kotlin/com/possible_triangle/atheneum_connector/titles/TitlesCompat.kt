package com.possible_triangle.atheneum_connector.titles

import com.possible_triangle.atheneum_connector.network.DisplayTitlePaket
import com.yungnickyoung.minecraft.travelerstitles.TravelersTitlesCommon
import com.yungnickyoung.minecraft.travelerstitles.render.TitleRenderer
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.event.TickEvent.ClientTickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

@EventBusSubscriber(Dist.CLIENT)
object TitlesCompat {

    private val RENDERER: TitleRenderer<DisplayTitlePaket> = TitleRenderer(
        1,
        TravelersTitlesCommon.CONFIG.waystones.enabled,
        TravelersTitlesCommon.CONFIG.waystones.textFadeInTime,
        TravelersTitlesCommon.CONFIG.waystones.textDisplayTime,
        TravelersTitlesCommon.CONFIG.waystones.textFadeOutTime,
        TravelersTitlesCommon.CONFIG.waystones.textColor,
        TravelersTitlesCommon.CONFIG.waystones.renderShadow,
        TravelersTitlesCommon.CONFIG.waystones.textSize,
        TravelersTitlesCommon.CONFIG.waystones.textXOffset,
        TravelersTitlesCommon.CONFIG.waystones.textYOffset,
        TravelersTitlesCommon.CONFIG.waystones.centerText
    )

    @SubscribeEvent
    fun clientTick(event: ClientTickEvent) {
        if (!Minecraft.getInstance().isPaused) {
            RENDERER.tick()
        }
    }

    @SubscribeEvent
    fun renderTitles(event: RenderGuiEvent.Pre) {
        if (!Minecraft.getInstance().options.renderDebug) {
            RENDERER.renderText(event.partialTick, event.guiGraphics)
        }
    }

    fun setTitle(packet: DisplayTitlePaket) {
        if (!RENDERER.matchesAnyRecentEntry { it.id == packet.id }) {
            RENDERER.displayTitle(packet.component, null)
            RENDERER.addRecentEntry(packet)
        }
    }

}