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
        titleTranslation = "exphardcore.config.title",
        description = "Expeditionary Hardcore settings",
        descriptionTranslation = "exphardcore.config.description"
)
public final class ExpeditionaryHardcoreConfig {
    public static final String T_PREF = "exphardcore.config.main.";

    @ConfigEntry(id = "allowChangingName", translation = T_PREF + "allowChangingName.name")
    @Comment(value = "Allow players to change their character name after creation", translation = T_PREF + "allowChangingName.comment")
    public static boolean allowChangingName = false;

    @ConfigEntry(id = "preventLogbookDrop", translation = T_PREF + "preventLogbookDrop.name")
    @Comment(value = "Prevent the Expeditioner's Logbook from being dropped", translation = T_PREF + "preventLogbookDrop.comment")
    public static boolean preventLogbookDrop = true;

    @ConfigEntry(id = "replacePlayerModel", translation = T_PREF + "replacePlayerModel.name")
    @Comment(value = "Replace the player model every life", translation = T_PREF + "replacePlayerModel.comment")
    public static boolean replacePlayerModel = true;

    @ConfigEntry(id = "enableLifeQuirks", translation = T_PREF + "enableLifeQuirks.name")
    @Comment(value = "Give each new character a random minor quirk", translation = T_PREF + "enableLifeQuirks.comment")
    public static boolean enableLifeQuirks = true;
}
