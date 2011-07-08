package com.tikal.videocall;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.tikal.applicationcontext.ApplicationContext;
import com.tikal.javax.media.mscontrol.mediagroup.VideoMediaGroup;
import com.tikal.preferences.VideoCall_Preferences;
import com.tikal.softphone.IPhone;
import com.tikal.softphone.R;

public class VideoCall extends Activity {
	private static final String LOG_TAG = "VideoCall";
	private static final int SHOW_PREFERENCES = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videocall);
		Log.d(LOG_TAG, "OnCreate");
		
		//use to mute, etc.
//		AudioMediaGroup audioMediaGroup = (AudioMediaGroup) ApplicationContext.contextTable
//		.get("audioMediaGroup");
		
		VideoMediaGroup videoMediaGroup = (VideoMediaGroup) ApplicationContext.contextTable
		.get("videoMediaGroup");
		Log.d(LOG_TAG, "videoMediaGroup: " + videoMediaGroup);
		if(videoMediaGroup != null) {
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			videoMediaGroup.setSurfaceTx(findViewById(R.id.video_capture_surface));
			videoMediaGroup.setSurfaceRx(findViewById(R.id.video_receive_surface),
						dm.widthPixels, dm.heightPixels);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LOG_TAG, "OnStart");
	}

	@Override
	protected void onResume() {
		super.onResume();
	
		final Button buttonTerminateCall = (Button) findViewById(R.id.button_terminate_call);
		buttonTerminateCall.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Hang ...");
				IPhone controller = (IPhone) ApplicationContext.contextTable
						.get("controller");
				if (controller != null) {
					controller.hang();
				}
			}
		});
		final Button ButtonUseFrontCamera = (Button) findViewById(R.id.button_use_front_camera);
		ButtonUseFrontCamera.setVisibility(0);
		ButtonUseFrontCamera.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// CameraCapture.getInstance().setUseFrontCamera(false);
			}
		});

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
		finish();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "OnDestroy");
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

}
