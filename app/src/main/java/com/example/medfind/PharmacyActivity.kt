package com.example.medfind

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class PharmacyActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var loadingIndicator: ProgressBar
    private val locationPermissionRequestCode = 1
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacy)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Bottom navigation
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_pharmacies -> true // Already on this screen
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_pharmacies
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            getCurrentLocationAndFetchPharmacies()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequestCode
            )
        }
    }

    private fun getCurrentLocationAndFetchPharmacies() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            loadingIndicator.visibility = View.VISIBLE
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13f))
                    fetchNearbyPharmacies(currentLatLng)
                } else {
                    Toast.makeText(this, "Couldn't get location", Toast.LENGTH_LONG).show()
                    loadingIndicator.visibility = View.GONE
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Location fetch failed: ${e.message}", Toast.LENGTH_LONG).show()
                loadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun fetchNearbyPharmacies(currentLatLng: LatLng) {
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${currentLatLng.latitude},${currentLatLng.longitude}" +
                "&radius=5000" +
                "&type=pharmacy" +
                "&fields=name,geometry/location,formatted_phone_number" +
                "&key=AIzaSyBRt8n544enSPGtnBQZ5TS0XgF4vwy8fUk"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@PharmacyActivity, "Failed to find pharmacies: ${e.message}", Toast.LENGTH_LONG).show()
                    loadingIndicator.visibility = View.GONE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                runOnUiThread {
                    try {
                        val jsonObject = JSONObject(json)
                        val status = jsonObject.getString("status")
                        if (status != "OK") {
                            Toast.makeText(this@PharmacyActivity, "API error: $status", Toast.LENGTH_LONG).show()
                            loadingIndicator.visibility = View.GONE
                            return@runOnUiThread
                        }

                        val results = jsonObject.getJSONArray("results")
                        var pharmacyCount = 0

                        for (i in 0 until results.length()) {
                            val place = results.getJSONObject(i)
                            val name = place.getString("name")
                            val geometry = place.getJSONObject("geometry")
                            val location = geometry.getJSONObject("location")
                            val lat = location.getDouble("lat")
                            val lng = location.getDouble("lng")
                            val latLng = LatLng(lat, lng)

                            val phone = if (place.has("formatted_phone_number")) {
                                place.getString("formatted_phone_number")
                            } else {
                                "No phone available"
                            }

                            mMap.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .snippet("Phone: $phone")
                            )
                            pharmacyCount++
                        }
                        Toast.makeText(this@PharmacyActivity, "Found $pharmacyCount pharmacies near you", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@PharmacyActivity, "Parse error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        loadingIndicator.visibility = View.GONE
                    }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    mMap.isMyLocationEnabled = true
                    getCurrentLocationAndFetchPharmacies()
                }
            } else {
                Toast.makeText(this, "Need location perms for pharmacies!", Toast.LENGTH_LONG).show()
            }
        }
    }
}