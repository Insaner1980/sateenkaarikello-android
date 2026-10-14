package fi.sateenkaarikello.ui

import fi.sateenkaarikello.domain.Wardrobe
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemNamesTest {
    @Test
    fun `every wardrobe item has a distinct name resource`() {
        val ids = Wardrobe.items.map { itemName(it.id) }
        assertEquals(Wardrobe.items.size, ids.toSet().size)
    }
}
