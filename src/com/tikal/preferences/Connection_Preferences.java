package com.tikal.preferences;


import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.tikal.softphone.R;


public class Connection_Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.connection_preferences);
	}
}
