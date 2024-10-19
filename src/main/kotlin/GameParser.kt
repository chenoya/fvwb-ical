package be.volley

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+00:00'")
private val DATE_ZONE = ZoneId.of("Europe/Brussels")

class GameParser(private val objectMapper: ObjectMapper) {

    fun parse(json: String): Pair<String, List<Game>> {
        val tree = objectMapper.readTree(json)

        val data = tree.get("pageProps")?.get("initialSections")?.get(0)?.get("data")
            ?: throw IllegalArgumentException("'data' is missing : " + tree.asText())
        val name = data.get("league")?.get("name")?.asText()
            ?: throw IllegalArgumentException("'name' is missing : " + data.asText())
        val games = data.get("games")
            ?: throw IllegalArgumentException("'games' is missing : " + data.asText())

        return Pair(
            name,
            (games as ArrayNode).map {
                Game(
                    parseDate(it),
                    parseTeam(it, "team1"),
                    parseTeam(it, "team2"),
                    parseAddress(it),
                    parseHall(it)
                )
            })
    }

    private fun parseDate(node: JsonNode): Instant {
        val textValue = node.get("start_date")?.asText()?.trim()
            ?: throw IllegalArgumentException("'start_date' is missing : " + node.asText())
        return LocalDateTime.parse(textValue, DATE_FORMAT).atZone(DATE_ZONE).toInstant()
    }

    private fun parseAddress(node: JsonNode): String? {
        val addressStreet = node.get("facility")?.get("venue_address")?.asText()?.trim()
        val addressCode = node.get("facility")?.get("venue_zip")?.asText()?.trim()
        val addressCity = node.get("facility")?.get("venue_city")?.asText()?.trim()

        val cityFull = listOfNotNull(addressCode, addressCity).filterNot(String::isEmpty).joinToString(" ")
        return listOfNotNull(addressStreet, cityFull)
            .filterNot(String::isEmpty)
            .joinToString("\n")
            .ifBlank { null }
    }

    private fun parseTeam(node: JsonNode, nb: String): String? {
        val team = node.get(nb)?.get("name")?.asText()
        val teamSub = node.get(nb)?.get("subname")?.asText()
        return listOfNotNull(team, teamSub)
            .filterNot(String::isEmpty)
            .joinToString(" ")
            .ifBlank { null }
    }

    private fun parseHall(node: JsonNode): String? {
        val addressName = node.get("facility")?.get("name")?.asText()?.trim()
        val addressLat = node.get("facility")?.get("lat")?.asText()?.trim()
        val addressLng = node.get("facility")?.get("lng")?.asText()?.trim()
        return listOfNotNull(addressName, addressLat, addressLng)
            .filterNot(String::isEmpty)
            .joinToString("\n")
            .ifBlank { null }
    }

}