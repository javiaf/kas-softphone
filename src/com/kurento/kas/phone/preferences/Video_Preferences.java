package com.kurento.kas.phone.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.kurento.kas.phone.softphone.R;

public class Video_Preferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.video_preferences);
	}

}
