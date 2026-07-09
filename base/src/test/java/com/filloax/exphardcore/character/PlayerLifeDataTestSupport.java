package com.filloax.exphardcore.character;

import net.minecraft.server.level.ServerPlayer;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

/**
 * Issues with stubbing the inline extension methods
 */
public final class PlayerLifeDataTestSupport implements AutoCloseable {
    private final MockedStatic<PlayerLifeDataKt> mocked;

    private PlayerLifeDataTestSupport() {
        this.mocked = Mockito.mockStatic(PlayerLifeDataKt.class);
    }

    public static PlayerLifeDataTestSupport install() {
        return new PlayerLifeDataTestSupport();
    }

    public PlayerLifeDataTestSupport stubAllLives(ServerPlayer player, List<PlayerLifeData> lives) {
        mocked.when(() -> PlayerLifeDataKt.getAllExpeditionLives(player)).thenReturn(lives);
        return this;
    }

    public PlayerLifeDataTestSupport stubCurrentLife(ServerPlayer player, PlayerLifeData life) {
        mocked.when(() -> PlayerLifeDataKt.getExpeditionLifeOrNull(player)).thenReturn(life);
        return this;
    }

    @Override
    public void close() {
        mocked.close();
    }
}
