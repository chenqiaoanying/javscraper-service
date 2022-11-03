package com.github.javscraper.extension

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.opencv.global.opencv_core.countNonZero
import org.bytedeco.opencv.global.opencv_imgcodecs.*
import org.bytedeco.opencv.opencv_barcode.BarcodeDetector
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Point2f
import org.bytedeco.opencv.opencv_core.Rect
import org.bytedeco.opencv.opencv_core.RectVector
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import kotlin.math.ceil
import kotlin.math.floor

fun CascadeClassifier.detectMultiScale(image: Mat): RectVector =
    RectVector().also { detectMultiScale(image, it) }

fun BarcodeDetector.detect(image: Mat): List<Rect> {
    val points = Mat()
    val result = mutableListOf<Rect>()
    if (detect(image, points)) {
        val rectangle = ArrayList<Point2f>(4)
        for (y in 0 until points.arrayHeight()) {
            for (x in 0 until 4) {
                rectangle += Point2f(points.ptr(y, x))
            }
        }
        val left = rectangle.minOf { floor(it.x()).toInt() }
        val right = rectangle.maxOf { ceil(it.x()).toInt() }
        val top = rectangle.minOf { floor(it.y()).toInt() }
        val bottom = rectangle.maxOf { ceil(it.y()).toInt() }
        result += Rect(left, top, right - left, bottom - top)
    }
    return result
}

fun ByteArray.toMat() = Mat(this, true)

fun ByteArray.decodeAsImage(flags: Int = IMREAD_ANYCOLOR): Mat =
    toMat().use { buf ->
        imdecode(buf, flags)
    }

fun Mat.encodeAsJpeg(quality: Int = 90): ByteArray =
    BytePointer().use {
        imencode(".jpeg", this, it, IntPointer(IMWRITE_JPEG_QUALITY, quality))
        it.stringBytes
    }

fun Mat.countNotZeroByColumn(): IntArray {
    val result = IntArray(cols())
    for (colIndex in 0 until cols()) {
        result[colIndex] = countNonZero(col(colIndex))
    }
    return result
}