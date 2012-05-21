package com.kurento.kas.phone.testutils;

import android.util.Log;

import com.kurento.commons.ua.CallListener;
import com.kurento.commons.ua.EndPointListener;
import com.kurento.commons.ua.event.CallEvent;
import com.kurento.commons.ua.event.EndPointEvent;

public class ControllerController implements EndPointListener, CallListener {
	private static final String LOG = ControllerController.class.getName();

	private EventListener sipEndPointListener;

	public ControllerController(String id) {
		sipEndPointListener = new EventListener("ControllerController-" + id);
	}

	@Override
	public void onEvent(CallEvent event) {
		Log.d(LOG, "onEvent -> CallEvent: " + event);
		sipEndPointListener.onEvent(event);
	}

	public CallEvent pollCallEvent(int timeoutSec) throws InterruptedException {
		Log.d(LOG, "Wait pollCallEventEvent for " + timeoutSec);
		return sipEndPointListener.poll(timeoutSec);
	}

	@Override
	public void onEvent(EndPointEvent event) {
		Log.d(LOG, "onEvent -> EndPointEvent: " + event);
		sipEndPointListener.onEvent(event);
	}

	public EndPointEvent pollEndPointEvent(int timeoutSec)
			throws InterruptedException {
		Log.d(LOG, "Wait pollEndPointEvent for " + timeoutSec);
		return sipEndPointListener.poll(timeoutSec);
	}

}
