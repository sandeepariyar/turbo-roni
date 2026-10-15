/*
 * Turbo Roni
 * Copyright (c) 2026 Sandeep Pariyar
 */

package com.turboroni.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.*

val Bg=Color(0xFF0B1417);val Panel=Color(0xFF18262A);val Lime=Color(0xFFBCF26C);val Soft=Color(0xFFA8BBB4);val White=Color(0xFFF0F8F0)
class MainActivity: ComponentActivity(){
    private val askNotification=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        if(Build.VERSION.SDK_INT>=33)askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val store=Store(this)
        setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Lime,background=Bg,surface=Panel,onSurface=White,onBackground=White)) {
            TurboApp(store, {action,id -> val i=Intent(this,QuestService::class.java).setAction(action);if(id!=null)i.putExtra("id",id);if(action=="START")androidx.core.content.ContextCompat.startForegroundService(this,i) else startService(i)})
        } }
    }
}

@Composable fun TurboApp(store:Store, service:(String,String?)->Unit){
    var page by remember { mutableStateOf("quests") };var version by remember{mutableIntStateOf(0)}
    var readyQuest by remember{mutableStateOf<Quest?>(null)};var readyStep by remember{mutableIntStateOf(0)}
    var finished by remember{mutableStateOf<Pair<Quest,Int>?>(null)}
    fun refresh(){version++}
    LaunchedEffect(readyQuest){if(readyQuest!=null){readyStep=0;delay(750);readyStep=1;delay(850);readyStep=2;delay(850);val q=readyQuest!!;readyQuest=null;service("START",q.id);page="active"}}
    Box(Modifier.fillMaxSize().background(Bg)) {
        if(readyQuest!=null){ReadyScreen(readyStep,readyQuest!!.name)}
        else if(page=="active"&&QuestClock.active){ActiveScreen(store,service,onDone={
            val q=QuestClock.quest!!;val elapsed=QuestClock.elapsedSeconds();val score=tier(elapsed,q.minutes*60,q.speed)
            // A completion only awards once: active session is stopped immediately.
            store.addRecord(Record(q.id,elapsed,score,System.currentTimeMillis()))
            if(score>0)store.badges+=1
            service("STOP",null);finished=q to score;refresh()
        })}
        else {Column {
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("TURBO RONI",fontWeight=FontWeight.Black,color=Lime,fontSize=19.sp,letterSpacing=1.sp);Text(store.avatar,fontSize=30.sp)}
            Box(Modifier.weight(1f)) {when(page){
                "quests"->QuestsPage(store,version,onStart={readyQuest=it},onChanged={refresh()})
                "vault"->VaultPage(store,version,{refresh()})
                "stopwatch"->StopwatchPage()
                "settings"->SettingsPage(store,{refresh()})
            }}
            NavigationBar(containerColor=Panel){listOf(Triple("quests","Quests",Icons.Default.Bolt),Triple("vault","Rewards",Icons.Default.EmojiEvents),Triple("stopwatch","Speed Lab",Icons.Default.Timer),Triple("settings","Settings",Icons.Default.Settings)).forEach{(key,title,icon)->NavigationBarItem(selected=page==key,onClick={page=key},icon={Icon(icon,title)},label={Text(title,fontSize=10.sp)})}}
        }}
        finished?.let{(q,t)->AlertDialog(onDismissRequest={finished=null;page="quests"},title={Text(if(t>0)"MISSION COMPLETE!" else "WELL DONE, RONI!")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(if(t>0)"🏆 ${tierName(t)}" else "You gave it a go. That's what counts!");Text(if(t>0)"+1 badge earned!" else "Try again whenever you're ready.")}},confirmButton={TextButton(onClick={finished=null;page="quests"}){Text("AWESOME!")}})}
    }
}
@Composable fun ReadyScreen(step:Int,quest:String){Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text(quest.uppercase(),color=Soft,fontSize=17.sp);Spacer(Modifier.height(30.dp));Text(listOf("READY?","SET...","GO!")[step],color=Lime,fontSize=65.sp,fontWeight=FontWeight.Black)}}

