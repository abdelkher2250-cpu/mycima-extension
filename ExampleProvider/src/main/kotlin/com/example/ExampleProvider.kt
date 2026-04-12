package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Document

class ExampleProvider : MainAPI() {
    // 1. Basic Site Configuration
    override var mainUrl = "https://w20.my-cima.net"
    override var name = "MyCima"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    // 2 & 3. Blank for now so the app compiles safely
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        return HomePageResponse(emptyList()) 
    }
    override suspend fun search(query: String): List<SearchResponse> {
        return emptyList()
    }

    // 4. Load the page (we will build this out later)
    override suspend fun load(url: String): LoadResponse {
        // For testing, we are just throwing an error if clicked directly
        throw NotImplementedError("Load not fully implemented yet") 
    }

    // 5. The Video Extractor we built!
    override suspend fun loadLinks(
        data: String, 
        isCasting: Boolean, 
        subtitleCallback: (SubtitleFile) -> Unit, 
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // Scenario A: Direct Video Sources
        val sourceTags = document.select("video source")
        for (source in sourceTags) {
            val videoUrl = source.attr("src")
            if (videoUrl.isNotBlank()) {
                val isM3u8 = videoUrl.contains(".m3u8")
                callback.invoke(
                    ExtractorLink(
                        name = this.name,
                        name = this.name,
                        url = videoUrl,
                        referer = mainUrl,
                        quality = Qualities.Unknown.value,
                        isM3u8 = isM3u8
                    )
                )
            }
        }

        // Scenario B: Embedded iFrames (External Hosts)
        val iframeTags = document.select("iframe")
        for (iframe in iframeTags) {
            val iframeUrl = iframe.attr("src")
            if (iframeUrl.startsWith("http")) {
                loadExtractor(iframeUrl, mainUrl, subtitleCallback, callback)
            }
        }

        return true
    }
}
