package com.sanlorng.coromusic.work.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import androidx.core.graphics.drawable.toBitmap

val Drawable.bitmap: Bitmap
    get() {
        return if (this is VectorDrawable) {
            Bitmap.createBitmap(intrinsicWidth,intrinsicHeight, Bitmap.Config.ARGB_8888).apply {
                Canvas(this).let {
                    setBounds(0,0,it.width,it.height)
                    draw(it)
                }
            }
        }else {
            toBitmap(100,100)
        }
    }