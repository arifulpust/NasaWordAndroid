package jp.co.worldearthandroid

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

@SuppressLint("MissingPermission")
fun Context.getLocation(): GeoLocation? {
    var latLong: GeoLocation? = null
    try {
        val locationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val LocationGps: Location? =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val LocationNetwork: Location? =
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val LocationPassive: Location? =
            locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)


        when {
            LocationGps != null -> {
                val lat = LocationGps.latitude
                val longi = LocationGps.longitude
                latLong = GeoLocation(lat, longi)
            }
            LocationNetwork != null -> {
                val lat = LocationNetwork.latitude
                val longi = LocationNetwork.longitude

                latLong = GeoLocation(lat, longi)
            }
            LocationPassive != null -> {
                val lat = LocationPassive.latitude
                val longi = LocationPassive.longitude
                latLong = GeoLocation(lat, longi)
            }
            else -> {
//                return null
            }
        }

    } catch (e: Exception) {

    }

    if (latLong == null) {
        return GeoLocation(0.0, 0.0)
    }

    return latLong
}
var  MIN_ALTITUDE=2.6214752977566265E7

fun Context.getMyDrawable(id: Int): Drawable? {

    return ContextCompat.getDrawable(this, id)
}