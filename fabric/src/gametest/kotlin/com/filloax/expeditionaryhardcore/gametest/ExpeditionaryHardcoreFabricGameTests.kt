package com.filloax.expeditionaryhardcore.gametest

import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class ExpeditionaryHardcoreFabricGameTests {
    @GameTest
    fun testMixinsLoaded(helper: GameTestHelper) {
        ExpeditionaryHardcoreGameTests.testMixinsLoaded(helper)
    }
}
