package dev.isxander.crashhelper.utils

import org.imgscalr.Scalr
import java.awt.image.BufferedImage

fun scaleImageToPixelCount(img: BufferedImage, desired: Int): BufferedImage {
    val current = img.width * img.height
    val mod = desired / current

    if (mod >= 1) return img

    return Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, img.width * mod, img.height * mod)
}