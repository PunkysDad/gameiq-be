package com.gameiq.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import org.slf4j.LoggerFactory

@Service
class YoutubeService(
    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {

    @Value("\${youtube.api-key}")
    private lateinit var youtubeApiKey: String

    private val logger = LoggerFactory.getLogger(YoutubeService::class.java)

    fun cleanSearchQuery(query: String): String {
        return query
            .replace(Regex("""^\d+\.\s*"""), "")
            .replace(Regex("""\s*\(\d+\s+seconds?\)"""), "")
            .trim()
    }

    fun searchExerciseVideos(query: String): List<YoutubeVideoResult> {
        val cleanedQuery = cleanSearchQuery(query)

        val uri = UriComponentsBuilder
            .fromHttpUrl("https://www.googleapis.com/youtube/v3/search")
            .queryParam("part", "snippet")
            .queryParam("q", "$cleanedQuery exercise tutorial")
            .queryParam("type", "video")
            .queryParam("maxResults", 5)
            .queryParam("order", "viewCount")
            .queryParam("key", youtubeApiKey)
            .build()
            .toUri()

        val response = restTemplate.getForObject(uri, String::class.java)
            ?: return emptyList()

        val root = objectMapper.readTree(response)
        val items = root.get("items") ?: return emptyList()

        return items.mapNotNull { item ->
            val videoId = item.get("id")?.get("videoId")?.asText() ?: return@mapNotNull null
            val snippet = item.get("snippet") ?: return@mapNotNull null
            val title = snippet.get("title")?.asText() ?: return@mapNotNull null
            val channelName = snippet.get("channelTitle")?.asText() ?: ""
            val thumbnails = snippet.get("thumbnails")
            val thumbnail = thumbnails?.get("high")?.get("url")?.asText()
                ?: thumbnails?.get("medium")?.get("url")?.asText()
                ?: ""

            YoutubeVideoResult(
                videoId = videoId,
                title = title,
                thumbnail = thumbnail,
                channelName = channelName
            )
        }
    }
}
