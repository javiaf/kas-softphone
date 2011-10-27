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
package com.kurento.kas.phone.videocall;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.kurento.commons.mscontrol.MediaSession;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.mscontrol.join.Joinable.Direction;
import com.kurento.commons.mscontrol.join.JoinableStream.StreamType;
import com.kurento.commons.mscontrol.mediacomponent.MediaComponent;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.sdp.enums.MediaType;
import com.kurento.commons.sdp.enums.Mode;
import com.kurento.kas.mscontrol.MSControlFactory;
import com.kurento.kas.mscontrol.MediaSessionAndroid;
import com.kurento.kas.mscontrol.mediacomponent.MediaComponentAndroid;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.preferences.VideoCall_Preferences;
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.softphone.R;
import com.kurento.kas.phone.softphone.ServiceUpdateUIListener;

public class VideoCall extends Activity implements ServiceUpdateUIListener {
	private static final String LOG_TAG = "VideoCall";
	private static final int SHOW_PREFERENCES = 1;
	private PowerManager.WakeLock wl;
	private boolean hang = false;
	private Map<MediaType, Mode> callDirectionMap;

	MediaComponent videoPlayerComponent = null;
	MediaComponent videoRecorderComponent = null;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		callDirectionMap = (Map<MediaType, Mode>) ApplicationContext.contextTable
				.get("callDirection");

		Mode videoMode = callDirectionMap.get(MediaType.VIDEO);

		if ((videoMode != null) && (Mode.RECVONLY.equals(videoMode)))
			setContentView(R.layout.videocall_receive);
		else if ((videoMode != null) && (Mode.SENDONLY.equals(videoMode)))
			setContentView(R.layout.videocall_send);
		else if ((videoMode != null) && (Mode.SENDRECV.equals(videoMode)))
			setContentView(R.layout.videocall);
		else
			setContentView(R.layout.onlycall);

