package com.example.futbolmanager

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

enum class Pos(val label: String) { POR("POR"), DEF("DEF"), MED("MED"), DEL("DEL") }

class Player(val id: Int, name: String, val pos: Pos, rating: Int) {
    var name by mutableStateOf(name)
    var rating by mutableStateOf(rating)
}

fun price(p: Player): Int = maxOf(1, (p.rating - 40) * (p.rating - 40) / 40)

val palette: List<Pair<Long, Long>> = listOf(
    0xFFD32F2FL to 0xFFFFFFFFL,
    0xFF1976D2L to 0xFFFFFFFFL,
    0xFF388E3CL to 0xFFFFEB3BL,
    0xFFFBC02DL to 0xFF212121L,
    0xFF7B1FA2L to 0xFFFFFFFFL,
    0xFF212121L to 0xFFFFFFFFL,
    0xFFF57C00L to 0xFF212121L,
    0xFF0097A7L to 0xFFFFFFFFL,
    0xFF5D4037L to 0xFFFFCC80L,
    0xFFC2185BL to 0xFFFFFFFFL
)

// Equipos ficticios que se parecen a equipos de la liga española (nombres y escudos NO reales)
private val reyColors: List<Pair<Long, Long>> = listOf(
    0xFF5E35B1L to 0xFFFFFFFFL,
    0xFF1A237EL to 0xFFA50044L,
    0xFFC62828L to 0xFFFFFFFFL,
    0xFFB71C1CL to 0xFFFFFFFFL,
    0xFF1565C0L to 0xFFFFFFFFL,
    0xFFFDD835L to 0xFF1565C0L,
    0xFF2E7D32L to 0xFFFFFFFFL,
    0xFFD32F2FL to 0xFFFFFFFFL,
    0xFF212121L to 0xFFEF6C00L,
    0xFF4FC3F7L to 0xFFFFFFFFL,
    0xFFC62828L to 0xFF0D47A1L,
    0xFF1976D2L to 0xFFFFFFFFL,
    0xFFE53935L to 0xFFFFFFFFL,
    0xFFC62828L to 0xFF212121L
)
val customColors: Map<Int, Pair<Long, Long>> = buildMap {
    reyColors.forEachIndexed { i, c -> put(100 + i, c) }
    val padres = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 13)
    padres.forEachIndexed { k, pi -> put(114 + k, reyColors[pi]) }
}
val customPattern: Map<Int, Int> = buildMap {
    for (i in 0 until 14) put(100 + i, i % 2)
    for (k in 0 until 12) put(114 + k, 2)
}

class Team(val id: Int, name: String, val players: MutableList<Player>) {
    var name by mutableStateOf(name)
    val c1: Long get() = customColors[id]?.first ?: palette[id % palette.size].first
    val c2: Long get() = customColors[id]?.second ?: palette[id % palette.size].second
    val pattern: Int get() = customPattern[id] ?: ((id / 3) % 3)
    val initials: String
        get() {
            val w = name.trim().split(" ").filter { it.isNotEmpty() }
            return if (w.size >= 2) (w[0].take(1) + w[1].take(1)).uppercase() else name.take(2).uppercase()
        }
}

data class Formation(val name: String, val def: Int, val med: Int, val del: Int)

val formations = listOf(
    Formation("4-4-2", 4, 4, 2),
    Formation("4-3-3", 4, 3, 3),
    Formation("3-5-2", 3, 5, 2)
)
val euroRounds = listOf("Octavos", "Cuartos", "Semifinales", "Final")
val minPos = mapOf(Pos.POR to 1, Pos.DEF to 4, Pos.MED to 5, Pos.DEL to 3)

// ---- Todo INVENTADO. Para tu liga real, cambia estos nombres (o usa el editor). ----
private val teamNames = listOf(
    "Rojos FC", "Atlético Sierra", "Real Valle", "Deportivo Río", "Unión Monte",
    "CD Olivar", "Racing Pinar", "Sporting Vega", "Villa Norte", "Athletic Costa",
    "CF Puente", "UD Castillo", "Real Dehesa", "SD Lagos", "CD Fuente",
    "Alcázar CF", "Unión Cañada", "Deportivo Encina", "Racing Peña", "Sporting Cerro"
)
// 14 equipos que se parecen a los de Primera (Copa del Rey) y 12 filiales (Copa de CyL). Todos inventados.
private val copaReyNames = listOf(
    "Real Madrigal CF", "FC Barcelonia", "Atlético Capitalino", "Athletic Vizcaya",
    "Real Sociedad Costera", "Villaroja CF", "Real Bética FC", "FC Sevillano",
    "Valentia CF", "Celta Atlántico", "Atlético Navarro", "Getafino CF",
    "Rayo Vallecito", "Real Maiorca"
)
private val copaCyLNames = listOf(
    "Real Madrigal C", "FC Barcelonia B", "Atlético Capitalino B", "Athletic Vizcaya B",
    "Real Sociedad Costera B", "Villaroja B", "Real Bética B", "FC Sevillano B",
    "Valentia B", "Celta Atlántico B", "Rayo Vallecito B", "Real Maiorca B"
)
private val firstNames = listOf(
    "Alex", "Dani", "Sergio", "Pablo", "Iván", "Hugo", "Marcos", "Raúl", "Adrián", "Javi",
    "Nico", "Álvaro", "Rubén", "Víctor", "Diego", "Óscar", "Mario", "Jorge", "Luis", "Carlos"
)
private val lastNames = listOf(
    "García", "Ruiz", "Martín", "López", "Sánchez", "Gómez", "Díaz", "Moreno", "Muñoz", "Alonso",
    "Gil", "Serrano", "Blanco", "Castro", "Ortega", "Rubio", "Molina", "Delgado", "Navarro", "Vega"
)

