package com.sounddeck

import com.sounddeck.core.model.Manifest
import com.sounddeck.core.model.PageConfig
import com.sounddeck.ui.IconRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageManagementAndIconTest {

    @Test
    fun testIconRegistryResolvesKnownIcons() {
        assertNotNull(IconRegistry.resolveIcon("volume_up"))
        assertNotNull(IconRegistry.resolveIcon("mic"))
        assertNotNull(IconRegistry.resolveIcon("videocam"))
        assertNotNull(IconRegistry.resolveIcon("celebration"))
        assertNotNull(IconRegistry.resolveIcon("sports_esports"))
        assertNotNull(IconRegistry.resolveIcon("play"))
    }

    @Test
    fun testIconRegistryAliases() {
        assertNotNull(IconRegistry.resolveIcon("speaker"))
        assertNotNull(IconRegistry.resolveIcon("mute"))
        assertNotNull(IconRegistry.resolveIcon("gamepad"))
        assertNotNull(IconRegistry.resolveIcon("fire"))
    }

    @Test
    fun testIconRegistryUnknownReturnsNull() {
        assertNull(IconRegistry.resolveIcon("non_existent_icon_xyz"))
        assertNull(IconRegistry.resolveIcon(null))
        assertNull(IconRegistry.resolveIcon(""))
    }

    @Test
    fun testCategoriesAvailable() {
        assertTrue(IconRegistry.categories.contains(IconRegistry.CATEGORY_ALL))
        assertTrue(IconRegistry.categories.contains(IconRegistry.CATEGORY_AUDIO))
        assertTrue(IconRegistry.categories.contains(IconRegistry.CATEGORY_STREAM))
        assertTrue(IconRegistry.categories.contains(IconRegistry.CATEGORY_ALERTS))
        assertTrue(IconRegistry.categories.contains(IconRegistry.CATEGORY_CONTROLS))
    }

    @Test
    fun testPageReorderingLogic() {
        val page1 = PageConfig(id = "1", name = "P1", gridRows = 3, gridCols = 4, pads = emptyList())
        val page2 = PageConfig(id = "2", name = "P2", gridRows = 3, gridCols = 4, pads = emptyList())
        val page3 = PageConfig(id = "3", name = "P3", gridRows = 3, gridCols = 4, pads = emptyList())

        val pages = mutableListOf(page1, page2, page3)
        // Move page3 to index 0
        val moved = pages.removeAt(2)
        pages.add(0, moved)

        assertEquals("3", pages[0].id)
        assertEquals("1", pages[1].id)
        assertEquals("2", pages[2].id)
    }

    @Test
    fun testPageRenamingLogic() {
        val page1 = PageConfig(id = "1", name = "Original", gridRows = 3, gridCols = 4, pads = emptyList())
        val updated = page1.copy(name = "Renamed Page")
        assertEquals("Renamed Page", updated.name)
    }
}
