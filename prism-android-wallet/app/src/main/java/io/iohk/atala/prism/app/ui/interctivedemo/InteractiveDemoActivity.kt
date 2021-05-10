package io.iohk.atala.prism.app.ui.interctivedemo

import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.iohk.atala.prism.app.neo.common.EventWrapperObserver
import io.iohk.cvp.R
import io.iohk.cvp.databinding.ActivityInteractiveDemoBinding

class InteractiveDemoActivity : FragmentActivity() {

    private lateinit var binding: ActivityInteractiveDemoBinding

    private val viewModel: InteractiveDemoViewModel by viewModels()

    private val stepsFragments = listOf(
        Fragment(R.layout.fragment_interactive_demo_step1),
        Fragment(R.layout.fragment_interactive_demo_step2),
        Fragment(R.layout.fragment_interactive_demo_step3),
    )

    private val fragmentAdapter = object : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = stepsFragments.size
        override fun createFragment(position: Int): Fragment = stepsFragments[position]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_interactive_demo)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        configureUI()
        setObservers()
    }

    private fun configureUI() {
        // Set the fragment adapter to the ViewPager2 instance
        binding.vpPager.adapter = fragmentAdapter
        // Link ViewPager2 with TabLayout (dots indicators)
        TabLayoutMediator(
            binding.tabDots,
            binding.vpPager,
            TabLayoutMediator.TabConfigurationStrategy { _, position ->
                binding.vpPager.setCurrentItem(position, true)
            }
        ).attach()
    }

    private fun setObservers() {
        viewModel.shouldFinish.observe(
            this,
            EventWrapperObserver {
                if (it) {
                    finish()
                }
            }
        )
    }

    override fun onBackPressed() {
        viewModel.previous()
    }
}
