package com.example.futbolmanager

import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val Gold = Color(0xFFFFC107)

@Composable
fun MenuScreen(
    hasSave: Boolean,
    teams: List<Team>,
    onNew: (Int) -> Unit,
    onContinue: () -> Unit,
    onEdit: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Image(
            painter = painterResource(R.drawable.logo_liga),
            contentDescription = "Liga Provincial",
            modifier = Modifier.size(170.dp).align(Alignment.CenterHorizontally)
        )
        if (hasSave) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("▶ Continuar partida")
            }
        }
        Text(
            if (hasSave) "O empieza una nueva tocando un equipo (borra la guardada):" else "Elige tu equipo para empezar:",
            Modifier.padding(vertical = 6.dp)
        )
        LazyColumn(Modifier.weight(1f)) {
            items(teams) { t ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onNew(t.id) }) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Crest(t, 36.dp)
                        Text(t.name, Modifier.weight(1f).padding(start = 12.dp), fontSize = 18.sp)
                        Text("Nivel ${teamLevel(t)}")
                    }
                }
            }
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text("✏️ Editar equipos y jugadores")
        }
        Text(
            "Creado por Sergio Barrero Riaño",
            Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally),
            fontSize = 12.sp
        )
    }
}

@Composable
fun EditorScreen(teams: List<Team>, onTeam: (Int) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Editor", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Toca un equipo para cambiar su nombre y el de sus jugadores (vale para partidas nuevas)", Modifier.padding(bottom = 8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(teams) { t ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onTeam(t.id) }) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Crest(t, 30.dp)
                        Text(t.name, Modifier.padding(start = 12.dp), fontSize = 18.sp)
                    }
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
fun HubScreen(
    g: Game,
    tick: Int,
    msg: String,
    onPlay: () -> Unit,
    onSim: () -> Unit,
    onTable: () -> Unit,
    onMarket: () -> Unit,
    onSquad: () -> Unit,
    onPalmares: () -> Unit,
    onAd: () -> Unit,
    onMenu: () -> Unit
) {
    val s = g.current
    val opp = g.opponent()?.let { g.byId(it) }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Crest(g.me, 56.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(g.me.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Temporada ${g.season} · Puesto ${g.position()}º")
                    }
                    Text("💰 ${g.budget} M€", fontWeight = FontWeight.Bold, color = Gold)
                }
            }
        }
        if (s != null && opp != null) {
            item {
                Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Próximo: ${g.slotTitle(s)}", fontWeight = FontWeight.Bold)
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (g.isHome()) {
                                Crest(g.me, 40.dp)
                                Text("  vs  ")
                                Crest(opp, 40.dp)
                            } else {
                                Crest(opp, 40.dp)
                                Text("  vs  ")
                                Crest(g.me, 40.dp)
                            }
                            Text("  ${opp.name}", fontSize = 18.sp)
                        }
                        Text(if (g.isHome()) "🏟 En casa" else "✈ Fuera")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Button(onClick = onPlay, modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("⚽ Jugar") }
                OutlinedButton(onClick = onSim, modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("⏩ Simular") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                OutlinedButton(onClick = onTable, modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("📊 Tabla") }
                OutlinedButton(onClick = onMarket, modifier = Modifier.weight(1f).padding(horizontal = 2.dp)) { Text("🔁 Fichajes") }
                OutlinedButton(onClick = onSquad, modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("👥 Plantilla") }
            }
        }
        item {
            OutlinedButton(onClick = onPalmares, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("🏆 Palmarés") }
        }
        item {
            Text(
                g.windowText() + (if (g.offers.isNotEmpty() && g.windowOpen()) " · 📩 ${g.offers.size} oferta(s)" else ""),
                Modifier.padding(top = 8.dp),
                fontSize = 13.sp
            )
        }
        item {
            Button(onClick = onAd, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text("▶ Ver anuncio y ganar +5 M€")
            }
        }
        if (msg.isNotEmpty()) {
            item { Text(msg, Modifier.padding(top = 10.dp), color = Gold) }
        }
        item { Text("📅 Calendario", Modifier.padding(top = 12.dp, bottom = 4.dp), fontWeight = FontWeight.Bold) }
        items(g.upcoming(8)) { line -> Text("• $line", Modifier.padding(vertical = 2.dp)) }
        item {
            OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("Menú principal (se guarda solo)")
            }
        }
    }
}

