package com.github.andreyasadchy.xtra.ui.follow.channels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.model.ui.SortChannel
import com.github.andreyasadchy.xtra.repository.BookmarksRepository
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.LocalFollowChannelRepository
import com.github.andreyasadchy.xtra.repository.OfflineRepository
import com.github.andreyasadchy.xtra.repository.SortChannelRepository
import com.github.andreyasadchy.xtra.repository.datasource.FollowedChannelsDataSource
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.tokenPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class FollowedChannelsViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val graphQLRepository: GraphQLRepository,
    private val helix: HelixApi,
    private val sortChannelRepository: SortChannelRepository,
    private val localFollowsChannel: LocalFollowChannelRepository,
    private val offlineRepository: OfflineRepository,
    private val bookmarksRepository: BookmarksRepository,
    private val okHttpClient: OkHttpClient,
) : ViewModel() {

    val filter = MutableStateFlow<Filter?>(null)
    val sortText = MutableStateFlow<CharSequence?>(null)

    val sort: String
        get() = filter.value?.sort ?: FollowedChannelsSortDialog.SORT_LAST_BROADCAST
    val order: String
        get() = filter.value?.order ?: FollowedChannelsSortDialog.ORDER_DESC

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow = filter.flatMapLatest { filter ->
        Pager(
            PagingConfig(pageSize = 15, prefetchDistance = 5, initialLoadSize = 15)
        ) {
            FollowedChannelsDataSource(
                localFollowsChannel = localFollowsChannel,
                offlineRepository = offlineRepository,
                bookmarksRepository = bookmarksRepository,
                okHttpClient = okHttpClient,
                coroutineScope = viewModelScope,
                filesDir = applicationContext.filesDir.path,
                userId = applicationContext.tokenPrefs().getString(C.USER_ID, null),
                helixHeaders = TwitchApiHelper.getHelixHeaders(applicationContext),
                helixApi = helix,
                gqlHeaders = TwitchApiHelper.getGQLHeaders(applicationContext, true),
                gqlApi = graphQLRepository,
                checkIntegrity = applicationContext.prefs().getBoolean(C.ENABLE_INTEGRITY, false) && applicationContext.prefs().getBoolean(C.USE_WEBVIEW_INTEGRITY, true),
                apiPref = applicationContext.prefs().getString(C.API_PREFS_FOLLOWED_CHANNELS, null)?.split(',') ?: TwitchApiHelper.followedChannelsApiDefaults,
                sort = when (sort) {
                    FollowedChannelsSortDialog.SORT_FOLLOWED_AT -> "created_at"
                    FollowedChannelsSortDialog.SORT_ALPHABETICALLY -> "login"
                    FollowedChannelsSortDialog.SORT_LAST_BROADCAST -> "last_broadcast"
                    else -> "last_broadcast"
                },
                order = when (order) {
                    FollowedChannelsSortDialog.ORDER_DESC -> "desc"
                    FollowedChannelsSortDialog.ORDER_ASC -> "asc"
                    else -> "desc"
                }
            )
        }.flow
    }.cachedIn(viewModelScope)

    suspend fun getSortChannel(id: String): SortChannel? {
        return sortChannelRepository.getById(id)
    }

    suspend fun saveSortChannel(item: SortChannel) {
        sortChannelRepository.save(item)
    }

    fun setFilter(sort: String?, order: String?) {
        filter.value = Filter(sort, order)
    }

    class Filter(
        val sort: String?,
        val order: String?,
    )
}
