package com.example.secureshe.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.example.secureshe.R
import org.json.JSONArray

data class RiskTile(
    val latMin: Double,
    val latMax: Double,
    val lngMin: Double,
    val lngMax: Double,
    val risk: Double
)

data class RouteCandidate(
    val points: List<LatLng>,
    val durationMin: Double,
    val distanceM: Double
)

data class SearchResult(
    val label: String,
    val location: LatLng
)

class SafePathViewModel(app: Application) : AndroidViewModel(app) {
    private val _tiles = MutableStateFlow<List<RiskTile>>(emptyList())
    val tiles: StateFlow<List<RiskTile>> = _tiles

    private val httpClient = OkHttpClient()

    val origin = MutableStateFlow<LatLng?>(null)
    val destination = MutableStateFlow<LatLng?>(null)
    val routes = MutableStateFlow<List<RouteCandidate>>(emptyList())
    val safest = MutableStateFlow<RouteCandidate?>(null)
    val fastest = MutableStateFlow<RouteCandidate?>(null)
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val currentLocation = MutableStateFlow<LatLng?>(null)
    val offRoute = MutableStateFlow(false)

    // Simple OSM Nominatim search state
    val searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchLoading = MutableStateFlow(false)
    val searchError = MutableStateFlow<String?>(null)

    enum class Profile { FASTEST, BALANCED, SAFEST }
    val profile = MutableStateFlow(Profile.BALANCED)

    data class Weights(val alphaTime: Double, val betaDistance: Double, val gammaRisk: Double)

    private fun currentWeights(hourOfDay: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): Weights {
        // Nighttime detection: 8pm–6am
        val isNight = hourOfDay >= 20 || hourOfDay <= 6
        return when (profile.value) {
            Profile.FASTEST -> Weights(alphaTime = 0.8, betaDistance = 0.2, gammaRisk = if (isNight) 0.2 else 0.1)
            Profile.SAFEST -> Weights(alphaTime = 0.3, betaDistance = 0.1, gammaRisk = if (isNight) 0.8 else 0.6)
            Profile.BALANCED -> Weights(alphaTime = 0.5, betaDistance = 0.2, gammaRisk = if (isNight) 0.5 else 0.3)
        }
    }

