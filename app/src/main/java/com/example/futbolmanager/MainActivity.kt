package com.example.futbolmanager

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ads.init(this)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { GameApp(this@MainActivity) }
            }
        }
    }
}

enum class Screen { MENU, EDITOR, EDIT_TEAM, LINEUP, MATCH, LOST, CHAMPION }

@Composable
fun GameApp(activity: Activity) {
    val prefs = remember { activity.getSharedPreferences("nombres", Context.MODE_PRIVATE) }
    val teams = remember { buildTeams().also { applySaved(prefs, it) } }
    var screen by remember { mutableStateOf(Screen.MENU) }
    var editId by remember { mutableIntStateOf(0) }
    var myId by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }
    var rivals by remember { mutableStateOf(listOf<Int>()) }
    val energy = remember { mutableStateMapOf<Int, Int>() }
    var formation by remember { mutableStateOf(formations[0]) }
    val selected = remember { mutableStateListOf<Int>() }
    var result by remember { mutableStateOf<MatchResult?>(null) }
    var starters by remember { mutableStateOf(listOf<Player>()) }

    fun en(p: Player): Int = energy[p.id] ?: 100
    fun autoPick(f: Formation) {
        selected.clear()
        selected.addAll(autoLineup(teams[myId], f) { en(it) }.map { it.id })
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Box(Modifier.weight(1f)) {
            when (screen) {
                Screen.MENU -> MenuScreen(
                    teams,
                    onPick = { id ->
                        myId = id
                        round = 0
                        rivals = teams.map { it.id }.filter { it != id }.shuffled().take(3)
                        energy.clear()
                        formation = formations[0]
                        autoPick(formation)
                        screen = Screen.LINEUP
                    },
                    onEdit = { screen = Screen.EDITOR }
                )
                Screen.EDITOR -> EditorScreen(
                    teams,
                    onTeam = { editId = it; screen = Screen.EDIT_TEAM },
                    onBack = { screen = Screen.MENU }
                )
                Screen.EDIT_TEAM -> EditTeamScreen(teams[editId], prefs) { screen = Screen.EDITOR }
                Screen.LINEUP -> LineupScreen(
                    me = teams[myId],
                    rival = teams[rivals[round]],
                    roundName = roundNames[round],
                    formation = formation,
                    selected = selected,
                    energyOf = { en(it) },
                    onFormation = { formation = it; autoPick(it) },
                    onAuto = { autoPick(formation) },
                    onPlay = {
                        val me = teams[myId]
                        val opp = teams[rivals[round]]
                        starters = me.players.filter { it.id in selected }
                        val oppLineup = autoLineup(opp, formations[Random.nextInt(formations.size)]) { 100 }
                        result = simulate(
                            me, starters, { effRating(it, en(it)) },
                            opp, oppLineup, { it.rating.toDouble() },
                            Random
                        )
                        screen = Screen.MATCH
                    }
                )
                Screen.MATCH -> result?.let { r ->
                    MatchScreen(teams[myId], teams[rivals[round]], r) {
                        if (r.userWon) {
                            val ids = starters.map { it.id }.toSet()
                            teams[myId].players.forEach { p ->
                                val e = en(p)
                                energy[p.id] = if (p.id in ids) (e - 30).coerceAtLeast(30)
                                else (e + 25).coerceAtMost(100)
                            }
                            if (round >= 2) {
                                screen = Screen.CHAMPION
                            } else {
                                round++
                                autoPick(formation)
                                screen = Screen.LINEUP
                            }
                        } else {
                            screen = Screen.LOST
                        }
                    }
                }
                Screen.LOST -> LostScreen(
                    roundName = roundNames[round],
                    onRetry = {
                        Ads.showRewarded(
                            activity,
                            onReward = { screen = Screen.LINEUP },
                            onFail = { screen = Screen.LINEUP }
                        )
                    },
                    onQuit = { screen = Screen.MENU }
                )
                Screen.CHAMPION -> CenterScreen("🏆 ¡Campeón con ${teams[myId].name}!", "Has ganado el torneo", "Volver al menú") {
                    screen = Screen.MENU
                }
            }
        }
        BannerAd()
    }
}

@Composable
fun MenuScreen(teams: List<Team>, onPick: (Int) -> Unit, onEdit: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("⚽ Copa Provincial", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Elige tu equipo y gana el torneo", Modifier.padding(bottom = 12.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(teams) { t ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onPick(t.id) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(t.name, fontSize = 18.sp)
                        Text("Nivel ${teamLevel(t)}")
                    }
                }
            }
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text("✏️ Editar equipos y jugadores")
        }
    }
}

