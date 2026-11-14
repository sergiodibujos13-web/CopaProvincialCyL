package com.example.futbolmanager

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ads.init(this)
        setContent {
            AppTheme { GameApp(this@MainActivity) }
        }
    }
}

enum class Screen { MENU, EDITOR, EDIT_TEAM, HUB, LINEUP, MATCH, TABLE, MARKET, MARKET_TEAM, SQUAD }

@Composable
fun GameApp(activity: Activity) {
    val prefs = remember { activity.getSharedPreferences("nombres", Context.MODE_PRIVATE) }
    val saveKey = "partida"
    var screen by remember { mutableStateOf(Screen.MENU) }
    var game by remember { mutableStateOf<Game?>(null) }
    val editWorld = remember { buildWorld().also { applySaved(prefs, it) } }
    var editId by remember { mutableIntStateOf(0) }
    var marketId by remember { mutableIntStateOf(0) }
    var formation by remember { mutableStateOf(formations[0]) }
    val selected = remember { mutableStateListOf<Int>() }
    var result by remember { mutableStateOf<MatchResult?>(null) }
    var starters by remember { mutableStateOf(listOf<Player>()) }
    var lastMsg by remember { mutableStateOf("") }
    val g = game
    val tick = g?.tick ?: 0

    LaunchedEffect(tick, g) {
        if (g != null) prefs.edit().putString(saveKey, g.toJson()).apply()
    }

    fun autoPick(f: Formation) {
        val gg = game ?: return
        selected.clear()
        selected.addAll(autoLineup(gg.me, f) { gg.en(it) }.map { it.id })
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Box(Modifier.weight(1f)) {
            when (screen) {
                Screen.MENU -> MenuScreen(
                    hasSave = prefs.contains(saveKey),
                    teams = editWorld.filter { it.id < 100 },
                    onNew = { id ->
                        val w = buildWorld().also { applySaved(prefs, it) }
                        val ng = Game(w, id)
                        ng.startSeason(ng.league.sortedByDescending { teamLevel(it) }.map { it.id })
                        game = ng
                        lastMsg = ""
                        formation = formations[0]
                        screen = Screen.HUB
                    },
                    onContinue = {
                        val txt = prefs.getString(saveKey, null)
                        if (txt != null) {
                            try {
                                game = Game.fromJson(txt)
                                lastMsg = ""
                                screen = Screen.HUB
                            } catch (e: Exception) {
                                prefs.edit().remove(saveKey).apply()
                            }
                        }
                    },
                    onEdit = { screen = Screen.EDITOR }
                )
                Screen.EDITOR -> EditorScreen(
                    editWorld,
                    onTeam = { editId = it; screen = Screen.EDIT_TEAM },
                    onBack = { screen = Screen.MENU }
                )
                Screen.EDIT_TEAM -> EditTeamScreen(editWorld.first { it.id == editId }, prefs) {
                    screen = Screen.EDITOR
                }
                Screen.HUB -> if (g != null) {
                    if (g.seasonDone) {
                        SeasonEndScreen(
                            g, tick,
                            onAd = {
                                Ads.showRewarded(
                                    activity,
                                    onReward = { g.budget += 10; g.adBonus = true; g.tick++ },
                                    onFail = {}
                                )
                            },
                            onNext = { g.nextSeason(); lastMsg = "" }
                        )
                    } else {
                        HubScreen(
                            g, tick, lastMsg,
                            onPlay = { autoPick(formation); screen = Screen.LINEUP },
                            onSim = {
                                val st = autoLineup(g.me, formation) { g.en(it) }
                                val r = g.playMatch(st)
                                lastMsg = g.describe(r)
                                g.applyUserResult(r, st)
                            },
                            onTable = { screen = Screen.TABLE },
                            onMarket = { screen = Screen.MARKET },
                            onSquad = { screen = Screen.SQUAD },
                            onAd = {
                                Ads.showRewarded(
                                    activity,
                                    onReward = { g.budget += 5; g.tick++ },
                                    onFail = {}
                                )
                            },
                            onMenu = { screen = Screen.MENU }
                        )
                    }
                }
                Screen.LINEUP -> if (g != null && g.current != null) {
                    LineupScreen(
                        me = g.me,
                        rival = g.byId(g.opponent()!!),
                        title = g.slotTitle(g.current!!) + (if (g.isHome()) " · 🏟 En casa" else " · ✈ Fuera"),
                        formation = formation,
                        selected = selected,
                        energyOf = { g.en(it) },
                        onFormation = { formation = it; autoPick(it) },
                        onAuto = { autoPick(formation) },
                        onPlay = {
                            starters = g.me.players.filter { it.id in selected }
                            result = g.playMatch(starters)
                            screen = Screen.MATCH
                        },
                        onBack = { screen = Screen.HUB }
                    )
                }
                Screen.MATCH -> if (g != null && g.current != null) {
                    val r = result
                    if (r != null) {
                        MatchScreen(
                            me = g.me,
                            rival = g.byId(g.opponent()!!),
                            res = r,
                            isHome = g.isHome(),
                            onContinue = {
                                lastMsg = g.describe(r)
                                g.applyUserResult(r, starters)
                                screen = Screen.HUB
                            }
                        )
                    }
                }
                Screen.TABLE -> if (g != null) TableScreen(g, tick) { screen = Screen.HUB }
                Screen.MARKET -> if (g != null) {
                    MarketScreen(g, tick, onTeam = { marketId = it; screen = Screen.MARKET_TEAM }, onBack = { screen = Screen.HUB })
                }
                Screen.MARKET_TEAM -> if (g != null) {
                    MarketTeamScreen(g, g.byId(marketId), tick) { screen = Screen.MARKET }
                }
                Screen.SQUAD -> if (g != null) SquadScreen(g, tick) { screen = Screen.HUB }
            }
        }
        BannerAd()
    }
}