		VideoCallService.setUpdateListener(this);
		hang = false;
		Log.d(LOG_TAG, "OnCreate " + hang);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "VideoCall");
		wl.acquire();

		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");

		if (controller != null) {
			MediaSession mediaSession = controller.getMediaSession();

			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			int Orientation = getWindowManager().getDefaultDisplay()
					.getOrientation();
			try {
				if ((videoMode != null)
						&& (Mode.SENDONLY.equals(videoMode) || Mode.SENDRECV
								.equals(videoMode))) {
					Parameters params = MSControlFactory.createParameters();
					params.put(MediaComponentAndroid.PREVIEW_SURFACE,
							(View) findViewById(R.id.video_capture_surface));
					params.put(MediaComponentAndroid.DISPLAY_ORIENTATION,
							Orientation);
					videoPlayerComponent = mediaSession.createMediaComponent(
							MediaComponentAndroid.VIDEO_PLAYER, params);
				}
			} catch (MsControlException e) {
				Log.e(LOG_TAG, e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage());
			}

			try {
				if ((videoMode != null)
						&& (Mode.RECVONLY.equals(videoMode) || Mode.SENDRECV
								.equals(videoMode))) {
					Parameters params = MSControlFactory.createParameters();
					params.put(MediaComponentAndroid.VIEW_SURFACE,
							(View) findViewById(R.id.video_receive_surface));
					params.put(MediaComponentAndroid.DISPLAY_WIDTH,
							dm.widthPixels);
					params.put(MediaComponentAndroid.DISPLAY_HEIGHT,
							dm.heightPixels);
					videoRecorderComponent = mediaSession.createMediaComponent(
							MediaComponentAndroid.VIDEO_RECORDER, params);
				}
			} catch (MsControlException e) {
				Log.e(LOG_TAG, e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage());
			}
		} else
			Log.e(LOG_TAG, "Controller is null");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(LOG_TAG, "OnNewIntent");
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "HANG = " + hang);
		if (!hang) {
			NetworkConnection nc = (NetworkConnection) ApplicationContext.contextTable
					.get("networkConnection");
			Log.e(LOG_TAG, "nc: " + nc);
			if (nc == null) {
				Log.e(LOG_TAG, "networkConnection is NULL");
				return;
			}

			try {
				if (videoPlayerComponent != null) {
					videoPlayerComponent.join(Direction.SEND,
							nc.getJoinableStream(StreamType.video));
					videoPlayerComponent.start();
				}
				if (videoRecorderComponent != null) {
					videoRecorderComponent.join(Direction.RECV,
							nc.getJoinableStream(StreamType.video));
					videoRecorderComponent.start();
				}

			} catch (MsControlException e) {
				Log.e(LOG_TAG, e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage());
			}

			final Button buttonTerminateCall = (Button) findViewById(R.id.button_terminate_call);
			buttonTerminateCall.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Log.d(LOG_TAG, "Hang ...");
					Controller controller = (Controller) ApplicationContext.contextTable
							.get("controller");
					if (controller != null) {
						controller.hang();
						hang = true;
					}
					finish();
				}
			});
			final Button buttonMute = (Button) findViewById(R.id.button_mute);

			buttonMute.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					MediaComponentAndroid audioPlayerComponent = (MediaComponentAndroid) ApplicationContext.contextTable
							.get("audioPlayerComponent");

					try {
						if (audioPlayerComponent != null) {
							if (audioPlayerComponent.isStarted())
								audioPlayerComponent.stop();
							else
								audioPlayerComponent.start();
						}
					} catch (MsControlException e) {
						e.printStackTrace();
					}
				}
			});

			final Button buttonSpeaker = (Button) findViewById(R.id.button_headset);

			buttonSpeaker.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					Controller controller = (Controller) ApplicationContext.contextTable
							.get("controller");
					if (controller == null) {
						Log.e(LOG_TAG, "controller is NULL");
						return;
					}

					NetworkConnection nc = (NetworkConnection) ApplicationContext.contextTable
							.get("networkConnection");
					if (nc == null) {
						Log.e(LOG_TAG, "networkConnection is NULL");
						return;
					}

					MediaSessionAndroid mediaSession = controller
							.getMediaSession();
					MediaComponentAndroid audioRecorderComponent = (MediaComponentAndroid) ApplicationContext.contextTable
							.get("audioRecorderComponent");
					try {
						if (audioRecorderComponent != null) {
							audioRecorderComponent.stop();
							audioRecorderComponent.unjoin(nc
									.getJoinableStream(StreamType.audio));
						}

						Parameters params = MSControlFactory.createParameters();

						Boolean speaker = (Boolean) ApplicationContext.contextTable
								.get("speaker");
						if (speaker != null) {

							if (speaker)
								params.put(MediaComponentAndroid.STREAM_TYPE,
										AudioManager.STREAM_MUSIC);
							else
								params.put(MediaComponentAndroid.STREAM_TYPE,
										AudioManager.STREAM_VOICE_CALL);

							speaker = !speaker;
							ApplicationContext.contextTable.put("speaker",
									speaker);

							audioRecorderComponent = mediaSession
									.createMediaComponent(
											MediaComponentAndroid.AUDIO_RECORDER,
											params);
							ApplicationContext.contextTable.put(
									"audioRecorderComponent",
									audioRecorderComponent);

							if (audioRecorderComponent != null) {
								audioRecorderComponent.join(Direction.RECV,
										nc.getJoinableStream(StreamType.audio));
								audioRecorderComponent.start();

							}
						}
					} catch (MsControlException e) {
						Log.e(LOG_TAG,
								"Exception change speaker." + e.toString());
						e.printStackTrace();
					}
				}
			});

			Log.e(LOG_TAG, "onResume OK");
		}
	}

	@Override
	public void finish() {
		Log.d(LOG_TAG, "Finish");
		super.finish();
	}

	@Override
	protected void onPause() {
		Log.d(LOG_TAG, "OnPause");
		if (videoRecorderComponent != null)
			videoRecorderComponent.stop();
		if (videoPlayerComponent != null)
			videoPlayerComponent.stop();
		super.onPause();
	}

	@Override
	protected void onRestart() {
		Log.d(LOG_TAG, "OnRestart");
		super.onRestart();
	}

	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "OnStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "OnDestroy ");
		if (wl != null)
			wl.release();
		super.onDestroy();
	}

	/* Menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.videocall_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case (R.id.menu_videocall_preferences):
			Intent videoCallPreferences = new Intent(this,
					VideoCall_Preferences.class);
			startActivityForResult(videoCallPreferences, SHOW_PREFERENCES);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SHOW_PREFERENCES) {
			Log.d(LOG_TAG, "Show preferences");
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());
		}
	}

	@Override
	public void update(Message message) {
		Log.d(LOG_TAG, "Message = " + message);
		finish();
	}

}
