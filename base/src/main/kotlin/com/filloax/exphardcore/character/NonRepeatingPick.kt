package com.filloax.exphardcore.character

/**
 * Avoid picking elements already picked in history, unless current "cycle" has
 * ended (avoids needing to track seen history separately)
 *
 * Also avoids on cycle start picking the last element as last cycle end
 */
fun <T : Any> selectCyclingNewElements(history: List<T?>, pool: Set<T>): Set<T> {
    if (pool.isEmpty()) return pool

    val seen = mutableSetOf<T>()
    for (element in history) {
        if (element == null || element !in pool) continue
        seen.add(element)
        if (seen.size == pool.size) seen.clear()
    }

    if (seen.isEmpty() && pool.size > 1) {
        val last = history.lastOrNull { it != null && it in pool }
        if (last != null) return pool - last
    }
    return pool - seen
}
