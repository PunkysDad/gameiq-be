package com.gameiq.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.slf4j.LoggerFactory

@Service
class YoutubeService(
    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {

    @Value("\${youtube.api-key}")
    private lateinit var youtubeApiKey: String

    private val logger = LoggerFactory.getLogger(YoutubeService::class.java)

    fun searchExerciseVideos(query: String): List<YoutubeVideoResult> {
        val url = "https://www.googleapis.com/youtube/v3/search" +
            "?part=snippet" +
            "&q=${java.net.URLEncoder.encode("$query exercise tutorial", Charsets.UTF_8)}" +
            "&type=video" +
            "&maxResults=5" +
            "&order=viewCount" +
            "&key=$youtubeApiKey"

        val response = restTemplate.getForObject(url, String::class.java)
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
