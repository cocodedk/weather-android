package dk.cocode.weather.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * org.json returns NaN / 0 / "null" where the API means "no value", which would
 * render as a real reading. These helpers collapse every one of those cases to a
 * Kotlin null so the formatters can show "--" instead of a fabricated number.
 */

fun JSONObject.doubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val v = optDouble(key, Double.NaN)
    return if (v.isNaN()) null else v
}

fun JSONObject.intOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
}

fun JSONObject.stringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key, "").takeIf { it.isNotEmpty() }
}

fun JSONArray.doubleAt(i: Int): Double? {
    if (i >= length() || isNull(i)) return null
    val v = optDouble(i, Double.NaN)
    return if (v.isNaN()) null else v
}

fun JSONArray.intAt(i: Int): Int? {
    if (i >= length() || isNull(i)) return null
    return optInt(i, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
}

fun JSONArray.stringAt(i: Int): String? {
    if (i >= length() || isNull(i)) return null
    return optString(i, "").takeIf { it.isNotEmpty() }
}
