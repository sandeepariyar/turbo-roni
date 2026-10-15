/*
 * Turbo Roni
 * Copyright (c) 2026 Sandeep Pariyar
 */

package com.turboroni.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Quest(val id: String, val name: String, val icon: String, val minutes: Int, val speed: Boolean = true)
data class Prize(val id: String, val name: String, val cost: Int = 10)
data class Record(val questId: String, val seconds: Int, val tier: Int, val at: Long)

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("turbo_roni", Context.MODE_PRIVATE)
    var name: String
        get() = prefs.getString("name", "Roni") ?: "Roni"
        set(v) { prefs.edit().putString("name", v).apply() }
    var avatar: String
        get() = prefs.getString("avatar", "🦍") ?: "🦍"
        set(v) { prefs.edit().putString("avatar", v).apply() }
    var soundOn: Boolean
        get() = prefs.getBoolean("sound", true)
        set(v) { prefs.edit().putBoolean("sound", v).apply() }
    var voiceOn: Boolean
        get() = prefs.getBoolean("voice", true)
        set(v) { prefs.edit().putBoolean("voice", v).apply() }
    var ticking: Boolean
        get() = prefs.getBoolean("tick", true)
        set(v) { prefs.edit().putBoolean("tick", v).apply() }
    var tickMode: Int
        get() = prefs.getInt("tick_mode", 0)
        set(v) { prefs.edit().putInt("tick_mode", v.coerceIn(0,2)).apply() }
    var pack: Int
        get() = prefs.getInt("pack", 0)
        set(v) { prefs.edit().putInt("pack", v).apply() }
    var badges: Int
        get() = prefs.getInt("badges", 0)
        set(v) { prefs.edit().putInt("badges", v.coerceAtLeast(0)).apply() }
    var redemptions: Int
        get() = prefs.getInt("redemptions", 0)
        set(v) { prefs.edit().putInt("redemptions", v).apply() }
    fun quests(): List<Quest> {
        val raw = prefs.getString("quests", null) ?: return listOf(
            Quest("dress", "Dressing Up", "👕", 5), Quest("eat", "Eating Champ", "🥣", 15, false),
            Quest("home", "Homework Hero", "📚", 20, false), Quest("teeth", "Brush Teeth", "🪥", 2, false),
            Quest("toys", "Toy Cleanup", "🧸", 5), Quest("bag", "School Bag", "🎒", 3))
        return runCatching { val a = JSONArray(raw); (0 until a.length()).map { val o = a.getJSONObject(it); Quest(o.getString("id"), o.getString("name"), o.optString("icon", "⚡"), o.getInt("minutes"), o.optBoolean("speed", true)) } }.getOrDefault(emptyList())
    }
    fun saveQuests(q: List<Quest>) { val a = JSONArray(); q.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon).put("minutes", it.minutes).put("speed", it.speed)) }; prefs.edit().putString("quests", a.toString()).apply() }
    fun records(): List<Record> { val a = JSONArray(prefs.getString("records", "[]")); return (0 until a.length()).map { val o = a.getJSONObject(it); Record(o.getString("id"), o.getInt("seconds"), o.getInt("tier"), o.getLong("at")) } }
    fun addRecord(record: Record) { val a = JSONArray(prefs.getString("records", "[]")); a.put(JSONObject().put("id", record.questId).put("seconds", record.seconds).put("tier", record.tier).put("at", record.at)); prefs.edit().putString("records", a.toString()).apply() }
    fun best(id: String): Int? = records().filter { it.questId == id }.minOfOrNull { it.seconds }
    fun prizes(): List<Prize> { val raw = prefs.getString("prizes", null) ?: return listOf(Prize("game", "Extra game time: 30 min"), Prize("movie", "Choose family movie"), Prize("wish", "My special wish")); val a=JSONArray(raw);return (0 until a.length()).map { val o=a.getJSONObject(it);Prize(o.getString("id"),o.getString("name"),o.getInt("cost")) } }
    fun savePrizes(prizes: List<Prize>) { val a=JSONArray();prizes.forEach { a.put(JSONObject().put("id",it.id).put("name",it.name).put("cost",it.cost)) };prefs.edit().putString("prizes",a.toString()).apply() }
    fun redeem(prize: Prize): Boolean { if(badges < prize.cost)return false; badges -= prize.cost; redemptions += 1; val a=JSONArray(prefs.getString("tickets","[]"));a.put(JSONObject().put("name",prize.name).put("at",System.currentTimeMillis()));prefs.edit().putString("tickets",a.toString()).apply();return true }
    fun tickets(): List<String> { val a=JSONArray(prefs.getString("tickets","[]"));return (0 until a.length()).map { a.getJSONObject(it).getString("name") } }
}

fun tier(elapsed: Int, limit: Int, speed: Boolean): Int {
    if (elapsed > limit) return 0
    if (!speed) return 1
    return when { elapsed <= limit*.4 -> 4; elapsed <= limit*.6 -> 3; elapsed <= limit*.8 -> 2; else -> 1 }
}
fun tierName(t: Int) = when(t) { 4 -> "DIAMOND MONSTER"; 3 -> "GOLD LIGHTNING"; 2 -> "SILVER ROCKET"; 1 -> "BRONZE STAR"; else -> "MISSION FINISHED" }