@Composable
fun EditorScreen(teams: List<Team>, onTeam: (Int) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Editor", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Toca un equipo para cambiar su nombre y el de sus jugadores", Modifier.padding(bottom = 8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(teams) { t ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onTeam(t.id) }) {
                    Text(t.name, Modifier.padding(16.dp), fontSize = 18.sp)
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}

@Composable
fun EditTeamScreen(team: Team, prefs: SharedPreferences, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Guardar y volver") }
        LazyColumn(Modifier.weight(1f)) {
            item {
                OutlinedTextField(
                    value = team.name,
                    onValueChange = {
                        team.name = it
                        prefs.edit().putString("t${team.id}", it).apply()
                    },
                    label = { Text("Nombre del equipo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                )
            }
            items(team.players) { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.pos.label, Modifier.width(44.dp))
                    OutlinedTextField(
                        value = p.name,
                        onValueChange = {
                            p.name = it
                            prefs.edit().putString("p${p.id}", it).apply()
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LineupScreen(
    me: Team,
    rival: Team,
    roundName: String,
    formation: Formation,
    selected: MutableList<Int>,
    energyOf: (Player) -> Int,
    onFormation: (Formation) -> Unit,
    onAuto: () -> Unit,
    onPlay: () -> Unit
) {
    fun count(pos: Pos) = me.players.count { it.id in selected && it.pos == pos }
    fun need(pos: Pos) = when (pos) {
        Pos.POR -> 1
        Pos.DEF -> formation.def
        Pos.MED -> formation.med
        Pos.DEL -> formation.del
    }
    val valid = Pos.values().all { count(it) == need(it) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("$roundName vs ${rival.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Nivel rival: ${teamLevel(rival)}")
        Row(Modifier.padding(vertical = 6.dp)) {
            formations.forEach { f ->
                val colors = if (f == formation) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                Button(onClick = { onFormation(f) }, colors = colors, modifier = Modifier.padding(end = 6.dp)) {
                    Text(f.name)
                }
            }
        }
        Text(Pos.values().joinToString("   ") { "${it.label} ${count(it)}/${need(it)}" })
        Row(Modifier.padding(vertical = 6.dp)) {
            OutlinedButton(onClick = onAuto, modifier = Modifier.padding(end = 8.dp)) { Text("Automático") }
            Button(onClick = onPlay, enabled = valid) { Text("⚽ Jugar partido") }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(me.players.sortedWith(compareBy({ it.pos.ordinal }, { -it.rating }))) { p ->
                val on = p.id in selected
                Row(
                    Modifier.fillMaxWidth()
                        .clickable {
                            if (on) selected.remove(p.id)
                            else if (count(p.pos) < need(p.pos)) selected.add(p.id)
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = on, onCheckedChange = null)
                    Text(p.pos.label, Modifier.width(44.dp).padding(start = 8.dp))
                    Text(p.name, Modifier.weight(1f))
                    Text("${p.rating} · ⚡${energyOf(p)}%")
                }
            }
        }
    }
}

@Composable
fun MatchScreen(me: Team, rival: Team, res: MatchResult, onDone: () -> Unit) {
    var minute by remember(res) { mutableIntStateOf(0) }
    LaunchedEffect(res) {
        while (minute < 90) {
            delay(300)
            minute++
        }
    }
    val shown = res.events.filter { it.minute <= minute }
    val last = shown.lastOrNull()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "${me.name}  ${last?.homeGoals ?: 0} - ${last?.awayGoals ?: 0}  ${rival.name}",
            fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        Text("⏱ $minute'", Modifier.padding(vertical = 6.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(shown.reversed()) { e -> Text("${e.minute}'  ${e.text}", Modifier.padding(vertical = 2.dp)) }
        }
        if (minute < 90) {
            OutlinedButton(onClick = { minute = 90 }, modifier = Modifier.fillMaxWidth()) { Text("Saltar") }
        } else {
            res.penalties?.let { Text(it, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp)) }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Continuar") }
        }
    }
}

@Composable
fun LostScreen(roundName: String, onRetry: () -> Unit, onQuit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("😞 Eliminado", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Has perdido en $roundName", Modifier.padding(vertical = 12.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("▶ Ver anuncio y repetir ronda") }
        OutlinedButton(onClick = onQuit, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Abandonar") }
    }
}

@Composable
fun CenterScreen(title: String, subtitle: String, button: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, Modifier.padding(vertical = 12.dp))
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(button) }
    }
}
