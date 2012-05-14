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

public class Connection_Preferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	private static boolean preferenceConnectionChanged = false;
	private static String info;

	private static final int SIP_PORT_MIN_DEF = 6060;
	private static final int SIP_PORT_MAX_DEF = 10000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceChanged(false);
		addPreferencesFromResource(R.layout.connection_preferences);
		this.getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	// TODO Add throw exception when params is null
	public static void setConnectionPreferences(Context context,
			Map<String, String> params) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();

		editor.putString(Keys_Preferences.SIP_LOCAL_USERNAME,
				params.get(Keys_Preferences.SIP_LOCAL_USERNAME));
		editor.putString(Keys_Preferences.SIP_LOCAL_PASSWORD,
				params.get(Keys_Preferences.SIP_LOCAL_PASSWORD));
		editor.putString(Keys_Preferences.SIP_LOCAL_DOMAIN,
				params.get(Keys_Preferences.SIP_LOCAL_DOMAIN));
		editor.putString(Keys_Preferences.SIP_PROXY_IP,
				params.get(Keys_Preferences.SIP_PROXY_IP));
		editor.putString(Keys_Preferences.SIP_PROXY_PORT,
				params.get(Keys_Preferences.SIP_PROXY_PORT));
		editor.putString(Keys_Preferences.SIP_MIN_LOCAL_PORT,
				params.get(Keys_Preferences.SIP_MIN_LOCAL_PORT));
		editor.putString(Keys_Preferences.SIP_MAX_LOCAL_PORT,
				params.get(Keys_Preferences.SIP_MAX_LOCAL_PORT));

		editor.commit();

	}

	public static Map<String, String> getConnectionPreferences(Context context) {
		Map<String, String> params = new HashMap<String, String>();
		String username, password, domain, ip, port, min_port, max_port;

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		username = settings.getString(Keys_Preferences.SIP_LOCAL_USERNAME, "");
		password = settings.getString(Keys_Preferences.SIP_LOCAL_PASSWORD, "");
		domain = settings.getString(Keys_Preferences.SIP_LOCAL_DOMAIN, "");

		ip = settings.getString(Keys_Preferences.SIP_PROXY_IP, "");
		port = settings.getString(Keys_Preferences.SIP_PROXY_PORT, "");

		min_port = settings.getString(Keys_Preferences.SIP_MIN_LOCAL_PORT,
				String.valueOf(SIP_PORT_MIN_DEF));
		max_port = settings.getString(Keys_Preferences.SIP_MAX_LOCAL_PORT,
				String.valueOf(SIP_PORT_MAX_DEF));

		if (username.equals("") || domain.equals("") || ip.equals("")
				|| port.equals(""))
			return null;

		params.put(Keys_Preferences.SIP_LOCAL_USERNAME, username);
		params.put(Keys_Preferences.SIP_LOCAL_PASSWORD, password);
		params.put(Keys_Preferences.SIP_LOCAL_DOMAIN, domain);

		params.put(Keys_Preferences.SIP_PROXY_IP, ip);
		params.put(Keys_Preferences.SIP_PROXY_PORT, port);

		params.put(Keys_Preferences.SIP_MIN_LOCAL_PORT, min_port);
		params.put(Keys_Preferences.SIP_MAX_LOCAL_PORT, max_port);

		return params;
	}

	public static String getConnectionPreferencesInfo(Context context) {

		Map<String, String> params = getConnectionPreferences(context);

		if (params != null) {
			info = "Connection preferences\n" + "User: \n"
					+ params.get(Keys_Preferences.SIP_LOCAL_USERNAME) + "@"
					+ params.get(Keys_Preferences.SIP_LOCAL_DOMAIN)
					+ "\n\nProxy: \n"
					+ params.get(Keys_Preferences.SIP_PROXY_IP) + ":"
					+ params.get(Keys_Preferences.SIP_PROXY_PORT);
		} else {
			info = "Connection preferences are incorrect";
		}
		return info;
	}

	public static String getConnectionNetPreferenceInfo(Context context) {
		Map<String, Object> params = getConnectionNetPreferences(context);
		if (params != null) {
			info = "Connection Net Preference\n"
					+ "Keep Alive\n"
					+ (Boolean) params
							.get(Keys_Preferences.MEDIA_NET_KEEP_ALIVE)
					+ "\n\nKeep Delay\n"
					+ (Long) params.get(Keys_Preferences.MEDIA_NET_KEEP_DELAY)
					+ "\n\nTransport\n"
					+ (String) params.get(Keys_Preferences.MEDIA_NET_TRANSPORT);
		} else {
			info = "Connection net preferences are incorrect";
		}
		return info;
	}

	public static Map<String, Object> getConnectionNetPreferences(
			Context context) {
		Map<String, Object> params = new HashMap<String, Object>();

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		params.put(Keys_Preferences.MEDIA_NET_KEEP_ALIVE, settings.getBoolean(
				Keys_Preferences.MEDIA_NET_KEEP_ALIVE, false));
		params.put(Keys_Preferences.MEDIA_NET_KEEP_DELAY, Long
				.parseLong(settings.getString(
						Keys_Preferences.MEDIA_NET_KEEP_DELAY, "10000")));
		params.put(Keys_Preferences.MEDIA_NET_TRANSPORT,
				settings.getString(Keys_Preferences.MEDIA_NET_TRANSPORT, "UDP"));

		return params;
	}

	public static Map<String, String> getStunPreferences(Context context) {
		Map<String, String> params = new HashMap<String, String>();
		String stunHost, stunPort;

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		Boolean stunEnable = settings.getBoolean(Keys_Preferences.STUN_ENABLE,
				false);

		if (stunEnable) {
			stunHost = settings.getString(Keys_Preferences.STUN_LIST,
					"193.147.51.24");
			stunPort = "3478";
		} else {
			stunHost = "";
			stunPort = "0";
		}

		// String stunHostAux = settings.getString(Keys_Preferences.STUN_LIST,
		// "193.147.51.24");
		// stunPort = "0";
		// if (stunHostAux.equals("-")) {
		// stunHostAux = settings.getString(Keys_Preferences.STUN_HOST, "-");
		// if (!stunHostAux.equals("-")) {
		// stunHost = stunHostAux;
		// stunPort = settings.getString(Keys_Preferences.STUN_HOST_PORT,
		// "3478");
		// } else {
		// stunHost = "";
		// stunPort = "0";
		// }
		// } else {
		// stunHost = stunHostAux;
		// stunPort = "3478";
		// }
		// if (stunHost.equals("") || stunPort.equals("")) {
		// stunHost = "";
		// stunPort = "0";
		// }
		Log.d("Connection_Preferences", "StunHost = " + stunHost + ":"
				+ stunPort);
		params.put(Keys_Preferences.STUN_HOST, stunHost);
		params.put(Keys_Preferences.STUN_HOST_PORT, stunPort);

		return params;
	}

	private synchronized static void setPreferenceChanged(boolean hasChanged) {
		preferenceConnectionChanged = hasChanged;
	}

	public static synchronized boolean isPreferenceChanged() {
		return preferenceConnectionChanged;
	}

	public static void resetChanged() {
		setPreferenceChanged(false);
	}

	@Override
	protected void onDestroy() {
		this.getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d(this.getClass().getName(), "Connection Preferecnces Changed : "
				+ sharedPreferences + " Key " + key);
		if (key.equals(Keys_Preferences.SIP_LOCAL_DOMAIN)
				|| key.equals(Keys_Preferences.SIP_LOCAL_PASSWORD)
				|| key.equals(Keys_Preferences.SIP_LOCAL_USERNAME)
				|| key.equals(Keys_Preferences.SIP_MAX_LOCAL_PORT)
				|| key.equals(Keys_Preferences.SIP_MIN_LOCAL_PORT)
				|| key.equals(Keys_Preferences.SIP_PROXY_IP)
				|| key.equals(Keys_Preferences.SIP_PROXY_PORT)
				|| key.equals(Keys_Preferences.MEDIA_NET_KEEP_ALIVE)
				|| key.equals(Keys_Preferences.MEDIA_NET_KEEP_DELAY)
				|| key.equals(Keys_Preferences.MEDIA_NET_TRANSPORT)
				|| key.equals(Keys_Preferences.STUN_ENABLE)
				|| key.equals(Keys_Preferences.STUN_LIST))
			setPreferenceChanged(true);
		else
			setPreferenceChanged(false);
	}
}
