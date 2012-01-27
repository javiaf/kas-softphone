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

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class Video_Preferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setPreferenceScreen(createPreferenceHierarchy());
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
		editTextMaxBw.setKey("MAX_BW");
		editTextMaxBw.setTitle("Max bandwidth");
		editTextMaxBw.setSummary("Select max bandwidth");

		generalCategory.addPreference(editTextMaxBw);

		// Max delay
		EditTextPreference editTextMaxDelay = new EditTextPreference(this);
		editTextMaxDelay.setDialogTitle("MAX_DELAY");
		editTextMaxDelay.setKey("MAX_DELAY");
		editTextMaxDelay.setTitle("Max delay (ms)");
		editTextMaxDelay.setSummary("Select max delay");

		generalCategory.addPreference(editTextMaxDelay);

		// Camera Facing
		CheckBoxPreference cameraFacing = new CheckBoxPreference(this);
		cameraFacing.setDefaultValue(false);
		cameraFacing.setKey("CAMERA_FRONT");
		cameraFacing.setTitle("Front Camera");
		cameraFacing
				.setSummary("If it selected, it used the Front Camera. Else, it used the Back Camera.");

		generalCategory.addPreference(cameraFacing);

		// ------//
		// Network Category
		PreferenceCategory networkCategory = new PreferenceCategory(this);
		networkCategory.setTitle("Network Preferences");
		root.addPreference(networkCategory);

		// KeepAliveEnable
		CheckBoxPreference keepAliveEnable = new CheckBoxPreference(this);
		keepAliveEnable.setDefaultValue(false);
		keepAliveEnable.setKey("KEEP_ALIVE");
		keepAliveEnable.setTitle("keep Alive ");
		keepAliveEnable.setSummary("If it selected, it used the Keep Alive.");
		networkCategory.addPreference(keepAliveEnable);

		// KeepAliveTime
		EditTextPreference keepAliveDelay = new EditTextPreference(this);
		keepAliveDelay.setDialogTitle("Keep Delay");
		keepAliveDelay.setKey("KEEP_DELAY");
		keepAliveDelay.setTitle("Keep delay (ms)");
		keepAliveDelay.setSummary("Select keep delay");
		networkCategory.addPreference(keepAliveDelay);

		// Transport
		CharSequence[] entriesTV = { "TCP", "UDP" };
		CharSequence[] entryValuesTV = { "TCP", "UDP" };
		ListPreference listTransportV = new ListPreference(this);
		listTransportV.setEntries(entriesTV);
		listTransportV.setEntryValues(entryValuesTV);
		listTransportV.setDefaultValue("UDP");
		listTransportV.setDialogTitle("Transport");
		listTransportV.setKey("TRANSPORT");
		listTransportV.setTitle("Transport");
		listTransportV.setSummary("Select transport");
		networkCategory.addPreference(listTransportV);
		
		root.addPreference(networkCategory);

		// ------//
		// Video Category
		PreferenceCategory videoCategory = new PreferenceCategory(this);
		videoCategory.setTitle("Video Preferences");
		root.addPreference(videoCategory);

		// Video codecs
		PreferenceScreen videoCodecPref = getPreferenceManager()
				.createPreferenceScreen(this);
		videoCodecPref.setKey("VIDEO_CODECS");
		videoCodecPref.setTitle("Video codecs");
		videoCodecPref.setSummary("Select video codecs");
		videoCategory.addPreference(videoCodecPref);

		// Codecs
		CheckBoxPreference nextVideoCodecPref = new CheckBoxPreference(this);
		nextVideoCodecPref.setDefaultValue(true);
		nextVideoCodecPref.setKey("H263_CODEC");
		nextVideoCodecPref.setTitle("H263");
		videoCodecPref.addPreference(nextVideoCodecPref);
		nextVideoCodecPref = new CheckBoxPreference(this);
		nextVideoCodecPref.setKey("MPEG4_CODEC");
		nextVideoCodecPref.setTitle("MPEG4");
		videoCodecPref.addPreference(nextVideoCodecPref);
		nextVideoCodecPref = new CheckBoxPreference(this);
		nextVideoCodecPref.setKey("H264_CODEC");
		nextVideoCodecPref.setTitle("H264");
		videoCodecPref.addPreference(nextVideoCodecPref);

		// Size Camera
		ArrayList<String> list = new ArrayList<String>();
		ListPreference listSizeCam = new ListPreference(this);
		Camera mCamera = Camera.open();

		if (mCamera != null) {
			boolean cif = false;
			boolean qcif = false;
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
			if (!cif)
				list.add("352x288");
			if (!qcif)
				list.add("176x144");
			mCamera.release();
		}
		CharSequence[] entries = list.toArray(new CharSequence[list.size()]);
		CharSequence[] entryValues = list
				.toArray(new CharSequence[list.size()]);
		listSizeCam.setEntries(entries);
		listSizeCam.setEntryValues(entryValues);
		listSizeCam.setDefaultValue("352x288");
		listSizeCam.setDialogTitle("Video size");
		listSizeCam.setKey("VIDEO_SIZE");
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
		listDirectionV.setKey("CALL_VIDEO_DIRECTION");
		listDirectionV.setTitle("Call video direction");
		listDirectionV.setSummary("Select call video direction");
		videoCategory.addPreference(listDirectionV);

		// Max Frame Rate
		EditTextPreference editTextMaxFr = new EditTextPreference(this);
		editTextMaxFr.setDialogTitle("Max frame rate");
		editTextMaxFr.setKey("MAX_FR");
		editTextMaxFr.setTitle("Max frame rate");
		editTextMaxFr.setSummary("Select max frame rate");
		videoCategory.addPreference(editTextMaxFr);

		// Max Frame Rate
		EditTextPreference editTextGop = new EditTextPreference(this);
		editTextGop.setDialogTitle("Gop size");
		editTextGop.setKey("GOP_SIZE");
		editTextGop.setTitle("Gop size");
		editTextGop.setSummary("Select size between two I-frames");
		videoCategory.addPreference(editTextGop);

		// Max Frame Rate
		EditTextPreference editTextQueue = new EditTextPreference(this);
		editTextQueue.setDialogTitle("Max Frame queue size");
		editTextQueue.setKey("QUEUE_SIZE");
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
		audioCodecPref.setKey("AUDIO_CODECS");
		audioCodecPref.setTitle("Audio codecs");
		audioCodecPref.setSummary("Select audio codecs");
		audioCategory.addPreference(audioCodecPref);

		// Codecs
		CheckBoxPreference nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setDefaultValue(true);
		nextAudioCodecPref.setKey("AMR_AUDIO_CODEC");
		nextAudioCodecPref.setTitle("AMR");
		audioCodecPref.addPreference(nextAudioCodecPref);
		nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setKey("MP2_AUDIO_CODEC");
		nextAudioCodecPref.setTitle("MP2");
		audioCodecPref.addPreference(nextAudioCodecPref);
		nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setKey("AAC_AUDIO_CODEC");
		nextAudioCodecPref.setTitle("AAC");
		audioCodecPref.addPreference(nextAudioCodecPref);
		nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setKey("PCMU_AUDIO_CODEC");
		nextAudioCodecPref.setTitle("PCMU");
		audioCodecPref.addPreference(nextAudioCodecPref);
		nextAudioCodecPref = new CheckBoxPreference(this);
		nextAudioCodecPref.setKey("PCMA_AUDIO_CODEC");
		nextAudioCodecPref.setTitle("PCMA");
		audioCodecPref.addPreference(nextAudioCodecPref);

		// Direction Call
		CharSequence[] entriesA = { "SEND/RECEIVE", "SEND ONLY", "RECEIVE ONLY" };
		CharSequence[] entryValuesA = { "SEND/RECEIVE", "SEND ONLY",
				"RECEIVE ONLY" };
		ListPreference listDirectionA = new ListPreference(this);
		listDirectionA.setEntries(entriesA);
		listDirectionA.setEntryValues(entryValuesA);
		listDirectionA.setDefaultValue("SEND/RECEIVE");
		listDirectionA.setDialogTitle("Call audio direction");
		listDirectionA.setKey("CALL_AUDIO_DIRECTION");
		listDirectionA.setTitle("Call audio direction");
		listDirectionA.setSummary("Select call audio direction");
		audioCategory.addPreference(listDirectionA);

		return root;
	}
}
