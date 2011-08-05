package com.tikal.videocall;

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

import com.tikal.android.mscontrol.MediaSessionAndroid;
import com.tikal.android.mscontrol.ParametersImpl;
import com.tikal.android.mscontrol.mediacomponent.MediaComponentAndroid;
import com.tikal.applicationcontext.ApplicationContext;
import com.tikal.mscontrol.MediaSession;
import com.tikal.mscontrol.MsControlException;
import com.tikal.mscontrol.Parameters;
import com.tikal.mscontrol.join.Joinable.Direction;
import com.tikal.mscontrol.join.JoinableStream.StreamType;
import com.tikal.mscontrol.mediacomponent.MediaComponent;
import com.tikal.mscontrol.networkconnection.NetworkConnection;
import com.tikal.preferences.VideoCall_Preferences;
import com.tikal.sip.Controller;
import com.tikal.softphone.R;
import com.tikal.softphone.ServiceUpdateUIListener;

public class VideoCall extends Activity implements ServiceUpdateUIListener {
	private static final String LOG_TAG = "VideoCall";
	private static final int SHOW_PREFERENCES = 1;
	private PowerManager.WakeLock wl;
	private boolean hang = false;

	MediaComponent videoPlayerComponent = null;
	MediaComponent videoRecorderComponent = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videocall);

		VideoCallService.setUpdateListener(this);
		hang = false;
		Log.d(LOG_TAG, "OnCreate " + hang);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "VideoCall");
		wl.acquire();

		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");
		// FIXME controller != null
		if (controller != null) {
			MediaSession mediaSession = controller.getMediaSession();
			try {
				DisplayMetrics dm = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(dm);
				int Orientation = getWindowManager().getDefaultDisplay()
						.getOrientation();

				Log.d(LOG_TAG, "W: " + dm.widthPixels + " H:" + dm.heightPixels
						+ " Orientation = " + Orientation);

				Parameters params = new ParametersImpl();
				params.put(MediaComponentAndroid.PREVIEW_SURFACE,
						(View) findViewById(R.id.video_capture_surface));
				params.put(MediaComponentAndroid.DISPLAY_ORIENTATION,
						Orientation);

				videoPlayerComponent = mediaSession.createMediaComponent(
						MediaComponentAndroid.VIDEO_PLAYER, params);

				params = new ParametersImpl();
				params.put(MediaComponentAndroid.VIEW_SURFACE,
						(View) findViewById(R.id.video_receive_surface));
				params.put(MediaComponentAndroid.DISPLAY_WIDTH, dm.widthPixels);
				params.put(MediaComponentAndroid.DISPLAY_HEIGHT,
						dm.heightPixels);
				videoRecorderComponent = mediaSession.createMediaComponent(
						MediaComponentAndroid.VIDEO_RECORDER, params);
			} catch (MsControlException e) {
				// TODO Auto-generated catch block
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
		// TODO Auto-generated method stub
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

					Log.d(LOG_TAG, "Button Mute push");
					Log.d(LOG_TAG,
							"isStarted(): " + audioPlayerComponent.isStarted());
					try {
						if (audioPlayerComponent.isStarted())
							audioPlayerComponent.stop();
						else
							audioPlayerComponent.start();
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
						// TODO: solo cambia al altavoz interno. El control de
						// que
						// altavoz se está usando habrá que hacerlo a nivel de
						// aplicación. El API no ofrece tanto detalle
						if (audioRecorderComponent != null) {
							audioRecorderComponent.stop();
							audioRecorderComponent.unjoin(nc
									.getJoinableStream(StreamType.audio));
						}

						Parameters params = new ParametersImpl();
						params.put(MediaComponentAndroid.STREAM_TYPE,
								AudioManager.STREAM_VOICE_CALL);
						audioRecorderComponent = mediaSession
								.createMediaComponent(
										MediaComponentAndroid.AUDIO_RECORDER,
										params);

						if (audioRecorderComponent != null) {
							audioRecorderComponent.join(Direction.RECV,
									nc.getJoinableStream(StreamType.audio));
							audioRecorderComponent.start();
						}
					} catch (MsControlException e) {
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
			Boolean headset = settings.getBoolean("HEADSET", true);
			Boolean mute = settings.getBoolean("MUTE", false);

		}
	}

	@Override
	public void update(Message message) {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "Message = " + message);
		finish();

	}

}
