package fi.sateenkaarikello.ui.lesson

import fi.sateenkaarikello.R
import fi.sateenkaarikello.domain.Choice
import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.Hand
import fi.sateenkaarikello.domain.Lesson
import fi.sateenkaarikello.domain.LessonPage
import fi.sateenkaarikello.domain.Mark
import fi.sateenkaarikello.domain.MarkColor
import fi.sateenkaarikello.domain.Quiz
import fi.sateenkaarikello.domain.Text
import fi.sateenkaarikello.domain.demo

private fun r(id: Int) = Text.Res(id)

private fun time(minutes: Int) = Text.Time(minutes)

private fun minutes(n: Int) = Text.Plural(R.plurals.minutes_count, n)

private fun num(n: Int) = Mark.NumberGlow(n)

private fun tick(m: Int) = Mark.TickGlow(m)

private fun bubble(m: Int) = Mark.Bubble(m, m.toString())

private fun sector(
    from: Int,
    to: Int,
    color: MarkColor,
) = Mark.Sector(from, to, color)

private fun choices(vararg ids: Int) = ids.mapIndexed { i, id -> Choice(r(id), correct = i == 0) }

private val Half = sector(0, 30, MarkColor.HALF)
private const val DAY = ClockTime.DAY

/**
 * Kellokoulun 9 oppituntia web-version LESSONS-listasta. Tekstit ovat strings.xml:ssä;
 * animaatiot ovat samat askeleet kuin webin async-demoissa. Ensimmäinen vaihtoehto on oikea
 * (näytettäessä järjestys sekoitetaan).
 */
