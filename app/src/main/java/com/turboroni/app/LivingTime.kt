// Copyright (c) 2026 Sandeep Pariyar. All rights reserved.
package com.turboroni.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.cos
import kotlin.math.sin

/**
 * The clock is the artwork: animated shader is painted INSIDE the numeral glyphs.
 * No circular progress meter or overlaid conventional clock face.
 */
@Composable
fun LivingTime(seconds: Int, totalSeconds: Int, paused: Boolean, modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= 33) {
        AndroidView(
            modifier = modifier,
            factory = { LivingTimeView(it) },
            update = { it.setTime(seconds, totalSeconds, paused) }
        )
    } else {
        androidx.compose.foundation.layout.Box(modifier) {
            androidx.compose.material3.Text(
                text = "%02d:%02d".format(seconds / 60, seconds % 60),
                color = Color(0xFFB9FF74),
                fontSize = androidx.compose.ui.unit.TextUnit(65f, androidx.compose.ui.unit.TextUnitType.Sp),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
            )
        }
    }
}

@RequiresApi(33)
private class LivingTimeView(context: Context) : View(context) {
    private val shader = RuntimeShader(SHADER)
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = this@LivingTimeView.shader
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(142, 255, 170) }
    private val start = SystemClock.elapsedRealtime()
    private var remaining = 0
    private var total = 1
    private var isPaused = false
    private var touchX = -9999f
    private var touchY = -9999f
    private var touchAt = 0L

    fun setTime(seconds: Int, totalSeconds: Int, paused: Boolean) {
        remaining = seconds.coerceAtLeast(0)
        total = totalSeconds.coerceAtLeast(1)
        isPaused = paused
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val t = (SystemClock.elapsedRealtime() - start) / 1000f
        val progress = remaining.toFloat() / total
        val touchAge = (SystemClock.elapsedRealtime() - touchAt) / 1000f
        shader.setFloatUniform("resolution", w, h)
        shader.setFloatUniform("time", t)
        shader.setFloatUniform("progress", progress)
        shader.setFloatUniform("touch", touchX, touchY)
        shader.setFloatUniform("touchAge", touchAge)
        shader.setFloatUniform("paused", if (isPaused) 1f else 0f)

        val label = "%02d:%02d".format(remaining / 60, remaining % 60)
        glyphPaint.textSize = minOf(w / 4.6f, h * 0.31f)
        val baseline = h * 0.51f - (glyphPaint.ascent() + glyphPaint.descent()) / 2f
        // The RuntimeShader is the glyph fill, not a layer behind the text.
        canvas.drawText(label, w * .5f, baseline, glyphPaint)

        // Free-floating sparks are tied to the remaining-time fraction.
        val count = (12 + progress * 55).toInt()
        for (i in 0 until count) {
            val angle = i * 2.399963f + t * .18f
            val orbit = (0.18f + ((i * 37) % 100) / 145f) * minOf(w, h)
            val x = w * .5f + cos(angle) * orbit
            val y = h * .5f + sin(angle) * orbit * .7f
            particlePaint.alpha = 55 + i % 4 * 35
            canvas.drawCircle(x, y, if (i % 8 == 0) 3.5f else 1.5f, particlePaint)
        }
        if (touchAge in 0f..1.2f) {
            particlePaint.alpha = ((1.2f - touchAge) / 1.2f * 200).toInt().coerceIn(0, 255)
            for (i in 0 until 24) {
                val a = i * 2.399963f
                val r = touchAge * 120f
                canvas.drawCircle(touchX + cos(a) * r, touchY + sin(a) * r, 2.8f, particlePaint)
            }
        }
        postInvalidateDelayed(if (isPaused) 100L else 33L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                touchAt = SystemClock.elapsedRealtime()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    companion object {
        private const val SHADER = """
uniform float2 resolution;
uniform float time;
uniform float progress;
uniform float2 touch;
uniform float touchAge;
uniform float paused;
half4 main(float2 xy) {
    float2 uv = xy / resolution;
    float2 t = touch / resolution;
    float dist = length(uv - t);
    float ripple = exp(-dist * 8.0) * sin(dist * 48.0 - touchAge * 13.0) * exp(-max(touchAge,0.0)*2.0);
    float flow = sin(uv.y * 20.0 + uv.x * 9.0 + time * 1.7 + ripple * 3.0);
    float grain = sin(uv.x * 91.0 - time * 1.3) * sin(uv.y * 67.0 + time);
    float heat = (1.0 - progress);
    float3 cool = float3(0.26, 0.92, 0.57);
    float3 bright = float3(0.79, 1.0, 0.35);
    float3 warm = float3(1.0, 0.65, 0.28);
    float3 c = mix(cool, bright, clamp(0.5 + 0.45 * flow + 0.1 * grain, 0.0, 1.0));
    c = mix(c, warm, smoothstep(0.78, 1.0, heat) * 0.6);
    c += max(ripple,0.0) * 0.22;
    return half4(half3(clamp(c, 0.0, 1.0)), 1.0);
}
"""
    }
}
