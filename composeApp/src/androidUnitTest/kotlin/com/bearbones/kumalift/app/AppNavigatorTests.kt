package com.bearbones.kumalift.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppNavigatorTests {

    @Test
    fun testInitialState() {
        val nav = AppNavigator()
        assertEquals(Tab.HOME, nav.selectedTab)
        assertTrue(nav.overlays.isEmpty())
        assertNull(nav.currentOverlay)
        // Back at Home with no overlays returns false (system can exit)
        assertFalse(nav.back())
    }

    @Test
    fun testSelectTabUpdatesTabAndClearsOverlays() {
        val nav = AppNavigator()
        nav.push(Overlay.Settings)
        assertEquals(1, nav.overlays.size)

        nav.selectTab(Tab.LIBRARY)
        assertEquals(Tab.LIBRARY, nav.selectedTab)
        assertTrue(nav.overlays.isEmpty())
        assertNull(nav.currentOverlay)
    }

    @Test
    fun testBackFromNonHomeTabGoesToHome() {
        val nav = AppNavigator(initialTab = Tab.PROGRESS)
        assertEquals(Tab.PROGRESS, nav.selectedTab)

        val handled = nav.back()
        assertTrue(handled)
        assertEquals(Tab.HOME, nav.selectedTab)

        // Second back at Home returns false
        assertFalse(nav.back())
        assertEquals(Tab.HOME, nav.selectedTab)
    }

    @Test
    fun testFlow47_StartWorkoutFromAnyTabReturnsToSameTabOnBack() {
        // 1. User is on Routines tab
        val nav = AppNavigator(initialTab = Tab.ROUTINES)

        // 2. Start workout opens ActiveLogger
        nav.push(Overlay.ActiveLogger)
        assertEquals(Tab.ROUTINES, nav.selectedTab)
        assertEquals(Overlay.ActiveLogger, nav.currentOverlay)

        // 3. User taps Back
        val handled = nav.back()
        assertTrue(handled)
        assertNull(nav.currentOverlay)
        // Must return to ROUTINES, not HOME!
        assertEquals(Tab.ROUTINES, nav.selectedTab)

        // 4. Repeat from Library tab
        nav.selectTab(Tab.LIBRARY)
        nav.push(Overlay.ActiveLogger)
        assertEquals(Tab.LIBRARY, nav.selectedTab)
        assertEquals(Overlay.ActiveLogger, nav.currentOverlay)

        assertTrue(nav.back())
        assertNull(nav.currentOverlay)
        assertEquals(Tab.LIBRARY, nav.selectedTab)
    }

    @Test
    fun testFlow48_FinishWorkoutShowsSummaryAndDoneGoesToHome() {
        val nav = AppNavigator(initialTab = Tab.ROUTINES)
        nav.push(Overlay.ActiveLogger)

        // Finish workout: pop ActiveLogger, push Summary
        nav.popOverlay()
        nav.push(Overlay.Summary)
        assertEquals(Overlay.Summary, nav.currentOverlay)

        // On Save & Close / Done: go to Home
        nav.selectTab(Tab.HOME)
        assertNull(nav.currentOverlay)
        assertTrue(nav.overlays.isEmpty())
        assertEquals(Tab.HOME, nav.selectedTab)
    }

    @Test
    fun testFlow49_OpenSettingsFromHomeOrMeReturnsToOriginOnBack() {
        // From Home
        val navHome = AppNavigator(initialTab = Tab.HOME)
        navHome.push(Overlay.Settings)
        assertEquals(Overlay.Settings, navHome.currentOverlay)
        assertTrue(navHome.back())
        assertNull(navHome.currentOverlay)
        assertEquals(Tab.HOME, navHome.selectedTab)

        // From Me
        val navMe = AppNavigator(initialTab = Tab.ME)
        navMe.push(Overlay.Settings)
        assertEquals(Overlay.Settings, navMe.currentOverlay)
        assertTrue(navMe.back())
        assertNull(navMe.currentOverlay)
        assertEquals(Tab.ME, navMe.selectedTab)
    }

    @Test
    fun testMultipleOverlaysPopInLIFOOrder() {
        val nav = AppNavigator()
        nav.push(Overlay.ActiveLogger)
        nav.push(Overlay.Settings)

        assertEquals(2, nav.overlays.size)
        assertEquals(Overlay.Settings, nav.currentOverlay)

        assertTrue(nav.back())
        assertEquals(1, nav.overlays.size)
        assertEquals(Overlay.ActiveLogger, nav.currentOverlay)

        assertTrue(nav.back())
        assertTrue(nav.overlays.isEmpty())
        assertNull(nav.currentOverlay)
        assertEquals(Tab.HOME, nav.selectedTab)
    }

    @Test
    fun testSaverRestoresTabAndOverlays() {
        val nav = AppNavigator(initialTab = Tab.PROGRESS)
        nav.push(Overlay.ActiveLogger)
        nav.push(Overlay.Settings)

        val saverScope = androidx.compose.runtime.saveable.SaverScope { true }
        val saved = with(AppNavigator.Saver) { saverScope.save(nav) } as? List<*>

        assertTrue(saved != null)
        assertEquals("PROGRESS", saved[0])

        val restored = AppNavigator.Saver.restore(saved)
        assertTrue(restored != null)
        assertEquals(Tab.PROGRESS, restored.selectedTab)
        assertEquals(2, restored.overlays.size)
        assertEquals(Overlay.ActiveLogger, restored.overlays[0])
        assertEquals(Overlay.Settings, restored.overlays[1])
    }

    @Test
    fun testSaverGracefullyHandlesInvalidOrEmptyList() {
        val corrupted = listOf("NON_EXISTENT_TAB", listOf("UNKNOWN_OVERLAY"))
        val restored = AppNavigator.Saver.restore(corrupted)

        assertTrue(restored != null)
        assertEquals(Tab.HOME, restored.selectedTab) // Fallback to HOME
        assertTrue(restored.overlays.isEmpty())
    }
}
