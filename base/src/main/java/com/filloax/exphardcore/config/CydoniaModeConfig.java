package com.filloax.exphardcore.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;

import static com.filloax.exphardcore.ExpeditionaryHardcore.MOD_ID;

@Config(MOD_ID + "_cydonia")
@ConfigInfo(
        title = "Expeditionary Hardcore (Cydonia Mode)",
        description = "Cydonia Mode settings"
)
public final class CydoniaModeConfig {
    public static final String T_PREF = "exphardcore.config.cydonia.";

    @ConfigEntry(id = "sendInfoToApibalego", translation = T_PREF + "sendInfoToApibalego")
    public static boolean sendInfoToApibalego = true;

    @ConfigEntry(id = "mainPlayer", translation = T_PREF + "mainPlayer")
    @Comment(
            value = "The main player for the server-wide team, by name or UUID. "
                    + "Everyone else respawns near them. Empty = no forced main.",
            translation = T_PREF + "mainPlayer.comment"
    )
    public static String mainPlayer = "";
}
