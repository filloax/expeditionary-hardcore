package com.filloax.exphardcore.compat

abstract class ModCompatChecker {
    val isApibalegoLoaded = isLoaded(ID_APIBALEGO)
    val isFtbTeamsLoaded = isLoaded(ID_FTB_TEAMS)

    abstract fun isLoaded(id: String): Boolean

    companion object {
        const val ID_APIBALEGO = "apibalego"
        const val ID_FTB_TEAMS = "ftbteams"
    }
}