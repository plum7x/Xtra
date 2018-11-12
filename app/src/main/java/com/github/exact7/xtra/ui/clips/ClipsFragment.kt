package com.github.exact7.xtra.ui.clips

import android.os.Bundle
import android.view.View
import com.github.exact7.xtra.R
import com.github.exact7.xtra.model.game.Game
import com.github.exact7.xtra.util.FragmentUtils
import kotlinx.android.synthetic.main.fragment_clips.*

class ClipsFragment : BaseClipsFragment() {

    private companion object {
        val sortOptions = listOf(R.string.trending, R.string.today, R.string.this_week, R.string.this_month, R.string.all_time)
        const val DEFAULT_INDEX = 2
    }

    private var channelName: String? = null
    private var game: Game? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        channelName = arguments?.getString("channel")
        game = arguments?.getParcelable("game")
        sortBar.setOnClickListener { FragmentUtils.showRadioButtonDialogFragment(requireActivity(), childFragmentManager, sortOptions, viewModel.selectedIndex) }
    }

    override fun initializeViewModel() {
        viewModel.period = Period.WEEK
        viewModel.selectedIndex = DEFAULT_INDEX
        viewModel.sortText.value = getString(sortOptions[DEFAULT_INDEX])
    }

    override fun onChange(index: Int, text: CharSequence, tag: Int?) {
        var period: Period? = null
        var trending = false
        when (tag) {
            R.string.trending -> trending = true
            R.string.today -> period = Period.DAY
            R.string.this_week -> period = Period.WEEK
            R.string.this_month -> period = Period.MONTH
            R.string.all_time -> period = Period.ALL
        }
        viewModel.period = period
        viewModel.trending = trending
        viewModel.sortText.postValue(text)
        viewModel.selectedIndex = index
//        viewModel.loadedInitial.value = null
        adapter.submitList(null)
//        loadData(true)
    }
}