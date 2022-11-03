package com.github.javscraper.service

import com.github.javscraper.extension.countNotZeroByColumn
import com.github.javscraper.extension.detect
import com.github.javscraper.extension.detectMultiScale
import com.github.javscraper.extension.toMat
import com.github.javscraper.utils.getTempPathForResource
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.opencv.global.opencv_imgcodecs.*
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_barcode.BarcodeDetector
import org.bytedeco.opencv.opencv_core.AbstractScalar
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.MatVector
import org.bytedeco.opencv.opencv_core.Point
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.Ignore

@Ignore
class FaceAndBoundaryDetectTest {
    @Test
    fun testFaceDetector() {
        val cascadeClassifier = CascadeClassifier(getTempPathForResource("haarcascade/haarcascade_frontalface_alt.xml"))

        val image = imdecode(ClassLoader.getSystemResourceAsStream("test2.jpeg")!!.readAllBytes().toMat(), IMREAD_ANYCOLOR)
        val rectVector = cascadeClassifier.detectMultiScale(image)
        println(
            rectVector.get()
                .map { listOf(it.x(), it.y(), it.height(), it.width()) }
        )
        rectVector.get().forEachIndexed { i, rect ->
            Mat(image, rect).writeToFile("C:\\Users\\Adam\\Desktop\\output_$i.jpeg")
        }
    }

    @Test
    fun testBarcodeDetector() {
        val barcodeDetector = BarcodeDetector()
        val image = imdecode(ClassLoader.getSystemResourceAsStream("test1.jpeg")!!.readAllBytes().toMat(), IMREAD_ANYCOLOR)
        val result = barcodeDetector.detect(image)

        result.map { rect ->
            println(rect.tl().let { it.x() to it.y() } to rect.br().let { it.x() to it.y() })
            Mat(image, rect).writeToFile("C:\\Users\\Adam\\Desktop\\barcode.jpeg")
        }
    }

    @Test
    fun testDetectContours() {
        val image = imdecode(ClassLoader.getSystemResourceAsStream("test1.jpeg")!!.readAllBytes().toMat(), IMREAD_ANYCOLOR)
        val grayImage = Mat()
        cvtColor(image, grayImage, COLOR_BGR2GRAY)
        grayImage.writeToFile("C:\\Users\\Adam\\Desktop\\grayImage.jpeg")
        val output = Mat()
        threshold(grayImage, output, 127.toDouble(), 255.toDouble(), 0)
        output.writeToFile("C:\\Users\\Adam\\Desktop\\threshold.jpeg")
        val contours = MatVector()
        val hierarchy = Mat()
        findContours(output, contours, hierarchy, RETR_TREE, CHAIN_APPROX_NONE)
        drawContours(image, contours, 3, AbstractScalar.RED, 5, LINE_8, hierarchy, Int.MAX_VALUE, Point(0, 0))
        image.writeToFile("C:\\Users\\Adam\\Desktop\\contours.jpeg")
        println()
    }

    @Test
    fun testCanny() {
        val image = imdecode(ClassLoader.getSystemResourceAsStream("test1.jpeg")!!.readAllBytes().toMat(), IMREAD_ANYCOLOR)
        val output = Mat()

        var startTime = System.currentTimeMillis()
        Canny(image, output, 100.toDouble(), 200.toDouble(), 3, true)
        println("duration for Canny() is ${System.currentTimeMillis() - startTime}ms")
        //output.writeToFile("C:\\Users\\Adam\\Desktop\\canny.jpeg")
        startTime = System.currentTimeMillis()
        val colSum = output.countNotZeroByColumn()
        println("duration for countNotZeroByColumn() is ${System.currentTimeMillis() - startTime}ms")
        println(colSum.asSequence().sorted().take(3).toList())
    }

    private fun Mat.writeToFile(path: String) =
        BytePointer().use { output ->
            imencode(path.substring(path.lastIndexOf('.')), this, output, IntPointer(IMWRITE_JPEG_QUALITY, 90))
            val outputFile = File(path)
            outputFile.delete()
            outputFile.writeBytes(output.stringBytes)
        }
}
