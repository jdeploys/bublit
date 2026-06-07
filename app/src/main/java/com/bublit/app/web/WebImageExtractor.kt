package com.bublit.app.web

import com.bublit.app.domain.ImageCandidate
import com.bublit.app.domain.ImageCandidateFilter

class WebImageExtractor(
    private val filter: ImageCandidateFilter = ImageCandidateFilter(),
) {
    fun parseCandidates(json: String): List<ImageCandidate> {
        if (!json.trimStart().startsWith("[")) return emptyList()

        val rawCandidates = objectPattern.findAll(json).mapNotNull { match ->
            val body = match.groupValues[1]
            val src = stringValue(body, "src").trim()
            if (src.isBlank()) {
                null
            } else {
                ImageCandidate(
                    url = src,
                    width = intValue(body, "width"),
                    height = intValue(body, "height"),
                    naturalWidth = intValue(body, "naturalWidth").takeIf { it > 0 },
                    naturalHeight = intValue(body, "naturalHeight").takeIf { it > 0 },
                    left = intValue(body, "left"),
                    top = intValue(body, "top"),
                )
            }
        }.toList()

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

private val objectPattern = Regex("""\{([^{}]*)}""")

private fun stringValue(body: String, key: String): String {
    val pattern = Regex(""""$key"\s*:\s*"([^"]*)"""")
    return pattern.find(body)?.groupValues?.get(1).orEmpty()
}

private fun intValue(body: String, key: String): Int {
    val pattern = Regex(""""$key"\s*:\s*(-?\d+)""")
    return pattern.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
}