@Composable fun QuestsPage(store:Store,version:Int,onStart:(Quest)->Unit,onChanged:()->Unit){
    val quests=remember(version){store.quests()};var adding by remember{mutableStateOf(false)}
    val art=listOf(R.drawable.quest_shirt,R.drawable.quest_food,R.drawable.quest_book,R.drawable.quest_tooth,R.drawable.quest_bag,R.drawable.quest_brain)
    val accents=listOf(Color(0xFFFFBC5E),Color(0xFF71D9FA),Color(0xFFE8A3FF),Color(0xFF79E3B0),Color(0xFFB9B3FF),Color(0xFFFFE088))
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        Text("PLAYER ONE  /  LEVEL 08",color=Soft,fontWeight=FontWeight.Bold,fontSize=12.sp,letterSpacing=1.sp)
        Text("HEY, ${store.name.uppercase()}!",color=White,fontWeight=FontWeight.Black,fontSize=31.sp)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(204.dp).clip(RoundedCornerShape(22.dp)).background(Panel)){
            Image(painterResource(R.drawable.quest_hero),contentDescription="Gorilla adventurer on a floating jungle island",contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xDD0A1518)))).height(92.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)){
                Text("TODAY'S ADVENTURE",fontSize=12.sp,fontWeight=FontWeight.Black,color=Lime)
                Text("YOUR WORLD IS WAITING!",fontSize=18.sp,fontWeight=FontWeight.Black,color=White)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Text("PICK YOUR NEXT QUEST",fontWeight=FontWeight.Black,fontSize=18.sp)
            Text("${store.badges} BADGES",fontSize=11.sp,fontWeight=FontWeight.Bold,color=Lime)
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)){
            items(quests.chunked(2)){pair->
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    pair.forEach{q->
                        val i=quests.indexOf(q);val accent=accents[i%accents.size];val best=store.best(q.id)
                        Column(Modifier.weight(1f).height(173.dp).clip(RoundedCornerShape(19.dp))
                            .background(Brush.verticalGradient(listOf(accent.copy(alpha=.26f),Color(0xFF17252C))))
                            .clickable{onStart(q)}.padding(12.dp),verticalArrangement=Arrangement.SpaceBetween){
                            Image(painterResource(art[i%art.size]),contentDescription=null,modifier=Modifier.size(82.dp))
                            Text(q.name,fontSize=16.sp,fontWeight=FontWeight.Black,maxLines=1,color=White)
                            Text("%02d:%02d  ▶".format(q.minutes,0),fontSize=13.sp,fontWeight=FontWeight.Black,color=accent)
                            if(best!=null)Text("BEST %02d:%02d".format(best/60,best%60),fontSize=10.sp,color=Soft)
                        }
                    }
                    if(pair.size==1)Spacer(Modifier.weight(1f))
                }
            }
            item{
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Color(0xFF253D36)).padding(13.dp),verticalAlignment=Alignment.CenterVertically){
                    Image(painterResource(R.drawable.quest_chest),contentDescription=null,modifier=Modifier.size(86.dp))
                    Column(Modifier.weight(1f)){
                        Text("WISH VAULT",fontWeight=FontWeight.Black,fontSize=20.sp)
                        Text("${store.badges} / 10 badges · Unlock surprises",color=Soft,fontSize=12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick={adding=true},modifier=Modifier.fillMaxWidth().height(54.dp)){
                    Icon(Icons.Default.Add,null);Text("  CREATE A QUEST")
                }
            }
        }
    }
    if(adding){var name by remember{mutableStateOf("")};var mins by remember{mutableStateOf("5")};var speed by remember{mutableStateOf(true)}
        AlertDialog(onDismissRequest={adding=false},title={Text("New quest")},text={Column{OutlinedTextField(name,{name=it},label={Text("Mission name")});OutlinedTextField(mins,{mins=it},label={Text("Target minutes")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));Row(verticalAlignment=Alignment.CenterVertically){Checkbox(speed,{speed=it});Text("Speed-based badges")}}},confirmButton={TextButton(onClick={val m=mins.toIntOrNull();if(name.isNotBlank()&&m!=null&&m in 1..180){store.saveQuests(store.quests()+Quest("custom_${System.currentTimeMillis()}",name.trim(),"✦",m,speed));onChanged();adding=false}}){Text("CREATE")}},dismissButton={TextButton(onClick={adding=false}){Text("CANCEL")}})
    }
}
@Composable fun ActiveScreen(store:Store,service:(String,String?)->Unit,onDone:()->Unit){
    val q=QuestClock.quest?:return
    val seconds=QuestClock.remainingSeconds()
    val progress=(QuestClock.remainingMs.toFloat()/QuestClock.limitMs.coerceAtLeast(1L)).coerceIn(0f,1f)
    val best=store.best(q.id)
    var bursts by remember { mutableIntStateOf(0) }
    var touch by remember { mutableStateOf(Offset.Zero) }
    val pulse=rememberInfiniteTransition(label="particle motion")
    val phase by pulse.animateFloat(initialValue=0f,targetValue=2f*PI.toFloat(),
        animationSpec=infiniteRepeatable(tween(6500,easing=LinearEasing)),label="orbit")
    val breathing by pulse.animateFloat(initialValue=.88f,targetValue=1.12f,
        animationSpec=infiniteRepeatable(tween(950, easing=FastOutSlowInEasing),RepeatMode.Reverse),label="breath")
    val urgent=seconds in 1..30
    val accent=if(urgent) Color(0xFFFFD16D) else Lime
    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal=20.dp,vertical=18.dp),
        horizontalAlignment=Alignment.CenterHorizontally){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Text("TURBO RONI",color=Lime,fontWeight=FontWeight.Black,fontSize=17.sp,letterSpacing=1.sp)
            IconButton(onClick={store.soundOn=!store.soundOn}) {
                Icon(if(store.soundOn)Icons.Default.VolumeUp else Icons.Default.VolumeOff,"Sound",tint=White)
            }
        }
        Spacer(Modifier.height(15.dp))
        Text(q.name.uppercase(),fontSize=18.sp,color=White,fontWeight=FontWeight.Black,letterSpacing=1.sp)
        Spacer(Modifier.weight(.6f))
        Box(Modifier.fillMaxWidth().aspectRatio(1f)
            .pointerInput(Unit){detectTapGestures(onTap={touch=it;bursts++})}
            .pointerInput(Unit){detectDragGestures(onDragStart={touch=it;bursts++},onDrag={change,_->touch=change.position;bursts++;change.consume()})},
            contentAlignment=Alignment.Center){
            Canvas(Modifier.fillMaxSize()){
                val radius=size.minDimension*.41f
                val centerPoint=center
                // Continuously orbiting particles. All movement is decorative and never changes the timer.
                for(i in 0 until 115){
                    val a=i*2.399963f+phase*(if(i%2==0)1f else -.55f)
                    val distance=sqrt((i+1)/115f)*radius*.91f
                    val point=Offset(centerPoint.x+cos(a)*distance,centerPoint.y+sin(a)*distance)
                    drawCircle(if(i%5==0)accent else Color(0xFF4FA780),
                        radius=if(i%8==0)3.5.dp.toPx()*breathing else 1.5.dp.toPx(),
                        center=point,alpha=.2f+(i%7)*.065f)
                }
                // Tapping or dragging creates a little spiral of sparkling stars.
                if(bursts>0){
                    for(i in 0 until 26){
                        val a=i*2.399963f+phase*2f+bursts*.15f
                        val d=6.dp.toPx()+sqrt((i+1)/26f)*34.dp.toPx()
                        drawCircle(if(i%2==0)accent else White,radius=(1.4f+(i%3)).dp.toPx(),
                            center=Offset(touch.x+cos(a)*d,touch.y+sin(a)*d),alpha=.65f)
                    }
                }
            }
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                LivingDigits(seconds,q.minutes*60,Modifier.fillMaxWidth().height(190.dp))
                Text(if(QuestClock.completed)"FINISHED!" else if(QuestClock.paused)"PAUSED" else if(urgent)"FINAL STRETCH" else "LET'S GO!",
                    color=accent,fontSize=13.sp,fontWeight=FontWeight.Bold,letterSpacing=1.3.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("BEST  ${best?.let{"%02d:%02d".format(it/60,it%60)}?:"--:--"}",
            color=Soft,fontWeight=FontWeight.Bold,fontSize=13.sp,letterSpacing=1.sp)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            OutlinedButton(onClick={service(if(QuestClock.paused)"RESUME" else "PAUSE",null)},
                modifier=Modifier.weight(1f).height(72.dp),shape=RoundedCornerShape(22.dp)){
                Icon(if(QuestClock.paused)Icons.Default.PlayArrow else Icons.Default.Pause,null)
                Text(if(QuestClock.paused)" PLAY" else " PAUSE",fontWeight=FontWeight.Bold)
            }
            Button(onClick=onDone,modifier=Modifier.weight(1.65f).height(72.dp),
                shape=RoundedCornerShape(22.dp)){
                Icon(Icons.Default.Flag,null)
                Text("  I'M DONE!",fontWeight=FontWeight.Black,fontSize=16.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable fun VaultPage(store:Store,version:Int,onChanged:()->Unit){
    var selected by remember{mutableStateOf<Prize?>(null)}
    var add by remember{mutableStateOf(false)}
    val prizes=remember(version){store.prizes()}
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
        item{Text("The Wish Vault",fontSize=28.sp,fontWeight=FontWeight.Black);Text("Collect 10 badges to unlock a wish!",color=Soft)}
        item{Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("🏆",fontSize=45.sp);Text("${store.badges} BADGES",fontSize=30.sp,color=Lime,fontWeight=FontWeight.Black);LinearProgressIndicator(progress={((store.badges%10)/10f)},modifier=Modifier.fillMaxWidth().padding(top=15.dp),color=Lime)}}
        item{Text("YOUR REWARDS",fontWeight=FontWeight.Black,letterSpacing=1.sp)}
        items(prizes){prize->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text("🎁",fontSize=30.sp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(prize.name,fontWeight=FontWeight.Bold);Text("${prize.cost} badges",color=Soft,fontSize=12.sp)};Button(enabled=store.badges>=prize.cost,onClick={selected=prize}){Text("UNLOCK")}}}
        item{OutlinedButton(onClick={add=true},modifier=Modifier.fillMaxWidth()){Text("+ Add a family reward")}}
        item{Text("PAST REDEMPTIONS  ${store.redemptions}",color=Soft,fontSize=12.sp)}
        items(store.tickets().reversed()){Text("✓ $it",color=Soft,fontSize=12.sp)}
    }
    selected?.let{prize->AlertDialog(onDismissRequest={selected=null},title={Text("Parent confirmation")},text={Text("Redeem ${prize.cost} badges for ${prize.name}? Ask a parent before confirming.")},confirmButton={TextButton(onClick={store.redeem(prize);selected=null;onChanged()}){Text("CONFIRM")}},dismissButton={TextButton(onClick={selected=null}){Text("CANCEL")}})}
    if(add){var name by remember{mutableStateOf("")};AlertDialog(onDismissRequest={add=false},title={Text("New family reward")},text={OutlinedTextField(name,{name=it},label={Text("Reward name")})},confirmButton={TextButton(onClick={if(name.isNotBlank()){store.savePrizes(store.prizes()+Prize("prize_${System.currentTimeMillis()}",name.trim()));add=false;onChanged()}}){Text("ADD")}},dismissButton={TextButton(onClick={add=false}){Text("CANCEL")}})}
}

@Composable fun StopwatchPage(){
    var running by remember{mutableStateOf(false)};var elapsed by remember{mutableLongStateOf(0L)};var start by remember{mutableLongStateOf(0L)};val laps=remember{mutableStateListOf<Long>()}
    LaunchedEffect(running){if(running){start=android.os.SystemClock.elapsedRealtime()-elapsed;while(running){elapsed=android.os.SystemClock.elapsedRealtime()-start;delay(40)}}}
    Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("Speed Lab",fontWeight=FontWeight.Black,fontSize=30.sp);Text("Race yourself, not anyone else.",color=Soft);Spacer(Modifier.height(80.dp));Text("%02d:%02d.%02d".format(elapsed/60000,(elapsed/1000)%60,(elapsed%1000)/10),fontWeight=FontWeight.Black,fontSize=47.sp,color=Lime);Spacer(Modifier.height(36.dp));Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Button(onClick={running=!running}){Text(if(running)"PAUSE" else "START")};OutlinedButton(onClick={if(running)laps.add(elapsed) else {elapsed=0;laps.clear()}}){Text(if(running)"LAP" else "RESET")}};Spacer(Modifier.height(28.dp));laps.forEachIndexed{index,l->Text("Lap ${index+1}  •  %.2f sec".format(l/1000f),color=Soft)}}
}

