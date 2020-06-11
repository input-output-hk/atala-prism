package io.iohk.cvp.views.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.iohk.cvp.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class TutorialStepOneFragment extends Fragment {

    public static TutorialStepOneFragment newInstance() {
        TutorialStepOneFragment tutorialStepOneFragment = new TutorialStepOneFragment();
        return tutorialStepOneFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tutorial_step_one, container, false);
    }
}
