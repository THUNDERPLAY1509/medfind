package com.example.medfind

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private val client = OkHttpClient()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicineAdapter
    private lateinit var searchInput: AutoCompleteTextView
    private var medicineList: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefs = getSharedPreferences("MedFindPrefs", Context.MODE_PRIVATE)
        searchInput = findViewById(R.id.searchInput)
        val searchButton = findViewById<ImageButton>(R.id.searchButton)
        recyclerView = findViewById(R.id.medList)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MedicineAdapter(emptyList())
        recyclerView.adapter = adapter

        // Load medicine list for autocomplete
        CoroutineScope(Dispatchers.Main).launch {
            medicineList = loadMedicineList()
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, medicineList)
            searchInput.setAdapter(adapter)
            searchInput.setOnItemClickListener { _, _, position, _ ->
                val selectedMedicine = adapter.getItem(position) as String
                searchInput.setText(selectedMedicine)
            }
        }

        // Search button click
        searchButton.setOnClickListener {
            val medicine = searchInput.text.toString().trim()
            if (medicine.isNotEmpty()) {
                if (medicineList.contains(medicine)) {
                    fetchSimilarMedicines(medicine)
                } else {
                    Toast.makeText(this, "Please select a medicine from the suggestions", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Enter a medicine", Toast.LENGTH_SHORT).show()
            }
        }

        // Bottom navigation
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> true // Already on this screen
                R.id.nav_pharmacies -> {
                    startActivity(Intent(this, PharmacyActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_search
    }

    private suspend fun loadMedicineList(): List<String> = withContext(Dispatchers.IO) {
        val medicines = mutableListOf<String>()
        try {
            val inputStream = resources.openRawResource(R.raw.medicinenames)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.useLines { lines ->
                lines.forEach { line ->
                    val name = line.trim()
                    if (name.isNotEmpty()) {
                        medicines.add(name)
                    }
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Error loading medicine list: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        medicines
    }

    private fun fetchSimilarMedicines(medicine: String) {
        val url = "http://192.168.147.47:5000/similar?medicine=$medicine"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "API failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                runOnUiThread {
                    try {
                        val jsonObject = JSONObject(json)
                        val similarArray = jsonObject.getJSONArray("similar")
                        val medList = mutableListOf<Medicine>()
                        for (i in 0 until similarArray.length()) {
                            val medJson = similarArray.getJSONObject(i)
                            val med = Medicine(
                                name = medJson.getString("name"),
                                similarity = medJson.getDouble("similarity")
                            )
                            medList.add(med)
                        }
                        adapter = MedicineAdapter(medList)
                        recyclerView.adapter = adapter
                        Toast.makeText(this@MainActivity, "Found ${medList.size} similar meds!", Toast.LENGTH_SHORT).show()

                        // Save to history
                        val historySet = sharedPrefs.getStringSet("searchHistory", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                        historySet.add(medicine)
                        sharedPrefs.edit().putStringSet("searchHistory", historySet).apply()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Parse error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}