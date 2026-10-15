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
       Text("PLAYER ONE  ·  LEVEL 08",color=Color(0xFFA9C5B9),fontSize=12.sp,fontWeight=FontWeight.Bold)
       Text("HEY, RONI!",color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Black)
       Spacer(Modifier.height(12.dp))
       Box(Modifier.fillMaxWidth().height(205.dp).background(Color(0xFF173E42),RoundedCornerShape(20.dp))
        .clickable { begin(missions.first()) },contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize()){
         val w=size.width;val h=size.height
         // Floating island, glowing crystals and playful gorilla silhouette.
         drawCircle(Color(0xFF295F68),h*.60f,Offset(w*.77f,h*.33f))
         drawCircle(Color(0xFF70B9B3),h*.19f,Offset(w*.82f,h*.19f),alpha=.45f)
         drawOval(Color(0xFF304F36),topLeft=Offset(w*.07f,h*.72f),
          size=androidx.compose.ui.geometry.Size(w*.88f,h*.24f))
         drawOval(Color(0xFF67A955),topLeft=Offset(w*.10f,h*.66f),
          size=androidx.compose.ui.geometry.Size(w*.82f,h*.17f))
         repeat(9){i->
          val x=w*(.15f+i*.085f)
          drawCircle(if(i%2==0)lime else Color(0xFF72D7D1),5f+(i%3)*3f,Offset(x,h*(.65f+(i%3)*.035f)))
         }
         // Gorilla mascot silhouette: head, ears, face and eyes.
         drawCircle(Color(0xFF162525),h*.21f,Offset(w*.54f,h*.39f))
         drawCircle(Color(0xFF162525),h*.09f,Offset(w*.37f,h*.39f))
         drawCircle(Color(0xFF162525),h*.09f,Offset(w*.71f,h*.39f))
         drawOval(Color(0xFFB19A79),topLeft=Offset(w*.42f,h*.34f),
          size=androidx.compose.ui.geometry.Size(w*.24f,h*.21f))
         drawCircle(Color.White,4.5f,Offset(w*.49f,h*.39f))
         drawCircle(Color.White,4.5f,Offset(w*.59f,h*.39f))
         drawCircle(Color(0xFF162525),2.5f,Offset(w*.49f,h*.39f))
         drawCircle(Color(0xFF162525),2.5f,Offset(w*.59f,h*.39f))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(15.dp)){
         Text("TODAY'S ADVENTURE",color=lime,fontWeight=FontWeight.Black,fontSize=12.sp)
         Text("YOUR WORLD IS WAITING!",color=Color.White,fontWeight=FontWeight.Black,fontSize=19.sp)
        }
       }
       Spacer(Modifier.height(15.dp))
       Text("PICK YOUR NEXT QUEST",color=Color.White,fontWeight=FontWeight.Black,fontSize=17.sp)
       Spacer(Modifier.height(9.dp))
       LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)){
        items(missions.chunked(2)){pair->
         Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
          pair.forEachIndexed{index,m->
           val ix=missions.indexOf(m)
           val accents=listOf(Color(0xFFFFB950),Color(0xFFEBA3F6),Color(0xFF68CFF5),Color(0xFF87E7AA),Color(0xFFF6C875),Color(0xFFB4A6FA))
           val accent=accents[ix%accents.size]
           Column(Modifier.weight(1f).height(147.dp).background(
            Color(0xFF203039),RoundedCornerShape(17.dp)).clickable{begin(m)}.padding(13.dp),
            verticalArrangement=Arrangement.SpaceBetween){
            Box(Modifier.size(60.dp).background(accent.copy(alpha=.18f),RoundedCornerShape(15.dp)),
             contentAlignment=Alignment.Center){
             Text(listOf("⚡","✦","◆","✚","✎","●")[ix],fontSize=38.sp,color=accent,fontWeight=FontWeight.Black)
            }
            Text(m.title,color=Color.White,fontWeight=FontWeight.Black,fontSize=17.sp,maxLines=1)
            Text("%02d:%02d  ▶".format(m.seconds/60,m.seconds%60),color=accent,fontWeight=FontWeight.Bold,fontSize=13.sp)
           }
          }
          if(pair.size==1)Spacer(Modifier.weight(1f))
         }
        }
       }
       Spacer(Modifier.height(9.dp))
       Row(Modifier.fillMaxWidth().background(Color(0xFF294037),RoundedCornerShape(17.dp))
        .padding(15.dp),verticalAlignment=Alignment.CenterVertically){
        Text("★",color=lime,fontSize=38.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)){
         Text("WISH VAULT",color=Color.White,fontWeight=FontWeight.Black,fontSize=17.sp)
         Text("$badges / 10 badges to a wish",color=Color(0xFFB8CFC4),fontSize=12.sp)
        }
       }
       Spacer(Modifier.height(8.dp))
       Row(verticalAlignment=Alignment.CenterVertically){
        Text("SOUND",color=Color.White,fontSize=11.sp)
        Switch(soundOn,{soundOn=it;prefs.edit().putBoolean("sound",it).apply()})
        Spacer(Modifier.width(6.dp))
        TextButton(onClick={pack=(pack+1)%5;prefs.edit().putInt("pack",pack).apply();beep(1)}){
         Text(listOf("MONSTER","ARCADE","JUNGLE","CLASSIC","CARTOON")[pack],fontSize=11.sp)
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
       Box(Modifier.fillMaxWidth().aspectRatio(1f),contentAlignment=Alignment.Center){
        LivingTime(seconds, m.seconds, paused, Modifier.fillMaxSize())
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
