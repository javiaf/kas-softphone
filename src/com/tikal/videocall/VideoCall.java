package com.tikal.videocall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.tikal.android.mscontrol.ParametersImpl;
import com.tikal.android.mscontrol.mediacomponent.MediaComponentAndroid;
import com.tikal.android.mscontrol.mediacomponent.VideoPlayerComponent;
import com.tikal.android.mscontrol.mediacomponent.VideoRecorderComponent;
import com.tikal.applicationcontext.ApplicationContext;
import com.tikal.mscontrol.MsControlException;
import com.tikal.mscontrol.MediaSession;
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

	MediaComponent videoPlayerComponent = null;
	MediaComponent videoRecorderComponent = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videocall);

		VideoCallService.setUpdateListener(this);
		Log.d(LOG_TAG, "OnCreate");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "VideoCall");
		wl.acquire();

		Controller controller = (Controller) ApplicationContext.contextTable.get("controller");
		// FIXME controller != null
		MediaSession mediaSession = controller.getMediaSession();
		try {
			Parameters params = new ParametersImpl();
			params.put(VideoPlayerComponent.PREVIEW_SURFACE, (View) findViewById(R.id.video_capture_surface) );

			videoPlayerComponent = mediaSession.createMediaComponent(
					MediaComponentAndroid.VIDEO_PLAYER, params);

			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			int Orientation = getWindowManager().getDefaultDisplay().getOrientation();
			Log.d(LOG_TAG, "W: " + dm.widthPixels + " H:" + dm.heightPixels + " Orientation = "
					+ Orientation);

			params = new ParametersImpl();
			params.put(VideoRecorderComponent.VIEW_SURFACE, (View) findViewById(R.id.video_receive_surface) );
			params.put(VideoRecorderComponent.DISPLAY_WIDTH, dm.widthPixels);
			params.put(VideoRecorderComponent.DISPLAY_HEIGHT, dm.heightPixels);
			videoRecorderComponent = mediaSession.createMediaComponent(
					MediaComponentAndroid.VIDEO_RECORDER, params);
		} catch (MsControlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			// TODO Auto-generated catch block
			e.printStackTrace();
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
				}
				finish();
			}
		});
//		final Button buttonMute = (Button) findViewById(R.id.button_mute);
//
//		buttonMute.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				AudioMediaGroup audioMediaGroup = (AudioMediaGroup) ApplicationContext.contextTable
//						.get("audioMediaGroup");
//				try {
//					Log.d(LOG_TAG, " Button Mute push");
//
//					if (((AudioPlayer) audioMediaGroup.getPlayer()).isPlaying()) {
//						Toast.makeText(VideoCall.this, "Mute", Toast.LENGTH_SHORT).show();
//						audioMediaGroup.getPlayer().stop(true);
//					} else {
//						Toast.makeText(VideoCall.this, "Speak", Toast.LENGTH_SHORT).show();
//						audioMediaGroup.getPlayer().play(URI.create(""), RTC.NO_RTC,
//								Parameters.NO_PARAMETER);
//					}
//
//				} catch (MsControlException e) {
//					e.printStackTrace();
//				}
//			}
//		});
//
//		final Button buttonSpeaker = (Button) findViewById(R.id.button_headset);
//
//		buttonSpeaker.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				AudioMediaGroup audioMediaGroup = (AudioMediaGroup) ApplicationContext.contextTable
//						.get("audioMediaGroup");
//				try {
//					if (((AudioRecorder) audioMediaGroup.getRecorder()).getStreamType() == AudioManager.STREAM_MUSIC) {
//						Toast.makeText(VideoCall.this, "Speaker", Toast.LENGTH_SHORT).show();
//						((AudioRecorder) audioMediaGroup.getRecorder())
//								.setStreamType(AudioManager.STREAM_VOICE_CALL);
//					} else {
//						Toast.makeText(VideoCall.this, "HeadSet", Toast.LENGTH_SHORT).show();
//						((AudioRecorder) audioMediaGroup.getRecorder())
//								.setStreamType(AudioManager.STREAM_MUSIC);
//					}
//					audioMediaGroup.getRecorder().record(URI.create(""), RTC.NO_RTC,
//							Parameters.NO_PARAMETER);
//				} catch (MsControlException e) {
//					e.printStackTrace();
//				}
//			}
//		});

		Log.e(LOG_TAG, "onResume OK");
	}

	@Override
	public void finish() {
		Log.d(LOG_TAG, "Finish");
		super.finish();
	}

	@Override
	protected void onPause() {
		Log.d(LOG_TAG, "OnPause");
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
		Log.d(LOG_TAG, "OnDestroy");
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
			Intent videoCallPreferences = new Intent(this, VideoCall_Preferences.class);
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
