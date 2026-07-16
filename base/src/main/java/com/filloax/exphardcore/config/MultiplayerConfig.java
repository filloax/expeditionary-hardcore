package com.filloax.exphardcore.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;

@Category("multiplayer")
public final class MultiplayerConfig {
    public static final String T_PREF = "exphardcore.config.multiplayer.";

    @ConfigEntry(id = "enableTeams", translation = T_PREF + "enableTeams.name")
    @Comment(
            value = "Enable team play: teammates respawn near their team's main player. "
                    + "Always on in cydonia mode.",
            translation = T_PREF + "enableTeams.comment"
    )
    public static boolean enableTeams = false;

    @ConfigEntry(id = "teammateRespawnRadiusMin", translation = T_PREF + "teammateRespawnRadiusMin.name")
    public static int teammateRespawnRadiusMin = 64;

    @ConfigEntry(id = "teammateRespawnRadiusMax", translation = T_PREF + "teammateRespawnRadiusMax.name")
    public static int teammateRespawnRadiusMax = 128;
}