private fun makeTeam(id: Int, name: String, rnd: Random, lo: Int, hi: Int): Team {
    val base = rnd.nextInt(lo, hi)
    val layout = listOf(Pos.POR to 2, Pos.DEF to 6, Pos.MED to 6, Pos.DEL to 4)
    val list = mutableListOf<Player>()
    var idx = 0
    for ((pos, count) in layout) {
        for (i in 0 until count) {
            val nm = firstNames.random(rnd) + " " + lastNames.random(rnd)
            list.add(Player(id * 100 + idx, nm, pos, (base + rnd.nextInt(-8, 9)).coerceIn(40, 95)))
            idx++
        }
    }
    return Team(id, name, list)
}

// Equipo REAL (con permiso del club): Atlético Candeleda. Solo nombre y posición, sin fotos ni más datos.
// Si hay menores de edad, solo salen con el permiso de sus padres.
const val REAL_ID = 2

private fun candeleda(): Team {
    val data = listOf(
        Pos.POR to "Sergio Morcuende", Pos.POR to "Javi Luis", Pos.POR to "Alejandro Vinuesa",
        Pos.DEF to "Juan Antonio Retamal", Pos.DEF to "Sergio Núñez", Pos.DEF to "Abel Gómez",
        Pos.DEF to "Samuel Núñez", Pos.DEF to "Marcos Corral",
        Pos.MED to "Daniel Almeida", Pos.MED to "Diego Velasco", Pos.MED to "Sebastián Loan",
        Pos.MED to "Daniel Calvo", Pos.MED to "Manuel Retamal", Pos.MED to "Óscar González",
        Pos.MED to "David Peinado", Pos.MED to "Juan Pablo Zárate",
        Pos.DEL to "Jesús Monforte", Pos.DEL to "Javier Pazos", Pos.DEL to "Mario Peinado",
        Pos.DEL to "Álvaro Vega", Pos.DEL to "Iván García", Pos.DEL to "Víctor Benicio"
    )
    val list = mutableListOf<Player>()
    data.forEachIndexed { i, par -> list.add(Player(REAL_ID * 100 + i, par.second, par.first, 80 + (i * 5 + 3) % 7)) }
    return Team(REAL_ID, "Atlético Candeleda", list)
}

fun buildWorld(): List<Team> {
    val rnd = Random(2026)
    val t = mutableListOf<Team>()
    teamNames.forEachIndexed { i, n ->
        val inventado = makeTeam(i, n, rnd, 56, 72)
        t.add(if (i == REAL_ID) candeleda() else inventado)
    }
    for (i in 0 until 14) t.add(makeTeam(100 + i, copaReyNames[i], rnd, 66, 80))
    for (i in 0 until 12) t.add(makeTeam(114 + i, copaCyLNames[i], rnd, 52, 66))
    return t
}

fun applySaved(prefs: SharedPreferences, teams: List<Team>) {
    for (t in teams) {
        if (t.id == REAL_ID) continue
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
) {
    val draw: Boolean get() = penalties == null && home == away
}

fun simulate(
    me: Team, myL: List<Player>, myEff: (Player) -> Double,
    opp: Team, oppL: List<Player>, oppEff: (Player) -> Double,
    rnd: Random, allowDraw: Boolean
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
    } else if (allowDraw) {
        won = false
    } else {
        val p = (0.5 + ((aM + dM) - (aO + dO)) / 400).coerceIn(0.35, 0.65)
        won = rnd.nextDouble() < p
        pen = "Penaltis: gana ${if (won) me.name else opp.name}"
    }
    return MatchResult(ev, g1, g2, pen, won)
}

// ---------- Simulación rápida de los partidos de la IA ----------
fun power(t: Team): Pair<Double, Double> {
    val l = autoLineup(t, formations[0]) { 100 }
    val a = l.filter { it.pos == Pos.MED || it.pos == Pos.DEL }.map { it.rating.toDouble() }.average()
    val d = l.filter { it.pos == Pos.POR || it.pos == Pos.DEF }.map { it.rating.toDouble() }.average()
    return a to d
}

