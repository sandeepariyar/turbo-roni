/* Copyright (c) 2026 Sandeep Pariyar. All rights reserved. */
package com.turboroni.app

import android.content.Context
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable fun LivingDigits(seconds:Int, total:Int, modifier:Modifier=Modifier){
    if(Build.VERSION.SDK_INT>=33){
        AndroidView(factory={DigitView(it)},modifier=modifier,update={it.setSeconds(seconds,total)})
    }else Box(modifier,contentAlignment=Alignment.Center){
        Text("%02d:%02d".format(seconds/60,seconds%60),fontSize=66.sp,fontWeight=FontWeight.Black,color=Color(0xFFB9FF74))
    }
}

@RequiresApi(33)
private class DigitView(context:Context):View(context){
    private var remaining=0
    private var total=1
    private val started=SystemClock.elapsedRealtime()
    private val shader=RuntimeShader("""
        uniform float2 resolution;
        uniform float time;
        uniform float progress;
        half4 main(float2 xy){
            float2 uv=xy/max(resolution,float2(1.0));
            float wave=sin(uv.x*19.0+time*1.6+sin(uv.y*23.0-time));
            float flow=0.5+0.5*wave;
            float3 green=mix(float3(0.12,0.58,0.46),float3(0.84,1.0,0.37),flow);
            float3 warm=float3(1.0,0.64,0.27);
            return half4(half3(mix(green,warm,smoothstep(0.15,0.0,progress)*0.6)),1.0);
        }
    """.trimIndent())
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
        shader=this@DigitView.shader
        typeface=Typeface.create("sans-serif-black",Typeface.BOLD)
        textAlign=Paint.Align.CENTER
    }
    fun setSeconds(seconds:Int,limit:Int){remaining=seconds.coerceAtLeast(0);total=limit.coerceAtLeast(1);invalidate()}
    override fun onDraw(canvas:AndroidCanvas){
        super.onDraw(canvas)
        val w=width.toFloat();val h=height.toFloat()
        if(w<=0f||h<=0f)return
        shader.setFloatUniform("resolution",w,h)
        shader.setFloatUniform("time",(SystemClock.elapsedRealtime()-started)/1000f)
        shader.setFloatUniform("progress",remaining.toFloat()/total)
        paint.textSize=minOf(w/4.8f,h*.32f)
        val y=h*.5f-(paint.ascent()+paint.descent())/2f
        canvas.drawText("%02d:%02d".format(remaining/60,remaining%60),w*.5f,y,paint)
        postInvalidateDelayed(33L)
    }
}
