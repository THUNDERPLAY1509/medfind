package com.example.medfind

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class HistoryActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        sharedPrefs = getSharedPreferences("MedFindPrefs", Context.MODE_PRIVATE)
        recyclerView = findViewById(R.id.historyList)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val historySet = sharedPrefs.getStringSet("searchHistory", emptySet())?.toList() ?: emptyList()
        adapter = HistoryAdapter(historySet) { medicine ->
            // On click, go back to MainActivity and search for this medicine
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("medicine", medicine)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Bottom navigation
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_pharmacies -> {
                    startActivity(Intent(this, PharmacyActivity::class.java))
                    true
                }
                R.id.nav_history -> true // Already on this screen
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_history
    }
}