object LessonCatalog {
    val all: List<Lesson> =
        listOf(
            Lesson(
                id = "viisarit",
                title = r(R.string.l_viisarit_title),
                description = r(R.string.l_viisarit_desc),
                pages =
                    listOf(
                        LessonPage(
                            240,
                            listOf(r(R.string.l_viisarit_p1a), r(R.string.l_viisarit_p1b)),
                            demo {
                                pause(700)
                                highlight(Hand.HOUR)
                            },
                        ),
                        LessonPage(
                            240,
                            listOf(r(R.string.l_viisarit_p2a)),
                            demo {
                                pause(700)
                                highlight(Hand.MINUTE)
                            },
                        ),
                        LessonPage(
                            240,
                            listOf(r(R.string.l_viisarit_p3a), r(R.string.l_viisarit_p3b)),
                            demo {
                                val marks = mutableListOf<Mark>()
                                for (n in 1..12) {
                                    marks += num(n)
                                    under(marks)
                                    pause(450)
                                }
                            },
                        ),
                    ),
                quiz =
                    Quiz(
                        240,
                        r(R.string.l_viisarit_q),
                        choices(R.string.l_viisarit_c1, R.string.l_viisarit_c2),
                        r(R.string.l_viisarit_hint),
                    ),
            ),
            Lesson(
                id = "kierros",
                title = r(R.string.l_kierros_title),
                description = r(R.string.l_kierros_desc),
                pages =
                    listOf(
                        LessonPage(
                            180,
                            listOf(r(R.string.l_kierros_p1a), r(R.string.l_kierros_p1b)),
                            demo {
                                say(r(R.string.l_kierros_say_start))
                                pause(1500)
                                say(r(R.string.l_kierros_say_go))
                                move(240, 6000, exact = true)
                                pause(6200)
                                say(r(R.string.l_kierros_say_end))
                            },
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_kierros_p2a), r(R.string.l_kierros_p2b)),
                            demo {
                                highlight(Hand.HOUR)
                                under(num(3))
                                pause(1000)
                                move(240, 6000, exact = true)
                                pause(6200)
                                under(num(3), num(4))
                                say(r(R.string.l_kierros_say_moved))
                            },
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_kierros_p3a), r(R.string.l_kierros_p3b)),
                            demo {
                                pause(500)
                                move(240, 6000, exact = true)
                                val marks = mutableListOf<Mark>()
                                for (m in 1..60) {
                                    marks += tick(m % 60)
                                    under(marks)
                                    say(minutes(m))
                                    pause(100)
                                }
                                say(r(R.string.l_kierros_say_hour))
                            },
                        ),
                    ),
                quiz =
                    Quiz(
                        180,
                        r(R.string.l_kierros_q),
                        choices(R.string.l_kierros_c1, R.string.l_kierros_c2, R.string.l_kierros_c3),
                        r(R.string.l_kierros_hint),
                    ),
            ),
            Lesson(
                id = "tasan",
                title = r(R.string.l_tasan_title),
                description = r(R.string.l_tasan_desc),
                pages =
                    listOf(
                        LessonPage(
                            180,
                            listOf(r(R.string.l_tasan_p1a)),
                            demo {
                                pause(600)
                                highlight(Hand.MINUTE)
                                under(num(12))
                            },
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_tasan_p2a), r(R.string.l_tasan_p2b)),
                            demo {
                                pause(600)
                                highlight(Hand.HOUR)
                                under(num(3))
                                pause(800)
                                say(time(180))
                            },
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_tasan_p3a), r(R.string.l_tasan_p3b)),
                            demo {
                                for (h in listOf(7, 9, 12, 5)) {
                                    under()
                                    say(null)
                                    move((h % 12) * 60, 1200)
                                    pause(1400)
                                    under(num(h))
                                    say(time((h % 12) * 60))
                                    pause(1700)
                                }
                            },
                        ),
                    ),
                quiz =
                    Quiz(
                        540,
                        r(R.string.l_tasan_q),
                        choices(R.string.l_tasan_c1, R.string.l_tasan_c2, R.string.l_tasan_c3),
                        r(R.string.l_tasan_hint),
                    ),
            ),
            Lesson(
                id = "puoli",
                title = r(R.string.l_puoli_title),
                description = r(R.string.l_puoli_desc),
                pages =
                    listOf(
                        LessonPage(
                            180,
                            listOf(r(R.string.l_puoli_p1a), r(R.string.l_puoli_p1b)),
                            demo {
                                pause(800)
                                move(210, 3500, exact = true)
                                pause(3700)
                                under(Half)
                                say(r(R.string.l_puoli_say_half))
                            },
                        ),
                        LessonPage(
                            210,
                            listOf(r(R.string.l_puoli_p2a), r(R.string.l_puoli_p2b)),
                            demo {
                                pause(600)
                                highlight(Hand.HOUR)
                                under(Half, num(3), num(4))
                            },
                            under = listOf(Half),
                        ),
                        LessonPage(
                            210,
                            listOf(r(R.string.l_puoli_p3a), r(R.string.l_puoli_p3b)),
                            demo {
                                pause(600)
                                under(Half, num(4))
                                say(r(R.string.l_puoli_say_digits))
                            },
                            under = listOf(Half),
                        ),
                        LessonPage(
                            210,
                            listOf(r(R.string.l_puoli_p4a), r(R.string.l_puoli_p4b)),
                            demo {
                                pause(600)
                                move(450, 1500)
                                pause(1700)
                                under(Half, num(7), num(8))
                                say(time(450))
                            },
                            under = listOf(Half),
                        ),
                    ),
                quiz =
                    Quiz(
                        330,
                        r(R.string.l_puoli_q),
                        choices(R.string.l_puoli_c1, R.string.l_puoli_c2, R.string.l_puoli_c3),
                        r(R.string.l_puoli_hint),
                    ),
            ),
            Lesson(
                id = "vartti",
                title = r(R.string.l_vartti_title),
                description = r(R.string.l_vartti_desc),
                pages =
                    listOf(
                        LessonPage(
                            180,
                            listOf(r(R.string.l_vartti_p1a), r(R.string.l_vartti_p1b)),
                            demo {
                                val colors = listOf(MarkColor.RED, MarkColor.HALF, MarkColor.GREEN, MarkColor.BLUE)
                                val marks = mutableListOf<Mark>()
                                pause(400)
                                for (k in 0 until 4) {
                                    marks += sector(k * 15, k * 15 + 15, colors[k])
                                    under(marks)
                                    say(Text.Res(R.string.say_quarter, listOf(k + 1)))
                                    pause(1000)
                                }
                                say(r(R.string.l_vartti_say_four))
                            },
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_vartti_p2a), r(R.string.l_vartti_p2b)),
                            demo {
                                pause(600)
                                move(195, 2500, exact = true)
                                pause(2700)
                                under(sector(0, 15, MarkColor.HALF), num(3))
                                say(time(195))
                            },
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_vartti_p3a), r(R.string.l_vartti_p3b)),
                            demo {
                                pause(600)
                                move(225, 4000, exact = true)
                                pause(4200)
                                under(sector(45, 60, MarkColor.BLUE), num(9))
                                say(time(225))
                            },
                        ),
                    ),
                quiz =
                    Quiz(
                        405,
                        r(R.string.l_vartti_q),
                        choices(R.string.l_vartti_c1, R.string.l_vartti_c2, R.string.l_vartti_c3),
                        r(R.string.l_vartti_hint),
                    ),
            ),
            Lesson(
                id = "ylivaille",
                title = r(R.string.l_ylivaille_title),
                description = r(R.string.l_ylivaille_desc),
                pages =
                    listOf(
                        LessonPage(
                            180,
                            listOf(r(R.string.l_ylivaille_p1a), r(R.string.l_ylivaille_p1b)),
                            demo {
                                pause(700)
                                move(200, 2500, exact = true)
                                pause(2700)
                                say(time(200))
                            },
                            under = listOf(sector(0, 30, MarkColor.YLI)),
                        ),
                        LessonPage(
                            200,
                            listOf(r(R.string.l_ylivaille_p2a), r(R.string.l_ylivaille_p2b)),
                            demo {
                                pause(700)
                                move(220, 2500, exact = true)
                                pause(2700)
                                say(time(220))
                            },
                            under = listOf(sector(30, 60, MarkColor.VAILLE)),
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_ylivaille_p3a), r(R.string.l_ylivaille_p3b)),
                            demo {
                                for (t in listOf(190, 230, 185, 235)) {
                                    say(null)
                                    move(t, 1200)
                                    pause(1400)
                                    say(time(t))
                                    pause(1600)
                                }
                            },
                            under = listOf(sector(0, 30, MarkColor.YLI), sector(30, 60, MarkColor.VAILLE)),
                        ),
                    ),
                quiz =
                    Quiz(
                        250,
                        r(R.string.l_ylivaille_q),
                        choices(R.string.l_ylivaille_c1, R.string.l_ylivaille_c2, R.string.l_ylivaille_c3),
                        r(R.string.l_ylivaille_hint),
                    ),
            ),
            Lesson(
                id = "viisi",
                title = r(R.string.l_viisi_title),
                description = r(R.string.l_viisi_desc),
                pages =
                    listOf(
                        LessonPage(
                            180,
                            listOf(r(R.string.l_viisi_p1a), r(R.string.l_viisi_p1b)),
                            demo {
                                val nums = mutableListOf<Mark>()
                                val bubbles = mutableListOf<Mark>()
                                pause(400)
                                for (n in 1..12) {
                                    nums += num(n)
                                    bubbles += bubble(n * 5)
                                    under(nums)
                                    over(bubbles)
                                    say(Text.Res(R.string.say_number_minutes, listOf(n, n * 5)))
                                    pause(800)
                                }
                            },
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_viisi_p2a), r(R.string.l_viisi_p2b)),
                            demo {
                                pause(500)
                                val bubbles = mutableListOf<Mark>()
                                for (k in 1..4) {
                                    move(180 + k * 5, 900, exact = true)
                                    pause(1000)
                                    bubbles += bubble(k * 5)
                                    over(bubbles)
                                    say(Text.Plain((k * 5).toString()))
                                    pause(500)
                                }
                                say(time(200))
                            },
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_viisi_p3a), r(R.string.l_viisi_p3b)),
                            demo {
                                pause(500)
                                move(205, 2000)
                                pause(2200)
                                under(num(5))
                                say(time(205))
                                pause(2400)
                                move(215, 1500, exact = true)
                                pause(1700)
                                under(num(7))
                                say(time(215))
                            },
                        ),
                    ),
                quiz =
                    Quiz(
                        520,
                        r(R.string.l_viisi_q),
                        choices(R.string.l_viisi_c1, R.string.l_viisi_c2, R.string.l_viisi_c3),
                        r(R.string.l_viisi_hint),
                    ),
            ),
            Lesson(
                id = "minuutit",
                title = r(R.string.l_minuutit_title),
                description = r(R.string.l_minuutit_desc),
                pages =
                    listOf(
                        LessonPage(
                            180,
                            listOf(r(R.string.l_minuutit_p1a), r(R.string.l_minuutit_p1b)),
                            demo {
                                pause(500)
                                val marks = mutableListOf<Mark>()
                                for (m in 1..5) {
                                    move(180 + m, 600, exact = true)
                                    pause(700)
                                    marks += tick(m)
                                    under(marks)
                                    say(minutes(m))
                                    pause(400)
                                }
                            },
                        ),
                        LessonPage(
                            190,
                            listOf(r(R.string.l_minuutit_p2a), r(R.string.l_minuutit_p2b)),
                            demo {
                                val marks = mutableListOf<Mark>(num(2))
                                under(marks)
                                say(minutes(10))
                                pause(1400)
                                for (m in listOf(11, 12)) {
                                    move(180 + m, 600, exact = true)
                                    pause(800)
                                    marks += tick(m)
                                    under(marks)
                                    say(minutes(m))
                                    pause(600)
                                }
                                say(time(192))
                            },
                        ),
                    ),
                quiz =
                    Quiz(
                        187,
                        r(R.string.l_minuutit_q),
                        choices(R.string.l_minuutit_c1, R.string.l_minuutit_c2, R.string.l_minuutit_c3),
                        r(R.string.l_minuutit_hint),
                    ),
            ),
            Lesson(
                id = "vrk",
                title = r(R.string.l_vrk_title),
                description = r(R.string.l_vrk_desc),
                h24Only = true,
                pages =
                    listOf(
                        LessonPage(
                            0,
                            listOf(r(R.string.l_vrk_p1a), r(R.string.l_vrk_p1b)),
                            demo {
                                pause(800)
                                highlight(Hand.HOUR)
                                say(r(R.string.l_vrk_say_first))
                                move(720, 5000, exact = true)
                                pause(5400)
                                say(r(R.string.l_vrk_say_second))
                                move(0, 5000, exact = true)
                                pause(5400)
                                say(r(R.string.l_vrk_say_day))
                            },
                            period = DAY,
                            help = true,
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_vrk_p2a), r(R.string.l_vrk_p2b)),
                            demo {
                                pause(600)
                                under(num(3))
                                say(r(R.string.l_vrk_say_night))
                                pause(2200)
                                move(900, 3000, exact = true)
                                pause(3200)
                                say(r(R.string.l_vrk_say_sum))
                            },
                            period = DAY,
                            help = true,
                        ),
                        LessonPage(
                            180,
                            listOf(r(R.string.l_vrk_p3a), r(R.string.l_vrk_p3b)),
                            demo {
                                say(r(R.string.l_vrk_say_night))
                                pause(2600)
                                move(900, 2000, exact = true)
                                pause(2200)
                                say(r(R.string.l_vrk_say_afternoon))
                            },
                            period = DAY,
                            help = true,
                        ),
                    ),
                quiz =
                    Quiz(
                        1140,
                        r(R.string.l_vrk_q),
                        choices(R.string.l_vrk_c1, R.string.l_vrk_c2, R.string.l_vrk_c3),
                        r(R.string.l_vrk_hint),
                        period = DAY,
                    ),
            ),
        )

    private val byId = all.associateBy { it.id }

    fun lesson(id: String): Lesson = byId.getValue(id)
}
