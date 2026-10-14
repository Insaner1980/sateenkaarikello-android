package fi.sateenkaarikello.domain

/**
 * Lapsen edistyminen. "Aloita alusta" korvaa tämän oletusarvoilla, mutta [Settings] säilyy.
 * [introShown] sisältää tasot ("0"–"4") ja "h24", joiden oppitunnit on jo näytetty automaattisesti.
 */
data class Progress(
    val stars: List<Int> = List(LEVEL_COUNT) { 0 },
    val challengeBest: Int = 0,
    val outfit: Outfit = Outfit.Default,
    val dollName: String = "",
    val lessonsDone: Set<String> = emptySet(),
    val introShown: Set<String> = emptySet(),
) {
    val totalStars: Int get() = stars.sum()

    /** Tallentaa tason tuloksen, jos se on parempi kuin aiempi. */
    fun withLevelResult(
        level: Int,
        stars: Int,
    ): Progress =
        if (stars >
            this.stars[level]
        ) {
            copy(stars = this.stars.toMutableList().also { it[level] = stars })
        } else {
            this
        }

    fun withChallengeScore(score: Int): Progress = if (score > challengeBest) copy(challengeBest = score) else this

    fun withLessonDone(id: String): Progress = copy(lessonsDone = lessonsDone + id)

    companion object {
        const val LEVEL_COUNT = 5
    }
}

/** Asetukset, jotka säilyvät "Aloita alusta" -toiminnossa. Oletus on 12 h. */
data class Settings(
    val sound: Boolean = true,
    val help: Boolean = true,
    val h24: Boolean = false,
)
