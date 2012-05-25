package com.kurento.kas.phone.testutils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.util.Log;

public class ActionsListener {

	private static final String LOG = ActionsListener.class.getName();

	private String id;
	private BlockingQueue<String> eventFifo = new LinkedBlockingQueue<String>();

	public ActionsListener(String id) {
		this.id = id;
	}

	// Listener Interface
	public void onEvent(String action) {
		Log.d(LOG, id + " - TEST FACILITY received an action:" + action);
		try {
			eventFifo.put(action);
		} catch (InterruptedException e) {
			Log.e(LOG, "Unable to insert action into FIFO", e);
		}
	}

	// TestInterface
	public String poll(int timeoutSec)
			throws InterruptedException {
		Log.d(LOG,
				id
						+ " - TEST FACILITY polling. Asking for new action. Wait until reception");
		String e = (String) eventFifo.poll(timeoutSec, TimeUnit.SECONDS);
		Log.e(LOG, id
				+ " - TEST FACILITY polling. Found new action, response: " + e);
		return e;
	}

}
