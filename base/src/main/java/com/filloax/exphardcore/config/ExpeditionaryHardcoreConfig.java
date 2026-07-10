package com.filloax.exphardcore.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;

import static com.filloax.exphardcore.ExpeditionaryHardcore.MOD_ID;

@Config(
        value = MOD_ID,
        categories = {
            RespawnConfig.class
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
}
