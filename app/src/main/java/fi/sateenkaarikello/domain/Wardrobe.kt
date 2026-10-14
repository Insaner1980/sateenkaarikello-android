package fi.sateenkaarikello.domain

/** Nuken vaatepaikat. Päähän ja Lisäksi voi jättää tyhjiksi. */
enum class Slot(
    val optional: Boolean,
) {
    HAIR(false),
    DRESS(false),
    SHOES(false),
    HEAD(true),
    EXTRA(true),
}

/** Vaate. [star] on kokonaistähtimäärä, jolla se avautuu (0 = heti). Nimi on strings.xml:ssä. */
data class Item(
    val id: String,
    val slot: Slot,
    val star: Int,
)

data class Outfit(
    val hair: String,
    val dress: String,
    val shoes: String,
    val head: String? = null,
    val extra: String? = null,
) {
    operator fun get(slot: Slot): String? =
        when (slot) {
            Slot.HAIR -> hair
            Slot.DRESS -> dress
            Slot.SHOES -> shoes
            Slot.HEAD -> head
            Slot.EXTRA -> extra
        }

    /** Vaihtaa paikan vaatteen. Pakollista paikkaa ei voi tyhjentää. */
    fun with(
        slot: Slot,
        id: String?,
    ): Outfit =
        when (slot) {
            Slot.HAIR -> copy(hair = id ?: hair)
            Slot.DRESS -> copy(dress = id ?: dress)
            Slot.SHOES -> copy(shoes = id ?: shoes)
            Slot.HEAD -> copy(head = id)
            Slot.EXTRA -> copy(extra = id)
        }

    fun isOn(item: Item): Boolean = this[item.slot] == item.id

    companion object {
        val Default = Outfit(hair = "hair-letit", dress = "dress-perus", shoes = "shoes-tossut")
    }
}

object Wardrobe {
    const val MAX_STARS = 15

    val items: List<Item> =
        listOf(
            Item("hair-letit", Slot.HAIR, 0),
            Item("dress-perus", Slot.DRESS, 0),
            Item("shoes-tossut", Slot.SHOES, 0),
            Item("dress-pilkku", Slot.DRESS, 1),
            Item("head-rusetti", Slot.HEAD, 2),
            Item("shoes-punaiset", Slot.SHOES, 3),
            Item("hair-pitkat", Slot.HAIR, 4),
            Item("dress-tahti", Slot.DRESS, 5),
            Item("head-kukat", Slot.HEAD, 6),
            Item("extra-laukku", Slot.EXTRA, 7),
            Item("shoes-saappaat", Slot.SHOES, 8),
            Item("hair-kiharat", Slot.HAIR, 9),
            Item("dress-prinsessa", Slot.DRESS, 10),
            Item("head-kruunu", Slot.HEAD, 11),
            Item("extra-siivet", Slot.EXTRA, 12),
            Item("shoes-kimalle", Slot.SHOES, 13),
            Item("hair-sateenkaari", Slot.HAIR, 14),
            Item("dress-sateenkaari", Slot.DRESS, 15),
        )

    private val byId = items.associateBy { it.id }

    fun item(id: String): Item = byId.getValue(id)

    fun itemsFor(slot: Slot): List<Item> = items.filter { it.slot == slot }

    fun newlyUnlocked(
        before: Int,
        after: Int,
    ): List<Item> = items.filter { it.star in (before + 1)..after }

    fun lockedCount(totalStars: Int): Int = items.count { it.star > totalStars }

    /** Painallus pukee vaatteen; vapaaehtoisen paikan vaate lähtee pois toisella painalluksella. */
    fun toggle(
        outfit: Outfit,
        item: Item,
    ): Outfit =
        if (outfit.isOn(item) &&
            item.slot.optional
        ) {
            outfit.with(item.slot, null)
        } else {
            outfit.with(item.slot, item.id)
        }
}
