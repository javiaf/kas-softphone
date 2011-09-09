package com.kurento.kas.phone.preferences;


import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.kurento.kas.phone.softphone.R;


public class Call_Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.calltype_preferences);
	}
}
