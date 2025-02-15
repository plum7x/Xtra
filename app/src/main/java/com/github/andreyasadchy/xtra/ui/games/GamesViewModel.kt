package com.github.andreyasadchy.xtra.ui.games

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.apollographql.apollo.ApolloClient
import com.github.andreyasadchy.xtra.api.HelixApi
import com.github.andreyasadchy.xtra.repository.GraphQLRepository
import com.github.andreyasadchy.xtra.repository.datasource.GamesDataSource
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import com.github.andreyasadchy.xtra.util.prefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class GamesViewModel @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val graphQLRepository: GraphQLRepository,
    private val helix: HelixApi,
    private val apolloClient: ApolloClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = GamesFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val flow = Pager(
        PagingConfig(pageSize = 30, prefetchDistance = 10, initialLoadSize = 30)
    ) {
        GamesDataSource(
            helixHeaders = TwitchApiHelper.getHelixHeaders(applicationContext),
            helixApi = helix,
            gqlHeaders = TwitchApiHelper.getGQLHeaders(applicationContext),
            tags = args.tags?.toList(),
            gqlApi = graphQLRepository,
            apolloClient = apolloClient,
            checkIntegrity = applicationContext.prefs().getBoolean(C.ENABLE_INTEGRITY, false) && applicationContext.prefs().getBoolean(C.USE_WEBVIEW_INTEGRITY, true),
            apiPref = applicationContext.prefs().getString(C.API_PREFS_GAMES, null)?.split(',') ?: TwitchApiHelper.gamesApiDefaults
        )
    }.flow.cachedIn(viewModelScope)
}
