package com.olafsapp.gsearch14

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.recyclerview.widget.ItemTouchHelper
import com.olafsapp.gsearch14.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar setup
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Settings"

        // SharedPreferences initialisieren
        sharedPreferences = getSharedPreferences("search_prefs", MODE_PRIVATE)

        setupSettings()
    }

    private fun setupSettings() {
        // Theme-Einstellung
        val currentTheme = sharedPreferences.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> binding.themeRadioGroup.check(binding.radioDark.id)
            AppCompatDelegate.MODE_NIGHT_NO -> binding.themeRadioGroup.check(binding.radioLight.id)
            else -> binding.themeRadioGroup.check(binding.radioSystem.id)
        }

        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                binding.radioDark.id -> AppCompatDelegate.MODE_NIGHT_YES
                binding.radioLight.id -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            sharedPreferences.edit {
                putInt("night_mode", newTheme)
            }

            AppCompatDelegate.setDefaultNightMode(newTheme)
        }

        // Swipe-Richtung Einstellung
        val swipeDirection = sharedPreferences.getString("swipe_direction", "both")
        when (swipeDirection) {
            "left" -> binding.swipeRadioGroup.check(binding.radioLeftOnly.id)
            "right" -> binding.swipeRadioGroup.check(binding.radioRightOnly.id)
            else -> binding.swipeRadioGroup.check(binding.radioBoth.id)
        }

        binding.swipeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newDirection = when (checkedId) {
                binding.radioLeftOnly.id -> "left"
                binding.radioRightOnly.id -> "right"
                else -> "both"
            }

            sharedPreferences.edit {
                putString("swipe_direction", newDirection)
            }
        }

        // Browser-Auswahl Einstellung
        val browserChoice = sharedPreferences.getString("browser_choice", "webview")
        when (browserChoice) {
            "external" -> binding.browserRadioGroup.check(binding.radioExternalBrowser.id)
            else -> binding.browserRadioGroup.check(binding.radioWebView.id)
        }

        binding.browserRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newChoice = when (checkedId) {
                binding.radioExternalBrowser.id -> "external"
                else -> "webview"
            }

            sharedPreferences.edit {
                putString("browser_choice", newChoice)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
