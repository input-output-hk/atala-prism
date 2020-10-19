package io.iohk.atala.prism.app.neo.ui.onboarding.welcometutorial;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.Arrays;
import java.util.List;

public class TutorialScrollableAdapter extends FragmentStateAdapter {

    private List<Fragment> tutorialFragments = Arrays.asList(TutorialStepOneFragment.newInstance(),
            TutorialStepTwoFragment.newInstance(), TutorialStepThreeFragment.newInstance());

    public TutorialScrollableAdapter(FragmentActivity fragmentActivity) {
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