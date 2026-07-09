package com.filloax.exphardcore.config;

import com.filloax.exphardcore.respawn.RespawnConfigDefaults;
import com.filloax.exphardcore.respawn.RespawnCenter;
import com.filloax.exphardcore.respawn.RespawnDistribution;
import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;

@Category("respawn")
public final class RespawnConfig {
    public static final String T_PREF = "exphardcore.config.respawn.";
    @ConfigEntry(id = "respawnRadiusMin", translation = T_PREF + "respawnRadiusMin.name")
    public static int respawnRadiusMin = RespawnConfigDefaults.INSTANCE.getRespawnRadiusMin();

    @ConfigEntry(id = "respawnRadiusMax", translation = T_PREF + "respawnRadiusMax.name")
    public static int respawnRadiusMax = RespawnConfigDefaults.INSTANCE.getRespawnRadiusMax();

    @ConfigEntry(id = "minDistanceFromLastRespawn", translation = T_PREF + "minDistanceFromLastRespawn.name")
    @Comment(
            value = "Only used if respawnCenter is not set to LAST_RESPAWN_POINT.",
            translation = T_PREF + "minDistanceFromLastRespawn.comment"
    )
    public static int minDistanceFromLastRespawn = RespawnConfigDefaults.INSTANCE.getMinDistanceFromLastRespawn();

    @ConfigEntry(id = "respawnCenter", translation = T_PREF + "respawnCenter.name")
    public static RespawnCenter respawnCenter = RespawnConfigDefaults.INSTANCE.getRespawnCenter();

    @ConfigEntry(id = "respawnDistribution", translation = T_PREF + "respawnDistribution.name")
    public static RespawnDistribution respawnDistribution = RespawnConfigDefaults.INSTANCE.getRespawnDistribution();

    @ConfigEntry(id = "respawnDistributionMidpoint", translation = T_PREF + "respawnDistributionMidpoint.name")
    @Comment(
            value = "Only used if respawnDistribution is set to MIDPOINT_MORE_LIKELY. "
                    + "Distance from the center where respawns are more likely to happen. "
                    + "Set to -1 to use the halfway point between respawnRadiusMin and respawnRadiusMax.",
            translation = T_PREF + "respawnDistributionMidpoint.comment"
    )
    public static int respawnDistributionMidpoint = RespawnConfigDefaults.INSTANCE.getRespawnDistributionMidpoint();

    @ConfigEntry(id = "avoidOceans", translation = T_PREF + "avoidOceans.name")
    public static boolean avoidOceans = RespawnConfigDefaults.INSTANCE.getAvoidOceans();
}
