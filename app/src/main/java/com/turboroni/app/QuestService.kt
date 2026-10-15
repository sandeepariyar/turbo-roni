/*
 * Turbo Roni
 * Copyright (c) 2026 Sandeep Pariyar
 */

package com.turboroni.app

import android.app.*
import android.content.*
import android.os.*
import android.media.AudioManager
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import java.util.Locale
import kotlin.math.ceil

object QuestClock {
    var active by mutableStateOf(false)
    var paused by mutableStateOf(false)
    var quest by mutableStateOf<Quest?>(null)
    var remainingMs by mutableLongStateOf(0L)
    var elapsedMs by mutableLongStateOf(0L)
    var limitMs by mutableLongStateOf(0L)
    var startAt: Long = 0
    var pausedRemaining: Long = 0
    var completed by mutableStateOf(false)
    fun remainingSeconds() = ceil(remainingMs / 1000.0).toInt().coerceAtLeast(0)
    fun elapsedSeconds() = (elapsedMs / 1000L).toInt()
}

class QuestService : Service(), TextToSpeech.OnInitListener {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: Store
    private var tts: TextToSpeech? = null
    private var voiceReady = false
    private var lastSecond = -1
    private val spoken = mutableSetOf<Int>()
    private val soundPool by lazy {
        SoundPool.Builder().setMaxStreams(3)
            .setAudioAttributes(android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_GAME)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
    }
    private val clips=mutableMapOf<String,Int>()
    private fun loadSounds(){
        for(pack in 0..4)for(event in listOf("tick","start","warning","finish")){
            val key="pack${pack}_${event}"
            val id=resources.getIdentifier(key,"raw",packageName)
            if(id!=0)clips[key]=soundPool.load(this,id,1)
        }
    }
    private val tick = object: Runnable { override fun run() { updateClock();handler.postDelayed(this, 120L) } }
    override fun onCreate() { super.onCreate();store=Store(this);createChannel();loadSounds();tts=TextToSpeech(this,this) }
    override fun onInit(status: Int) { if(status==TextToSpeech.SUCCESS) { voiceReady=true;tts?.language=Locale.ENGLISH;tts?.setSpeechRate(.93f) } }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            "START" -> {
                val id=intent.getStringExtra("id") ?: return START_NOT_STICKY
                val q=store.quests().find{it.id==id} ?: return START_NOT_STICKY
                QuestClock.quest=q;QuestClock.limitMs=q.minutes*60_000L;QuestClock.remainingMs=QuestClock.limitMs;QuestClock.elapsedMs=0L
                QuestClock.paused=false;QuestClock.completed=false;QuestClock.active=true
                QuestClock.startAt=SystemClock.elapsedRealtime();lastSecond=-1;spoken.clear()
                startForeground(27, notification());handler.removeCallbacks(tick);handler.post(tick);sound("start")
            }
            "PAUSE" -> { if(QuestClock.active&&!QuestClock.paused) { updateClock();QuestClock.pausedRemaining=QuestClock.remainingMs;QuestClock.paused=true;updateNotification() } }
            "RESUME" -> { if(QuestClock.active&&QuestClock.paused) { QuestClock.startAt=SystemClock.elapsedRealtime()-(QuestClock.limitMs-QuestClock.pausedRemaining);QuestClock.paused=false;updateNotification() } }
            "STOP" -> { QuestClock.active=false;handler.removeCallbacks(tick);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf() }
        }
        return START_NOT_STICKY
    }
    private fun updateClock() {
        if(!QuestClock.active||QuestClock.paused)return
        val elapsed=(SystemClock.elapsedRealtime()-QuestClock.startAt).coerceAtLeast(0)
        QuestClock.elapsedMs=elapsed
        QuestClock.remainingMs=(QuestClock.limitMs-elapsed).coerceAtLeast(0)
        val second=QuestClock.remainingSeconds()
        if(second!=lastSecond) {
            lastSecond=second
            if(second>0 && store.ticking && (store.tickMode==1 || (store.tickMode==2 && second<=60) || (store.tickMode==0 && second<=30))) sound("tick")
            if(second>30 && second%60==0 && second !in spoken){spoken.add(second);say("Amazing ${store.name}! ${second/60} ${if(second/60==1)"minute" else "minutes"} left!");sound("milestone")}
            if(second==30 && spoken.add(30)){say("Thirty seconds left, ${store.name}! You've got this!");sound("warning")}
            if(second==0 && !QuestClock.completed){QuestClock.completed=true; say("Well done ${store.name}! Mission finished!");sound("finish")}
            updateNotification()
        }
    }
    private fun sound(event: String) {
        if(!store.soundOn)return
        val key="pack${store.pack.coerceIn(0,4)}_${event}"
        clips[key]?.let{ soundPool.play(it,if(event=="tick") .35f else .75f,
            if(event=="tick") .35f else .75f,1,0,1f) }
    }
    private fun say(message:String){if(store.soundOn&&store.voiceOn&&voiceReady)tts?.speak(message,TextToSpeech.QUEUE_ADD,null,"cue_${SystemClock.uptimeMillis()}")}
    private fun createChannel(){val channel=NotificationChannel("quest", "Active speed quests", NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager::class.java).createNotificationChannel(channel)}
    private fun notification(): Notification {
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val action=if(QuestClock.paused)"RESUME" else "PAUSE"
        val control=PendingIntent.getService(this,1,Intent(this,QuestService::class.java).setAction(action),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val secs=QuestClock.remainingSeconds();val label="%02d:%02d".format(secs/60,secs%60)
        return Notification.Builder(this,"quest").setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("${QuestClock.quest?.name ?: "Quest"} • $label")
            .setContentText(if(QuestClock.paused)"Paused" else if(QuestClock.completed)"Well done! Tap to finish." else "Turbo Roni is running")
            .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null,if(QuestClock.paused)"Resume" else "Pause",control).build()).build()
    }
    private fun updateNotification(){if(QuestClock.active)getSystemService(NotificationManager::class.java).notify(27,notification())}
    override fun onDestroy(){handler.removeCallbacks(tick);tts?.stop();tts?.shutdown();soundPool.release();super.onDestroy()}
}
