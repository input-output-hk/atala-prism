package io.iohk.cvp.views.utils.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.iohk.cvp.views.fragments.TutorialStepOneFragment;
import io.iohk.cvp.views.fragments.TutorialStepThreeFragment;
import io.iohk.cvp.views.fragments.TutorialStepTwoFragment;

public class TutorialScrolleableAdapter extends FragmentStateAdapter {

    private List<Fragment> tutorialFragments = Arrays.asList(TutorialStepOneFragment.newInstance(),
            TutorialStepTwoFragment.newInstance(),TutorialStepThreeFragment.newInstance());

    public TutorialScrolleableAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
       return tutorialFragments.get(position);
    }

    @Override
    public int getItemCount() {
        return tutorialFragments.size();
    }
}