    // Reusable haversine distance helper (meters)
    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val sa = Math.sin(dLat / 2)
        val sb = Math.sin(dLng / 2)
        val aa = sa * sa + Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) * sb * sb
        val c = 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1 - aa))
        return R * c
    }

    fun setProfile(p: Profile) {
        profile.value = p
        // Recompute selection using current routes so profile switch feels instant
        reselectRoutes()
    }

    fun reselectRoutes() {
        val list = routes.value
        if (list.isEmpty()) return
        // Always compute fastest for comparison UI
        fastest.value = list.minByOrNull { it.durationMin }
        // Select highlighted route based on current profile
        val chosen = when (profile.value) {
            Profile.FASTEST -> fastest.value
            Profile.BALANCED, Profile.SAFEST -> selectSafestRoute(list)
        }
        safest.value = chosen
    }

    init {
        loadRiskTilesFromAssets()
    }

    private fun loadRiskTilesFromAssets() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val isr = InputStreamReader(ctx.assets.open("risk_grid_default.json"))
                val arr = Gson().fromJson(isr, Array<RiskTile>::class.java)
                _tiles.value = arr.toList()
            } catch (_: Exception) {
                _tiles.value = emptyList()
            }
        }
    }

    private fun worldPlaceholder(tile: RiskTile): Boolean {
        return tile.latMin <= 0.0 && tile.latMax >= 80.0 && tile.lngMin <= -170.0 && tile.lngMax >= 170.0
    }

    private fun ensureDemoTilesForRoute(route: RouteCandidate) {
        val current = _tiles.value
        val hasOnlyWorld = current.size == 1 && worldPlaceholder(current.first())
        if (current.isNotEmpty() && !hasOnlyWorld) return

        if (route.points.isEmpty()) return
        var minLat = Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE
        route.points.forEach { p ->
            if (p.latitude < minLat) minLat = p.latitude
            if (p.latitude > maxLat) maxLat = p.latitude
            if (p.longitude < minLng) minLng = p.longitude
            if (p.longitude > maxLng) maxLng = p.longitude
        }
        val padLat = (maxLat - minLat).coerceAtLeast(0.05) * 0.5
        val padLng = (maxLng - minLng).coerceAtLeast(0.05) * 0.5
        minLat -= padLat; maxLat += padLat; minLng -= padLng; maxLng += padLng

        val step = ((maxLat - minLat).coerceAtLeast(0.1) / 20.0).coerceIn(0.002, 0.03)
        val out = mutableListOf<RiskTile>()
        var lat = minLat
        while (lat < maxLat) {
            var lng = minLng
            while (lng < maxLng) {
                val centerLat = lat + step / 2
                val centerLng = lng + step / 2
                val noise = kotlin.math.abs(kotlin.math.sin(centerLat * 18.0) * kotlin.math.cos(centerLng * 21.0))
                val risk = 0.2 + 0.6 * noise
                out.add(RiskTile(latMin = lat, latMax = lat + step, lngMin = lng, lngMax = lng + step, risk = risk))
                lng += step
            }
            lat += step
        }
        _tiles.value = out
    }

    private fun ensureDemoTilesForRoutes(candidates: List<RouteCandidate>) {
        val current = _tiles.value
        val hasOnlyWorld = current.size == 1 && worldPlaceholder(current.first())
        if (current.isNotEmpty() && !hasOnlyWorld) return
        if (candidates.isEmpty()) return

        var minLat = Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE
        var anyPoint = false
        candidates.forEach { route ->
            route.points.forEach { p ->
                anyPoint = true
                if (p.latitude < minLat) minLat = p.latitude
                if (p.latitude > maxLat) maxLat = p.latitude
                if (p.longitude < minLng) minLng = p.longitude
                if (p.longitude > maxLng) maxLng = p.longitude
            }
        }
        if (!anyPoint) return

        val padLat = (maxLat - minLat).coerceAtLeast(0.05) * 0.5
        val padLng = (maxLng - minLng).coerceAtLeast(0.05) * 0.5
        minLat -= padLat; maxLat += padLat; minLng -= padLng; maxLng += padLng

        val step = ((maxLat - minLat).coerceAtLeast(0.1) / 20.0).coerceIn(0.002, 0.03)
        val out = mutableListOf<RiskTile>()
        var lat = minLat
        while (lat < maxLat) {
            var lng = minLng
            while (lng < maxLng) {
                val centerLat = lat + step / 2
                val centerLng = lng + step / 2
                val noise = kotlin.math.abs(kotlin.math.sin(centerLat * 18.0) * kotlin.math.cos(centerLng * 21.0))
                val risk = 0.2 + 0.6 * noise
                out.add(RiskTile(latMin = lat, latMax = lat + step, lngMin = lng, lngMax = lng + step, risk = risk))
                lng += step
            }
            lat += step
        }
        _tiles.value = out
    }

    fun selectSafestRoute(routes: List<RouteCandidate>): RouteCandidate? {
        val weights = currentWeights()
        return selectRouteByWeights(routes, weights)
    }

    private fun selectRouteByWeights(routes: List<RouteCandidate>, weights: Weights): RouteCandidate? {
        val tilesSnapshot = tiles.value
        fun pointRisk(p: LatLng): Double {
            val t = tilesSnapshot.firstOrNull {
                p.latitude >= it.latMin && p.latitude <= it.latMax &&
                        p.longitude >= it.lngMin && p.longitude <= it.lngMax
            }
            return t?.risk ?: 0.3
        }
        fun haversineMeters(a: LatLng, b: LatLng): Double {
            val R = 6371000.0
            val dLat = Math.toRadians(b.latitude - a.latitude)
            val dLng = Math.toRadians(b.longitude - a.longitude)
            val sa = Math.sin(dLat / 2)
            val sb = Math.sin(dLng / 2)
            val aa = sa * sa + Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) * sb * sb
            val c = 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1 - aa))
            return R * c
        }
        fun avgRisk(route: RouteCandidate): Double {
            var riskLen = 0.0
            var totalLen = 0.0
            for (i in 0 until (route.points.size - 1)) {
                val a = route.points[i]
                val b = route.points[i + 1]
                val seg = haversineMeters(a, b)
                totalLen += seg
                val mid = LatLng((a.latitude + b.latitude) / 2.0, (a.longitude + b.longitude) / 2.0)
                riskLen += seg * pointRisk(mid)
            }
            return if (totalLen > 0) riskLen / totalLen else 0.3
        }

        // Nighttime pruning: discard very risky routes entirely
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = hour >= 20 || hour <= 6
        val riskThreshold = if (isNight) 0.5 else 0.8

        var best: RouteCandidate? = null
        var bestScore = Double.POSITIVE_INFINITY
        for (r in routes) {
            val rRisk = avgRisk(r)
            if (isNight && rRisk > riskThreshold) continue
            val timeCost = r.durationMin
            val distanceKm = r.distanceM / 1000.0
            val score = weights.alphaTime * timeCost + weights.betaDistance * distanceKm + weights.gammaRisk * rRisk
            if (score < bestScore) {
                bestScore = score
                best = r
            }
        }
        return best
    }

    fun setOrigin(latLng: LatLng) { origin.value = latLng }
    fun setDestination(latLng: LatLng) { destination.value = latLng }
    fun setCurrentLocation(latLng: LatLng) { currentLocation.value = latLng }

    fun computeSafestRoute() {
        val o = origin.value
        val d = destination.value
        if (o == null || d == null) {
            error.value = "Select origin and destination"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            loading.value = true
            error.value = null
            try {
                val fetched = fetchDirections(o, d)
                routes.value = fetched
                if (fetched.isNotEmpty()) {
                    // Populate demo risk tiles covering all candidates before selection
                    ensureDemoTilesForRoutes(fetched)
                }
                // Also compute fastest for UI delta display
                fastest.value = fetched.minByOrNull { it.durationMin }
                val best = when (profile.value) {
                    Profile.FASTEST -> fastest.value
                    Profile.BALANCED, Profile.SAFEST -> selectSafestRoute(fetched)
                }
                safest.value = best
                if (fetched.isEmpty()) {
                    // Fallback: show a simple straight line so the user sees something
                    val straightDist = haversineMeters(o, d)
                    val walkingSpeedMps = 1.3 // ~4.7 km/h reasonable walking speed
                    val etaMin = if (straightDist > 0) (straightDist / walkingSpeedMps) / 60.0 else 0.0
                    val fallback = RouteCandidate(points = listOf(o, d), durationMin = etaMin, distanceM = straightDist)
                    routes.value = listOf(fallback)
                    fastest.value = fallback
                    safest.value = fallback
                    // Populate demo tiles so risk display is meaningful even on fallback
                    ensureDemoTilesForRoute(fallback)
                    error.value = "No route found"
                }
            } catch (e: Exception) {
                Log.e("SafePathVM", "Directions error: ${e.message}")
                // Fallback: draw a straight line to indicate the path visually
                val o2 = origin.value
                val d2 = destination.value
                if (o2 != null && d2 != null) {
                    val straightDist = haversineMeters(o2, d2)
                    val walkingSpeedMps = 1.3
                    val etaMin = if (straightDist > 0) (straightDist / walkingSpeedMps) / 60.0 else 0.0
                    val fallback = RouteCandidate(points = listOf(o2, d2), durationMin = etaMin, distanceM = straightDist)
                    routes.value = listOf(fallback)
                    fastest.value = fallback
                    safest.value = fallback
                    ensureDemoTilesForRoute(fallback)
                }
                error.value = e.message
            } finally {
                loading.value = false
            }
        }
    }

    suspend fun geocodeGoogle(query: String, limit: Int = 5): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val apiKey = ctx.getString(R.string.google_maps_key)
            if (apiKey.isBlank()) return@withContext emptyList<SearchResult>()
            val q = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$q&key=$apiKey"
            val req = Request.Builder().url(url).header("User-Agent", "SecureShe/1.0 (geocode via Google)").build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList<SearchResult>()
                val body = resp.body?.string() ?: return@withContext emptyList<SearchResult>()
                val json = JSONObject(body)
                val status = json.optString("status")
                if (status != "OK") return@withContext emptyList<SearchResult>()
                val results = json.getJSONArray("results")
                val out = mutableListOf<SearchResult>()
                val count = kotlin.math.min(results.length(), limit)
                for (i in 0 until count) {
                    val r = results.getJSONObject(i)
                    val formatted = r.optString("formatted_address")
                    val loc = r.getJSONObject("geometry").getJSONObject("location")
                    val lat = loc.optDouble("lat")
                    val lng = loc.optDouble("lng")
                    out.add(SearchResult(label = formatted, location = LatLng(lat, lng)))
                }
                return@withContext out
            }
        }
    }

    fun computeRouteRisk(route: RouteCandidate): Double {
        val tilesSnapshot = tiles.value
        fun pointRisk(p: LatLng): Double {
            val t = tilesSnapshot.firstOrNull {
                p.latitude >= it.latMin && p.latitude <= it.latMax &&
                        p.longitude >= it.lngMin && p.longitude <= it.lngMax
            }
            return t?.risk ?: 0.3
        }
        var riskLen = 0.0
        var totalLen = 0.0
        for (i in 0 until (route.points.size - 1)) {
            val a = route.points[i]
            val b = route.points[i + 1]
            val seg = kotlin.run {
                val R = 6371000.0
                val dLat = Math.toRadians(b.latitude - a.latitude)
                val dLng = Math.toRadians(b.longitude - a.longitude)
                val sa = Math.sin(dLat / 2)
                val sb = Math.sin(dLng / 2)
                val aa = sa * sa + Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) * sb * sb
                val c = 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1 - aa))
                R * c
            }
            totalLen += seg
            val mid = LatLng((a.latitude + b.latitude) / 2.0, (a.longitude + b.longitude) / 2.0)
            riskLen += seg * pointRisk(mid)
        }
        return if (totalLen > 0) riskLen / totalLen else 0.3
    }

    private fun fetchDirections(origin: LatLng, dest: LatLng): List<RouteCandidate> {
        // Prefer Google Directions if available; fallback to OSRM
        try {
            val googleRoutes = fetchGoogleRoutes(origin, dest, alternatives = true)
            if (googleRoutes.isNotEmpty()) return googleRoutes
        } catch (_: Exception) {
            // Ignore and fallback
        }

        val base = fetchOsrmRoutes(origin, dest, alternatives = 3)
        val out = mutableListOf<RouteCandidate>()
        out.addAll(base)
        // If OSRM returned only one or two routes, synthesize extra candidates using vias chosen for diversity/risk
        if (out.size < 3) {
            val viaCandidates = buildViaCandidates(origin, dest)
                .filter { distanceMetersToSegment(it, origin, dest) > 75.0 }
                .take(6)
            for (via in viaCandidates) {
                try {
                    val r1 = fetchOsrmRoutes(origin, via, alternatives = 1).firstOrNull()
                    val r2 = fetchOsrmRoutes(via, dest, alternatives = 1).firstOrNull()
                    if (r1 != null && r2 != null) {
                        out.add(
                            RouteCandidate(
                                points = r1.points + r2.points,
                                durationMin = r1.durationMin + r2.durationMin,
                                distanceM = r1.distanceM + r2.distanceM
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Ignore failed variations
                }
            }
        }
        return out
    }

    private fun buildViaCandidates(origin: LatLng, dest: LatLng): List<LatLng> {
        val vias = mutableListOf<LatLng>()
        val mid = LatLng((origin.latitude + dest.latitude) / 2.0, (origin.longitude + dest.longitude) / 2.0)
        // Compute direction and a perpendicular vector in degrees
        val dLat = dest.latitude - origin.latitude
        val dLng = dest.longitude - origin.longitude
        val maxDelta = kotlin.math.max(kotlin.math.abs(dLat), kotlin.math.abs(dLng))
        val baseOffset = (maxDelta * 0.2).coerceIn(0.003, 0.03) // ~0.3–3km depending on scale
        val perpLat = -dLng
        val perpLng = dLat
        fun norm(x: Double, y: Double): Pair<Double, Double> {
            val n = kotlin.math.sqrt(x * x + y * y)
            return if (n == 0.0) (0.0 to 0.0) else (x / n to y / n)
        }
        val (nx, ny) = norm(perpLat, perpLng)
        // Perpendicular offsets around mid to encourage distinct corridors
        val scales = listOf(0.5, 1.0)
        for (s in scales) {
            val offLat = nx * baseOffset * s
            val offLng = ny * baseOffset * s
            vias.add(LatLng(mid.latitude + offLat, mid.longitude + offLng))
            vias.add(LatLng(mid.latitude - offLat, mid.longitude - offLng))
        }
        // Cardinal offsets around mid
        vias.add(LatLng(mid.latitude + baseOffset, mid.longitude))
        vias.add(LatLng(mid.latitude - baseOffset, mid.longitude))
        vias.add(LatLng(mid.latitude, mid.longitude + baseOffset))
        vias.add(LatLng(mid.latitude, mid.longitude - baseOffset))

        // Risk-biased via for SAFEST/BALANCED: pick lowest-risk tile near the corridor
        val prof = profile.value
        if (prof == Profile.SAFEST || prof == Profile.BALANCED) {
            val tilesSnapshot = tiles.value
            if (tilesSnapshot.isNotEmpty()) {
                // Bounding box around the OD with margin
                var minLat = kotlin.math.min(origin.latitude, dest.latitude)
                var maxLat = kotlin.math.max(origin.latitude, dest.latitude)
                var minLng = kotlin.math.min(origin.longitude, dest.longitude)
                var maxLng = kotlin.math.max(origin.longitude, dest.longitude)
                val mLat = (maxLat - minLat).coerceAtLeast(0.02) * 0.5
                val mLng = (maxLng - minLng).coerceAtLeast(0.02) * 0.5
                minLat -= mLat; maxLat += mLat; minLng -= mLng; maxLng += mLng
                val candidateTiles = tilesSnapshot.filter { t ->
                    t.latMax >= minLat && t.latMin <= maxLat && t.lngMax >= minLng && t.lngMin <= maxLng
                }
                val low = candidateTiles.minByOrNull { it.risk }
                if (low != null) {
                    val center = LatLng((low.latMin + low.latMax) / 2.0, (low.lngMin + low.lngMax) / 2.0)
                    vias.add(center)
                }
            }
        }
        return vias
    }

    private fun distanceMetersToSegment(p: LatLng, a: LatLng, b: LatLng): Double {
        // Quick equirectangular projection approximation
        val lat0 = Math.toRadians((a.latitude + b.latitude) / 2.0)
        val meterPerDegLat = 111_320.0
        val meterPerDegLng = 111_320.0 * kotlin.math.cos(lat0)
        fun toXY(pt: LatLng): Pair<Double, Double> {
            return (pt.longitude * meterPerDegLng) to (pt.latitude * meterPerDegLat)
        }
        val ap = toXY(p)
        val aa = toXY(a)
        val bb = toXY(b)
        val vx = bb.first - aa.first
        val vy = bb.second - aa.second
        val wx = ap.first - aa.first
        val wy = ap.second - aa.second
        val segLen2 = vx * vx + vy * vy
        val t = if (segLen2 == 0.0) 0.0 else ((wx * vx + wy * vy) / segLen2).coerceIn(0.0, 1.0)
        val projX = aa.first + t * vx
        val projY = aa.second + t * vy
        val dx = ap.first - projX
        val dy = ap.second - projY
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun fetchGoogleRoutes(origin: LatLng, dest: LatLng, alternatives: Boolean): List<RouteCandidate> {
        val ctx = getApplication<Application>()
        val apiKey = ctx.getString(R.string.google_maps_key)
        if (apiKey.isBlank()) return emptyList()
        val alt = if (alternatives) "true" else "false"
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&mode=walking&alternatives=$alt&key=$apiKey"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "SecureShe/1.0 (routing via Google Directions)")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IllegalStateException("Google HTTP ${resp.code}: ${body.take(120)}")
            val json = JSONObject(body)
            val status = json.optString("status")
            if (status != "OK") {
                val msg = json.optString("error_message")
                throw IllegalStateException("Google Directions: $status ${if (msg.isNotBlank()) "- $msg" else ""}")
            }
            val routesJson = json.getJSONArray("routes")
            val result = mutableListOf<RouteCandidate>()
            for (i in 0 until routesJson.length()) {
                val r = routesJson.getJSONObject(i)
                val overview = r.getJSONObject("overview_polyline").getString("points")
                val legs = r.getJSONArray("legs")
                var durationSec = 0.0
                var distanceM = 0.0
                for (j in 0 until legs.length()) {
                    val leg = legs.getJSONObject(j)
                    durationSec += leg.getJSONObject("duration").getDouble("value")
                    distanceM += leg.getJSONObject("distance").getDouble("value")
                }
                val pts = decodePolyline(overview)
                result.add(RouteCandidate(points = pts, durationMin = durationSec / 60.0, distanceM = distanceM))
            }
            return result
        }
    }

    private fun fetchOsrmRoutes(origin: LatLng, dest: LatLng, alternatives: Int): List<RouteCandidate> {
        // OSRM demo server, no API key required. Be polite with a User-Agent.
        // OSRM's 'alternatives' is a boolean; request alternatives when caller asks for more than one
        val alt = if (alternatives > 1) "true" else "false"
        val url = "https://router.project-osrm.org/route/v1/foot/${origin.longitude},${origin.latitude};${dest.longitude},${dest.latitude}?overview=full&geometries=polyline&alternatives=$alt&steps=false&continue_straight=true"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "SecureShe/1.0 (routing via OSRM)")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IllegalStateException("OSRM HTTP ${resp.code}: ${body.take(120)}")
            val json = JSONObject(body)
            val code = json.optString("code")
            if (code != "Ok") throw IllegalStateException("OSRM status: $code")
            val routesJson = json.getJSONArray("routes")
            val result = mutableListOf<RouteCandidate>()
            for (i in 0 until routesJson.length()) {
                val r = routesJson.getJSONObject(i)
                val overview = r.getString("geometry")
                val durationSec = r.getDouble("duration")
                val distanceM = r.getDouble("distance")
                val pts = decodePolyline(overview)
                result.add(RouteCandidate(points = pts, durationMin = durationSec / 60.0, distanceM = distanceM))
            }
            return result
        }
    }

    fun searchPlacesOSM(query: String, limit: Int = 5) {
        if (query.isBlank()) {
            searchResults.value = emptyList()
            searchError.value = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            searchLoading.value = true
            searchError.value = null
            try {
                val q = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?q=$q&format=json&limit=$limit"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SecureShe/1.0 (search via Nominatim)")
                    .header("Accept-Language", "en")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                    val body = resp.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    val out = mutableListOf<SearchResult>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val name = o.optString("display_name")
                        val lat = o.optString("lat").toDoubleOrNull()
                        val lon = o.optString("lon").toDoubleOrNull()
                        if (lat != null && lon != null) {
                            out.add(SearchResult(label = name, location = LatLng(lat, lon)))
                        }
                    }
                    searchResults.value = out
                }
            } catch (e: Exception) {
                searchError.value = e.message
            } finally {
                searchLoading.value = false
            }
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                // Correct precedence: (b and 0x1f) shl shift
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                // Correct precedence: (b and 0x1f) shl shift
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(lat / 1E5, lng / 1E5)
            poly.add(latLng)
        }
        return poly
    }

    // --- Deviation detection ---
    fun evaluateDeviation(thresholdMeters: Double = 50.0) {
        val loc = currentLocation.value ?: return
        val route = safest.value ?: return
        val dist = distanceToRouteMeters(loc, route)
        offRoute.value = dist > thresholdMeters
    }

    private fun distanceToRouteMeters(p: LatLng, route: RouteCandidate): Double {
        if (route.points.size < 2) return Double.MAX_VALUE
        var best = Double.MAX_VALUE
        // Use local equirectangular projection for speed
        val lat0 = Math.toRadians(route.points[0].latitude)
        val cosLat0 = Math.cos(lat0)
        fun toXY(pt: LatLng): Pair<Double, Double> {
            val x = (pt.longitude) * cosLat0
            val y = (pt.latitude)
            return x to y
        }
        val pr = toXY(p)
        for (i in 0 until route.points.size - 1) {
            val a = toXY(route.points[i])
            val b = toXY(route.points[i + 1])
            val segDx = b.first - a.first
            val segDy = b.second - a.second
            val segLen2 = segDx * segDx + segDy * segDy
            val t = if (segLen2 == 0.0) 0.0 else ((pr.first - a.first) * segDx + (pr.second - a.second) * segDy) / segLen2
            val clampedT = t.coerceIn(0.0, 1.0)
            val projX = a.first + clampedT * segDx
            val projY = a.second + clampedT * segDy
            val dx = pr.first - projX
            val dy = pr.second - projY
            // convert deg to meters approximation
            val meterPerDegLat = 111_320.0
            val meterPerDegLng = 111_320.0 * cosLat0
            val distM = Math.sqrt((dx * meterPerDegLng) * (dx * meterPerDegLng) + (dy * meterPerDegLat) * (dy * meterPerDegLat))
            if (distM < best) best = distM
        }
        return best
    }
}


