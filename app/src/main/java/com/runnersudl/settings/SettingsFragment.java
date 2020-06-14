package com.runnersudl.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.runnersudl.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_pref);
    }
}