@Composable fun SettingsPage(store:Store,onChanged:()->Unit){
    var name by remember{mutableStateOf(store.name)};var avatar by remember{mutableStateOf(store.avatar)};var sound by remember{mutableStateOf(store.soundOn)};var voice by remember{mutableStateOf(store.voiceOn)};var ticking by remember{mutableStateOf(store.ticking)};var tickMode by remember{mutableIntStateOf(store.tickMode)};var pack by remember{mutableIntStateOf(store.pack)}
    val context=LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Text("Player Settings",fontSize=28.sp,fontWeight=FontWeight.Black);Text("Make Turbo Roni your own.",color=Soft)}
        item{Text("Choose your monster",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("🦍","👾","🐸","🦖","🚀").forEach{e->Box(Modifier.size(52.dp).clip(CircleShape).background(if(avatar==e)Lime else Panel).clickable{avatar=e;store.avatar=e;onChanged()},contentAlignment=Alignment.Center){Text(e,fontSize=27.sp)}}}}
        item{OutlinedTextField(name,{name=it;store.name=it;onChanged()},label={Text("Player name")},modifier=Modifier.fillMaxWidth())}
        item{SettingSwitch("Sound effects",sound){sound=it;store.soundOn=it}}
        item{SettingSwitch("Voice coach",voice){voice=it;store.voiceOn=it}}
        item{SettingSwitch("Clock ticking sound",ticking){ticking=it;store.ticking=it}}
        item{Text("WHEN SHOULD THE CLOCK TICK?",fontWeight=FontWeight.Black,letterSpacing=1.sp)}
        items(listOf("Only the final 30 seconds","Every second","Last 60 seconds").indices.toList()){i->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel)
                .clickable{tickMode=i;store.tickMode=i}.padding(12.dp),
                verticalAlignment=Alignment.CenterVertically){
                RadioButton(selected=tickMode==i,onClick={tickMode=i;store.tickMode=i})
                Text(listOf("Last 30 seconds","All the way","Last 60 seconds")[i])
            }
        }
        item{Text("SOUND WORLD",fontWeight=FontWeight.Black,letterSpacing=1.sp)}
        items(listOf("Monster Choir","Arcade Rush","Jungle Beat","Classic Clock","Cartoon Boing").indices.toList()){i->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).clickable{pack=i;store.pack=i}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=pack==i,onClick={pack=i;store.pack=i})
                Text(listOf("Monster Choir","Arcade Rush","Jungle Beat","Classic Clock","Cartoon Boing")[i],modifier=Modifier.weight(1f))
                TextButton(onClick={
                    val resource=context.resources.getIdentifier("pack${i}_tick","raw",context.packageName)
                    if(resource!=0)android.media.MediaPlayer.create(context,resource)?.apply{
                        setOnCompletionListener{it.release()};start()
                    }
                }){Text("▶")}
}}
        item{Text("Original synthesized ticking and musical cues. Choose a clock style and how often it ticks.",color=Soft,fontSize=12.sp)}
    }
}
@Composable fun SettingSwitch(label:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,modifier=Modifier.weight(1f));Switch(checked=checked,onCheckedChange=onChange)}}
