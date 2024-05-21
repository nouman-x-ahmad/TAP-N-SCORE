package com.noumanahmad.tapandscore
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock


class GameTimer(private val timeLimit: Long) {
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private val paint: Paint = Paint()

    init {
        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.textAlign = Paint.Align.RIGHT
    }

    fun start() {
        startTime = SystemClock.elapsedRealtime()
    }

    fun update() {
        elapsedTime = SystemClock.elapsedRealtime() - startTime
    }

    fun draw(canvas: Canvas, x: Float, y: Float) {
        val seconds = (elapsedTime / 1000) % 60
        val minutes = (elapsedTime / 1000) / 60
        val timeString = "%02d:%02d".format(minutes, seconds)
        canvas.drawText(timeString, x, y, paint)
    }

    fun isTimeUp(): Boolean {
        return elapsedTime >= timeLimit
    }
}
