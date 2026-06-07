package com.bublit.app.web

import com.bublit.app.domain.ImageCandidate
import com.bublit.app.domain.ImageCandidateFilter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WebImageExtractor(
    private val filter: ImageCandidateFilter = ImageCandidateFilter(),
) {
    fun parseCandidates(json: String): List<ImageCandidate> {
        val trimmedJson = json.trim()
        if (!trimmedJson.startsWith("[")) return emptyList()

        val rawCandidates = runCatching {
            jsonParser.parseToJsonElement(trimmedJson).jsonArray.mapNotNull { element ->
                val body = element.jsonObject
                val src = body.stringValue("src").trim()
                if (src.isBlank()) {
                    null
                } else {
                    ImageCandidate(
                        url = src,
                        width = body.intValue("width"),
                        height = body.intValue("height"),
                        naturalWidth = body.intValue("naturalWidth").takeIf { it > 0 },
                        naturalHeight = body.intValue("naturalHeight").takeIf { it > 0 },
                        left = body.intValue("left"),
                        top = body.intValue("top"),
                    )
                }
            }
        }.getOrDefault(emptyList())

        return filter.retainComicImages(rawCandidates)
    }

    companion object {
        const val DOM_IMAGE_SCRIPT: String = """
            (function() {
              const images = Array.from(document.images || []);
              return JSON.stringify(images.map(function(img) {
                const rect = img.getBoundingClientRect();
                return {
                  src: img.currentSrc || img.src || img.getAttribute('data-src') || '',
                  width: Math.round(rect.width || img.width || 0),
                  height: Math.round(rect.height || img.height || 0),
                  naturalWidth: img.naturalWidth || 0,
                  naturalHeight: img.naturalHeight || 0,
                  left: Math.round(rect.left + window.scrollX),
                  top: Math.round(rect.top + window.scrollY)
                };
              }));
            })();
        """
    }
}

private val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private fun JsonObject.stringValue(key: String): String {
    return this[key]?.jsonPrimitive?.content.orEmpty()
}

private fun JsonObject.intValue(key: String): Int {
    return this[key]?.jsonPrimitive?.intOrNull ?: 0
}
