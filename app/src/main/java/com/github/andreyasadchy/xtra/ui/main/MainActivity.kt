package com.github.andreyasadchy.xtra.ui.main

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.ActivityMainBinding
import com.github.andreyasadchy.xtra.model.Account
import com.github.andreyasadchy.xtra.model.NotLoggedIn
import com.github.andreyasadchy.xtra.model.offline.OfflineVideo
import com.github.andreyasadchy.xtra.model.ui.Clip
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.model.ui.Video
import com.github.andreyasadchy.xtra.ui.channel.ChannelPagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.common.FragmentHost
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.download.HasDownloadDialog
import com.github.andreyasadchy.xtra.ui.games.GameMediaFragmentDirections
import com.github.andreyasadchy.xtra.ui.games.GamePagerFragmentDirections
import com.github.andreyasadchy.xtra.ui.player.BasePlayerFragment
import com.github.andreyasadchy.xtra.ui.player.clip.ClipPlayerFragment
import com.github.andreyasadchy.xtra.ui.player.offline.OfflinePlayerFragment
import com.github.andreyasadchy.xtra.ui.player.stream.StreamPlayerFragment
import com.github.andreyasadchy.xtra.ui.player.video.VideoPlayerFragment
import com.github.andreyasadchy.xtra.ui.view.SlidingLayout
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.DisplayUtils
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.applyTheme
import com.github.andreyasadchy.xtra.util.isInPortraitOrientation
import com.github.andreyasadchy.xtra.util.isLightTheme
import com.github.andreyasadchy.xtra.util.isNetworkAvailable
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.shortToast
import com.github.andreyasadchy.xtra.util.toast
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SlidingLayout.Listener {

    companion object {
        const val KEY_CODE = "code"
        const val KEY_VIDEO = "video"

        const val INTENT_OPEN_DOWNLOADS_TAB = 0
        const val INTENT_OPEN_DOWNLOADED_VIDEO = 1
        const val INTENT_OPEN_PLAYER = 2
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController
    var playerFragment: BasePlayerFragment? = null
        private set
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.setNetworkAvailable(isNetworkAvailable)
        }
    }
    private lateinit var prefs: SharedPreferences
    var settingsResultLauncher: ActivityResultLauncher<Intent>? = null
    var loginResultLauncher: ActivityResultLauncher<Intent>? = null
    var logoutResultLauncher: ActivityResultLauncher<Intent>? = null
    var orientation: Int = 1

    //Lifecycle methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orientation = resources.configuration.orientation
        prefs = prefs()
        if (prefs.getBoolean(C.FIRST_LAUNCH2, true)) {
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.root_preferences, false)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.player_button_preferences, true)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.player_menu_preferences, true)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.buffer_preferences, true)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.proxy_preferences, true)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.token_preferences, true)
            PreferenceManager.setDefaultValues(this@MainActivity, R.xml.api_token_preferences, true)
            prefs.edit {
                putBoolean(C.FIRST_LAUNCH2, false)
                putInt(C.LANDSCAPE_CHAT_WIDTH, DisplayUtils.calculateLandscapeWidthByPercent(this@MainActivity, 30))
                if (resources.getBoolean(R.bool.isTablet)) {
                    putString(C.PORTRAIT_COLUMN_COUNT, "2")
                    putString(C.LANDSCAPE_COLUMN_COUNT, "3")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    putString(C.THEME, "4")
                }
            }
        }
        if (prefs.getBoolean(C.FIRST_LAUNCH, true)) {
            prefs.edit {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    putString(C.CHAT_IMAGE_LIBRARY, "2")
                }
                putBoolean(C.FIRST_LAUNCH, false)
            }
        }
        if (prefs.getBoolean(C.FIRST_LAUNCH1, true)) {
            prefs.edit {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    putString(C.PLAYER_BACKGROUND_PLAYBACK, "1")
                } else {
                    putString(C.PLAYER_BACKGROUND_PLAYBACK, "0")
                }
                putBoolean(C.FIRST_LAUNCH1, false)
            }
        }
        if (prefs.getBoolean(C.FIRST_LAUNCH3, true)) {
            prefs.edit {
                val langPref = prefs.getString(C.UI_LANGUAGE, "")
                if (!langPref.isNullOrBlank() && langPref != "auto") {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langPref))
                }
                putBoolean(C.FIRST_LAUNCH3, false)
            }
        }
        if (prefs.getBoolean(C.FIRST_LAUNCH5, true)) {
            prefs.edit {
                if (prefs().getString(C.GQL_CLIENT_ID2, "kd1unb4b3q4t58fwlpcbzcbnm76a8fp") == "kd1unb4b3q4t58fwlpcbzcbnm76a8fp" && prefs().getString(C.GQL_TOKEN2, null).isNullOrBlank()) {
                    putString(C.GQL_CLIENT_ID2, "ue6666qo983tsx6so1t0vnawi233wa")
                    putString(C.GQL_REDIRECT2, "https://www.twitch.tv/settings/connections")
                }
                putBoolean(C.FIRST_LAUNCH5, false)
            }
        }
        if (prefs.getBoolean(C.FIRST_LAUNCH6, true)) {
            prefs.edit {
                if (prefs.getString(C.PLAYER_PROXY, "1")?.toIntOrNull() == 0) {
                    putBoolean(C.PLAYER_STREAM_PROXY, true)
                }
                putBoolean(C.FIRST_LAUNCH6, false)
            }
        }
        if (prefs.getBoolean(C.FIRST_LAUNCH7, true)) {
            prefs.edit {
                when {
                    MediaCodecSelector.DEFAULT.getDecoderInfos(MimeTypes.VIDEO_H265, false, false).none { it.hardwareAccelerated } -> {
                        putString(C.TOKEN_SUPPORTED_CODECS, "h264")
                    }
                    MediaCodecSelector.DEFAULT.getDecoderInfos(MimeTypes.VIDEO_AV1, false, false).none { it.hardwareAccelerated } -> {
                        putString(C.TOKEN_SUPPORTED_CODECS, "h265,h264")
                    }
                }
                putBoolean(C.FIRST_LAUNCH7, false)
            }
        }
        if (prefs.getBoolean(C.FIRST_LAUNCH8, true)) {
            prefs.edit {
                if (prefs.getString(C.UI_CUTOUTMODE, "0") == "1") {
                    putBoolean(C.UI_DRAW_BEHIND_CUTOUTS, true)
                }
                putBoolean(C.FIRST_LAUNCH8, false)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.integrity.collectLatest {
                    if (it != null && it != "done" && prefs.getBoolean(C.ENABLE_INTEGRITY, false) && prefs.getBoolean(C.USE_WEBVIEW_INTEGRITY, true)) {
                        IntegrityDialog.show(supportFragmentManager, it)
                        viewModel.integrity.value = "done"
                    }
                }
            }
        }
        applyTheme()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setNavBarColor(isInPortraitOrientation)
        val ignoreCutouts = prefs.getBoolean(C.UI_DRAW_BEHIND_CUTOUTS, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = if (ignoreCutouts) {
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            } else {
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.displayCutout())
            }
            binding.navHostFragment.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
            }
            binding.navBarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
            }
            windowInsets
        }
        settingsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                recreate()
            }
        }
        loginResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                restartActivity()
            }
        }
        logoutResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            restartActivity()
        }

        val notInitialized = savedInstanceState == null
        initNavigation()
        var flag = notInitialized && !isNetworkAvailable
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.newNetworkStatus.collectLatest { online ->
                    if (online != null) {
                        if (online) {
                            if (prefs.getBoolean(C.VALIDATE_TOKENS, true)) {
                                viewModel.validate(prefs.getString(C.HELIX_CLIENT_ID, "ilfexgv3nnljz3isbm257gzwrzr7bi"), TwitchApiHelper.getGQLHeaders(this@MainActivity, true), this@MainActivity)
                            }
                        }
                        if (flag) {
                            shortToast(if (online) R.string.connection_restored else R.string.no_connection)
                        } else {
                            flag = true
                        }
                        viewModel.newNetworkStatus.value = null
                    }
                }
            }
        }
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        restorePlayerFragment()
        handleIntent(intent)
    }

    private fun setNavBarColor(isPortrait: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> window.isNavigationBarContrastEnforced = !isPortrait
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    window.navigationBarColor = if (isPortrait) {
                        Color.TRANSPARENT
                    } else {
                        ContextCompat.getColor(this, if (!isLightTheme) R.color.darkScrim else R.color.lightScrim)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    if (!isLightTheme) {
                        window.navigationBarColor = if (isPortrait) Color.TRANSPARENT else ContextCompat.getColor(this, R.color.darkScrim)
                    }
                }
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                    @Suppress("DEPRECATION")
                    if (!isLightTheme) {
                        if (isPortrait) {
                            window.navigationBarColor = Color.TRANSPARENT
                            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                        } else {
                            window.navigationBarColor = ContextCompat.getColor(this, R.color.darkScrim)
                            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setNavBarColor(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
        orientation = newConfig.orientation
    }

    override fun onResume() {
        super.onResume()
        restorePlayerFragment()
    }

    override fun onDestroy() {
        unregisterReceiver(networkReceiver)
        if (isFinishing) {
            playerFragment?.onClose()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun restartActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) })
        overridePendingTransition(0, 0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            0 -> {
                if (grantResults.isNotEmpty() && grantResults.indexOf(PackageManager.PERMISSION_DENIED) == -1) {
                    val fragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)?.childFragmentManager?.fragments?.getOrNull(0)
                    if (fragment is HasDownloadDialog) {
                        fragment.showDownloadDialog()
                    } else if (fragment is FragmentHost && fragment.currentFragment is HasDownloadDialog) {
                        (fragment.currentFragment as HasDownloadDialog).showDownloadDialog()
                    }
                } else {
                    toast(R.string.permission_denied)
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && Build.VERSION.SDK_INT < Build.VERSION_CODES.S && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            prefs.getString(C.PLAYER_BACKGROUND_PLAYBACK, "0") == "0" && playerFragment?.enterPictureInPicture() == true) {
            try {
                enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            } catch (e: IllegalStateException) {
                //device doesn't support PIP
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.data.toString()
            when {
                url.contains("twitch.tv/videos/") -> {
                    val id = url.substringAfter("twitch.tv/videos/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    val offset = url.substringAfter("?t=").takeIf { it.isNotBlank() }?.let { (TwitchApiHelper.getDuration(it)?.toDouble() ?: 0.0) * 1000.0 }
                    if (!id.isNullOrBlank()) {
                        viewModel.loadVideo(id, prefs.getString(C.HELIX_CLIENT_ID, "ilfexgv3nnljz3isbm257gzwrzr7bi"), Account.get(this).helixToken, TwitchApiHelper.getGQLHeaders(this), prefs.getBoolean(C.ENABLE_INTEGRITY, false) && prefs.getBoolean(C.USE_WEBVIEW_INTEGRITY, true))
                        lifecycleScope.launch {
                            repeatOnLifecycle(Lifecycle.State.STARTED) {
                                viewModel.video.collectLatest { video ->
                                    if (video != null) {
                                        if (!video.id.isNullOrBlank()) {
                                            startVideo(video, offset, offset != null)
                                        }
                                        viewModel.video.value = null
                                    }
                                }
                            }
                        }
                    }
                }
                url.contains("/clip/") -> {
                    val id = url.substringAfter("/clip/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    if (!id.isNullOrBlank()) {
                        viewModel.loadClip(id, prefs.getString(C.HELIX_CLIENT_ID, "ilfexgv3nnljz3isbm257gzwrzr7bi"), Account.get(this).helixToken, TwitchApiHelper.getGQLHeaders(this), prefs.getBoolean(C.ENABLE_INTEGRITY, false) && prefs.getBoolean(C.USE_WEBVIEW_INTEGRITY, true))
                        lifecycleScope.launch {
                            repeatOnLifecycle(Lifecycle.State.STARTED) {
                                viewModel.clip.collectLatest { clip ->
                                    if (clip != null) {
                                        if (!clip.id.isNullOrBlank()) {
                                            startClip(clip)
                                        }
                                        viewModel.clip.value = null
                                    }
                                }
                            }
                        }
                    }
                }
                url.contains("clips.twitch.tv/") -> {
                    val id = url.substringAfter("clips.twitch.tv/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    if (!id.isNullOrBlank()) {
                        viewModel.loadClip(id, prefs.getString(C.HELIX_CLIENT_ID, "ilfexgv3nnljz3isbm257gzwrzr7bi"), Account.get(this).helixToken, TwitchApiHelper.getGQLHeaders(this), prefs.getBoolean(C.ENABLE_INTEGRITY, false) && prefs.getBoolean(C.USE_WEBVIEW_INTEGRITY, true))
                        lifecycleScope.launch {
                            repeatOnLifecycle(Lifecycle.State.STARTED) {
                                viewModel.clip.collectLatest { clip ->
                                    if (clip != null) {
                                        if (!clip.id.isNullOrBlank()) {
                                            startClip(clip)
                                        }
                                        viewModel.clip.value = null
                                    }
                                }
                            }
                        }
                    }
                }
                url.contains("twitch.tv/directory/category/") -> {
                    val slug = url.substringAfter("twitch.tv/directory/category/").takeIf { it.isNotBlank() }?.substringBefore("/")
                    if (!slug.isNullOrBlank()) {
                        playerFragment?.minimize()
                        navController.navigate(
                            if (prefs.getBoolean(C.UI_GAMEPAGER, true)) {
                                GamePagerFragmentDirections.actionGlobalGamePagerFragment(
                                    gameSlug = slug
                                )
                            } else {
                                GameMediaFragmentDirections.actionGlobalGameMediaFragment(
                                    gameSlug = slug
                                )
                            }
                        )
                    }
                }
                url.contains("twitch.tv/directory/game/") -> {
                    val name = url.substringAfter("twitch.tv/directory/game/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    if (!name.isNullOrBlank()) {
                        playerFragment?.minimize()
                        navController.navigate(
                            if (prefs.getBoolean(C.UI_GAMEPAGER, true)) {
                                GamePagerFragmentDirections.actionGlobalGamePagerFragment(
                                    gameName = Uri.decode(name)
                                )
                            } else {
                                GameMediaFragmentDirections.actionGlobalGameMediaFragment(
                                    gameName = Uri.decode(name)
                                )
                            }
                        )
                    }
                }
                else -> {
                    val login = url.substringAfter("twitch.tv/").takeIf { it.isNotBlank() }?.let { it.substringBefore("?", it.substringBefore("/")) }
                    if (!login.isNullOrBlank()) {
                        viewModel.loadUser(login, prefs.getString(C.HELIX_CLIENT_ID, "ilfexgv3nnljz3isbm257gzwrzr7bi"), Account.get(this).helixToken, TwitchApiHelper.getGQLHeaders(this), prefs.getBoolean(C.ENABLE_INTEGRITY, false) && prefs.getBoolean(C.USE_WEBVIEW_INTEGRITY, true))
                        lifecycleScope.launch {
                            repeatOnLifecycle(Lifecycle.State.STARTED) {
                                viewModel.user.collectLatest { user ->
                                    if (user != null) {
                                        if (!user.channelId.isNullOrBlank() || !user.channelLogin.isNullOrBlank()) {
                                            playerFragment?.minimize()
                                            navController.navigate(ChannelPagerFragmentDirections.actionGlobalChannelPagerFragment(
                                                channelId = user.channelId,
                                                channelLogin = user.channelLogin,
                                                channelName = user.channelName,
                                                channelLogo = user.channelLogo,
                                            ))
                                        }
                                        viewModel.user.value = null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            when (intent?.getIntExtra(KEY_CODE, -1)) {
                INTENT_OPEN_DOWNLOADS_TAB -> binding.navBar.selectedItemId = if (prefs.getBoolean(C.UI_SAVEDPAGER, true)) R.id.savedPagerFragment else R.id.savedMediaFragment
                INTENT_OPEN_DOWNLOADED_VIDEO -> startOfflineVideo(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(KEY_VIDEO, OfflineVideo::class.java)!!
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(KEY_VIDEO)!!
                    }
                )
                INTENT_OPEN_PLAYER -> playerFragment?.maximize() //TODO if was closed need to reopen
            }
        }
    }

//Navigation listeners

    fun startStream(stream: Stream) {
//        playerFragment?.play(stream)
        startPlayer(StreamPlayerFragment.newInstance(stream))
    }

    fun startVideo(video: Video, offset: Double?, ignoreSavedPosition: Boolean = false) {
        startPlayer(VideoPlayerFragment.newInstance(video, offset, ignoreSavedPosition))
    }

    fun startClip(clip: Clip) {
        startPlayer(ClipPlayerFragment.newInstance(clip))
    }

    fun startOfflineVideo(video: OfflineVideo) {
        startPlayer(OfflinePlayerFragment.newInstance(video))
    }

//SlidingLayout.Listener

    override fun onMaximize() {
        viewModel.onMaximize()
    }

    override fun onMinimize() {
        viewModel.onMinimize()
    }

    override fun onClose() {
        closePlayer()
    }

//Player methods

    private fun startPlayer(fragment: BasePlayerFragment) {
//        if (playerFragment == null) {
        playerFragment = fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.playerContainer, fragment).commit()
        viewModel.onPlayerStarted()
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && prefs.getString(C.PLAYER_BACKGROUND_PLAYBACK, "0") == "0") {
            setPictureInPictureParams(PictureInPictureParams.Builder().setAutoEnterEnabled(true).build())
        }
    }

    fun closePlayer() {
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .remove(supportFragmentManager.findFragmentById(R.id.playerContainer)!!)
                .commit()
        playerFragment = null
        viewModel.onPlayerClosed()
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setPictureInPictureParams(PictureInPictureParams.Builder().setAutoEnterEnabled(false).build())
        }
    }

    private fun restorePlayerFragment() {
        if (viewModel.isPlayerOpened) {
            if (playerFragment == null) {
                playerFragment = supportFragmentManager.findFragmentById(R.id.playerContainer) as BasePlayerFragment?
            } else {
                if (playerFragment?.secondViewIsHidden() == true && prefs.getString(C.PLAYER_BACKGROUND_PLAYBACK, "0") == "0") {
                    playerFragment?.maximize()
                }
            }
        }
    }

    fun popFragment() {
        navController.navigateUp()
    }

    private fun initNavigation() {
        navController = (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).navController
        navController.setGraph(navController.navInflater.inflate(R.navigation.nav_graph).also {
            if ((Account.get(this) !is NotLoggedIn && ((prefs.getString(C.UI_STARTONFOLLOWED, "1")?.toIntOrNull() ?: 1) < 2)) || (Account.get(this) is NotLoggedIn && ((prefs.getString(C.UI_STARTONFOLLOWED, "1")?.toIntOrNull() ?: 1) == 0))) {
                if (prefs.getBoolean(C.UI_FOLLOWPAGER, true)) {
                    it.setStartDestination(R.id.followPagerFragment)
                } else {
                    it.setStartDestination(R.id.followMediaFragment)
                }
            }
        }, null)
        binding.navBar.apply {
            if (!prefs.getBoolean(C.UI_THEME_BOTTOM_NAV_COLOR, true) && prefs.getBoolean(C.UI_THEME_MATERIAL3, true)) {
                setBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
            }
            menu.add(Menu.NONE, R.id.rootGamesFragment, Menu.NONE, R.string.games).setIcon(R.drawable.ic_games_black_24dp)
            menu.add(Menu.NONE, R.id.rootTopFragment, Menu.NONE, R.string.popular).setIcon(R.drawable.ic_trending_up_black_24dp)
            if (prefs.getBoolean(C.UI_FOLLOWPAGER, true)) {
                menu.add(Menu.NONE, R.id.followPagerFragment, Menu.NONE, R.string.following).setIcon(R.drawable.ic_favorite_black_24dp)
            } else {
                menu.add(Menu.NONE, R.id.followMediaFragment, Menu.NONE, R.string.following).setIcon(R.drawable.ic_favorite_black_24dp)
            }
            if (prefs.getBoolean(C.UI_SAVEDPAGER, true)) {
                menu.add(Menu.NONE, R.id.savedPagerFragment, Menu.NONE, R.string.saved).setIcon(R.drawable.ic_file_download_black_24dp)
            } else {
                menu.add(Menu.NONE, R.id.savedMediaFragment, Menu.NONE, R.string.saved).setIcon(R.drawable.ic_file_download_black_24dp)
            }
            setupWithNavController(navController)
            setOnItemSelectedListener {
                NavigationUI.onNavDestinationSelected(it, navController)
                return@setOnItemSelectedListener true
            }
            setOnItemReselectedListener {
                if (!navController.popBackStack(it.itemId, false)) {
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)?.childFragmentManager?.fragments?.getOrNull(0)
                    if (currentFragment is Scrollable) {
                        currentFragment.scrollToTop()
                    }
                }
            }
        }
    }
}