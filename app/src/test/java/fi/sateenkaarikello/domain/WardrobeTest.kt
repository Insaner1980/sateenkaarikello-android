package fi.sateenkaarikello.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WardrobeTest {
    @Test
    fun `eighteen items with three free and one per star`() {
        assertEquals(18, Wardrobe.items.size)
        assertEquals(3, Wardrobe.items.count { it.star == 0 })
        assertEquals(
            (1..15).toList(),
            Wardrobe.items
                .filter { it.star > 0 }
                .map { it.star }
                .sorted(),
        )
        assertEquals(
            18,
            Wardrobe.items
                .map { it.id }
                .toSet()
                .size,
        )
    }

    @Test
    fun `only head and extra are optional`() {
        assertEquals(setOf(Slot.HEAD, Slot.EXTRA), Slot.entries.filter { it.optional }.toSet())
    }

    @Test
    fun `default outfit uses the free items`() {
        val o = Outfit.Default
        assertEquals("hair-letit", o.hair)
        assertEquals("dress-perus", o.dress)
        assertEquals("shoes-tossut", o.shoes)
        assertNull(o.head)
        assertNull(o.extra)
    }

    @Test
    fun `newly unlocked items between star totals`() {
        assertEquals(
            listOf("shoes-punaiset", "hair-pitkat"),
            Wardrobe.newlyUnlocked(before = 2, after = 4).map { it.id },
        )
        assertTrue(Wardrobe.newlyUnlocked(5, 5).isEmpty())
    }

    @Test
    fun `items left counts locked items`() {
        assertEquals(15, Wardrobe.lockedCount(0))
        assertEquals(0, Wardrobe.lockedCount(15))
    }

    @Test
    fun `toggling an optional item takes it off`() {
        val crown = Wardrobe.item("head-kruunu")
        val on = Wardrobe.toggle(Outfit.Default, crown)
        assertEquals("head-kruunu", on.head)
        assertNull(Wardrobe.toggle(on, crown).head)
    }

    @Test
    fun `toggling a required item keeps it on`() {
        val dress = Wardrobe.item("dress-perus")
        assertEquals("dress-perus", Wardrobe.toggle(Outfit.Default, dress).dress)
        val star = Wardrobe.item("dress-tahti")
        assertEquals("dress-tahti", Wardrobe.toggle(Outfit.Default, star).dress)
    }

    @Test
    fun `outfit slot access`() {
        val o = Outfit.Default.with(Slot.EXTRA, "extra-siivet")
        assertEquals("extra-siivet", o[Slot.EXTRA])
        assertEquals("shoes-tossut", o[Slot.SHOES])
        assertFalse(o.isOn(Wardrobe.item("extra-laukku")))
        assertTrue(o.isOn(Wardrobe.item("extra-siivet")))
    }

    @Test
    fun `items by slot keep the web order`() {
        assertEquals(
            listOf("hair-letit", "hair-pitkat", "hair-kiharat", "hair-sateenkaari"),
            Wardrobe.itemsFor(Slot.HAIR).map { it.id },
        )
    }
}
