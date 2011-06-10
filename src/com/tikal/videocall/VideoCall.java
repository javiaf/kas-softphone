package com.tikal.videocall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.tikal.applicationcontext.ApplicationContext;
import com.tikal.media.AudioInfo;
import com.tikal.media.VideoInfo;
import com.tikal.media.audio.AudioCapture;
import com.tikal.media.audio.AudioReceive;
import com.tikal.media.camera.CameraCapture;
import com.tikal.media.camera.CameraReceive;
import com.tikal.preferences.VideoCall_Preferences;
import com.tikal.softphone.IPhone;
import com.tikal.softphone.R;

public class VideoCall extends Activity implements Runnable {
	private static final String LOG_TAG = "VideoCall";
	private static final int SHOW_PREFERENCES = 1;

	private AudioCapture audioCapture;
	private AudioReceive audioReceive;
	private CameraCapture cameraCapture;
	private CameraReceive cameraReceive;

	private String sdp = "";

	public static boolean isVideoCall = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videocall);
		Log.d(LOG_TAG, "OnCreate");
		/* Create Threads Audio/Video Capture/Receive */
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
				"GdC");
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Log.d(LOG_TAG, "OnStart");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.d(LOG_TAG, "OnResume");
		Bundle extras = getIntent().getExtras();
		isVideoCall = true;
		if (isVideoCall) {

			VideoInfo vi = (VideoInfo) extras.getSerializable("VideoInfo");
			AudioInfo ai = (AudioInfo) extras.getSerializable("AudioInfo");

			String sdpVideo = (String) extras.getSerializable("SdpVideo");
			String sdpAudio = (String) extras.getSerializable("SdpAudio");
			Log.d(LOG_TAG, "Accept Call: SdpAudio -> " + sdpAudio.toString());
			Log.d(LOG_TAG, "Accept Call: SdpVideo -> " + sdpVideo.toString());
			if (!sdpAudio.equals("")) {
				audioCapture = new AudioCapture(ai);
				audioReceive = new AudioReceive(AudioManager.STREAM_MUSIC,
						sdpAudio);
				Thread tAudioCapture = new Thread(audioCapture);
				Thread tAudioReceive = new Thread(audioReceive);
				tAudioCapture.start();
				tAudioReceive.start();
				Log.d(LOG_TAG, "Audio is OK");
			}

			if (!sdpVideo.equals("")) {
				cameraCapture = new CameraCapture(
						findViewById(R.id.video_capture_surface), vi);
				cameraReceive = new CameraReceive(
						findViewById(R.id.video_receive_surface), vi, sdpVideo);

				// Thread tCameraCapture = new Thread(cameraCapture);
				Thread tCameraReceive = new Thread(cameraReceive);

				// tCameraCapture.start();
				tCameraReceive.start();
				Log.d(LOG_TAG, "Video is OK");
			}
			
			// thiz.start();

		}

		final Button buttonTerminateCall = (Button) findViewById(R.id.button_terminate_call);
		buttonTerminateCall.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				IPhone controller = (IPhone) ApplicationContext.contextTable
						.get("controller");
				Log.d(LOG_TAG, "Push Button Terminate controller " + controller);
				if (controller != null) {
					Log.d(LOG_TAG, "Push Button Terminate");
					controller.hang();
					Log.d(LOG_TAG, "Hang ...");
					isVideoCall = false;
					setResult(RESULT_OK);
					finish();
				} else {
					finish();
				}
			}
		});
		final Button ButtonUseFrontCamera = (Button) findViewById(R.id.button_use_front_camera);
		ButtonUseFrontCamera.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// CameraCapture.getInstance().setUseFrontCamera(false);
			}
		});

	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		try {
			Log.d(LOG_TAG, "Finish");
			// MediaRx.stopRx();
			cameraCapture.release();
			cameraReceive.release();
			audioCapture.release();
			audioReceive.release();
			Log.d(LOG_TAG, "Release All");
		} catch (Exception e) {
			Log.e(LOG_TAG, "Exception:" + e.toString());
		}

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.d(LOG_TAG, "OnPause");
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		Log.d(LOG_TAG, "OnRestart");
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.d(LOG_TAG, "OnStop");
		isVideoCall = false;
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(LOG_TAG, "OnDestroy");
		isVideoCall = false;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		String filename = Environment.getExternalStorageDirectory()
		// .getAbsolutePath() + "/DCIM/Camera/vide.mp4";
				.getAbsolutePath() + "sdp.sdp";
		Log.d(LOG_TAG, "File:" + filename);

		// MediaRx.startVideoRx(sdp, this);// sdp);

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
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SHOW_PREFERENCES) {
			Log.d(LOG_TAG, "Show preferences");
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());
			Boolean headset = settings.getBoolean("HEADSET", true);
			Boolean mute = settings.getBoolean("MUTE", false);

		}
	}

	// @Override
	// public void putVideoFrameRx(int[] rgb, int width, int height) {
	// if (cameraReceive != null)
	// cameraReceive.putFrame(rgb, width, height);
	//
	// }
	//
	// @Override
	// public void putAudioSamplesRx(byte[] audio, int length) {
	// if (audioReceive != null)
	// audioReceive.putAudio(audio, length);
	// }
}
