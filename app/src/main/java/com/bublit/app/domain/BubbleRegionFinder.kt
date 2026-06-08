package com.bublit.app.domain

import kotlin.math.ceil

class BubbleRegionFinder(
    private val minBrightLuma: Double = 0.72,
    private val minRegionWidthPx: Int = 32,
    private val minRegionHeightPx: Int = 24,
) {
    fun find(
        imageWidth: Int,
        imageHeight: Int,
        seedBounds: BubbleBounds,
        lumaAt: (x: Int, y: Int) -> Double,
    ): BubbleRegionCandidate? {
        if (imageWidth <= 0 || imageHeight <= 0 || seedBounds.width <= 0 || seedBounds.height <= 0) {
            return null
        }

        val searchBounds = seedBounds.expandedSearchBounds(imageWidth, imageHeight)
        val visited = BooleanArray(searchBounds.width * searchBounds.height)
        var best: RegionAccumulator? = null

        for (y in searchBounds.top until searchBounds.bottom) {
            for (x in searchBounds.left until searchBounds.right) {
                val localIndex = searchBounds.localIndex(x, y)
                if (visited[localIndex] || lumaAt(x, y) < minBrightLuma) continue

                val region = floodFillBrightRegion(
                    startX = x,
                    startY = y,
                    searchBounds = searchBounds,
                    seedBounds = seedBounds,
                    visited = visited,
                    lumaAt = lumaAt,
                )

                if (region.isUsable(seedBounds) && (best == null || region.area > best.area)) {
                    best = region
                }
            }
        }

        return best?.toCandidate()
    }

    private fun floodFillBrightRegion(
        startX: Int,
        startY: Int,
        searchBounds: BubbleBounds,
        seedBounds: BubbleBounds,
        visited: BooleanArray,
        lumaAt: (x: Int, y: Int) -> Double,
    ): RegionAccumulator {
        val queueX = IntArray(searchBounds.width * searchBounds.height)
        val queueY = IntArray(searchBounds.width * searchBounds.height)
        var readIndex = 0
        var writeIndex = 0
        val region = RegionAccumulator()

        queueX[writeIndex] = startX
        queueY[writeIndex] = startY
        writeIndex++
        visited[searchBounds.localIndex(startX, startY)] = true

        while (readIndex < writeIndex) {
            val x = queueX[readIndex]
            val y = queueY[readIndex]
            readIndex++
            val luma = lumaAt(x, y)

            region.add(x, y, luma, seedBounds)

            listOf(
                x - 1 to y,
                x + 1 to y,
                x to y - 1,
                x to y + 1,
            ).forEach { (nextX, nextY) ->
                if (nextX !in searchBounds.left until searchBounds.right ||
                    nextY !in searchBounds.top until searchBounds.bottom
                ) {
                    return@forEach
                }
                val nextIndex = searchBounds.localIndex(nextX, nextY)
                if (visited[nextIndex] || lumaAt(nextX, nextY) < minBrightLuma) return@forEach

                visited[nextIndex] = true
                queueX[writeIndex] = nextX
                queueY[writeIndex] = nextY
                writeIndex++
            }
        }

        return region
    }

    private fun RegionAccumulator.isUsable(seedBounds: BubbleBounds): Boolean {
        return width >= minRegionWidthPx &&
            height >= minRegionHeightPx &&
            bounds.intersects(seedBounds)
    }

    private fun BubbleBounds.expandedSearchBounds(imageWidth: Int, imageHeight: Int): BubbleBounds {
        val horizontalPadding = maxOf(48, ceil(width * 3.0).toInt())
        val verticalPadding = maxOf(48, ceil(height * 1.5).toInt())
        val left = (this.left - horizontalPadding).coerceAtLeast(0)
        val top = (this.top - verticalPadding).coerceAtLeast(0)
        val right = (this.right + horizontalPadding).coerceAtMost(imageWidth)
        val bottom = (this.bottom + verticalPadding).coerceAtMost(imageHeight)
        return BubbleBounds(left = left, top = top, width = right - left, height = bottom - top)
    }

    private fun BubbleBounds.localIndex(x: Int, y: Int): Int {
        return (y - top) * width + (x - left)
    }

    private class RegionAccumulator {
        var left: Int = Int.MAX_VALUE
            private set
        var top: Int = Int.MAX_VALUE
            private set
        var right: Int = Int.MIN_VALUE
            private set
        var bottom: Int = Int.MIN_VALUE
            private set
        var area: Int = 0
            private set
        private var lumaTotal: Double = 0.0

        val width: Int
            get() = right - left + 1

        val height: Int
            get() = bottom - top + 1

        val bounds: BubbleBounds
            get() = BubbleBounds(left = left, top = top, width = width, height = height)

        fun add(x: Int, y: Int, luma: Double, seedBounds: BubbleBounds) {
            left = minOf(left, x)
            top = minOf(top, y)
            right = maxOf(right, x)
            bottom = maxOf(bottom, y)
            area++
            lumaTotal += luma
        }

        fun toCandidate(): BubbleRegionCandidate {
            return BubbleRegionCandidate(
                bounds = bounds,
                backgroundLuma = if (area == 0) 0.0 else lumaTotal / area,
            )
        }
    }
}

private fun BubbleBounds.intersects(other: BubbleBounds): Boolean {
    return left < other.right &&
        right > other.left &&
        top < other.bottom &&
        bottom > other.top
}
