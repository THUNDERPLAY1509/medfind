package com.example.medfind

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class RegisterActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        sharedPrefs = getSharedPreferences("MedFindPrefs", MODE_PRIVATE)
        usernameInput = findViewById(R.id.registerUsernameInput)
        passwordInput = findViewById(R.id.registerPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        val registerButton = findViewById<Button>(R.id.registerSubmitButton)
        val backToLoginButton = findViewById<TextView>(R.id.backToLoginButton)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Validation
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if username already exists
            if (sharedPrefs.contains("user_$username")) {
                Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Register the user
            sharedPrefs.edit().putString("user_$username", password).apply()
            Toast.makeText(this, "Registered successfully, Now login.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        backToLoginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}