fun poisson(lam: Double, rnd: Random): Int {
    val l = exp(-lam)
    var k = 0
    var p = 1.0
    do {
        k++
        p *= rnd.nextDouble()
    } while (p > l)
    return k - 1
}

fun quickScore(h: Team, a: Team, rnd: Random): Pair<Int, Int> {
    val (ah, dh) = power(h)
    val (aa, da) = power(a)
    val lh = 1.45 * (ah / da).pow(2.5)
    val la = 1.15 * (aa / dh).pow(2.5)
    return poisson(lh, rnd) to poisson(la, rnd)
}

fun quickWinner(a: Team, b: Team, rnd: Random): Int {
    val (x, y) = quickScore(a, b, rnd)
    return if (x > y) a.id else if (y > x) b.id else if (rnd.nextBoolean()) a.id else b.id
}

class StandRow(val id: Int) {
    var pj = 0
    var w = 0
    var d = 0
    var l = 0
    var gf = 0
    var gc = 0
    val pts: Int get() = w * 3 + d
    val dg: Int get() = gf - gc
}

class Pal(var liga: Int = 0, var champions: Int = 0, var europa: Int = 0)

class KoTie(val a: Int, val b: Int, val ga: Int, val gb: Int, val pen: Boolean, val w: Int)

class Offer(val playerId: Int, val buyerId: Int, val amount: Int)

class Cup(val name: String) {
    var ko: MutableList<MutableList<KoTie>> = mutableListOf()
    var groups: List<List<Int>> = emptyList()
    var rows = HashMap<Int, StandRow>()
    var teams: MutableList<Int> = mutableListOf()
    var round = 0
}

data class Slot(val euro: Boolean, val idx: Int)

val koNames = listOf("Cuartos", "Semifinales", "Final")

fun buildFixtures(ids: List<Int>): List<List<Pair<Int, Int>>> {
    val n = ids.size
    val arr = ids.toMutableList()
    val rounds = mutableListOf<List<Pair<Int, Int>>>()
    for (r in 0 until n - 1) {
        val m = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until n / 2) {
            val a = arr[i]
            val b = arr[n - 1 - i]
            val aHome = if (i == 0) r % 2 == 1 else r % 2 == 0
            m.add(if (aHome) a to b else b to a)
        }
        rounds.add(m)
        val last = arr.removeAt(n - 1)
        arr.add(1, last)
    }
    return rounds + rounds.map { rd -> rd.map { it.second to it.first } }
}

private fun readRows(a: JSONArray): HashMap<Int, StandRow> {
    val m = HashMap<Int, StandRow>()
    for (i in 0 until a.length()) {
        val x = a.getJSONArray(i)
        val r = StandRow(x.getInt(0))
        r.pj = x.getInt(1); r.w = x.getInt(2); r.d = x.getInt(3)
        r.l = x.getInt(4); r.gf = x.getInt(5); r.gc = x.getInt(6)
        m[r.id] = r
    }
    return m
}

private fun rowsJson(m: Map<Int, StandRow>): JSONArray {
    val a = JSONArray()
    for (r in m.values) a.put(JSONArray().put(r.id).put(r.pj).put(r.w).put(r.d).put(r.l).put(r.gf).put(r.gc))
    return a
}

private fun cupJson(c: Cup): JSONObject {
    val o = JSONObject()
    o.put("teams", JSONArray(c.teams))
    o.put("round", c.round)
    val ga = JSONArray()
    for (grp in c.groups) ga.put(JSONArray(grp))
    o.put("groups", ga)
    o.put("rows", rowsJson(c.rows))
    val ka = JSONArray()
    for (ties in c.ko) {
        val ra = JSONArray()
        for (t in ties) ra.put(JSONArray().put(t.a).put(t.b).put(t.ga).put(t.gb).put(if (t.pen) 1 else 0).put(t.w))
        ka.put(ra)
    }
    o.put("ko", ka)
    return o
}

private fun readCup(name: String, o: JSONObject): Cup {
    val c = Cup(name)
    c.round = o.getInt("round")
    val ta = o.getJSONArray("teams")
    c.teams = mutableListOf()
    for (i in 0 until ta.length()) c.teams.add(ta.getInt(i))
    val ga = o.getJSONArray("groups")
    val gl = mutableListOf<List<Int>>()
    for (i in 0 until ga.length()) {
        val ar = ga.getJSONArray(i)
        val l = mutableListOf<Int>()
        for (k in 0 until ar.length()) l.add(ar.getInt(k))
        gl.add(l)
    }
    c.groups = gl
    c.rows = readRows(o.getJSONArray("rows"))
    val ka = o.optJSONArray("ko")
    if (ka != null) {
        for (i in 0 until ka.length()) {
            val ra = ka.getJSONArray(i)
            val ties = mutableListOf<KoTie>()
            for (k in 0 until ra.length()) {
                val t = ra.getJSONArray(k)
                ties.add(KoTie(t.getInt(0), t.getInt(1), t.getInt(2), t.getInt(3), t.getInt(4) == 1, t.getInt(5)))
            }
            c.ko.add(ties)
        }
    }
    return c
}

