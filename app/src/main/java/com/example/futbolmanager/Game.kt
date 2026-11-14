package com.example.futbolmanager

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

enum class Pos(val label: String) { POR("POR"), DEF("DEF"), MED("MED"), DEL("DEL") }

class Player(val id: Int, name: String, val pos: Pos, val rating: Int) {
    var name by mutableStateOf(name)
}

class Team(val id: Int, name: String, val players: List<Player>) {
    var name by mutableStateOf(name)
}

data class Formation(val name: String, val def: Int, val med: Int, val del: Int)

val formations = listOf(
    Formation("4-4-2", 4, 4, 2),
    Formation("4-3-3", 4, 3, 3),
    Formation("3-5-2", 3, 5, 2)
)
val roundNames = listOf("Cuartos de final", "Semifinales", "Final")

private val teamNames = listOf(
    "Rojos FC", "Atlético Sierra", "Real Valle", "Deportivo Río",
    "Unión Monte", "CD Olivar", "Racing Pinar", "Sporting Vega"
)
private val firstNames = listOf(
    "Alex", "Dani", "Sergio", "Pablo", "Iván", "Hugo", "Marcos", "Raúl", "Adrián", "Javi",
    "Nico", "Álvaro", "Rubén", "Víctor", "Diego", "Óscar", "Mario", "Jorge", "Luis", "Carlos"
)
private val lastNames = listOf(
    "García", "Ruiz", "Martín", "López", "Sánchez", "Gómez", "Díaz", "Moreno", "Muñoz", "Alonso",
    "Gil", "Serrano", "Blanco", "Castro", "Ortega", "Rubio", "Molina", "Delgado", "Navarro", "Vega"
)

fun buildTeams(): List<Team> {
    val rnd = Random(2026)
    val layout = listOf(Pos.POR to 2, Pos.DEF to 6, Pos.MED to 6, Pos.DEL to 4)
    return teamNames.mapIndexed { t, n ->
        val base = 58 + rnd.nextInt(0, 14)
        val list = mutableListOf<Player>()
        var idx = 0
        for ((pos, count) in layout) {
            for (i in 0 until count) {
                val name = firstNames.random(rnd) + " " + lastNames.random(rnd)
                list.add(Player(t * 100 + idx, name, pos, (base + rnd.nextInt(-8, 9)).coerceIn(40, 95)))
                idx++
            }
        }
        Team(t, n, list)
    }
}

fun applySaved(prefs: SharedPreferences, teams: List<Team>) {
    for (t in teams) {
        prefs.getString("t${t.id}", null)?.let { t.name = it }
        for (p in t.players) prefs.getString("p${p.id}", null)?.let { p.name = it }
    }
}

fun teamLevel(t: Team): Int =
    t.players.map { it.rating }.sortedDescending().take(11).average().toInt()

fun effRating(p: Player, energy: Int): Double = p.rating * (70 + 30 * energy / 100.0) / 100.0

fun autoLineup(team: Team, f: Formation, energyOf: (Player) -> Int): List<Player> {
    fun best(pos: Pos, n: Int) = team.players.filter { it.pos == pos }
        .sortedByDescending { effRating(it, energyOf(it)) }.take(n)
    return best(Pos.POR, 1) + best(Pos.DEF, f.def) + best(Pos.MED, f.med) + best(Pos.DEL, f.del)
}

data class MatchEvent(val minute: Int, val text: String, val homeGoals: Int, val awayGoals: Int)

data class MatchResult(
    val events: List<MatchEvent>,
    val home: Int,
    val away: Int,
    val penalties: String?,
    val userWon: Boolean
)

fun simulate(
    me: Team, myL: List<Player>, myEff: (Player) -> Double,
    opp: Team, oppL: List<Player>, oppEff: (Player) -> Double,
    rnd: Random
): MatchResult {
    fun atk(l: List<Player>, e: (Player) -> Double) =
        l.filter { it.pos == Pos.MED || it.pos == Pos.DEL }.map { e(it) }.average()
    fun dfn(l: List<Player>, e: (Player) -> Double) =
        l.filter { it.pos == Pos.POR || it.pos == Pos.DEF }.map { e(it) }.average()

    val aM = atk(myL, myEff); val dM = dfn(myL, myEff)
    val aO = atk(oppL, oppEff); val dO = dfn(oppL, oppEff)
    val ev = mutableListOf<MatchEvent>()
    var g1 = 0
    var g2 = 0
    ev.add(MatchEvent(0, "¡Pitido inicial!", 0, 0))
    for (m in 1..90) {
        if (m == 45) ev.add(MatchEvent(45, "Descanso", g1, g2))
        if (rnd.nextDouble() < 0.11) {
            val mine = rnd.nextDouble() < aM / (aM + aO)
            val a = if (mine) aM else aO
            val d = if (mine) dO else dM
            val l = if (mine) myL else oppL
            val tn = if (mine) me.name else opp.name
            val who = l.filter { it.pos == Pos.MED || it.pos == Pos.DEL }.random(rnd)
            if (rnd.nextDouble() < (0.30 * a / d).coerceIn(0.08, 0.55)) {
                if (mine) g1++ else g2++
                ev.add(MatchEvent(m, "⚽ ¡GOL de ${who.name} ($tn)!", g1, g2))
            } else {
                ev.add(MatchEvent(m, "Ocasión de ${who.name} ($tn), ¡no entra!", g1, g2))
            }
        }
    }
    ev.add(MatchEvent(90, "Final del partido", g1, g2))
    var pen: String? = null
    val won: Boolean
    if (g1 != g2) {
        won = g1 > g2
    } else {
        val p = (0.5 + ((aM + dM) - (aO + dO)) / 400).coerceIn(0.35, 0.65)
        won = rnd.nextDouble() < p
        pen = "Empate. Penaltis: gana ${if (won) me.name else opp.name}"
    }
    return MatchResult(ev, g1, g2, pen, won)
}
