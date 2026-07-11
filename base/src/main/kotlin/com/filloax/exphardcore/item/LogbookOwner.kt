package com.filloax.exphardcore.item

import com.filloax.exphardcore.character.getExpeditionLifeOrNull
import com.filloax.exphardcore.client.clientPlayerLifeData
import com.filloax.exphardcore.item.ExpeditionaryHardcoreDataComponents.LOGBOOK_OWNER
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.ChatFormatting
import net.minecraft.core.UUIDUtil
import net.minecraft.core.component.DataComponentGetter
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipProvider
import java.util.UUID
import java.util.function.Consumer

data class LogbookOwner(val lifeId: UUID, val characterName: String) : TooltipProvider {
    override fun addToTooltip(
        context: Item.TooltipContext,
        tooltipAdder: Consumer<Component>,
        tooltipFlag: TooltipFlag,
        componentGetter: DataComponentGetter,
    ) {
        tooltipAdder.accept(
            Component.translatable("item.exphardcore.expeditioner_logbook.desc", characterName)
                .withStyle(ChatFormatting.AQUA)
        )
    }

    companion object {
        val CODEC: Codec<LogbookOwner> = RecordCodecBuilder.create { builder ->
            builder.group(
                UUIDUtil.CODEC.fieldOf("lifeId").forGetter(LogbookOwner::lifeId),
                Codec.STRING.fieldOf("characterName").forGetter(LogbookOwner::characterName),
            ).apply(builder, ::LogbookOwner)
        }

        val STREAM_CODEC: StreamCodec<io.netty.buffer.ByteBuf, LogbookOwner> = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, LogbookOwner::lifeId,
            ByteBufCodecs.STRING_UTF8, LogbookOwner::characterName,
            ::LogbookOwner,
        )

        // Ownerless logbook counts as yours only while you haven't completed
        // character creation (so it cannot be dropped if you just spawned)
        @JvmStatic
        fun isLogbookOwnedBy(stack: ItemStack, player: Player): Boolean {
            if (stack.item !is ExpeditionersLogbookItem) return false
            val owner = stack.get(LOGBOOK_OWNER)

            if (owner == null) {
                val currentLife = if (player is ServerPlayer)
                    player.getExpeditionLifeOrNull()
                else
                    clientPlayerLifeData
                return currentLife?.didCreation != true
            }

            val currentLifeId = if (player is ServerPlayer)
                player.getExpeditionLifeOrNull()?.id
            else
                clientPlayerLifeData?.id

            return currentLifeId == owner.lifeId
        }
    }
}
