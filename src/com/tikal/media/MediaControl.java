package com.tikal.media;

import com.tikal.softphone.R;
import com.tikal.softphone.SoftPhone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tikal.media.audio.*;
import com.tikal.media.camera.*;

public class MediaControl extends Activity {
	private static final String LOG_TAG = "MediaControl";

	Vibrator vibrator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_call);
		Log.d(LOG_TAG, "MediaControl Created");

		Bundle extras = getIntent().getExtras();

		String uri = (String) extras.getSerializable("Uri");

		TextView text = (TextView) findViewById(R.id.incoming_sip);
		text.setText(uri);
		
		ImageView imageCall = (ImageView) findViewById(R.id.image_call);
		// imageCall.setImageURI(uri)
		// Ring Ring and Vibrate phone
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		long[] pattern = { 0, 1000, 2000, 3000 };

		vibrator.vibrate(pattern, 1);

	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		Log.d(LOG_TAG, "New Intent");
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Log.d(LOG_TAG, "onStart");
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		Log.d(LOG_TAG, "onRestart");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.d(LOG_TAG, "onResume");

		final Button buttonCall = (Button) findViewById(R.id.button_call_accept);
		buttonCall.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				vibrator.cancel();
				setResult(RESULT_OK);
				finish();
			}
		});

		final Button buttonReject = (Button) findViewById(R.id.button_call_reject);
		buttonReject.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				vibrator.cancel();
				setResult(RESULT_CANCELED);
				finish();
			}
		});

	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.d(LOG_TAG, "onStop");
		vibrator.cancel();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.d(LOG_TAG, "onPause");
		vibrator.cancel();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy");
		vibrator.cancel();
	}

}
