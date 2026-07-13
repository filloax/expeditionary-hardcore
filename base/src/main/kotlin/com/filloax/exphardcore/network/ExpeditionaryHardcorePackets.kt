package com.filloax.exphardcore.network

import com.filloax.exphardcore.character.CharacterCreationData
import com.filloax.exphardcore.character.LifeHandler.createExpeditionCharacter
import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.exphardcore.client.clientPlayerLifeData
import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.FxLibServices
import com.filloax.fxlib.api.codec.streamCodec
import com.filloax.fxlib.api.networking.ToClientContext
import com.filloax.fxlib.api.networking.ToServerContext
import com.filloax.fxlib.api.networking.TrackedEntityData
import com.filloax.fxlib.api.networking.playC2S
import com.filloax.fxlib.api.networking.playS2C
import com.filloax.fxlib.api.optional
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.TypeAndCodec
import net.minecraft.resources.Identifier
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

private typealias RStreamCodec<T> = StreamCodec<RegistryFriendlyByteBuf, T>

object ExpeditionaryHardcorePackets {
    object Types {
        val CHARACTER_CREATION = id("character_creation")
        val LIFE_SYNC = id("life_sync")
    }

    val CHARACTER_CREATION = ServerboundCharacterCreationPacket.ENTRY
    val LIFE_SYNC = ClientboundLifeSyncPacket.ENTRY


    fun registerPacketsC2S() {
        FxLibServices.networking.packetRegistrator.apply {
            playC2S(CHARACTER_CREATION, ServerPacketHandlers::handleCharacterCreation)
        }
    }

    fun registerPacketsS2C() {
        FxLibServices.networking.packetRegistrator.apply {
            playS2C(LIFE_SYNC, ClientPacketHandlers::handleLifeSync)
        }
        registerAllTrackedData()
    }
}

class ServerboundCharacterCreationPacket(
    override val name: String,
) : CustomPacketPayload, CharacterCreationData {
    companion object {
        val CODEC: RStreamCodec<ServerboundCharacterCreationPacket> = ByteBufCodecs.stringUtf8(32)
            .cast<RegistryFriendlyByteBuf>()
            .map(::ServerboundCharacterCreationPacket, ServerboundCharacterCreationPacket::name)

        val TYPE = CustomPacketPayload.Type<ServerboundCharacterCreationPacket>(ExpeditionaryHardcorePackets.Types.CHARACTER_CREATION)
        val ENTRY = TypeAndCodec(TYPE, CODEC)
    }

    override fun type() = TYPE
}

private object ServerPacketHandlers {
    fun handleCharacterCreation(packet: ServerboundCharacterCreationPacket, context: ToServerContext) {
        val player = context.player
        player.createExpeditionCharacter(packet)
    }
}

class ClientboundLifeSyncPacket(
    val lifeData: PlayerLifeData?,
) : CustomPacketPayload {
    companion object {
        val CODEC: RStreamCodec<ClientboundLifeSyncPacket> = StreamCodec.composite(
            ByteBufCodecs.optional(PlayerLifeData.serializer().streamCodec()), ClientboundLifeSyncPacket::lifeData.optional(),
        ) { lifeData -> ClientboundLifeSyncPacket(lifeData.getOrNull()) }

        val TYPE = CustomPacketPayload.Type<ClientboundLifeSyncPacket>(ExpeditionaryHardcorePackets.Types.LIFE_SYNC)
        val ENTRY = TypeAndCodec(TYPE, CODEC)
    }

    override fun type() = TYPE
}

private object ClientPacketHandlers {
    fun handleLifeSync(packet: ClientboundLifeSyncPacket, context: ToClientContext) {
        clientPlayerLifeData = packet.lifeData
    }
}