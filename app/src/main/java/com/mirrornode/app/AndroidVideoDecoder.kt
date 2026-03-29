package com.mirrornode.app

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.view.Surface

object AndroidVideoDecoder {
    private const val MIME_TYPE = "video/avc"
    private const val DEFAULT_WIDTH = 1920
    private const val DEFAULT_HEIGHT = 1080

    private var codec: MediaCodec? = null
    private var surface: Surface? = null
    private var started = false
    private var configured = false
    private var pendingCsd0: ByteArray? = null
    private var pendingCsd1: ByteArray? = null

    @Synchronized
    fun setSurface(newSurface: Surface?) {
        surface = newSurface
        if (surface == null) {
            releaseCodec()
            configured = false
        } else if (started) {
            configureCodecIfReady()
        }
    }

    @Synchronized
    fun start() {
        started = true
        configureCodecIfReady()
    }

    @Synchronized
    fun stop() {
        started = false
        releaseCodec()
        configured = false
        pendingCsd0 = null
        pendingCsd1 = null
    }

    @Synchronized
    fun flush() {
        runCatching { codec?.flush() }
    }

    @Synchronized
    fun queueSample(data: ByteArray, ptsUs: Long, frameType: Int): Boolean {
        extractCodecConfig(data)
        configureCodecIfReady()

        val activeCodec = codec ?: return false
        if (!configured) {
            return false
        }

        drainOutput(activeCodec)

        val inputIndex = activeCodec.dequeueInputBuffer(0)
        if (inputIndex < 0) {
            return false
        }

        val inputBuffer = activeCodec.getInputBuffer(inputIndex) ?: return false
        inputBuffer.clear()
        if (inputBuffer.capacity() < data.size) {
            activeCodec.queueInputBuffer(inputIndex, 0, 0, ptsUs, 0)
            return false
        }

        inputBuffer.put(data)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && frameType == 1) {
            MediaCodec.BUFFER_FLAG_KEY_FRAME
        } else {
            0
        }
        activeCodec.queueInputBuffer(inputIndex, 0, data.size, ptsUs, flags)
        drainOutput(activeCodec)
        return true
    }

    private fun extractCodecConfig(data: ByteArray) {
        val nalUnits = findNalUnits(data)
        for (nalUnit in nalUnits) {
            val type = nalUnit.firstOrNull()?.toInt()?.and(0x1F) ?: continue
            when (type) {
                7 -> pendingCsd0 = nalUnit
                8 -> pendingCsd1 = nalUnit
            }
        }
    }

    private fun configureCodecIfReady() {
        if (!started || configured) {
            return
        }
        val activeSurface = surface ?: return
        val csd0 = pendingCsd0 ?: return
        val csd1 = pendingCsd1 ?: return

        releaseCodec()

        val newCodec = MediaCodec.createDecoderByType(MIME_TYPE)
        val format = MediaFormat.createVideoFormat(MIME_TYPE, DEFAULT_WIDTH, DEFAULT_HEIGHT).apply {
            setByteBuffer("csd-0", java.nio.ByteBuffer.wrap(csd0))
            setByteBuffer("csd-1", java.nio.ByteBuffer.wrap(csd1))
        }

        newCodec.configure(format, activeSurface, null, 0)
        newCodec.start()
        codec = newCodec
        configured = true
    }

    private fun releaseCodec() {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
    }

    private fun drainOutput(activeCodec: MediaCodec) {
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val outputIndex = activeCodec.dequeueOutputBuffer(bufferInfo, 0)
            when {
                outputIndex >= 0 -> activeCodec.releaseOutputBuffer(outputIndex, true)
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                else -> return
            }
        }
    }

    private fun findNalUnits(data: ByteArray): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        var index = 0
        while (index < data.size - 4) {
            val start = findStartCode(data, index) ?: break
            val nalStart = start.second
            val next = findStartCode(data, nalStart) ?: (0 to data.size)
            val nalEnd = next.second - next.first
            if (nalStart < nalEnd && nalEnd <= data.size) {
                result += data.copyOfRange(nalStart, nalEnd)
            }
            index = nalEnd
        }
        return result
    }

    private fun findStartCode(data: ByteArray, from: Int): Pair<Int, Int>? {
        var i = from
        while (i < data.size - 3) {
            if (data[i] == 0.toByte() && data[i + 1] == 0.toByte()) {
                if (data[i + 2] == 1.toByte()) {
                    return 3 to i + 3
                }
                if (i < data.size - 4 && data[i + 2] == 0.toByte() && data[i + 3] == 1.toByte()) {
                    return 4 to i + 4
                }
            }
            i += 1
        }
        return null
    }
}