@Composable
fun TableScreen(g: Game, tick: Int, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            listOf("Liga", "Copa del Rey", "Copa de CyL").forEachIndexed { i, n ->
                val colors = if (tab == i) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                Button(
                    onClick = { tab = i },
                    colors = colors,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                ) { Text(n, fontSize = 13.sp) }
            }
        }
        if (tab == 0) LeagueTable(g, Modifier.weight(1f))
        else CupTables(g, if (tab == 1) g.cl else g.el, Modifier.weight(1f))
    }
}

@Composable
fun TableHeader() {
    Row(Modifier.fillMaxWidth()) {
        Text("#", Modifier.width(32.dp), fontSize = 12.sp)
        Text("", Modifier.width(22.dp))
        Text("Equipo", Modifier.weight(1f).padding(start = 6.dp), fontSize = 12.sp)
        Text("PJ", Modifier.width(26.dp), fontSize = 12.sp)
        Text("G", Modifier.width(22.dp), fontSize = 12.sp)
        Text("E", Modifier.width(22.dp), fontSize = 12.sp)
        Text("P", Modifier.width(22.dp), fontSize = 12.sp)
        Text("DG", Modifier.width(32.dp), fontSize = 12.sp)
        Text("Pts", Modifier.width(30.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TeamRow(g: Game, r: StandRow, pos: Int, bar: Color) {
    val t = g.byId(r.id)
    val mine = t.id == g.myId
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(24.dp).background(bar))
        Text("$pos", Modifier.width(28.dp).padding(start = 4.dp), fontSize = 13.sp)
        Crest(t, 22.dp)
        Text(
            t.name,
            Modifier.weight(1f).padding(start = 6.dp),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
            color = if (mine) Gold else Color.Unspecified
        )
        Text("${r.pj}", Modifier.width(26.dp), fontSize = 13.sp)
        Text("${r.w}", Modifier.width(22.dp), fontSize = 13.sp)
        Text("${r.d}", Modifier.width(22.dp), fontSize = 13.sp)
        Text("${r.l}", Modifier.width(22.dp), fontSize = 13.sp)
        Text("${r.dg}", Modifier.width(32.dp), fontSize = 13.sp)
        Text("${r.pts}", Modifier.width(30.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LeagueTable(g: Game, modifier: Modifier) {
    val tb = g.table()
    Column(modifier) {
        Text("🔵 Copa del Rey (1º-2º) · 🟠 Copa de CyL (3º-6º) · 🔴 Descenso", Modifier.padding(bottom = 6.dp), fontSize = 13.sp)
        TableHeader()
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(tb) { i, r ->
                val bar = when {
                    i < 2 -> Color(0xFF42A5F5)
                    i < 6 -> Color(0xFFFFA726)
                    i >= 17 -> Color(0xFFEF5350)
                    else -> Color.Transparent
                }
                TeamRow(g, r, i + 1, bar)
            }
        }
    }
}

@Composable
fun CupTables(g: Game, c: Cup, modifier: Modifier) {
    LazyColumn(modifier) {
        item {
            Text("🟢 Los 2 primeros de cada grupo pasan a cuartos", Modifier.padding(bottom = 6.dp), fontSize = 13.sp)
        }
        itemsIndexed(c.groups) { gi, grp ->
            Column(Modifier.padding(bottom = 10.dp)) {
                Text("Grupo ${'A' + gi}", fontWeight = FontWeight.Bold, color = Gold)
                TableHeader()
                g.groupTable(c, grp).forEachIndexed { i, r ->
                    TeamRow(g, r, i + 1, if (i < 2) Color(0xFF66BB6A) else Color.Transparent)
                }
            }
        }
        item {
            Column(Modifier.padding(top = 4.dp)) {
                Text("⚔️ Eliminatorias", fontWeight = FontWeight.Bold, color = Gold)
                if (c.ko.isEmpty() && c.teams.isEmpty()) {
                    Text("Se juegan cuando termine la fase de grupos.", fontSize = 13.sp)
                }
                c.ko.forEachIndexed { ri, ties ->
                    Text(koNames[ri], Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                    ties.forEach { t ->
                        val an = g.byId(t.a).name
                        val bn = g.byId(t.b).name
                        Text(
                            "$an ${t.ga}-${t.gb} $bn" + (if (t.pen) " (pen.)" else "") + " → ${g.byId(t.w).name}",
                            Modifier.padding(vertical = 2.dp),
                            fontSize = 13.sp
                        )
                    }
                }
                if (c.teams.size >= 2) {
                    Text(koNames[c.ko.size] + " (pendiente)", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                    c.teams.chunked(2).forEach { par ->
                        Text(
                            g.byId(par[0]).name + " vs " + g.byId(par[1]).name,
                            Modifier.padding(vertical = 2.dp),
                            fontSize = 13.sp
                        )
                    }
                }
                if (c.teams.size == 1) {
                    Text("🏆 Campeón: " + g.byId(c.teams[0]).name, Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MarketScreen(g: Game, tick: Int, onTeam: (Int) -> Unit, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        Text(g.windowText() + " · 💰 ${g.budget} M€", Modifier.padding(vertical = 6.dp), fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            listOf("Fichar", "Traspasos", "Ofertas (${g.offers.size})").forEachIndexed { i, n ->
                val colors = if (tab == i) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                Button(
                    onClick = { tab = i },
                    colors = colors,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                ) { Text(n, fontSize = 12.sp) }
            }
        }
        if (msg.isNotEmpty()) Text(msg, color = Gold)
        if (tab == 0) {
            LazyColumn(Modifier.weight(1f)) {
                items(g.league.filter { it.id != g.myId }) { t ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onTeam(t.id) }) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Crest(t, 30.dp)
                            Text(t.name, Modifier.weight(1f).padding(start = 12.dp), fontSize = 17.sp)
                            Text("Nivel ${teamLevel(t)}")
                        }
                    }
                }
            }
        } else if (tab == 1) {
            LazyColumn(Modifier.weight(1f)) {
                if (g.transfers.isEmpty()) {
                    item { Text("Todavía no hay traspasos.") }
                }
                items(g.transfers.toList()) { line -> Text(line, Modifier.padding(vertical = 4.dp), fontSize = 13.sp) }
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                if (g.offers.isEmpty()) {
                    item { Text("No tienes ofertas ahora mismo. Llegan durante las ventanas de mercado.") }
                }
                items(g.offers.toList()) { o ->
                    val p = g.me.players.firstOrNull { it.id == o.playerId }
                    if (p != null) {
                        Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${g.byId(o.buyerId).name} ofrece ${o.amount} M€ por ${p.name} (${p.pos.label} ${p.rating})")
                                Row(Modifier.padding(top = 6.dp)) {
                                    Button(
                                        onClick = { msg = g.acceptOffer(o) ?: "✅ Traspaso hecho: +${o.amount} M€" },
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) { Text("Aceptar") }
                                    OutlinedButton(onClick = { g.rejectOffer(o); msg = "Oferta rechazada" }) { Text("Rechazar") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketTeamScreen(g: Game, t: Team, tick: Int, onBack: () -> Unit) {
    var msg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        Text("${t.name} · 💰 ${g.budget} M€ · Tu plantilla ${g.me.players.size}/24", Modifier.padding(vertical = 6.dp))
        if (msg.isNotEmpty()) Text(msg, color = Gold)
        LazyColumn(Modifier.weight(1f)) {
            items(t.players.sortedByDescending { it.rating }) { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.pos.label, Modifier.width(40.dp))
                    PlayerFace(p, 34.dp)
                    Text(p.name, Modifier.weight(1f).padding(start = 6.dp))
                    Text("${p.rating}", Modifier.width(34.dp))
                    Button(onClick = { msg = g.buy(p, t) ?: "✅ Fichado ${p.name}" }) { Text("${price(p)} M€") }
                }
            }
        }
    }
}

@Composable
fun SquadScreen(g: Game, tick: Int, onBack: () -> Unit) {
    var msg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        Text("Tu plantilla ${g.me.players.size}/24 · 💰 ${g.budget} M€ (vendes al 80 %)", Modifier.padding(vertical = 6.dp))
        if (msg.isNotEmpty()) Text(msg, color = Gold)
        LazyColumn(Modifier.weight(1f)) {
            items(g.me.players.sortedWith(compareBy({ it.pos.ordinal }, { -it.rating }))) { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.pos.label, Modifier.width(40.dp))
                    PlayerFace(p, 34.dp)
                    Text(p.name, Modifier.weight(1f).padding(start = 6.dp))
                    Text("${p.rating}", Modifier.width(34.dp))
                    OutlinedButton(onClick = { msg = g.sell(p) ?: "💰 Vendido ${p.name}" }) {
                        Text("${price(p) * 8 / 10} M€")
                    }
                }
            }
        }
    }
}

@Composable
fun LineupScreen(
    me: Team,
    rival: Team,
    title: String,
    formation: Formation,
    selected: MutableList<Int>,
    energyOf: (Player) -> Int,
    onFormation: (Formation) -> Unit,
    onAuto: () -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Crest(rival, 36.dp)
            Column(Modifier.padding(start = 10.dp)) {
                Text("vs ${rival.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("$title · Nivel rival ${teamLevel(rival)}")
            }
        }
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
            OutlinedButton(onClick = onBack, modifier = Modifier.padding(end = 6.dp)) { Text("←") }
            OutlinedButton(onClick = onAuto, modifier = Modifier.padding(end = 6.dp)) { Text("Automático") }
            Button(onClick = onPlay, enabled = valid) { Text("⚽ Jugar") }
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
                    PlayerFace(p, 34.dp)
                    Text(p.name, Modifier.weight(1f).padding(start = 6.dp))
                    Text("${p.rating} · ⚡${energyOf(p)}%")
                }
            }
        }
    }
}

@Composable
fun MatchScreen(
    me: Team,
    rival: Team,
    res: MatchResult,
    isHome: Boolean,
    onContinue: () -> Unit
) {
    var minute by remember(res) { mutableIntStateOf(0) }
    LaunchedEffect(res) {
        while (minute < 90) {
            delay(300)
            minute++
        }
    }
    val shown = res.events.filter { it.minute <= minute }
    val last = shown.lastOrNull()
    val mine = last?.homeGoals ?: 0
    val theirs = last?.awayGoals ?: 0
    val left = if (isHome) me else rival
    val right = if (isHome) rival else me
    val leftGoals = if (isHome) mine else theirs
    val rightGoals = if (isHome) theirs else mine
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Crest(left, 48.dp)
            Text("$leftGoals - $rightGoals", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Crest(right, 48.dp)
        }
        Text("${left.name} vs ${right.name}", Modifier.padding(top = 4.dp))
        Text("⏱ $minute'", Modifier.padding(vertical = 6.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(shown.reversed()) { e -> Text("${e.minute}'  ${e.text}", Modifier.padding(vertical = 2.dp)) }
        }
        if (minute < 90) {
            OutlinedButton(onClick = { minute = 90 }, modifier = Modifier.fillMaxWidth()) { Text("Saltar") }
        } else {
            res.penalties?.let { Text(it, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp)) }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continuar") }
        }
    }
}

@Composable
fun PalmaresScreen(g: Game, tick: Int, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            item { Text("🏆 Trofeos por equipo", fontWeight = FontWeight.Bold, color = Gold) }
            val list = g.teams.filter {
                val p = g.pal[it.id]
                val total = if (p == null) 0 else p.liga + p.champions + p.europa
                total > 0 || it.id == g.myId
            }.sortedByDescending {
                val p = g.pal[it.id]
                if (p == null) 0 else (p.liga + p.champions + p.europa) * 10 + p.champions
            }
            items(list) { t ->
                val p = g.pal[t.id]
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Crest(t, 30.dp)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(
                            t.name,
                            fontWeight = if (t.id == g.myId) FontWeight.Bold else FontWeight.Normal,
                            color = if (t.id == g.myId) Gold else Color.Unspecified
                        )
                        Text(
                            "Liga ${p?.liga ?: 0} · Copa del Rey ${p?.champions ?: 0} · Copa de CyL ${p?.europa ?: 0}",
                            fontSize = 13.sp
                        )
                    }
                }
            }
            item { Text("📜 Campeones por temporada", Modifier.padding(top = 14.dp), fontWeight = FontWeight.Bold, color = Gold) }
            if (g.champs.isEmpty()) {
                item { Text("Aún no ha terminado ninguna temporada.", fontSize = 13.sp) }
            }
            items(g.champs.toList()) { line -> Text(line, Modifier.padding(vertical = 3.dp), fontSize = 13.sp) }
        }
    }
}

@Composable
fun SeasonEndScreen(g: Game, tick: Int, msg: String, onAd: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Fin de la temporada ${g.season}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Has acabado ${g.lastPos}º", fontSize = 20.sp, modifier = Modifier.padding(vertical = 6.dp))
        if (g.lastPos >= 18) {
            Text("📉 Descenso: como no hay 2ª división, pagas una sanción del presupuesto", color = Color(0xFFEF5350))
        }
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            items(g.lastSummary) { (txt, amt) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(txt, Modifier.weight(1f))
                    Text(if (amt >= 0) "+$amt M€" else "$amt M€", fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("💰 Presupuesto: ${g.budget} M€", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gold)
        if (msg.isNotEmpty()) Text(msg, Modifier.padding(top = 6.dp), color = Gold)
        if (!g.adBonus) {
            OutlinedButton(onClick = onAd, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("▶ Ver anuncio y ganar +10 M€")
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("Empezar temporada ${g.season + 1}")
        }
    }
}
