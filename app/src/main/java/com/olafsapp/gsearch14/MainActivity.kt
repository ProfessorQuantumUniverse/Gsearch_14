package com.olafsapp.gsearch14

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.addListener
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.olafsapp.gsearch14.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var historyAdapter: SearchHistoryAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private var searchHistory = mutableListOf<SearchHistoryItem>()
    private var isWebViewVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SharedPreferences initialisieren
        sharedPreferences = getSharedPreferences("search_prefs", MODE_PRIVATE)

        // Prüfen ob App vom Widget gestartet wurde
        handleWidgetLaunch()

        // Setup verschiedener Komponenten
        setupBackPressedHandler()
        setupWebView()
        setupSearchHistory()
        setupListeners()
        setupBubbleToolbar()
        setupInitialAnimations()

        // Theme aus Einstellungen laden
        loadThemePreference()

        // Suchverlauf anzeigen falls vorhanden
        updateHistoryVisibility()
    }

    private fun setupInitialAnimations() {
        // Initiale Animation für die Haupt-Card
        binding.searchCard.apply {
            alpha = 0f
            translationY = 100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Animation für die Historie-Card mit Verzögerung
        binding.historyCard.apply {
            alpha = 0f
            translationY = 80f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Animation für die Bubble-Toolbar
        binding.bubbleToolbar.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(0.92f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(400)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }
    }

    private fun setupBubbleToolbar() {
        // Theme Toggle Button mit Animation
        binding.themeToggleButton.setOnClickListener {
            animateButtonClick(binding.themeToggleButton) {
                toggleTheme()
            }
        }

        // History Button mit Animation
        binding.historyButton.setOnClickListener {
            animateButtonClick(binding.historyButton) {
                clearSearchHistory()
            }
        }
    }

    private fun animateButtonClick(button: View, action: () -> Unit) {
        // Klick-Animation
        button.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(OvershootInterpolator(2f))
                    .withEndAction {
                        action.invoke()
                    }
                    .start()
            }
            .start()
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.webView.canGoBack() && binding.webView.isVisible -> {
                        binding.webView.goBack()
                    }
                    binding.webView.isVisible -> {
                        showSearchInterface()
                    }
                    else -> {
                        finish()
                    }
                }
            }
        })
    }

    private fun setupWebView() {
        // SwipeRefreshLayout konfigurieren
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.webView.reload()
        }

        binding.webView.apply {
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.swipeRefreshLayout.isRefreshing = true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.swipeRefreshLayout.isRefreshing = false
                    // FAB mit Animation anzeigen
                    if (!binding.quickSearchFab.isVisible) {
                        binding.quickSearchFab.apply {
                            visibility = View.VISIBLE
                            alpha = 0f
                            scaleX = 0.3f
                            scaleY = 0.3f
                            animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(300)
                                .setInterpolator(OvershootInterpolator(1.5f))
                                .start()
                        }
                    }
                }

                override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }

            settings.apply {
                @Suppress("SetJavaScriptEnabled")
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
            }
        }
    }

    private fun setupSearchHistory() {
        loadSearchHistory()

        historyAdapter = SearchHistoryAdapter(mutableListOf()) { clickedItem ->
            // Auf geklickten Eintrag zurücksetzen und suchen
            binding.searchEditText.setText(clickedItem.query)
            // Suchtyp-Chip auswählen
            when (clickedItem.searchType) {
                "Images" -> binding.searchTypeChipGroup.check(binding.chipImages.id)
                "Videos" -> binding.searchTypeChipGroup.check(binding.chipVideos.id)
                "News"   -> binding.searchTypeChipGroup.check(binding.chipNews.id)
                else      -> binding.searchTypeChipGroup.check(binding.chipAll.id)
            }
            binding.aiToggle.isChecked = clickedItem.useAI
            performSearch()
        }

        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
            setHasFixedSize(true)
        }

        // Aktualisiere Adapter mit Historie
        historyAdapter.updateHistory(searchHistory)

        // Clear History Button
        binding.clearHistoryButton.setOnClickListener {
            animateButtonClick(binding.clearHistoryButton) {
                clearSearchHistory()
            }
        }
    }

    private fun setupListeners() {
        // Suche-Button
        binding.searchButton.setOnClickListener {
            animateSearchButtonClick()
        }

        // Enter-Taste im Suchfeld
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                animateSearchButtonClick()
                true
            } else {
                false
            }
        }

        // Quick Search FAB
        binding.quickSearchFab.setOnClickListener {
            animateButtonClick(binding.quickSearchFab) {
                showSearchInterface()
            }
        }

        // Chip-Auswahl Animation
        binding.searchTypeChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // Leichte Pulse-Animation für ausgewählten Chip
            if (checkedIds.isNotEmpty()) {
                val selectedChip = findViewById<View>(checkedIds[0])
                selectedChip?.let { chip ->
                    chip.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(150)
                        .withEndAction {
                            chip.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .start()
                        }
                        .start()
                }
            }
        }

        // AI Toggle Animation mit dynamischem Text
        binding.aiToggle.setOnCheckedChangeListener { _, isChecked ->
            // Text basierend auf Status aktualisieren
            binding.aiToggle.text = if (isChecked) "Deactivate AI" else "Deactivate AI"

            // Rotation Animation
            val rotation = if (isChecked) 360f else -360f
            binding.aiToggle.animate()
                .rotation(rotation)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Initialen Text für AI Toggle setzen
        binding.aiToggle.text = if (binding.aiToggle.isChecked) "Deactivate AI" else "Activate AI"
    }

    private fun animateSearchButtonClick() {
        // Suchbutton Animation
        binding.searchButton.apply {
            animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .setInterpolator(OvershootInterpolator(2f))
                        .withEndAction {
                            performSearch()
                        }
                        .start()
                }
                .start()
        }

        // Pulseffekt für das Suchfeld
        binding.searchTextInputLayout.animate()
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(200)
            .withEndAction {
                binding.searchTextInputLayout.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun performSearch() {
        val query = binding.searchEditText.text.toString().trim()

        if (query.isEmpty()) {
            Toast.makeText(this, "Bitte geben Sie einen Suchbegriff ein", Toast.LENGTH_SHORT).show()
            return
        }

        // Zur Historie hinzufügen
        val searchType = getSelectedSearchType()
        val useAI = binding.aiToggle.isChecked
        addToHistory(query, searchType, useAI)

        // URL erstellen
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val baseUrl = if (useAI) "https://www.google.com/search?q=" else "https://www.google.com/search?q="
        val typeParam = when (searchType) {
            "Images" -> "&tbm=isch"
            "Videos" -> "&tbm=vid"
            "News" -> "&tbm=nws"
            else -> ""
        }
        val aiParam = if (useAI) "" else "&udm=14"

        val searchUrl = "$baseUrl$encodedQuery$typeParam$aiParam"

        // Zur WebView wechseln mit Animation
        showWebView(searchUrl)
    }

    private fun showWebView(url: String) {
        // Animation für den Übergang zur WebView
        binding.nestedScrollView.animate()
            .alpha(0f)
            .translationY(-50f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.nestedScrollView.visibility = View.GONE
                binding.bubbleToolbar.animate()
                    .alpha(0f)
                    .translationY(100f)
                    .setDuration(200)
                    .withEndAction {
                        binding.bubbleToolbar.visibility = View.GONE
                    }
                    .start()

                // WebView anzeigen
                binding.swipeRefreshLayout.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }

                binding.webView.loadUrl(url)
                isWebViewVisible = true
            }
            .start()
    }

    private fun showSearchInterface() {
        // Animation für den Übergang zurück zur Suchoberfläche
        binding.swipeRefreshLayout.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.swipeRefreshLayout.visibility = View.GONE
                binding.quickSearchFab.animate()
                    .alpha(0f)
                    .scaleX(0.3f)
                    .scaleY(0.3f)
                    .setDuration(200)
                    .withEndAction {
                        binding.quickSearchFab.visibility = View.GONE
                    }
                    .start()

                // Suchoberfläche anzeigen
                binding.nestedScrollView.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    translationY = 50f
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }

                binding.bubbleToolbar.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    translationY = 100f
                    animate()
                        .alpha(0.92f)
                        .translationY(0f)
                        .setDuration(300)
                        .setStartDelay(150)
                        .setInterpolator(OvershootInterpolator(1.2f))
                        .start()
                }

                isWebViewVisible = false
            }
            .start()
    }

    private fun getSelectedSearchType(): String {
        return when (binding.searchTypeChipGroup.checkedChipId) {
            binding.chipImages.id -> "Images"
            binding.chipVideos.id -> "Videos"
            binding.chipNews.id -> "News"
            else -> "All"
        }
    }

    private fun addToHistory(query: String, searchType: String, useAI: Boolean) {
        val timestamp = System.currentTimeMillis()
        val newItem = SearchHistoryItem(query, searchType, useAI, timestamp)

        // Duplikate entfernen
        searchHistory.removeAll { it.query == query && it.searchType == searchType }

        // An den Anfang hinzufügen
        searchHistory.add(0, newItem)

        // Maximal 20 Einträge behalten
        if (searchHistory.size > 20) {
            searchHistory = searchHistory.take(20).toMutableList()
        }

        // Speichern und UI aktualisieren
        saveSearchHistory()
        historyAdapter.updateHistory(searchHistory)
        updateHistoryVisibility()
    }

    private fun clearSearchHistory() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Suchverlauf löschen")
            .setMessage("Möchten Sie den gesamten Suchverlauf löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                // Animation für das Verschwinden der Historie
                binding.historyCard.animate()
                    .alpha(0f)
                    .scaleY(0.8f)
                    .setDuration(300)
                    .withEndAction {
                        searchHistory.clear()
                        saveSearchHistory()
                        historyAdapter.updateHistory(searchHistory)
                        updateHistoryVisibility()

                        // Wieder einblenden falls noch Einträge vorhanden
                        if (searchHistory.isNotEmpty()) {
                            binding.historyCard.animate()
                                .alpha(1f)
                                .scaleY(1f)
                                .setDuration(300)
                                .start()
                        }
                    }
                    .start()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun toggleTheme() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        val newNightMode = if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }

        // Theme-Einstellung speichern
        sharedPreferences.edit {
            putInt("night_mode", newNightMode)
        }

        // Sofortiger Theme-Wechsel ohne komplexe Animation
        AppCompatDelegate.setDefaultNightMode(newNightMode)

        // Einfache Übergangsanimation für bessere UX
        val rootView = window.decorView
        rootView.animate()
            .alpha(0.8f)
            .setDuration(150)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                rootView.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun loadThemePreference() {
        val savedNightMode = sharedPreferences.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedNightMode)
    }

    private fun saveSearchHistory() {
        val json = gson.toJson(searchHistory)
        sharedPreferences.edit {
            putString("search_history", json)
        }
    }

    private fun loadSearchHistory() {
        val json = sharedPreferences.getString("search_history", null)
        if (json != null) {
            try {
                val type: Type = object : TypeToken<MutableList<SearchHistoryItem>>() {}.type
                val loadedHistory: MutableList<SearchHistoryItem> = gson.fromJson(json, type)
                searchHistory.clear()
                searchHistory.addAll(loadedHistory)
            } catch (e: Exception) {
                searchHistory.clear()
            }
        }
    }

    private fun updateHistoryVisibility() {
        if (searchHistory.isNotEmpty()) {
            if (binding.historyCard.visibility != View.VISIBLE) {
                binding.historyCard.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    scaleY = 0.8f
                    animate()
                        .alpha(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
        } else {
            binding.historyCard.visibility = View.GONE
        }
    }

    private fun handleWidgetLaunch() {
        // Prüfen ob App vom Widget gestartet wurde
        val isFromWidget = intent.getBooleanExtra("from_widget", false)

        if (isFromWidget) {
            // AI standardmäßig deaktivieren für Widget-Launches
            binding.aiToggle.isChecked = false

            // Tastatur erst nach den Animationen anzeigen
            binding.searchCard.post {
                // Längere Verzögerung für zuverlässige Tastatur-Anzeige
                binding.searchCard.postDelayed({
                    showKeyboardForWidget()
                }, 800) // Warten bis alle Animationen fertig sind
            }

            // Suchkarte hervorheben mit Animation
            binding.searchCard.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(300)
                .withEndAction {
                    binding.searchCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }
    }

    private fun showKeyboardForWidget() {
        // Sicherstellen, dass das Suchfeld fokussiert ist
        binding.searchEditText.requestFocus()

        // InputMethodManager holen
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager

        // Mehrere Versuche für zuverlässige Tastatur-Anzeige
        binding.searchEditText.post {
            // Erster Versuch: Standard-Methode
            imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)

            // Zweiter Versuch nach kurzer Verzögerung
            binding.searchEditText.postDelayed({
                if (!imm.isAcceptingText) {
                    imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
                }
            }, 200)

            // Dritter Versuch als Fallback
            binding.searchEditText.postDelayed({
                if (!imm.isAcceptingText) {
                    // Alternative Methode - Tastatur umschalten
                    imm.toggleSoftInput(android.view.inputmethod.InputMethodManager.SHOW_FORCED, 0)
                }
            }, 500)

            // Vierter Versuch - sehr aggressiv
            binding.searchEditText.postDelayed({
                if (!imm.isAcceptingText) {
                    // Fenster-Flags setzen für Tastatur-Anzeige
                    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                    imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
                }
            }, 800)
        }
    }
}
