// Copyright (c) 2026 Sandeep Pariyar. All rights reserved.
package com.turboroni.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * GPU-powered ambient touch-responsive plasma.
 * Does not affect the mission clock or scoring.
 * Android 13+ uses AGSL; older devices use a lightweight Compose fallback.
 */
@Composable
fun EnergyField(modifier: Modifier = Modifier, intensity: Float = 0.65f) {
    if (Build.VERSION.SDK_INT >= 33) {
        AndroidView(
            modifier = modifier,
            factory = { context -> PlasmaView(context) },
            update = { it.energy = intensity.coerceIn(0f, 1f) }
        )
    } else {
        Canvas(modifier) {
            val c = center
            val r = size.minDimension * 0.43f
            repeat(45) { i ->
                val angle = i * 2.399963f
                val d = r * ((i + 1) / 45f)
                drawCircle(
                    color = if (i % 3 == 0) Color(0xFFB9FF74) else Color(0xFF398E70),
                    radius = if (i % 5 == 0) 3f else 1.6f,
                    center = Offset(c.x + cos(angle) * d, c.y + sin(angle) * d),
                    alpha = intensity * 0.6f
                )
            }
        }
    }
}

@RequiresApi(33)
private class PlasmaView(context: Context) : View(context) {
    var energy: Float = 0.65f
    private var touchX = -1000f
    private var touchY = -1000f
    private val started = SystemClock.elapsedRealtime()
    private val shader = RuntimeShader(AGSL)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = this@PlasmaView.shader }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        shader.setFloatUniform("resolution", width.toFloat(), height.toFloat())
        shader.setFloatUniform("time", (SystemClock.elapsedRealtime() - started) / 1000f)
        shader.setFloatUniform("touch", touchX, touchY)
        shader.setFloatUniform("energy", energy)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        postInvalidateDelayed(33L) // cap ambient animation at ~30fps
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x; touchY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchX = -1000f; touchY = -1000f
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
        private const val AGSL = """
uniform float2 resolution;
uniform float2 touch;
uniform float time;
uniform float energy;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1,311.7))) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - resolution * 0.5) / max(1.0, min(resolution.x, resolution.y));
    float radius = length(uv);
    float angle = atan(uv.y, uv.x);
    float wave = sin(angle * 6.0 + time * 0.7 + sin(radius * 17.0 - time * 1.4));
    float ring = exp(-pow((radius - 0.39 - wave * 0.012) * 34.0, 2.0));
    float2 t = (touch - resolution * 0.5) / max(1.0, min(resolution.x, resolution.y));
    float ripple = exp(-length(uv-t)*8.0) * (0.5+0.5*sin(length(uv-t)*55.0-time*8.0));
    float sparks = pow(max(0.0, sin(angle*75.0 + time*1.5) * sin(radius*80.0-time)), 16.0);
    float glow = clamp((ring*0.85 + ripple*0.8 + sparks*0.1)*energy,0.0,1.0);
    float3 base = float3(0.045,0.085,0.085);
    float3 green = float3(0.43,0.96,0.48);
    return half4(half3(mix(base,green,glow)),1.0);
}
"""
    }
}
