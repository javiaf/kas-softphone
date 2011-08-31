package com.kurento.kas.phone.softphone;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Register extends Activity {
	private static final String LOG_TAG = "Register";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_register);
	}

	@Override
	protected void onResume() {
		super.onResume();

		final Button saveButton = (Button) findViewById(R.id.ok_register);
		saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				EditText textlocalusername = (EditText) findViewById(R.id.Textpreference_local_username);
				EditText textlocaldomain = (EditText) findViewById(R.id.Textpreference_local_domain);
				EditText textproxyip = (EditText) findViewById(R.id.Textpreference_proxy_ip);
				EditText textproxyport = (EditText) findViewById(R.id.Textpreference_proxy_port);
				Log.d(LOG_TAG,
						textlocalusername.getText() + "; "
								+ textlocaldomain.getText() + "; "
								+ textproxyip.getText() + "; "
								+ textproxyport.getText());
				
				if (textlocalusername.getText().toString().isEmpty()
						|| textlocaldomain.getText().toString().isEmpty()
						|| textproxyip.getText().toString().isEmpty()
						|| textproxyport.getText().toString().isEmpty()) {
					Log.d(LOG_TAG, "Data empty");
					Toast.makeText(Register.this, "Register all data, please",
							Toast.LENGTH_LONG);
				} else {
					SharedPreferences settings = PreferenceManager
							.getDefaultSharedPreferences(getBaseContext());
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("LOCAL_USERNAME", textlocalusername
							.getText().toString());
					editor.putString("LOCAL_DOMAIN", textlocaldomain.getText()
							.toString());
					editor.putString("PROXY_IP", textproxyip.getText()
							.toString());
					editor.putString("PROXY_PORT", textproxyport.getText()
							.toString());
					editor.commit();
					Log.d(LOG_TAG, "All data ok");
					setResult(RESULT_OK);
					finish();
				}
			}
		});

		final Button cancelButton = (Button) findViewById(R.id.cancel_register);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}
	
	@Override
	protected void onPause() {
		Log.d(LOG_TAG, "onPause");
		super.onPause();
	}
		@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "onDestroy");
		super.onDestroy();
	}

}
