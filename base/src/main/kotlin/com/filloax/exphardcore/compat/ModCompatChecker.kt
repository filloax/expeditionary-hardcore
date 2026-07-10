package com.filloax.exphardcore.compat

abstract class ModCompatChecker {
    val isApibalegoLoaded = isLoaded(ID_APIBALEGO)

    abstract fun isLoaded(id: String): Boolean

    companion object {
        const val ID_APIBALEGO = "apibalego"
    }
}