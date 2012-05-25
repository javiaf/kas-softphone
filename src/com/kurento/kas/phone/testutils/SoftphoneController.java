package com.kurento.kas.phone.testutils;

import android.util.Log;

public class SoftphoneController {
	private static final String LOG = SoftphoneController.class.getName();

	private ActionsListener softphoneListener;

	public SoftphoneController(String id) {
		softphoneListener = new ActionsListener("SoftphoneController-" + id);
	}

	public String pollAction(int timeoutSec) throws InterruptedException {
		Log.d(LOG, "Wait pollAction for " + timeoutSec);
		return softphoneListener.poll(timeoutSec);
	}

	public void onEvent(String action) {
		Log.d(LOG, "onEvent -> Action: " + action);
		softphoneListener.onEvent(action);
	}
}
