/*
Softphone application for Android. It can make video calls using SIP with different video formats and audio formats.
Copyright (C) 2011 Tikal Technologies

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 3
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kurento.kas.phone.preferences;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kurento.kas.phone.softphone.R;

public class Stun_Preferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	private static boolean preferenceStunChanged = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceChanged(false);
		addPreferencesFromResource(R.layout.stun_preferences);
		this.getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	public static Map<String, String> getStunPreferences(Context context) {
		Map<String, String> params = new HashMap<String, String>();
		String stunHost, stunPort;

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		String stunHostAux = settings.getString(Keys_Preferences.STUN_LIST,
				"stun.xten.com");
		if (stunHostAux.equals("-")) {
			stunHostAux = settings.getString(Keys_Preferences.STUN_HOST, "-");
			if (!stunHostAux.equals("-")) {
				stunHost = stunHostAux;
				stunPort = settings.getString(Keys_Preferences.STUN_HOST_PORT,
						"3478");
			} else {
				stunHost = "";
				stunPort = "0";
			}
		} else {
			stunHost = stunHostAux;
			stunPort = "3478";
		}

		params.put(Keys_Preferences.STUN_HOST, stunHost);
		params.put(Keys_Preferences.STUN_HOST_PORT, stunPort);

		return params;
	}

	@Override
	protected void onDestroy() {
		this.getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	private synchronized static void setPreferenceChanged(boolean hasChanged) {
		preferenceStunChanged = hasChanged;
	}

	public static synchronized boolean isPreferenceChanged() {
		return preferenceStunChanged;
	}

	public static void resetChanged() {
		setPreferenceChanged(false);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d(this.getClass().getName(), "Stun Preferecnces Changed: "
				+ sharedPreferences);
		setPreferenceChanged(true);
	}
}
