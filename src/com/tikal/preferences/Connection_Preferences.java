package com.tikal.preferences;


import com.tikal.softphone.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class Connection_Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.connection_preferences);
	}
}
