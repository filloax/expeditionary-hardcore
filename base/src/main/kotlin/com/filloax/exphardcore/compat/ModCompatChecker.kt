package com.filloax.exphardcore.compat

abstract class ModCompatChecker {
    val isApibalegoLoaded = isLoaded(ID_APIBALEGO)
    val isFtbTeamsLoaded = isLoaded(ID_FTB_TEAMS)
    val isDistantHorizonsLoaded = isLoaded(ID_DISTANT_HORIZONS)
    val isIrisLoaded = isLoaded(ID_IRIS)

    abstract fun isLoaded(id: String): Boolean

    companion object {
        const val ID_APIBALEGO = "apibalego"
        const val ID_FTB_TEAMS = "ftbteams"
        const val ID_DISTANT_HORIZONS = "distanthorizons"
        const val ID_IRIS = "iris"
    }
}