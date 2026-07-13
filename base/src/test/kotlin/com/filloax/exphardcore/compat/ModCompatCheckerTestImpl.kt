package com.filloax.exphardcore.compat

/**
 * Registered as the ServiceLoader impl for unit tests, which run against
 * `base` alone with no fabric/neoforge module to provide the real one.
 */
class ModCompatCheckerTestImpl : ModCompatChecker() {
    override fun isLoaded(id: String): Boolean = false
}
