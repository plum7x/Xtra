package com.github.exact7.xtra.ui.main

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.exact7.xtra.R
import com.github.exact7.xtra.model.LoggedIn
import com.github.exact7.xtra.model.NotValidated
import com.github.exact7.xtra.model.User
import com.github.exact7.xtra.repository.AuthRepository
import com.github.exact7.xtra.repository.OfflineRepository
import com.github.exact7.xtra.repository.TwitchService
import com.github.exact7.xtra.ui.download.DownloadService
import com.github.exact7.xtra.ui.login.LoginActivity
import com.github.exact7.xtra.util.C
import com.github.exact7.xtra.util.DownloadUtils
import com.github.exact7.xtra.util.Event
import com.github.exact7.xtra.util.TwitchApiHelper
import com.github.exact7.xtra.util.prefs
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

class MainViewModel @Inject constructor(
        application: Application,
        private val repository: TwitchService,
        private val authRepository: AuthRepository,
        private val offlineRepository: OfflineRepository): ViewModel() {

    private val _user = MutableLiveData<User>()
    val user: LiveData<User>
        get() = _user

    private val playerMaximized = MutableLiveData<Boolean>()
    private val _isNetworkAvailable = MutableLiveData<Event<Boolean>>()
    val isNetworkAvailable: LiveData<Event<Boolean>>
        get() = _isNetworkAvailable

    val isPlayerMaximized: Boolean
        get() = playerMaximized.value == true

    var isPlayerOpened = false
        private set

    var wasInPictureInPicture = false
    var orientationBeforePictureInPicture = 0

    private val compositeDisposable = CompositeDisposable()

    init {
        GlobalScope.launch {
            offlineRepository.getRequestsAsync().await().forEach {
                if (DownloadService.activeRequests.add(it.offlineVideoId)) {
                    DownloadUtils.download(application, it, application.prefs().getString(C.DOWNLOAD_NETWORK_PREFERENCE, "3") == "2")
                }
            }
        }
    }

    fun setUser(user: User) {
        if (_user.value != user) {
            _user.value = user
        }
    }

    fun onMaximize() {
        playerMaximized.value = true
    }

    fun onMinimize() {
        if (playerMaximized.value != false)
            playerMaximized.value = false
    }

    fun onPlayerStarted() {
        isPlayerOpened = true
        playerMaximized.value = true
    }

    fun onPlayerClosed() {
        isPlayerOpened = false
        playerMaximized.value = false
    }

    fun setNetworkAvailable(available: Boolean) {
        if (_isNetworkAvailable.value?.peekContent() != available) {
            _isNetworkAvailable.value = Event(available)
        }
    }

    fun validate(activity: Activity) {
        val user = user.value
        if (TwitchApiHelper.validated) {
            if (user is LoggedIn) {
                repository.loadUserEmotes(user.token, user.id, compositeDisposable)
            }
            return
        }
        if (user is NotValidated) {
            TwitchApiHelper.validated = true
            authRepository.validate(user.token)
                    .subscribe({
                        setUser(LoggedIn(user))
                        repository.loadUserEmotes(user.token, user.id, compositeDisposable)
                    }, {
                        if (it is HttpException && it.code() == 401) {
                            with(activity) {
                                User.set(activity, null)
                                Toast.makeText(this, getString(R.string.token_expired), Toast.LENGTH_LONG).show()
                                if (!isPlayerMaximized) {
                                    startActivityForResult(Intent(this, LoginActivity::class.java).putExtra("expired", true), 2)
                                }
                            }
                        }
                    })
                    .addTo(compositeDisposable)
        }
    }

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }
}