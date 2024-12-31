package com.github.andreyasadchy.xtra.ui.clips.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.CommonRecyclerViewLayoutBinding
import com.github.andreyasadchy.xtra.databinding.SortBarBinding
import com.github.andreyasadchy.xtra.model.ui.Clip
import com.github.andreyasadchy.xtra.model.ui.SortChannel
import com.github.andreyasadchy.xtra.model.ui.SortGame
import com.github.andreyasadchy.xtra.ui.clips.BaseClipsFragment
import com.github.andreyasadchy.xtra.ui.clips.ClipsAdapter
import com.github.andreyasadchy.xtra.ui.common.FragmentHost
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.common.Sortable
import com.github.andreyasadchy.xtra.ui.games.GamePagerFragmentArgs
import com.github.andreyasadchy.xtra.ui.main.IntegrityDialog
import com.github.andreyasadchy.xtra.ui.videos.VideosSortDialog
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.prefs
import com.github.andreyasadchy.xtra.util.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ClipsFragment : BaseClipsFragment(), Scrollable, Sortable, VideosSortDialog.OnFilter {

    private var _binding: CommonRecyclerViewLayoutBinding? = null
    private val binding get() = _binding!!
    private val args: GamePagerFragmentArgs by navArgs()
    private val viewModel: ClipsViewModel by viewModels()
    private lateinit var pagingAdapter: PagingDataAdapter<Clip, out RecyclerView.ViewHolder>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CommonRecyclerViewLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val showDialog: (Clip) -> Unit = {
            lastSelectedItem = it
            showDownloadDialog()
        }
        pagingAdapter = if (args.channelId != null || args.channelLogin != null) {
            ChannelClipsAdapter(this, showDialog)
        } else {
            ClipsAdapter(this, showDialog, args.gameId != null || args.gameSlug != null || args.gameName != null)
        }
        setAdapter(binding.recyclerView, pagingAdapter)
    }

    override fun initialize() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.filter.value == null) {
                if (args.channelId != null || args.channelLogin != null) {
                    val sortValues = args.channelId?.let {
                        viewModel.getSortChannel(it)?.takeIf { it.saveSort == true }
                    } ?: viewModel.getSortChannel("default")
                    viewModel.setFilter(
                        period = sortValues?.clipPeriod,
                        languageIndex = 0,
                        saveSort = sortValues?.saveSort,
                    )
                } else {
                    val sortValues = args.gameId?.let {
                        viewModel.getSortGame(it)?.takeIf { it.saveSort == true }
                    } ?: viewModel.getSortGame("default")
                    viewModel.setFilter(
                        period = sortValues?.clipPeriod,
                        languageIndex = sortValues?.clipLanguageIndex,
                        saveSort = sortValues?.saveSort,
                    )
                }
                viewModel.sortText.value = requireContext().getString(
                    R.string.sort_and_period,
                    requireContext().getString(R.string.view_count),
                    requireContext().getString(
                        when (viewModel.period) {
                            VideosSortDialog.PERIOD_DAY -> R.string.today
                            VideosSortDialog.PERIOD_WEEK -> R.string.this_week
                            VideosSortDialog.PERIOD_MONTH -> R.string.this_month
                            VideosSortDialog.PERIOD_ALL -> R.string.all_time
                            else -> R.string.this_week
                        }
                    )
                )
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.flow.collectLatest { pagingData ->
                    pagingAdapter.submitData(pagingData)
                }
            }
        }
        initializeAdapter(binding, pagingAdapter)
    }

    override fun setupSortBar(sortBar: SortBarBinding) {
        sortBar.root.visible()
        sortBar.root.setOnClickListener {
            VideosSortDialog.newInstance(
                period = viewModel.period,
                languageIndex = viewModel.languageIndex,
                clipChannel = args.channelId != null,
                saveSort = viewModel.saveSort,
                saveDefault = if (pagingAdapter is ClipsAdapter) requireContext().prefs().getBoolean(C.SORT_DEFAULT_GAME_CLIPS, false) else requireContext().prefs().getBoolean(C.SORT_DEFAULT_CHANNEL_CLIPS, false)
            ).show(childFragmentManager, null)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sortText.collectLatest {
                    sortBar.sortText.text = it
                }
            }
        }
    }

    override fun onChange(sort: String, sortText: CharSequence, period: String, periodText: CharSequence, type: String, languageIndex: Int, saveSort: Boolean, saveDefault: Boolean) {
        if ((parentFragment as? FragmentHost)?.currentFragment == this) {
            viewLifecycleOwner.lifecycleScope.launch {
                binding.scrollTop.gone()
                pagingAdapter.submitData(PagingData.empty())
                viewModel.setFilter(period, languageIndex, saveSort)
                viewModel.sortText.value = requireContext().getString(R.string.sort_and_period, sortText, periodText)
                if (!args.gameId.isNullOrBlank() || !args.gameName.isNullOrBlank()) {
                    val sortValues = args.gameId?.let { viewModel.getSortGame(it) }
                    if (saveSort) {
                        if (sortValues != null) {
                            sortValues.apply {
                                this.saveSort = true
                                clipPeriod = period
                                clipLanguageIndex = languageIndex
                            }
                        } else {
                            args.gameId?.let {
                                SortGame(
                                    id = it,
                                    saveSort = true,
                                    clipPeriod = period,
                                    clipLanguageIndex = languageIndex
                                )
                            }
                        }
                    } else {
                        sortValues?.apply {
                            this.saveSort = false
                        }
                    }?.let { viewModel.saveSortGame(it) }
                    if (saveDefault) {
                        if (sortValues != null) {
                            sortValues.apply {
                                this.saveSort = saveSort
                            }
                        } else {
                            args.gameId?.let {
                                SortGame(
                                    id = it,
                                    saveSort = saveSort
                                )
                            }
                        }?.let { viewModel.saveSortGame(it) }
                        val sortDefaults = viewModel.getSortGame("default")
                        if (sortDefaults != null) {
                            sortDefaults.apply {
                                clipPeriod = period
                                clipLanguageIndex = languageIndex
                            }
                        } else {
                            SortGame(
                                id = "default",
                                clipPeriod = period,
                                clipLanguageIndex = languageIndex
                            )
                        }.let { viewModel.saveSortGame(it) }
                    }
                    if (saveDefault != requireContext().prefs().getBoolean(C.SORT_DEFAULT_GAME_CLIPS, false)) {
                        requireContext().prefs().edit { putBoolean(C.SORT_DEFAULT_GAME_CLIPS, saveDefault) }
                    }
                } else {
                    if (!args.channelId.isNullOrBlank() || !args.channelLogin.isNullOrBlank()) {
                        val sortValues = args.channelId?.let { viewModel.getSortChannel(it) }
                        if (saveSort) {
                            if (sortValues != null) {
                                sortValues.apply {
                                    this.saveSort = true
                                    clipPeriod = period
                                }
                            } else {
                                args.channelId?.let {
                                    SortChannel(
                                        id = it,
                                        saveSort = true,
                                        clipPeriod = period
                                    )
                                }
                            }
                        } else {
                            sortValues?.apply {
                                this.saveSort = false
                            }
                        }?.let { viewModel.saveSortChannel(it) }
                        if (saveDefault) {
                            if (sortValues != null) {
                                sortValues.apply {
                                    this.saveSort = saveSort
                                }
                            } else {
                                args.channelId?.let {
                                    SortChannel(
                                        id = it,
                                        saveSort = saveSort
                                    )
                                }
                            }?.let { viewModel.saveSortChannel(it) }
                            val sortDefaults = viewModel.getSortChannel("default")
                            if (sortDefaults != null) {
                                sortDefaults.apply {
                                    clipPeriod = period
                                }
                            } else {
                                SortChannel(
                                    id = "default",
                                    clipPeriod = period
                                )
                            }.let { viewModel.saveSortChannel(it) }
                        }
                        if (saveDefault != requireContext().prefs().getBoolean(C.SORT_DEFAULT_CHANNEL_CLIPS, false)) {
                            requireContext().prefs().edit { putBoolean(C.SORT_DEFAULT_CHANNEL_CLIPS, saveDefault) }
                        }
                    }
                }
            }
        }
    }

    override fun scrollToTop() {
        binding.recyclerView.scrollToPosition(0)
    }

    override fun onNetworkRestored() {
        pagingAdapter.retry()
    }

    override fun onIntegrityDialogCallback(callback: String?) {
        (parentFragment as? IntegrityDialog.CallbackListener)?.onIntegrityDialogCallback("refresh")
        if (callback == "refresh") {
            pagingAdapter.refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
