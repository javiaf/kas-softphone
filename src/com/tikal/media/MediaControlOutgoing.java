package com.tikal.media;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tikal.controlcontacts.ControlContacts;
import com.tikal.softphone.R;

public class MediaControlOutgoing extends Activity {
	private static final String LOG_TAG = "MediaControlOutgoing";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_call_outgoingcall);
		

		Bundle extras = getIntent().getExtras();
		String uri = (String) extras.getSerializable("Uri");
		Integer id = (Integer) extras.getSerializable("Id");
		Log.d(LOG_TAG, "Media Control Outgoing Created; uri = " + uri + " id = " + id);
		TextView text = (TextView) findViewById(R.id.outgoing_sip);
		text.setText(uri);

		ImageView imageCall = (ImageView) findViewById(R.id.image_call);

		ControlContacts controlcontacts = new ControlContacts(this);
		
		Bitmap bm = controlcontacts.getPhoto(id);
		
		if (bm != null) {
			
			imageCall.setImageBitmap(bm);
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LOG_TAG, "onStart");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(LOG_TAG, "onRestart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume");

		final Button buttonReject = (Button) findViewById(R.id.button_call_reject);
		buttonReject.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Call Canceled");
				finish();
				setResult(RESULT_CANCELED);

			}
		});

	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "onStop");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "onPause");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy");
	}

}
