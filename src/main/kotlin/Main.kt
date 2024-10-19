package be.volley

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


fun main() {
    val objectMapper = ObjectMapper()
    val gameParser = GameParser(objectMapper)
    val calendarGenerator = CalendarGenerator()

    val dataSource = objectMapper.readValue(object {}.javaClass.getResource("/data-source.json"),
        object : TypeReference<Map<String, Map<String, Map<String, String>>>>() {})

    val urlMapping = mutableMapOf<String, MutableMap<String, MutableMap<String, MutableMap<String, String>>>>()
    val filenameRegex = Regex("[^0-9a-zA-Z_\\-. ]")

    dataSource.forEach { (year, x) ->
        x.forEach { (prov, y) ->
            y.forEach { (level, url) ->
                println("Working on $year - $prov - $level - $url")

                File("download").mkdirs()
                val urlSplit = url.split("/")
                val dataFile = File("download", urlSplit[urlSplit.size - 1].replace(filenameRegex, ""))

                val json: String
                if (dataFile.exists()) {
                    json = dataFile.readText()
                } else {
                    val request = HttpRequest.newBuilder(URI(url)).GET().build()
                    json = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body()
                    dataFile.writeText(json)
                }

                val (name, games) = gameParser.parse(json)
                val cleanGames = games.filterNot {
                    it.team1?.startsWith("bye", true) ?: false
                            || it.team2?.startsWith("bye", true) ?: false
                            || it.team1?.startsWith("lbye", true) ?: false
                            || it.team2?.startsWith("lbye", true) ?: false
                }

                val teams = cleanGames.mapNotNull { it.team1 }.toSet()
                for (team in teams) {
                    val filteredGames = cleanGames.filter { it.team1 == team || it.team2 == team }

                    val calendarName = "$name - $team".replace(filenameRegex, "")
                    val ical = calendarGenerator.toIcs(level, calendarName, filteredGames)
                    File("docs/ics/$year/$prov/$level").mkdirs()
                    File("docs/ics/$year/$prov/$level/$calendarName.ics").writeText(ical)

                    urlMapping.getOrPut(year) { mutableMapOf() }
                        .getOrPut(prov) { mutableMapOf() }
                        .getOrPut(level) { mutableMapOf() }
                        .put(team, "/ics/$year/$prov/$level/$calendarName.ics")
                }
            }
        }
    }

    File("docs/ics").mkdirs()
    File("docs/ics/mapping.json").writeText(objectMapper.writeValueAsString(urlMapping))

}
