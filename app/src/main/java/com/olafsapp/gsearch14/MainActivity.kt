package com.olafsapp.gsearch14

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.olafsapp.gsearch14.databinding.ActivityMainBinding
import java.lang.reflect.Type
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var historyAdapter: SearchHistoryAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private var searchHistory = mutableListOf<SearchHistoryItem>()
    private var isWebViewVisible = false

    // KI Toggle Button Setup - Viel intuitiver als der alte Switch!
    private var isAiEnabled = false

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

        // Initialen Zustand für AI Toggle setzen
        binding.aiToggleSwitch.isChecked = false
        updateAiLabels(false)
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
        // Settings Button (ersetzt den Theme Toggle)
        binding.themeToggleButton.setOnClickListener {
            animateButtonClick(binding.themeToggleButton) {
                startActivity(Intent(this, SettingsActivity::class.java))
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

        historyAdapter = SearchHistoryAdapter(
            mutableListOf(),
            { clickedItem ->
                // Auf geklickten Eintrag zurücksetzen und suchen
                binding.searchEditText.setText(clickedItem.query)
                // Suchtyp-Chip auswählen
                when (clickedItem.searchType) {
                    "Images" -> binding.searchTypeChipGroup.check(binding.chipImages.id)
                    "Videos" -> binding.searchTypeChipGroup.check(binding.chipVideos.id)
                    "News"   -> binding.searchTypeChipGroup.check(binding.chipNews.id)
                    else      -> binding.searchTypeChipGroup.check(binding.chipAll.id)
                }
                // KI-Status aus Historie übernehmen
                updateAiToggleState(clickedItem.useAI)
                performSearch()
            },
            { item, position ->
                // Löschen-Callback
                showDeleteConfirmationDialog(item) {
                    removeFromHistory(item)
                }
            }
        )

        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
            setHasFixedSize(true
            )
        }

        // Aktualisiere Adapter mit Historie
        historyAdapter.updateHistory(searchHistory)

        // Clear History Button
        binding.clearHistoryButton.setOnClickListener {
            animateButtonClick(binding.clearHistoryButton) {
                clearSearchHistory()
            }
        }

        // Swipe to Delete für Historie-Einträge mit konfigurierbarer Richtung
        val swipeDirection = getSwipeDirection()
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, swipeDirection) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < historyAdapter.itemCount) {
                    val item = historyAdapter.getItemAtPosition(position)
                    // Löschen mit Bestätigungsdialog
                    showDeleteConfirmationDialog(item) {
                        removeFromHistory(item)
                    }
                } else {
                    // Position ungültig - Adapter zurücksetzen
                    historyAdapter.notifyItemChanged(position)
                }
            }

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                // Bewegungsflags basierend auf den Einstellungen setzen
                val swipeFlags = getSwipeDirection()
                return makeMovementFlags(0, swipeFlags)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                val itemView = viewHolder.itemView
                val swipeThreshold = itemView.width * 0.25f
                val allowedSwipeDirections = getSwipeDirection()

                // Nur zeichnen wenn wirklich geswiped wird
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    // Bestimme aktuelle Swipe-Richtung
                    val currentDirection = if (dX > 0) ItemTouchHelper.RIGHT else ItemTouchHelper.LEFT

                    // Prüfe ob die aktuelle Richtung erlaubt ist
                    if (allowedSwipeDirections and currentDirection != 0) {
                        val backgroundColor = androidx.core.graphics.ColorUtils.setAlphaComponent(
                            "#f44336".toColorInt(),
                            (Math.abs(dX) / swipeThreshold * 255).toInt().coerceIn(0, 255)
                        )
                        val icon = R.drawable.ic_delete

                        // Hintergrund zeichnen
                        val background = backgroundColor.toDrawable()
                        val iconDrawable = ContextCompat.getDrawable(this@MainActivity, icon)

                        if (dX > 0) {
                            // Swipe nach rechts
                            background.setBounds(itemView.left, itemView.top, (itemView.left + dX).toInt(), itemView.bottom)
                            iconDrawable?.setBounds(
                                itemView.left + 32.dp,
                                itemView.top + (itemView.height / 2) - 12.dp,
                                itemView.left + 56.dp,
                                itemView.top + (itemView.height / 2) + 12.dp
                            )
                        } else {
                            // Swipe nach links
                            background.setBounds((itemView.right + dX).toInt(), itemView.top, itemView.right, itemView.bottom)
                            iconDrawable?.setBounds(
                                itemView.right - 56.dp,
                                itemView.top + (itemView.height / 2) - 12.dp,
                                itemView.right - 32.dp,
                                itemView.top + (itemView.height / 2) + 12.dp
                            )
                        }

                        // Hintergrund und Icon zeichnen
                        background.draw(c)
                        iconDrawable?.draw(c)

                        // Transparenz basierend auf Swipe-Distanz
                        val alpha = Math.max(0.3f, 1f - Math.abs(dX) / itemView.width.toFloat())
                        itemView.alpha = alpha
                    } else {
                        // Nicht erlaubte Richtung - Swipe blockieren
                        itemView.alpha = 1f
                        // Swipe zurücksetzen wenn nicht erlaubt
                        if (Math.abs(dX) > swipeThreshold) {
                            itemView.translationX = 0f
                        }
                    }
                } else {
                    // Swipe beendet - Transparenz zurücksetzen
                    itemView.alpha = 1f
                }

                // Nur super.onChildDraw aufrufen wenn Richtung erlaubt ist
                val currentDirection = if (dX > 0) ItemTouchHelper.RIGHT else ItemTouchHelper.LEFT
                if (allowedSwipeDirections and currentDirection != 0) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }

            override fun onChildDrawOver(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder?,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // Sicherstellen, dass der View wieder normal aussieht
                viewHolder.itemView.alpha = 1f
                viewHolder.itemView.translationX = 0f
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.25f // 25% der Breite für Swipe-Trigger
            }
        })

        // ItemTouchHelper an RecyclerView anhängen
        itemTouchHelper.attachToRecyclerView(binding.historyRecyclerView)
    }

    private fun getSwipeDirection(): Int {
        val swipeDirectionPref = sharedPreferences.getString("swipe_direction", "both")
        return when (swipeDirectionPref) {
            "left" -> ItemTouchHelper.LEFT
            "right" -> ItemTouchHelper.RIGHT
            else -> ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
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

        // AI Toggle Switch - Mit visueller Label-Aktualisierung
        binding.aiToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            isAiEnabled = isChecked
            updateAiLabels(isChecked)

            // Kurzes Feedback
            val feedbackText = if (isChecked) {
                getString(R.string.ai_activated_toast)
            } else {
                getString(R.string.ai_deactivated_toast)
            }
            Toast.makeText(this, feedbackText, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAiToggleState(enabled: Boolean) {
        isAiEnabled = enabled
        binding.aiToggleSwitch.isChecked = enabled
        updateAiLabels(enabled)
    }

    private fun updateAiLabels(enabled: Boolean) {
        if (enabled) {
            // Mit KI aktiviert - starkes Hervorheben
            binding.aiLabelOff.apply {
                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
                typeface = android.graphics.Typeface.DEFAULT
                // Weniger prominent machen
                alpha = 0.5f
                scaleX = 0.9f
                scaleY = 0.9f
                background = null
                elevation = 0f
                // Padding zurücksetzen
                setPadding(0, 0, 0, 0)
            }
            binding.aiLabelOn.apply {
                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                // Stark hervorheben - KEINE Animation mehr
                alpha = 1f
                scaleX = 1.1f
                scaleY = 1.1f
                elevation = 8f
                // Schöner roter Hintergrund mit abgerundeten Ecken
                val shape = android.graphics.drawable.GradientDrawable()
                shape.setColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                shape.cornerRadius = 20f // Abgerundete Ecken
                background = shape
                // Mehr Padding für vollständige Rundungen
                setPadding(24, 12, 24, 12)
            }
        } else {
            // Ohne KI (Standard) - starkes Hervorheben
            binding.aiLabelOff.apply {
                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                // Stark hervorheben - KEINE Animation mehr
                alpha = 1f
                scaleX = 1.1f
                scaleY = 1.1f
                elevation = 8f
                // Schöner blauer Hintergrund mit abgerundeten Ecken
                val shape = android.graphics.drawable.GradientDrawable()
                shape.setColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_dark))
                shape.cornerRadius = 20f // Abgerundete Ecken
                background = shape
                // Mehr Padding für vollständige Rundungen
                setPadding(24, 12, 24, 12)
            }
            binding.aiLabelOn.apply {
                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
                typeface = android.graphics.Typeface.DEFAULT
                // Weniger prominent machen
                alpha = 0.5f
                scaleX = 0.9f
                scaleY = 0.9f
                background = null
                elevation = 0f
                // Padding zurücksetzen
                setPadding(0, 0, 0, 0)
            }
        }
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
            Toast.makeText(this, "Please enter a valid search term!", Toast.LENGTH_SHORT).show()
            return
        }

        // Zur Historie hinzufügen
        val searchType = getSelectedSearchType()
        val useAI = isAiEnabled // Verwendet jetzt die neue Variable
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

        // Browser-Einstellung prüfen
        val browserChoice = sharedPreferences.getString("browser_choice", "webview")

        if (browserChoice == "external") {
            // Externen Browser öffnen
            openInExternalBrowser(searchUrl)
        } else {
            // WebView verwenden
            showWebView(searchUrl)
        }
    }

    private fun openInExternalBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Error while opening the Browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showWebView(url: String) {
        // Tastatur explizit schließen, bevor WebView angezeigt wird
        closeKeyboard()

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

        // Duplikate entfernen - jetzt auch useAI berücksichtigen
        searchHistory.removeAll { it.query == query && it.searchType == searchType && it.useAI == useAI }

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
            .setTitle("Delete Search History")
            .setMessage("Do you really want to delete your search history?")
            .setPositiveButton("Delete") { _, _ ->
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
            .setNegativeButton("Cancel", null)
            .show()
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
            } catch (_: Exception) {
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
            // Sanfte Animation beim Verstecken
            binding.historyCard.animate()
                .alpha(0f)
                .scaleY(0.8f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    binding.historyCard.visibility = View.GONE
                }
                .start()
        }

        // RecyclerView Layout aktualisieren
        binding.historyRecyclerView.post {
            binding.historyRecyclerView.requestLayout()
            // Gesamte NestedScrollView aktualisieren
            binding.nestedScrollView.requestLayout()
        }
    }

    private fun handleWidgetLaunch() {
        // Prüfen ob App vom Widget gestartet wurde
        val isFromWidget = intent.getBooleanExtra("from_widget", false)

        if (isFromWidget) {
            // AI standardmäßig deaktivieren für Widget-Launches
            updateAiToggleState(false)

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
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager

        // Mehrere Versuche für zuverlässige Tastatur-Anzeige
        binding.searchEditText.post {
            // Erster Versuch: Standard-Methode
            imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)

            // Zweiter Versuch nach kurzer Verzögerung
            binding.searchEditText.postDelayed({
                if (!imm.isAcceptingText) {
                    imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }, 200)

            // Dritter Versuch als Fallback
            binding.searchEditText.postDelayed({
                if (!imm.isAcceptingText) {
                    // Alternative Methode - Tastatur umschalten
                    imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }, 500)

            // Vierter Versuch - sehr aggressiv
            binding.searchEditText.postDelayed({
                if (!imm.isAcceptingText) {
                    // Fenster-Flags setzen für Tastatur-Anzeige
                    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                    imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }, 800)
        }
    }

    // Funktionen für Swipe-to-Delete Feature
    private fun showDeleteConfirmationDialog(item: SearchHistoryItem, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Entry")
            .setMessage("Do you want to delete \"${item.query}\" from history?")
            .setPositiveButton("Delete") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Adapter zurücksetzen um das geschwipte Element zurückzusetzen
                historyAdapter.notifyItemChanged(historyAdapter.itemCount)
            }
            .setOnCancelListener {
                // Adapter zurücksetzen um das geschwipte Element zurückzusetzen
                historyAdapter.notifyItemChanged(historyAdapter.itemCount)
            }
            .show()
    }

    private fun removeFromHistory(item: SearchHistoryItem) {
        val position = searchHistory.indexOf(item)
        if (position >= 0) {
            // Aus der Hauptliste entfernen
            searchHistory.removeAt(position)
            saveSearchHistory()

            // Adapter aktualisieren - sowohl aus der internen Liste als auch UI
            historyAdapter.removeItem(position)
            updateHistoryVisibility()

            // Toast mit Undo-Option
            Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show()
        }
    }

    // Extension für dp zu px Konvertierung
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun closeKeyboard() {
        // Sicherstellen, dass das Suchfeld nicht mehr fokussiert ist
        binding.searchEditText.clearFocus()

        // InputMethodManager holen
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager

        // Tastatur schließen
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }
}
