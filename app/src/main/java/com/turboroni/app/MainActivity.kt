// Copyright (c) 2026 Sandeep Pariyar. All rights reserved.
package com.turboroni.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

private val lime=Color(0xFFBBFF71)
private val dark=Color(0xFF0C1718)
private val panel=Color(0xFF1B3029)
data class Mission(val title:String,val seconds:Int)
private val missions=listOf(
 Mission("Get Dressed",300),Mission("Brush Teeth",120),Mission("Tidy Toys",300),
 Mission("Pack School Bag",240),Mission("Homework",900),Mission("Eating",1200))
class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  val prefs=getSharedPreferences("turbo_roni",MODE_PRIVATE)
  setContent {
   var current by remember { mutableStateOf<Mission?>(null) }
   var startAt by remember { mutableLongStateOf(0L) }
   var remaining by remember { mutableLongStateOf(0L) }
   var paused by remember { mutableStateOf(false) }
   var phase by remember { mutableStateOf("HOME") }
   var goAt by remember { mutableLongStateOf(0L) }
   var seconds by remember { mutableIntStateOf(0) }
   var badges by remember { mutableIntStateOf(prefs.getInt("badges",0)) }
   var soundOn by remember { mutableStateOf(prefs.getBoolean("sound",true)) }
   var pack by remember { mutableIntStateOf(prefs.getInt("pack",0)) }
   var tickMode by remember { mutableIntStateOf(prefs.getInt("tickMode",0)) }
   var lastTick by remember { mutableIntStateOf(-1) }
   var bestVersion by remember { mutableIntStateOf(0) }
   val tone=remember { ToneGenerator(AudioManager.STREAM_MUSIC,65) }
   DisposableEffect(Unit){onDispose { tone.release() }}
   fun beep(kind:Int){if(soundOn)tone.startTone(
     when(pack){0->ToneGenerator.TONE_PROP_BEEP;1->ToneGenerator.TONE_DTMF_1;2->ToneGenerator.TONE_DTMF_4;3->ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;else->ToneGenerator.TONE_PROP_ACK},
     if(kind==0)45 else 170)}
   fun begin(m:Mission){current=m;remaining=m.seconds*1000L;seconds=m.seconds;paused=false;phase="READY";goAt=SystemClock.elapsedRealtime();lastTick=-1}
   LaunchedEffect(phase,paused,current){
    while(current!=null && phase!="HOME" && phase!="RESULT"){
     val now=SystemClock.elapsedRealtime()
     if(phase=="READY"){
      val elapsed=now-goAt
      val stage=when {elapsed<800->"READY";elapsed<1600->"SET";elapsed<2400->"GO!";else->"RUN"}
      if(stage=="RUN"){phase="RUN";startAt=now;beep(1)}
      else if(stage!=phase){phase=stage;beep(1)}
     }else if(phase=="RUN"&&!paused){
      val left=(remaining-(now-startAt)).coerceAtLeast(0)
      seconds=ceil(left/1000.0).toInt()
      if(seconds!=lastTick && seconds>0){
       if(tickMode==1 || (tickMode==0 && seconds<=30) || (tickMode==2 && seconds<=60))beep(0)
       lastTick=seconds
      }
      if(left==0L){phase="RESULT";beep(1)}
     }
     delay(50)
    }
   }
   MaterialTheme(colorScheme=darkColorScheme(primary=lime,background=dark,surface=panel,onPrimary=dark,onBackground=Color.White)){
    Column(Modifier.fillMaxSize().background(dark).padding(18.dp)){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
      Text("TURBO RONI",fontWeight=FontWeight.Black,fontSize=22.sp,color=lime)
      Text("🏅 $badges",color=Color.White,fontWeight=FontWeight.Bold)
     }
     Spacer(Modifier.height(18.dp))
     when(phase){
      "HOME"->{
       Text("CHOOSE YOUR QUEST",fontWeight=FontWeight.Bold,color=Color.White)
       Spacer(Modifier.height(16.dp))
       LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)){
        items(missions){m->
         Row(Modifier.fillMaxWidth().background(panel,RoundedCornerShape(18.dp)).clickable{begin(m)}.padding(18.dp),
          horizontalArrangement=Arrangement.SpaceBetween){
          Text(m.title,color=Color.White,fontWeight=FontWeight.Bold)
          Text("${m.seconds/60} MIN  ▶",color=lime)
         }
        }
       }
       Text("CLOCK SOUND",color=Color.White,fontWeight=FontWeight.Bold)
       val names=listOf("Monster","Arcade","Jungle","Classic","Cartoon")
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
        names.forEachIndexed{idx,name->
         Text(name,modifier=Modifier.weight(1f).background(if(pack==idx)lime else panel,RoundedCornerShape(9.dp))
          .clickable{pack=idx;prefs.edit().putInt("pack",idx).apply();beep(1)}.padding(vertical=12.dp),
          color=if(pack==idx)dark else Color.White,fontSize=10.sp,textAlign=TextAlign.Center)
        }
       }
       Spacer(Modifier.height(12.dp))
       Row(verticalAlignment=Alignment.CenterVertically){
        Text("Sound",color=Color.White);Switch(soundOn,{soundOn=it;prefs.edit().putBoolean("sound",it).apply()})
        Spacer(Modifier.width(8.dp))
        Text("Tick:",color=Color.White)
        TextButton(onClick={tickMode=(tickMode+1)%3;prefs.edit().putInt("tickMode",tickMode).apply()}){
         Text(listOf("Last 30s","Always","Last 60s")[tickMode])
        }
       }
      }
      "READY","SET","GO!"->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
       Text(phase,color=lime,fontWeight=FontWeight.Black,fontSize=76.sp)
      }
      "RUN"->{
       val m=current!!
       var spark by remember { mutableIntStateOf(0) }
       var pos by remember { mutableStateOf(Offset.Zero) }
       var orbit by remember { mutableFloatStateOf(0f) }
       LaunchedEffect(Unit){while(true){orbit+=.015f;delay(32)}}
       Text(m.title.uppercase(),color=Color.White,fontWeight=FontWeight.Bold,
        modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
       Spacer(Modifier.weight(.5f))
       Box(Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(Unit){
        detectTapGestures{pos=it;spark++}
       },contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize()){
         val r=size.minDimension*.42f
         drawCircle(panel,r+18.dp.toPx())
         drawCircle(Color(0xFF385447),r,style=Stroke(10.dp.toPx()))
         drawArc(lime,-90f,360f*(seconds.toFloat()/m.seconds).coerceIn(0f,1f),false,
          topLeft=Offset(center.x-r,center.y-r),
          size=androidx.compose.ui.geometry.Size(r*2,r*2),
          style=Stroke(10.dp.toPx(),cap=StrokeCap.Round))
         repeat(105){i->
          val a=i*2.399963f+orbit
          val d=sqrt((i+1)/105f)*r*.9f
          drawCircle(if(i%4==0)lime else Color(0xFF61B68A),if(i%7==0)3f else 1.5f,
           Offset(center.x+cos(a)*d,center.y+sin(a)*d),alpha=.3f+(i%5)*.1f)
         }
         if(spark>0)repeat(22){i->
          val a=i*2.399963f+orbit*3f
          drawCircle(lime,3f,Offset(pos.x+cos(a)*36f,pos.y+sin(a)*36f))
         }
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally){
         Text("%02d:%02d".format(seconds/60,seconds%60),color=Color.White,fontSize=68.sp,fontWeight=FontWeight.Black)
         Text(if(paused)"PAUSED" else "LET'S GO!",color=lime,fontWeight=FontWeight.Bold)
        }
       }
       Spacer(Modifier.height(16.dp))
       val best=prefs.getInt("best_${m.title}",0)
       Text("BEST: "+if(best>0)"%02d:%02d".format(best/60,best%60) else "--:--",
        color=Color.LightGray,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
       Spacer(Modifier.weight(1f))
       Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
        OutlinedButton(onClick={
         if(paused){startAt=SystemClock.elapsedRealtime();paused=false}
         else{remaining=(remaining-(SystemClock.elapsedRealtime()-startAt)).coerceAtLeast(0);paused=true}
        },modifier=Modifier.weight(1f).height(70.dp)){Text(if(paused)"PLAY" else "PAUSE")}
        Button(onClick={
         val elapsed=m.seconds-seconds
         if(!paused && seconds>0 && elapsed>0 && m.title!="Eating" && m.title!="Homework"){
          val old=prefs.getInt("best_${m.title}",0)
          if(old==0 || elapsed<old)prefs.edit().putInt("best_${m.title}",elapsed).apply()
          badges++;prefs.edit().putInt("badges",badges).apply()
         }
         phase="RESULT";beep(1)
        },modifier=Modifier.weight(1.4f).height(70.dp)){Text("I'M DONE!",fontWeight=FontWeight.Black)}
       }
      }
      "RESULT"->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
       Column(horizontalAlignment=Alignment.CenterHorizontally){
        Text("QUEST COMPLETE!",color=lime,fontWeight=FontWeight.Black,fontSize=28.sp)
        Spacer(Modifier.height(16.dp))
        Text("Great work, Roni!",color=Color.White,fontSize=24.sp)
        Spacer(Modifier.height(25.dp))
        Button(onClick={current=null;phase="HOME"}){Text("NEXT QUEST")}
       }
      }
     }
    }
   }
  }
 }
}
