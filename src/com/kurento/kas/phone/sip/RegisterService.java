package com.kurento.kas.phone.sip;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

public class RegisterService extends Service {

	private final static String LOG_TAG = RegisterService.class.getName();

	private static SipEndPointImpl endpoint;

	@Override
	public IBinder onBind(Intent intent) {
		// Bundle boundel = intent.getExtras();
		return null;
	}

	@Override
	public void onCreate() {
		Log.d(LOG_TAG, "on create method.");

	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(LOG_TAG, "On start method.");

		if (getEndpoint() != null) {
			int expiresTime = getEndpoint().getExpires();
			Log.d(LOG_TAG,
					"Sending register from Second service with expires time  "
							+ expiresTime);
			try {
				getEndpoint().register();
			} catch (ServerInternalErrorException e) {
				this.stopSelf();
			}
		}
		this.stopSelf();
	}

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "On destroy method.");
	}

	public static SipEndPointImpl getEndpoint() {
		return endpoint;
	}

	public static void setEndpoint(SipEndPointImpl endpoint) {
		RegisterService.endpoint = endpoint;
	}

}
