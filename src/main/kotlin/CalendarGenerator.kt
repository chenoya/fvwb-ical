package be.volley

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import biweekly.property.Method
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.UUID

private const val PRODUCT_ID = "-//chenoya//FVWB//FR"
private const val MATCH_DURATION = 2 * 60 * 60L
private const val CALENDAR_NAME_PROP = "X-WR-CALNAME"
private const val CALENDAR_COLOR_PROP = "X-APPLE-CALENDAR-COLOR"
private const val CALENDAR_COLOR_VALUE = "#23BEFC"
private val TIMESTAMP = LocalDate.of(2024, 9, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()

class CalendarGenerator {

    fun toIcs(summaryPrefix: String, name: String, games: List<Game>): String {
        val ical = ICalendar()
        ical.setProductId(PRODUCT_ID)
        ical.setMethod(Method.publish())
        ical.setName(name)
        ical.setExperimentalProperty(CALENDAR_NAME_PROP, name)
        ical.setExperimentalProperty(CALENDAR_COLOR_PROP, CALENDAR_COLOR_VALUE)

        for (game in games) {
            val event = VEvent()
            event.setSummary("$summaryPrefix - ${game.team1 ?: '?'} - ${game.team2 ?: '?'}")
            event.setDateStart(Date.from(game.date))
            event.setDateEnd(Date.from(game.date.plusSeconds(MATCH_DURATION)))
            game.hall.let { event.setDescription(it) }
            game.address.let { event.setLocation(it) }

            // uid from hashcode of game info and fixed timestamp to have reproducible ics generation
            event.setUid(UUID.nameUUIDFromBytes(game.hashCode().toBigInteger().toByteArray()).toString())
            event.setDateTimeStamp(Date.from(TIMESTAMP))

            ical.addEvent(event)
        }

        return Biweekly.write(ical).go()
    }

}
