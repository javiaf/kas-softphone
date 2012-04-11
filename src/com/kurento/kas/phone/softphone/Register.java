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

import com.kurento.kas.phone.preferences.Keys_Preferences;

public class Register extends Activity {
	private static final String LOG_TAG = Register.class.getName();

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
				EditText textlocalpassword = (EditText) findViewById(R.id.Textpreference_local_password);
				EditText textlocaldomain = (EditText) findViewById(R.id.Textpreference_local_domain);
				EditText textproxyip = (EditText) findViewById(R.id.Textpreference_proxy_ip);
				EditText textproxyport = (EditText) findViewById(R.id.Textpreference_proxy_port);
				Log.d(LOG_TAG,
						textlocalusername.getText() + "; "
								+ textlocaldomain.getText() + "; "
								+ textproxyip.getText() + "; "
								+ textproxyport.getText());

				if (textlocalusername.getText().toString().length() == 0
						|| textlocaldomain.getText().toString().length() == 0
						|| textproxyip.getText().toString().length() == 0
						|| textproxyport.getText().toString().length() == 0) {
					Log.d(LOG_TAG, "Data empty");
					Toast.makeText(Register.this, "Register all data, please",
							Toast.LENGTH_LONG);
				} else {
					SharedPreferences settings = PreferenceManager
							.getDefaultSharedPreferences(getBaseContext());
					SharedPreferences.Editor editor = settings.edit();
					editor.putString(Keys_Preferences.SIP_LOCAL_USERNAME,
							textlocalusername
							.getText().toString());
					editor.putString(Keys_Preferences.SIP_LOCAL_PASSWORD,
							textlocalpassword
							.getText().toString());
					editor.putString(Keys_Preferences.SIP_LOCAL_DOMAIN,
							textlocaldomain.getText()
							.toString());
					editor.putString(Keys_Preferences.SIP_PROXY_IP, textproxyip.getText()
							.toString());
					editor.putString(Keys_Preferences.SIP_PROXY_PORT,
							textproxyport.getText()
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
