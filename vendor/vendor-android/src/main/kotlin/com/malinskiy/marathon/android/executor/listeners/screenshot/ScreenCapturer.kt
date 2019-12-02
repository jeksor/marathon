package com.malinskiy.marathon.android.executor.listeners.screenshot

import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.RawImage
import com.android.ddmlib.TimeoutException
import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.awt.image.RenderedImage
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.imageio.stream.FileImageOutputStream
import kotlin.system.measureTimeMillis


class ScreenCapturer(
    val device: AndroidDevice,
    private val attachmentManager: AttachmentManager,
    val test: Test
) {

    var attachment: Attachment? = null

    suspend fun start() = coroutineScope {
        var isWritten = false
        val attachment = attachmentManager.createAttachment(
            FileType.SCREENSHOT,
            AttachmentType.SCREENSHOT
        )
        val outputStream = FileImageOutputStream(attachment.file)
        val writer = GifSequenceWriter(outputStream, TYPE_INT_ARGB, DELAY, true)
        while (isActive) {
            val capturingTimeMillis = measureTimeMillis {
                getScreenshot()?.let {
                    writer.writeToSequence(it)
                    isWritten = true
                }
            }
            val sleepTimeMillis = when {
                (DELAY - capturingTimeMillis) < 0 -> 0
                else -> DELAY - capturingTimeMillis
            }
            delay(sleepTimeMillis)
        }
        writer.close()
        outputStream.close()

        if (isWritten) {
            this@ScreenCapturer.attachment = attachment
        }
    }

    private fun getScreenshot(): RenderedImage? {
        return try {
            val screenshot = device.ddmsDevice.getScreenshot(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            val bufferedImage = bufferedImageFrom(rawImage = screenshot)
            Scalr.resize(bufferedImage, Scalr.Method.SPEED, Scalr.Mode.AUTOMATIC, 1280, 720)
        } catch (e: TimeoutException) {
            logger.error(e) { "Timeout. Exiting" }
            null
        } catch (e: IOException) {
            null
        } catch (e: AdbCommandRejectedException) {
            logger.error(e) { "Adb is not responding. Exiting" }
            null
        }
    }

    private fun bufferedImageFrom(rawImage: RawImage): BufferedImage {
        val image = BufferedImage(rawImage.width, rawImage.height, TYPE_INT_ARGB)

        var index = 0
        val bytesPerPixel = rawImage.bpp shr 3
        for (y in 0 until rawImage.height) {
            for (x in 0 until rawImage.width) {
                image.setRGB(x, y, rawImage.getARGB(index) or -0x1000000)
                index += bytesPerPixel
            }
        }
        return image
    }


    companion object {
        const val DELAY = 500
        const val TIMEOUT_MS = 300L
        val logger = MarathonLogging.logger(ScreenCapturer::class.java.simpleName)
    }
}