class Game(val teams: List<Team>, val myId: Int) {
    var tick by mutableStateOf(0)
    var season = 1
    var budget = 60
    var slotIndex = 0
    var seed = 1
    var seasonDone = false
    var adBonus = false
    var userOut = false
    var userQual = false
    var userCupWins = 0
    var lastPos = 0
    var lastSummary: List<Pair<String, Int>> = emptyList()
    var energy = HashMap<Int, Int>()
    var rows = HashMap<Int, StandRow>()
    var fixtures: List<List<Pair<Int, Int>>> = emptyList()
    var cl = Cup("Copa del Rey")
    var el = Cup("Copa de CyL")
    var userCup: Cup? = null
    var pal = HashMap<Int, Pal>()
    var champs: MutableList<String> = mutableListOf()
    var transfers: MutableList<String> = mutableListOf()
    var offers: MutableList<Offer> = mutableListOf()

    private val map = teams.associateBy { it.id }
    private val rnd: Random = Random.Default

    // Huecos del calendario: jornadas de liga y, entre medias, Champions / Europa League
    val slots: List<Slot> = buildList {
        val groupAfter = listOf(4, 7, 10, 13, 16, 19)
        val koAfter = listOf(25, 30, 35)
        for (j in 0 until 38) {
            add(Slot(false, j))
            val g = groupAfter.indexOf(j + 1)
            if (g >= 0) add(Slot(true, g))
            val k = koAfter.indexOf(j + 1)
            if (k >= 0) add(Slot(true, 6 + k))
        }
    }

    val league: List<Team> get() = teams.filter { it.id < 100 }
    val me: Team get() = byId(myId)
    fun byId(id: Int): Team = map.getValue(id)
    fun en(p: Player): Int = energy[p.id] ?: 100

    val current: Slot?
        get() = if (seasonDone || slotIndex >= slots.size) null else slots[slotIndex]

    private fun leaguePair(j: Int): Pair<Int, Int> =
        fixtures[j].first { it.first == myId || it.second == myId }

    private fun cupPair(c: Cup, day: Int): Pair<Int, Int> {
        val grp = c.groups.first { myId in it }
        return buildFixtures(grp)[day].first { it.first == myId || it.second == myId }
    }

    fun opponent(): Int? {
        val s = current ?: return null
        if (!s.euro) {
            val p = leaguePair(s.idx)
            return if (p.first == myId) p.second else p.first
        }
        val c = userCup ?: return null
        if (s.idx < 6) {
            val p = cupPair(c, s.idx)
            return if (p.first == myId) p.second else p.first
        }
        val k = c.teams.indexOf(myId)
        if (k < 0) return null
        return c.teams[k xor 1]
    }

    fun isHome(): Boolean {
        val s = current ?: return true
        if (!s.euro) return leaguePair(s.idx).first == myId
        val c = userCup ?: return true
        if (s.idx < 6) return cupPair(c, s.idx).first == myId
        return c.teams.indexOf(myId) % 2 == 0
    }

    fun slotTitle(s: Slot): String {
        if (!s.euro) return "Liga · Jornada ${s.idx + 1}"
        val nm = if (userCup === el) "Copa de CyL" else "Copa del Rey"
        return nm + " · " + (if (s.idx < 6) "Fase de grupos J${s.idx + 1}" else koNames[s.idx - 6])
    }

    private fun newCup(name: String, ids: List<Int>): Cup {
        val c = Cup(name)
        c.groups = ids.chunked(4)
        for (id in ids) c.rows[id] = StandRow(id)
        return c
    }

    fun startSeason(ranking: List<Int>) {
        val r = Random(seed)
        rows = HashMap()
        for (t in league) rows[t.id] = StandRow(t.id)
        fixtures = buildFixtures(league.map { it.id }.shuffled(r))
        val primera = teams.filter { it.id in 100..113 }.map { it.id }.shuffled(r)
        val filiales = teams.filter { it.id in 114..125 }.map { it.id }.shuffled(r)
        // Copa del Rey: 1º y 2º de la liga + 14 equipos tipo Primera. Copa de CyL: del 3º al 6º + 12 filiales.
        cl = newCup("Copa del Rey", (ranking.take(2) + primera).shuffled(r))
        el = newCup("Copa de CyL", (ranking.subList(2, 6) + filiales).shuffled(r))
        userCup = if (cl.groups.any { myId in it }) cl else if (el.groups.any { myId in it }) el else null
        userCupWins = 0
        userOut = false
        userQual = false
        slotIndex = 0
        seasonDone = false
        adBonus = false
        energy = HashMap()
        skipAuto()
        offers = mutableListOf()
        doDeals(8)
        maybeOffer()
        tick++
    }

