package com.github.andreyasadchy.xtra.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.ui.ChannelViewerList
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewerListViewModel @Inject constructor(
    private val graphQLRepository: GraphQLRepository,
) : ViewModel() {

    val integrity = MutableStateFlow<String?>(null)

    private val _viewerList = MutableStateFlow<ChannelViewerList?>(null)
    val viewerList: StateFlow<ChannelViewerList?> = _viewerList
    private var isLoading = false

    fun loadViewerList(gqlHeaders: Map<String, String>, channelLogin: String?) {
        if (_viewerList.value == null && !isLoading) {
            isLoading = true
            viewModelScope.launch {
                try {
                    _viewerList.value = graphQLRepository.loadChannelViewerList(gqlHeaders, channelLogin).data?.user?.channel?.chatters?.let { response ->
                        ChannelViewerList(
                            broadcasters = response.broadcasters?.mapNotNull { it.login } ?: emptyList(),
                            moderators = response.moderators?.mapNotNull { it.login } ?: emptyList(),
                            vips = response.vips?.mapNotNull { it.login } ?: emptyList(),
                            viewers = response.viewers?.mapNotNull { it.login } ?: emptyList(),
                            count = response.count
                        )
                    }
                } catch (e: Exception) {
                    if (e.message == "failed integrity check" && integrity.value == null) {
                        integrity.value = "refresh"
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }
}