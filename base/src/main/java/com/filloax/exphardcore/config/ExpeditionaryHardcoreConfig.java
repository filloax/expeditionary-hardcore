package com.filloax.exphardcore.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;

import static com.filloax.exphardcore.ExpeditionaryHardcore.MOD_ID;

@Config(
        value = MOD_ID,
        categories = {
            RespawnConfig.class,
            MultiplayerConfig.class
        }
)
@ConfigInfo(
        title = "Expeditionary Hardcore",
        description = "Expeditionary Hardcore settings"
)
public final class ExpeditionaryHardcoreConfig {
    public static final String T_PREF = "exphardcore.config.main.";

    @ConfigEntry(id = "allowChangingName", translation = T_PREF + "allowChangingName")
    public static boolean allowChangingName = false;

    @ConfigEntry(id = "preventLogbookDrop", translation = T_PREF + "preventLogbookDrop")
    public static boolean preventLogbookDrop = true;

    @ConfigEntry(id = "replacePlayerModel", translation = T_PREF + "replacePlayerModel")
    public static boolean replacePlayerModel = true;

    @ConfigEntry(id = "enableLifeQuirks", translation = T_PREF + "enableLifeQuirks")
    public static boolean enableLifeQuirks = true;

    @ConfigEntry(id = "respawnDistantHorizonsDisableSeconds", translation = T_PREF + "respawnDistantHorizonsDisableSeconds.name")
    @Comment(
            value = "If Distant Horizons is loaded: seconds it gets disabled after respawning elsewhere. 0 disables this. Used to avoid LoD islands, etc",
            translation = T_PREF + "respawnDistantHorizonsDisableSeconds.comment"
    )
    public static int respawnDistantHorizonsDisableSeconds = 15;
}