    fun nextSeason() {
        val rank = table().map { it.id }
        for (t in teams) for (p in t.players) p.rating = (p.rating + rnd.nextInt(-3, 4)).coerceIn(40, 95)
        season++
        seed = rnd.nextInt()
        startSeason(rank)
    }

    private fun userInRound(idx: Int): Boolean {
        val c = userCup ?: return false
        if (idx < 6) return true
        return !userOut && userQual && userCupWins == idx - 6 && myId in c.teams
    }

    private fun skipAuto() {
        while (slotIndex < slots.size) {
            val s = slots[slotIndex]
            if (!s.euro) return
            if (userInRound(s.idx)) return
            playEuroSlot(s.idx, null)
            slotIndex++
        }
        seasonDone = true
        closeSeason()
    }

    private fun playEuroSlot(idx: Int, res: MatchResult?) {
        for (c in listOf(cl, el)) {
            if (idx < 6) playGroupDay(c, idx, res) else playKnockout(c, res)
        }
    }

    private fun playGroupDay(c: Cup, day: Int, res: MatchResult?) {
        for (grp in c.groups) {
            for ((h, a) in buildFixtures(grp)[day]) {
                if (res != null && (h == myId || a == myId)) {
                    if (h == myId) record(c.rows, h, a, res.home, res.away)
                    else record(c.rows, h, a, res.away, res.home)
                } else {
                    val (x, y) = quickScore(byId(h), byId(a), rnd)
                    record(c.rows, h, a, x, y)
                }
            }
        }
        if (day == 5) {
            val firsts = mutableListOf<Int>()
            val seconds = mutableListOf<Int>()
            for (grp in c.groups) {
                val t = groupTable(c, grp)
                firsts.add(t[0].id)
                seconds.add(t[1].id)
            }
            c.teams = mutableListOf(
                firsts[0], seconds[1], firsts[1], seconds[0],
                firsts[2], seconds[3], firsts[3], seconds[2]
            )
            c.round = 0
            if (c === userCup) {
                userQual = myId in c.teams
                if (!userQual) userOut = true
            }
        }
    }

    private fun playKnockout(c: Cup, res: MatchResult?) {
        if (c.teams.size < 2) return
        val next = mutableListOf<Int>()
        val ties = mutableListOf<KoTie>()
        var k = 0
        while (k + 1 < c.teams.size) {
            val a = c.teams[k]
            val b = c.teams[k + 1]
            val winner: Int
            val ga: Int
            val gb: Int
            val pen: Boolean
            if (res != null && (a == myId || b == myId)) {
                val other = if (a == myId) b else a
                ga = if (a == myId) res.home else res.away
                gb = if (a == myId) res.away else res.home
                pen = res.penalties != null
                if (res.userWon) {
                    winner = myId
                    userCupWins++
                } else {
                    winner = other
                    userOut = true
                }
            } else {
                val (x, y) = quickScore(byId(a), byId(b), rnd)
                ga = x
                gb = y
                pen = x == y
                winner = if (x > y) a else if (y > x) b else if (rnd.nextBoolean()) a else b
            }
            ties.add(KoTie(a, b, ga, gb, pen, winner))
            next.add(winner)
            k += 2
        }
        c.ko.add(ties)
        c.teams = next
        c.round++
    }

    private fun record(m: HashMap<Int, StandRow>, x: Int, y: Int, gx: Int, gy: Int) {
        val rx = m.getValue(x)
        val ry = m.getValue(y)
        rx.pj++; ry.pj++
        rx.gf += gx; rx.gc += gy
        ry.gf += gy; ry.gc += gx
        if (gx > gy) { rx.w++; ry.l++ } else if (gx < gy) { ry.w++; rx.l++ } else { rx.d++; ry.d++ }
    }

    private fun sorted(list: List<StandRow>): List<StandRow> =
        list.sortedWith(compareByDescending<StandRow> { it.pts }.thenByDescending { it.dg }.thenByDescending { it.gf })

    fun table(): List<StandRow> = sorted(rows.values.toList())

    fun groupTable(c: Cup, grp: List<Int>): List<StandRow> = sorted(grp.map { c.rows.getValue(it) })

    fun position(): Int = table().indexOfFirst { it.id == myId } + 1

    fun playMatch(starters: List<Player>): MatchResult {
        val s = current!!
        val opp = byId(opponent()!!)
        val oppLineup = autoLineup(opp, formations[rnd.nextInt(formations.size)]) { 100 }
        val allowDraw = !s.euro || s.idx < 6
        return simulate(me, starters, { effRating(it, en(it)) }, opp, oppLineup, { it.rating.toDouble() }, rnd, allowDraw)
    }

