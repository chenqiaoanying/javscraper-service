package com.github.javscraper.service

import com.github.javscraper.extension.*
import com.github.javscraper.utils.getTempPathForResource
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_barcode.BarcodeDetector
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Rect
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URL
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Service
class ImageProxyService(
    private val webClient: WebClient,
    private val fileCacheService: FileCacheService
) {
    private val cascadeClassifierFile = "haarcascade/haarcascade_frontalface_alt.xml"
    private val cascadeClassifier = getTempPathForResource(cascadeClassifierFile)?.let { CascadeClassifier(it) } ?: throw IllegalArgumentException("missing file $cascadeClassifierFile")
    private val barcodeDetector = BarcodeDetector()

    fun getImage(imgSrc: URL, quality: Double, enableAutoCut: Boolean): Mono<ByteArray> =
        fileCacheService.getCache(imgSrc.toString()).toMono()
            .switchIfEmpty {
                webClient.get().uri { imgSrc.toURI() }.retrieve()
                    .bodyToMono(ByteArray::class.java)
                    .doOnNext { fileCacheService.saveCache(imgSrc.toString(), it) }
            }
            .map {
                if (enableAutoCut) {
                    getCover(it.decodeAsImage())
                } else {
                    it.decodeAsImage()
                }
            }
            .map { it.encodeAsJpeg((quality * 100).roundToInt()) }

    private fun getCover(image: Mat): Mat {
        val centerX = image.arrayWidth() / 2
        val defaultCoverWidth = (image.arrayHeight() * 0.706).toInt()
        val barcodeRect by lazy { barcodeDetector.detect(image).maxByOrNull { it.width() * it.height() } }
        val faceRect by lazy { cascadeClassifier.detectMultiScale(image).get().maxByOrNull { it.width() * it.height() } }
        val boundaries by lazy { detectBoundary(image).map { it.index }.toList() }
        return when {
            image.arrayWidth().toDouble() / image.arrayHeight() <= 1 -> image
            boundaries.isEmpty() && barcodeRect == null -> image
            barcodeRect != null && barcodeRect!!.br().x() >= centerX -> image.chooseLeftSide(boundaries, defaultCoverWidth)
            barcodeRect != null && barcodeRect!!.tl().x() <= centerX -> image.chooseRightSide(boundaries, defaultCoverWidth)
            faceRect != null && faceRect!!.br().x() >= centerX -> image.chooseRightSide(boundaries, defaultCoverWidth)
            faceRect != null && faceRect!!.tl().x() <= centerX -> image.chooseLeftSide(boundaries, defaultCoverWidth)
            else -> image.chooseRightSide(boundaries, defaultCoverWidth)
        }
    }

    private fun Mat.chooseLeftSide(boundaries: List<Int>, defaultWidth: Int): Mat =
        boundaries.minByOrNull { (defaultWidth - it).absoluteValue }
            ?.let { Mat(this, Rect(0, 0, it, arrayHeight())) }
            ?: Mat(this, Rect(0, 0, defaultWidth, arrayHeight()))

    private fun Mat.chooseRightSide(boundaries: List<Int>, defaultWidth: Int): Mat =
        boundaries.minByOrNull { (arrayWidth() - defaultWidth - it).absoluteValue }
            ?.let { Mat(this, Rect(it, 0, arrayWidth() - it, arrayHeight())) }
            ?: Mat(this, Rect(arrayWidth() - defaultWidth, 0, defaultWidth, arrayHeight()))

    private fun detectBoundary(image: Mat): Sequence<IndexedValue<Int>> {
        val output = Mat()
        opencv_imgproc.Canny(image, output, 100.toDouble(), 200.toDouble(), 3, true)
        return output.countNotZeroByColumn().withIndex().asSequence().filter { it.value.toDouble() / image.arrayHeight() > 0.7 }.take(2)
    }
}