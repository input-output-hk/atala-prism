package io.iohk.atala.prism.app.neo.ui.onboarding.welcometutorial;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.iohk.cvp.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class TutorialStepTwoFragment extends Fragment {

    public static TutorialStepTwoFragment newInstance() {
        return new TutorialStepTwoFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tutorial_step_two, container, false);
    }
}