    fun describe(r: MatchResult): String {
        val s = current ?: return ""
        val o = byId(opponent() ?: return "")
        val score = if (isHome()) me.name + " " + r.home + "-" + r.away + " " + o.name
        else o.name + " " + r.away + "-" + r.home + " " + me.name
        return slotTitle(s) + ": " + score + (r.penalties?.let { " · $it" } ?: "")
    }

    fun applyUserResult(res: MatchResult, starters: List<Player>) {
        val s = current ?: return
        val ids = starters.map { it.id }.toSet()
        for (p in me.players) {
            val e = en(p)
            energy[p.id] = if (p.id in ids) (e - 10).coerceAtLeast(40) else (e + 15).coerceAtMost(100)
        }
        if (!s.euro) {
            val pair = leaguePair(s.idx)
            val opp = if (pair.first == myId) pair.second else pair.first
            record(rows, myId, opp, res.home, res.away)
            for ((h, a) in fixtures[s.idx]) {
                if (h != myId && a != myId) {
                    val (x, y) = quickScore(byId(h), byId(a), rnd)
                    record(rows, h, a, x, y)
                }
            }
        } else {
            playEuroSlot(s.idx, res)
        }
        slotIndex++
        skipAuto()
        if (!s.euro && !seasonDone) marketTick()
        tick++
    }

    fun upcoming(n: Int): List<String> {
        val out = mutableListOf<String>()
        var i = slotIndex
        while (i < slots.size && out.size < n) {
            val s = slots[i]
            if (!s.euro) {
                val p = leaguePair(s.idx)
                val home = p.first == myId
                val o = byId(if (home) p.second else p.first)
                out.add("J${s.idx + 1} · " + (if (home) "vs " else "en ") + o.name)
            } else {
                val c = userCup
                if (c != null) {
                    val nm = if (c === cl) "Copa del Rey" else "Copa de CyL"
                    if (s.idx < 6) {
                        val p = cupPair(c, s.idx)
                        val home = p.first == myId
                        val o = byId(if (home) p.second else p.first)
                        out.add(nm + " · Grupos J${s.idx + 1} · " + (if (home) "vs " else "en ") + o.name)
                    } else if (!userOut) {
                        out.add(nm + " · " + koNames[s.idx - 6])
                    }
                }
            }
            i++
        }
        return out
    }

    private fun closeSeason() {
        val pos = position()
        lastPos = pos
        val champLiga = table()[0].id
        val champCl = if (cl.teams.size == 1) cl.teams[0] else null
        val champEl = if (el.teams.size == 1) el.teams[0] else null
        pal.getOrPut(champLiga) { Pal() }.liga++
        if (champCl != null) pal.getOrPut(champCl) { Pal() }.champions++
        if (champEl != null) pal.getOrPut(champEl) { Pal() }.europa++
        val nCl = if (champCl != null) byId(champCl).name else "-"
        val nEl = if (champEl != null) byId(champEl).name else "-"
        champs.add(0, "Temporada $season · Liga: ${byId(champLiga).name} · Copa del Rey: $nCl · Copa de CyL: $nEl")
        val out = mutableListOf<Pair<String, Int>>()
        val liga = when {
            pos == 1 -> 30
            pos == 2 -> 22
            pos == 3 -> 18
            pos == 4 -> 15
            pos <= 7 -> 10
            pos <= 10 -> 6
            pos <= 17 -> 3
            else -> 0
        }
        out.add(Pair("Liga: puesto $pos", liga))
        if (pos == 1) out.add(Pair("🏆 Trofeo de Liga", 20))
        val c = userCup
        if (c != null) {
            val isCl = c === cl
            val nm = if (isCl) "Copa del Rey" else "Copa de CyL"
            val part = if (isCl) 10 else 5
            val prize = if (isCl) listOf(0, 12, 20, 30, 50) else listOf(0, 6, 10, 15, 25)
            val reached = listOf("", "cuartos", "semifinales", "la final", "campeón")
            val stage = if (!userQual) 0 else 1 + userCupWins
            out.add(Pair("$nm: participación", part))
            if (stage > 0) out.add(Pair("$nm: llegar a ${reached[stage]}", prize[stage]))
            if (stage >= 4) {
                val trofeo = if (isCl) 30 else 15
                out.add(Pair("🏆 Trofeo $nm", trofeo))
            }
        }
        if (pos >= 18) out.add(Pair("📉 Descenso (sin 2ª división): sanción", -15))
        lastSummary = out
        budget = maxOf(0, budget + out.sumOf { it.second })
    }

    // ---------- Fichajes ----------
    private fun canGive(t: Team, p: Player): Boolean =
        t.players.count { it.pos == p.pos } - 1 >= (minPos[p.pos] ?: 0)

