package io.iohk.cvp.views.utils.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.crashlytics.android.Crashlytics;

import io.iohk.cvp.core.exception.CaseNotFoundException;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.views.fragments.ConnectionsListFragment;

public class ConnectionTabsAdapter extends FragmentPagerAdapter {

    private final ConnectionsListFragment universitiesListFragment;
    private final int mNumOfTabs;

    public ConnectionTabsAdapter(FragmentManager fm, int NumOfTabs,
                                 ConnectionsListFragment universitiesListFragment) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.mNumOfTabs = NumOfTabs;
        this.universitiesListFragment = universitiesListFragment;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                return universitiesListFragment;
            default:
                Crashlytics.logException(
                        new CaseNotFoundException("Couldn't find fragment for tab " + position,
                                ErrorCode.TAB_NOT_FOUND));
                return universitiesListFragment;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}

