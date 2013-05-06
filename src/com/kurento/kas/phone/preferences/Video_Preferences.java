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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class Video_Preferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	private static boolean preferenceMediaChanged = false;
	private static String info;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceChanged(false);
		setPreferenceScreen(createPreferenceHierarchy());
		this.getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	private PreferenceScreen createPreferenceHierarchy() {
		// Root
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(
				this);

		// General Category
		PreferenceCategory generalCategory = new PreferenceCategory(this);
		generalCategory.setTitle("General Preferences");
		root.addPreference(generalCategory);

		// Max bandwidth
		EditTextPreference editTextMaxBw = new EditTextPreference(this);
		editTextMaxBw.setDialogTitle("MAX_BW");
		editTextMaxBw.setKey(Keys_Preferences.MEDIA_GENERAL_MAX_BW);
		editTextMaxBw.setTitle("Max bandwidth (kbps)");
		editTextMaxBw.setSummary("Select max bandwidth");

		generalCategory.addPreference(editTextMaxBw);

		// Max delay
		EditTextPreference editTextMaxDelay = new EditTextPreference(this);
		editTextMaxDelay.setDialogTitle("MAX_DELAY");
		editTextMaxDelay.setKey(Keys_Preferences.MEDIA_GENERAL_MAX_DELAY);
		editTextMaxDelay.setTitle("Max delay (ms)");
		editTextMaxDelay.setSummary("Select max delay");

		generalCategory.addPreference(editTextMaxDelay);

		// Max delay
		CheckBoxPreference syncMediaStreams = new CheckBoxPreference(this);
		syncMediaStreams.setDefaultValue(false);
		syncMediaStreams
				.setKey(Keys_Preferences.MEDIA_GENERAL_SYNC_MEDIA_STREAMS);
		syncMediaStreams.setTitle("Received media streams synchronization");
		syncMediaStreams.setSummary("Synchronize received media streams.");

		generalCategory.addPreference(syncMediaStreams);

		// Camera Facing
		CheckBoxPreference cameraFacing = new CheckBoxPreference(this);
		cameraFacing.setDefaultValue(false);
		cameraFacing.setKey(Keys_Preferences.MEDIA_GENERAL_FRONT_CAMERA);
		cameraFacing.setTitle("Front Camera");
		cameraFacing
				.setSummary("If it selected, it used the Front Camera. Else, it used the Back Camera.");

		generalCategory.addPreference(cameraFacing);

		// // ------//
		// // Network Category
		// PreferenceCategory networkCategory = new PreferenceCategory(this);
		// networkCategory.setTitle("Network Preferences");
		// root.addPreference(networkCategory);
		//
		// // KeepAliveEnable
		// CheckBoxPreference keepAliveEnable = new CheckBoxPreference(this);
		// keepAliveEnable.setDefaultValue(false);
		// keepAliveEnable.setKey(Keys_Preferences.MEDIA_NET_KEEP_ALIVE);
		// keepAliveEnable.setTitle("keep Alive ");
		// keepAliveEnable.setSummary("If it selected, it used the Keep Alive.");
		// networkCategory.addPreference(keepAliveEnable);
		//
		// // KeepAliveTime
		// EditTextPreference keepAliveDelay = new EditTextPreference(this);
		// keepAliveDelay.setDialogTitle("Keep Delay");
		// keepAliveDelay.setKey(Keys_Preferences.MEDIA_NET_KEEP_DELAY);
		// keepAliveDelay.setTitle("Keep delay (ms)");
		// keepAliveDelay.setSummary("Select keep delay");
		// networkCategory.addPreference(keepAliveDelay);
		//
		// // Transport
		// CharSequence[] entriesTV = { "TCP", "UDP" };
		// CharSequence[] entryValuesTV = { "TCP", "UDP" };
		// ListPreference listTransportV = new ListPreference(this);
		// listTransportV.setEntries(entriesTV);
		// listTransportV.setEntryValues(entryValuesTV);
		// listTransportV.setDefaultValue("UDP");
		// listTransportV.setDialogTitle("Transport");
		// listTransportV.setKey(Keys_Preferences.MEDIA_NET_TRANSPORT);
		// listTransportV.setTitle("Transport");
		// listTransportV.setSummary("Select transport");
		// networkCategory.addPreference(listTransportV);
		//
		// root.addPreference(networkCategory);

		// ------//
		// Video Category
		PreferenceCategory videoCategory = new PreferenceCategory(this);
		videoCategory.setTitle("Video Preferences");
		root.addPreference(videoCategory);

		// Video codecs
		PreferenceScreen videoCodecPref = getPreferenceManager()
				.createPreferenceScreen(this);
		videoCodecPref.setKey(Keys_Preferences.MEDIA_VIDEO_CODECS);
		videoCodecPref.setTitle("Video codecs");
		videoCodecPref.setSummary("Select video codecs");
		videoCategory.addPreference(videoCodecPref);

		// Codecs
		CheckBoxPreference nextVideoCodecPref = new CheckBoxPreference(this);
		nextVideoCodecPref.setKey(Keys_Preferences.MEDIA_VIDEO_H263_CODEC);
		nextVideoCodecPref.setTitle("H263");
		videoCodecPref.addPreference(nextVideoCodecPref);
		nextVideoCodecPref = new CheckBoxPreference(this);
		nextVideoCodecPref.setDefaultValue(true);
		nextVideoCodecPref.setKey(Keys_Preferences.MEDIA_VIDEO_MPEG4_CODEC);
		nextVideoCodecPref.setTitle("MPEG4");
		videoCodecPref.addPreference(nextVideoCodecPref);
		nextVideoCodecPref = new CheckBoxPreference(this);
		nextVideoCodecPref.setKey(Keys_Preferences.MEDIA_VIDEO_H264_CODEC);
		nextVideoCodecPref.setTitle("H264");
		videoCodecPref.addPreference(nextVideoCodecPref);

		// Video local port range
		PreferenceScreen videoLocalPortRangecPref = getPreferenceManager()
				.createPreferenceScreen(this);
		videoLocalPortRangecPref
				.setKey(Keys_Preferences.MEDIA_VIDEO_LOCAL_PORT_RANGE);
		videoLocalPortRangecPref.setTitle("Video local port range");
		videoLocalPortRangecPref.setSummary("Select video local port range");
		videoCategory.addPreference(videoLocalPortRangecPref);

		EditTextPreference editTextMinVideoPort = new EditTextPreference(this);
		editTextMinVideoPort.setDialogTitle("Min video local port range");
		editTextMinVideoPort
				.setKey(Keys_Preferences.MEDIA_MIN_VIDEO_LOCAL_PORT);
		editTextMinVideoPort.setTitle("Min video local port range");
		editTextMinVideoPort
				.setSummary("Select min video local port of the range.");
		videoLocalPortRangecPref.addPreference(editTextMinVideoPort);
		EditTextPreference editTextMaxVideoPort = new EditTextPreference(this);
		editTextMaxVideoPort.setDialogTitle("Max video local port range");
		editTextMaxVideoPort
				.setKey(Keys_Preferences.MEDIA_MAX_VIDEO_LOCAL_PORT);
		editTextMaxVideoPort.setTitle("Max video local port range");
		editTextMaxVideoPort
				.setSummary("Select max video local port of the range.");
		videoLocalPortRangecPref.addPreference(editTextMaxVideoPort);

		// Size Camera
		ArrayList<String> list = new ArrayList<String>();
		ListPreference listSizeCam = new ListPreference(this);

		boolean cif = false;
		boolean qcif = false;

		Camera mCamera = null;
		try {
			mCamera = Camera.open();
		} catch (Exception e) {
			Log.e(Video_Preferences.class.getName(),
					"Exception: " + e.getMessage(), e);
		}
		if (mCamera != null) {
			Camera.Parameters parameteres = mCamera.getParameters();
			List<Size> sizes = parameteres.getSupportedPreviewSizes();
			for (int i = 0; i < sizes.size(); i++) {
				list.add(sizes.get(i).width + "x" + sizes.get(i).height);
				if (cif == false)
					if (sizes.get(i).width == 352 && sizes.get(i).height == 288)
						cif = true;
				if (qcif == false)
					if (sizes.get(i).width == 176 && sizes.get(i).height == 144)
						qcif = true;
			}
			mCamera.release();
		}
		if (!cif)
			list.add("352x288");
		if (!qcif)
			list.add("176x144");

		CharSequence[] entries = list.toArray(new CharSequence[list.size()]);
		CharSequence[] entryValues = list
				.toArray(new CharSequence[list.size()]);
		listSizeCam.setEntries(entries);
		listSizeCam.setEntryValues(entryValues);
		listSizeCam.setDefaultValue("352x288");
		listSizeCam.setDialogTitle("Video size");
		listSizeCam.setKey(Keys_Preferences.MEDIA_VIDEO_SIZE);
		listSizeCam.setTitle("Video size");
		listSizeCam.setSummary("Select a video size");
		videoCategory.addPreference(listSizeCam);

		// Direction Call
		CharSequence[] entriesV = { "SEND/RECEIVE", "SEND ONLY", "RECEIVE ONLY" };
		CharSequence[] entryValuesV = { "SEND/RECEIVE", "SEND ONLY",
				"RECEIVE ONLY" };
		ListPreference listDirectionV = new ListPreference(this);
		listDirectionV.setEntries(entriesV);
		listDirectionV.setEntryValues(entryValuesV);
		listDirectionV.setDefaultValue("SEND/RECEIVE");
		listDirectionV.setDialogTitle("Call video direction");
		listDirectionV.setKey(Keys_Preferences.MEDIA_VIDEO_CALL_DIRECTION);
		listDirectionV.setTitle("Call video direction");
		listDirectionV.setSummary("Select call video direction");
		videoCategory.addPreference(listDirectionV);

		// Max Frame Rate
		EditTextPreference editTextMaxFr = new EditTextPreference(this);
		editTextMaxFr.setDialogTitle("Max frame rate");
		editTextMaxFr.setKey(Keys_Preferences.MEDIA_VIDEO_MAX_FR);
		editTextMaxFr.setTitle("Max frame rate");
		editTextMaxFr.setSummary("Select max frame rate");
		videoCategory.addPreference(editTextMaxFr);

		// Max Frame Rate
		EditTextPreference editTextGop = new EditTextPreference(this);
		editTextGop.setDialogTitle("Gop size");
		editTextGop.setKey(Keys_Preferences.MEDIA_VIDEO_GOP_SIZE);
		editTextGop.setTitle("Gop size");
		editTextGop.setSummary("Select size between two I-frames");
		videoCategory.addPreference(editTextGop);

		// Max Frame Rate
		EditTextPreference editTextQueue = new EditTextPreference(this);
		editTextQueue.setDialogTitle("Max Frame queue size");
		editTextQueue.setKey(Keys_Preferences.MEDIA_VIDEO_QUEUE_SIZE);
		editTextQueue.setTitle("Max Frame queue size");
		editTextQueue.setSummary("Select max frame queue size");
		videoCategory.addPreference(editTextQueue);

		// ----//
		// Audio Category
		PreferenceCategory audioCategory = new PreferenceCategory(this);
		audioCategory.setTitle("Audio Preferences");
		root.addPreference(audioCategory);

		// Audio codecs
		PreferenceScreen audioCodecPref = getPreferenceManager()
				.createPreferenceScreen(this);
		audioCodecPref.setKey(Keys_Preferences.MEDIA_AUDIO_CODECS);
		audioCodecPref.setTitle("Audio codecs");
		audioCodecPref.setSummary("Select audio codecs");
		audioCategory.addPreference(audioCodecPref);

		// Codecs
		CheckBoxPreference nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setDefaultValue(true);
		nextAudioCodecPref.setKey(Keys_Preferences.MEDIA_AUDIO_AMR_CODEC);
		nextAudioCodecPref.setTitle("AMR");
		audioCodecPref.addPreference(nextAudioCodecPref);
		nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setKey(Keys_Preferences.MEDIA_AUDIO_MP2_CODEC);
		nextAudioCodecPref.setTitle("MP2");
		audioCodecPref.addPreference(nextAudioCodecPref);
		nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setKey(Keys_Preferences.MEDIA_AUDIO_AAC_CODEC);
		nextAudioCodecPref.setTitle("AAC");
		audioCodecPref.addPreference(nextAudioCodecPref);
		nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setKey(Keys_Preferences.MEDIA_AUDIO_PCMU_CODEC);
		nextAudioCodecPref.setTitle("PCMU");
		audioCodecPref.addPreference(nextAudioCodecPref);
		nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setKey(Keys_Preferences.MEDIA_AUDIO_PCMA_CODEC);
		nextAudioCodecPref.setTitle("PCMA");
		audioCodecPref.addPreference(nextAudioCodecPref);

		// Audio local port range
		PreferenceScreen audioLocalPortRangecPref = getPreferenceManager()
				.createPreferenceScreen(this);
		audioLocalPortRangecPref
				.setKey(Keys_Preferences.MEDIA_AUDIO_LOCAL_PORT_RANGE);
		audioLocalPortRangecPref.setTitle("Audio local port range");
		audioLocalPortRangecPref.setSummary("Select audio local port range");
		audioCategory.addPreference(audioLocalPortRangecPref);

		EditTextPreference editTextMinAudioPort = new EditTextPreference(this);
		editTextMinAudioPort.setDialogTitle("Min video local port range");
		editTextMinAudioPort
				.setKey(Keys_Preferences.MEDIA_MIN_AUDIO_LOCAL_PORT);
		editTextMinAudioPort.setTitle("Min audio local port range");
		editTextMinAudioPort
				.setSummary("Select min audio local port of the range.");
		audioLocalPortRangecPref.addPreference(editTextMinAudioPort);
		EditTextPreference editTextMaxAudioPort = new EditTextPreference(this);
		editTextMaxAudioPort.setDialogTitle("Max audio local port range");
		editTextMaxAudioPort
				.setKey(Keys_Preferences.MEDIA_MAX_AUDIO_LOCAL_PORT);
		editTextMaxAudioPort.setTitle("Max audio local port range");
		editTextMaxAudioPort
				.setSummary("Select max audio local port of the range.");
		audioLocalPortRangecPref.addPreference(editTextMaxAudioPort);

		// Direction Call
		CharSequence[] entriesA = { "SEND/RECEIVE", "SEND ONLY", "RECEIVE ONLY" };
		CharSequence[] entryValuesA = { "SEND/RECEIVE", "SEND ONLY",
				"RECEIVE ONLY" };
		ListPreference listDirectionA = new ListPreference(this);
		listDirectionA.setEntries(entriesA);
		listDirectionA.setEntryValues(entryValuesA);
		listDirectionA.setDefaultValue("SEND/RECEIVE");
		listDirectionA.setDialogTitle("Call audio direction");
		listDirectionA.setKey(Keys_Preferences.MEDIA_AUDIO_CALL_DIRECTION);
		listDirectionA.setTitle("Call audio direction");
		listDirectionA.setSummary("Select call audio direction");
		audioCategory.addPreference(listDirectionA);

		return root;
	}

	public static void setMediaPreferences(Context context,
			Map<String, Object> params) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();

		editor.putBoolean(Keys_Preferences.MEDIA_GENERAL_FRONT_CAMERA,
				(Boolean) params
						.get(Keys_Preferences.MEDIA_GENERAL_FRONT_CAMERA));

		editor.putString(Keys_Preferences.MEDIA_GENERAL_MAX_BW,
				(String) params.get(Keys_Preferences.MEDIA_GENERAL_MAX_BW));
		editor.putString(Keys_Preferences.MEDIA_GENERAL_MAX_DELAY,
				(String) params.get(Keys_Preferences.MEDIA_GENERAL_MAX_DELAY));
		editor.putBoolean(Keys_Preferences.MEDIA_GENERAL_SYNC_MEDIA_STREAMS,
				(Boolean) params
						.get(Keys_Preferences.MEDIA_GENERAL_SYNC_MEDIA_STREAMS));
		editor.putString(Keys_Preferences.MEDIA_VIDEO_CALL_DIRECTION,
				(String) params
						.get(Keys_Preferences.MEDIA_VIDEO_CALL_DIRECTION));
		editor.putString(Keys_Preferences.MEDIA_AUDIO_CALL_DIRECTION,
				(String) params
						.get(Keys_Preferences.MEDIA_AUDIO_CALL_DIRECTION));

		// Set Audio Codec
		editor.putBoolean(Keys_Preferences.MEDIA_AUDIO_AMR_CODEC,
				(Boolean) params.get(Keys_Preferences.MEDIA_AUDIO_AMR_CODEC));
		editor.putBoolean(Keys_Preferences.MEDIA_AUDIO_AAC_CODEC,
				(Boolean) params.get(Keys_Preferences.MEDIA_AUDIO_AAC_CODEC));
		editor.putBoolean(Keys_Preferences.MEDIA_AUDIO_MP2_CODEC,
				(Boolean) params.get(Keys_Preferences.MEDIA_AUDIO_MP2_CODEC));
		editor.putBoolean(Keys_Preferences.MEDIA_AUDIO_PCMA_CODEC,
				(Boolean) params.get(Keys_Preferences.MEDIA_AUDIO_PCMA_CODEC));
		editor.putBoolean(Keys_Preferences.MEDIA_AUDIO_PCMU_CODEC,
				(Boolean) params.get(Keys_Preferences.MEDIA_AUDIO_PCMU_CODEC));

		// Set Video Codec
		editor.putBoolean(Keys_Preferences.MEDIA_VIDEO_H263_CODEC,
				(Boolean) params.get(Keys_Preferences.MEDIA_VIDEO_H263_CODEC));
		editor.putBoolean(Keys_Preferences.MEDIA_VIDEO_H264_CODEC,
				(Boolean) params.get(Keys_Preferences.MEDIA_VIDEO_H264_CODEC));
		editor.putBoolean(Keys_Preferences.MEDIA_VIDEO_MPEG4_CODEC,
				(Boolean) params.get(Keys_Preferences.MEDIA_VIDEO_MPEG4_CODEC));

		editor.putString(Keys_Preferences.MEDIA_MIN_AUDIO_LOCAL_PORT,
				(String) params
						.get(Keys_Preferences.MEDIA_MIN_AUDIO_LOCAL_PORT));
		editor.putString(Keys_Preferences.MEDIA_MAX_AUDIO_LOCAL_PORT,
				(String) params
						.get(Keys_Preferences.MEDIA_MAX_AUDIO_LOCAL_PORT));

		editor.putString(Keys_Preferences.MEDIA_MIN_VIDEO_LOCAL_PORT,
				(String) params
						.get(Keys_Preferences.MEDIA_MIN_VIDEO_LOCAL_PORT));
		editor.putString(Keys_Preferences.MEDIA_MAX_VIDEO_LOCAL_PORT,
				(String) params
						.get(Keys_Preferences.MEDIA_MAX_VIDEO_LOCAL_PORT));

		editor.putString(Keys_Preferences.MEDIA_VIDEO_SIZE,
				(String) params.get(Keys_Preferences.MEDIA_VIDEO_SIZE));

		editor.putString(Keys_Preferences.MEDIA_VIDEO_MAX_FR,
				(String) params.get(Keys_Preferences.MEDIA_VIDEO_MAX_FR));

		editor.putString(Keys_Preferences.MEDIA_VIDEO_GOP_SIZE,
				(String) params.get(Keys_Preferences.MEDIA_VIDEO_GOP_SIZE));

		editor.putString(Keys_Preferences.MEDIA_VIDEO_QUEUE_SIZE,
				(String) params.get(Keys_Preferences.MEDIA_VIDEO_QUEUE_SIZE));
		editor.commit();

	}

	private synchronized static void setPreferenceChanged(boolean hasChanged) {
		preferenceMediaChanged = hasChanged;
	}

	public static synchronized boolean isPreferenceChanged() {
		return preferenceMediaChanged;
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
		Log.d(this.getClass().getName(), "Video Preferecnces Changed "
				+ sharedPreferences);
		setPreferenceChanged(true);
	}

}