    fun jornada(): Int = slots.take(slotIndex).count { !it.euro }

    fun windowOpen(): Boolean {
        val j = jornada()
        return j <= 3 || j in 19..21
    }

    fun windowText(): String {
        val j = jornada()
        return when {
            j <= 3 -> "🟢 Mercado de verano abierto (cierra tras la J4)"
            j in 19..21 -> "🟢 Mercado de invierno abierto (cierra tras la J22)"
            j < 19 -> "🔴 Mercado cerrado · abre en invierno (J20)"
            else -> "🔴 Mercado cerrado · abre en verano (próxima temporada)"
        }
    }

    private fun dateLabel(): String {
        val j = jornada()
        val w = if (j <= 3) "Verano" else if (j in 19..22) "Invierno" else "Fuera de plazo"
        return "T$season · $w · " + (if (j == 0) "pretemporada" else "tras J$j")
    }

    private fun doDeals(n: Int) {
        val label = dateLabel()
        val others = league.filter { it.id != myId }
        var done = 0
        var tries = 0
        while (done < n && tries < 80) {
            tries++
            val buyer = others.random(rnd)
            val seller = others.random(rnd)
            if (buyer.id == seller.id) continue
            if (buyer.players.size >= 22 || seller.players.size <= 17) continue
            val p = seller.players.random(rnd)
            if (!canGive(seller, p)) continue
            val pr = price(p)
            seller.players.remove(p)
            buyer.players.add(p)
            transfers.add(0, "$label: ${buyer.name} ficha a ${p.name} (${p.pos.label} ${p.rating}) de ${seller.name} por $pr M€")
            done++
        }
        while (transfers.size > 150) transfers.removeAt(transfers.size - 1)
    }

    private fun maybeOffer() {
        if (offers.size >= 3 || rnd.nextDouble() > 0.5) return
        val cand = me.players.filter { it.rating >= 58 && canGive(me, it) && offers.none { o -> o.playerId == it.id } }
        if (cand.isEmpty()) return
        val buyers = league.filter { it.id != myId && it.players.size < 22 }
        if (buyers.isEmpty()) return
        val p = cand.random(rnd)
        val amount = maxOf(1, price(p) * (80 + rnd.nextInt(51)) / 100)
        offers.add(Offer(p.id, buyers.random(rnd).id, amount))
    }

    private fun marketTick() {
        val j = jornada()
        if (j == 4 || j == 22) offers.clear()
        when {
            j in 1..3 -> doDeals(3)
            j == 19 -> doDeals(6)
            j in 20..21 -> doDeals(3)
        }
        if (windowOpen()) maybeOffer()
    }

    fun acceptOffer(o: Offer): String? {
        if (!windowOpen()) return "El mercado está cerrado"
        val p = me.players.firstOrNull { it.id == o.playerId }
        if (p == null) {
            offers.remove(o)
            tick++
            return "Ese jugador ya no está en tu plantilla"
        }
        if (!canGive(me, p)) return "Necesitas más jugadores en esa posición"
        val buyer = byId(o.buyerId)
        me.players.remove(p)
        buyer.players.add(p)
        budget += o.amount
        transfers.add(0, "${dateLabel()}: ${buyer.name} ficha a ${p.name} de ${me.name} por ${o.amount} M€")
        offers.remove(o)
        tick++
        return null
    }

    fun rejectOffer(o: Offer) {
        offers.remove(o)
        tick++
    }

    fun buy(p: Player, from: Team): String? {
        if (!windowOpen()) return "El mercado está cerrado (abre en verano y en invierno)"
        val pr = price(p)
        if (me.players.size >= 24) return "Plantilla llena (máximo 24)"
        if (pr > budget) return "No tienes presupuesto suficiente"
        if (!canGive(from, p)) return "Ese equipo no puede quedarse sin jugadores en esa posición"
        from.players.remove(p)
        me.players.add(p)
        budget -= pr
        transfers.add(0, "${dateLabel()}: ${me.name} ficha a ${p.name} (${p.pos.label} ${p.rating}) de ${from.name} por $pr M€")
        tick++
        return null
    }

    fun sell(p: Player): String? {
        if (!windowOpen()) return "El mercado está cerrado (abre en verano y en invierno)"
        if (!canGive(me, p)) return "Necesitas más jugadores en esa posición"
        val dest = league.filter { it.id != myId && it.players.size < 24 }.random(rnd)
        val money = price(p) * 8 / 10
        me.players.remove(p)
        dest.players.add(p)
        budget += money
        transfers.add(0, "${dateLabel()}: ${dest.name} ficha a ${p.name} de ${me.name} por $money M€")
        tick++
        return null
    }

    // ---------- Guardado ----------
    fun toJson(): String {
        val o = JSONObject()
        o.put("ver", 3)
        o.put("my", myId)
        o.put("season", season)
        o.put("budget", budget)
        o.put("slot", slotIndex)
        o.put("seed", seed)
        o.put("done", seasonDone)
        o.put("out", userOut)
        o.put("qual", userQual)
        o.put("wins", userCupWins)
        o.put("bonus", adBonus)
        o.put("lastpos", lastPos)
        o.put("cup", if (userCup == null) "" else if (userCup === cl) "cl" else "el")
        val ts = JSONArray()
        for (t in teams) {
            val to = JSONObject()
            to.put("id", t.id)
            to.put("name", t.name)
            val pa = JSONArray()
            for (p in t.players) pa.put(JSONArray().put(p.id).put(p.name).put(p.pos.ordinal).put(p.rating))
            to.put("pl", pa)
            ts.put(to)
        }
        o.put("teams", ts)
        o.put("rows", rowsJson(rows))
        o.put("cl", cupJson(cl))
        o.put("el", cupJson(el))
        val ea = JSONArray()
        for ((k, v) in energy) ea.put(JSONArray().put(k).put(v))
        o.put("energy", ea)
        val sa = JSONArray()
        for (s in lastSummary) sa.put(JSONArray().put(s.first).put(s.second))
        o.put("sum", sa)
        val pa2 = JSONArray()
        for ((k, v) in pal) pa2.put(JSONArray().put(k).put(v.liga).put(v.champions).put(v.europa))
        o.put("pal", pa2)
        o.put("champs", JSONArray(champs))
        o.put("transfers", JSONArray(transfers))
        val oa = JSONArray()
        for (f in offers) oa.put(JSONArray().put(f.playerId).put(f.buyerId).put(f.amount))
        o.put("offers", oa)
        return o.toString()
    }

    companion object {
        fun fromJson(s: String): Game {
            val o = JSONObject(s)
            if (o.getInt("ver") != 3) throw IllegalStateException("partida antigua")
            val ts = o.getJSONArray("teams")
            val list = ArrayList<Team>()
            for (i in 0 until ts.length()) {
                val to = ts.getJSONObject(i)
                val pa = to.getJSONArray("pl")
                val pl = mutableListOf<Player>()
                for (k in 0 until pa.length()) {
                    val a = pa.getJSONArray(k)
                    pl.add(Player(a.getInt(0), a.getString(1), Pos.values()[a.getInt(2)], a.getInt(3)))
                }
                list.add(Team(to.getInt("id"), to.getString("name"), pl))
            }
            val g = Game(list, o.getInt("my"))
            g.season = o.getInt("season")
            g.budget = o.getInt("budget")
            g.slotIndex = o.getInt("slot")
            g.seed = o.getInt("seed")
            g.seasonDone = o.getBoolean("done")
            g.userOut = o.getBoolean("out")
            g.userQual = o.getBoolean("qual")
            g.userCupWins = o.getInt("wins")
            g.adBonus = o.getBoolean("bonus")
            g.lastPos = o.getInt("lastpos")
            g.fixtures = buildFixtures(g.league.map { it.id }.shuffled(Random(g.seed)))
            g.rows = readRows(o.getJSONArray("rows"))
            g.cl = readCup("Copa del Rey", o.getJSONObject("cl"))
            g.el = readCup("Copa de CyL", o.getJSONObject("el"))
            val uc = o.getString("cup")
            g.userCup = if (uc == "cl") g.cl else if (uc == "el") g.el else null
            val ea = o.getJSONArray("energy")
            for (i in 0 until ea.length()) {
                val a = ea.getJSONArray(i)
                g.energy[a.getInt(0)] = a.getInt(1)
            }
            val sa = o.getJSONArray("sum")
            val sum = mutableListOf<Pair<String, Int>>()
            for (i in 0 until sa.length()) {
                val a = sa.getJSONArray(i)
                sum.add(Pair(a.getString(0), a.getInt(1)))
            }
            g.lastSummary = sum
            val pj = o.optJSONArray("pal")
            if (pj != null) {
                for (i in 0 until pj.length()) {
                    val a = pj.getJSONArray(i)
                    val p = Pal()
                    p.liga = a.getInt(1)
                    p.champions = a.getInt(2)
                    p.europa = a.getInt(3)
                    g.pal[a.getInt(0)] = p
                }
            }
            val cj = o.optJSONArray("champs")
            if (cj != null) for (i in 0 until cj.length()) g.champs.add(cj.getString(i))
            val tj = o.optJSONArray("transfers")
            if (tj != null) for (i in 0 until tj.length()) g.transfers.add(tj.getString(i))
            val oj = o.optJSONArray("offers")
            if (oj != null) {
                for (i in 0 until oj.length()) {
                    val a = oj.getJSONArray(i)
                    g.offers.add(Offer(a.getInt(0), a.getInt(1), a.getInt(2)))
                }
            }
            return g
        }
    }
}
