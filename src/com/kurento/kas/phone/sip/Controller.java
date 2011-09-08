package com.kurento.kas.phone.sip;

import java.net.InetAddress;
import java.util.ArrayList;

import android.util.Log;

import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.sip.SipCall;
import com.kurento.commons.sip.SipCallListener;
import com.kurento.commons.sip.SipEndPoint;
import com.kurento.commons.sip.SipEndPointListener;
import com.kurento.commons.sip.UA;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.event.SipCallEvent;
import com.kurento.commons.sip.event.SipEndPointEvent;
import com.kurento.commons.sip.event.SipEventType;
import com.kurento.commons.sip.exception.ServerInternalErrorException;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.kas.media.AudioCodecType;
import com.kurento.kas.media.VideoCodecType;
import com.kurento.kas.mscontrol.MSControlFactory;
import com.kurento.kas.mscontrol.MediaSessionAndroid;
import com.kurento.kas.mscontrol.ParametersImpl;
import com.kurento.kas.mscontrol.networkconnection.ConnectionType;
import com.kurento.commons.mscontrol.join.Joinable.Direction;
import com.kurento.kas.phone.softphone.CallListener;
import com.kurento.kas.phone.softphone.CallNotifier;
import com.kurento.kas.phone.softphone.IPhone;

public class Controller implements SipEndPointListener, SipCallListener,
		IPhone, CallNotifier {

	public final static String LOG_TAG = "Controller";
	private UA ua = null;
	private SipEndPoint endPoint = null;;
	private SipEndPointEvent pendingEndPointEvent;
	private SipCall currentCall;
	private SipCall incomingCall;

	private CallListener callListener;

	private MediaSessionAndroid mediaSession;

	public UA getUa() {
		return ua;
	}

	public MediaSessionAndroid getMediaSession() {
		return mediaSession;
	}

	public void initUA(ArrayList<AudioCodecType> audioCodecs,
			ArrayList<VideoCodecType> videoCodecs, InetAddress localAddress,
			ConnectionType connectionType, String proxyIP, int proxyPort,
			String localUser, String localRealm) throws Exception {

		Parameters params = new ParametersImpl();
		params.put(MediaSessionAndroid.AUDIO_CODECS, audioCodecs);
		params.put(MediaSessionAndroid.VIDEO_CODECS, videoCodecs);
		params.put(MediaSessionAndroid.LOCAL_ADDRESS, localAddress);
		params.put(MediaSessionAndroid.CONNECTION_TYPE, connectionType);

		mediaSession = MSControlFactory.createMediaSession(params);
		Log.d(LOG_TAG, "mediaSession: " + this.mediaSession);
		UaFactory.setMediaSession(mediaSession);

		SipConfig sipConfig = new SipConfig();
		sipConfig.setLocalAddress(localAddress.getHostAddress());
		sipConfig.setLocalPort(6060);
		sipConfig.setProxyAddress(proxyIP);
		sipConfig.setProxyPort(proxyPort);

		Log.d(LOG_TAG, "CONFIGURATION User Agent: " + sipConfig);

		if (ua != null) {
			ua.terminate();
			Log.d(LOG_TAG, "UA Terminate");
		}

		ua = UaFactory.getInstance(sipConfig);

		register(localUser, localRealm);
	}

	public boolean isRegister() {
		return (endPoint != null);
	}

	public void finishUA() throws Exception {
		if (ua != null)
			ua.terminate();
		Log.d(LOG_TAG, "FinishUA");
	}

	private void register(String localUser, String localRealm) throws Exception {
		Log.d(LOG_TAG, "localUser: " + localUser + "; localReal: " + localRealm);
		endPoint = ua.registerEndPoint(localUser, localRealm, null, 20, this);
	}

	@Override
	public void onEvent(SipEndPointEvent event) {
		SipEventType eventType = event.getEventType();
		Log.d(LOG_TAG, "onEvent  SipEndPointEvent: " + eventType.toString());

		if (SipEndPointEvent.INCOMING_CALL.equals(eventType)) {
			this.pendingEndPointEvent = event;
			incomingCall = pendingEndPointEvent.getCallSource();
			incomingCall.addListener(this);
			try {

				Log.d(LOG_TAG, "Call Source: "
						+ event.getCallSource().toString());
				Log.d(LOG_TAG, "Me llama Uri: "
						+ event.getCallSource().getRemoteUri());
				if (callListener != null)
					callListener.incomingCall(event.getCallSource()
							.getRemoteUri());
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString());
				e.printStackTrace();
			}
		}
		if (SipEndPointEvent.REGISTER_USER_SUCESSFUL.equals(eventType)) {
			this.pendingEndPointEvent = event;
			try {
				if (callListener != null)
					callListener.registerUserSucessful();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString());
				e.printStackTrace();
			}
		}
		if (SipEndPointEvent.REGISTER_USER_FAIL.equals(eventType)) {
			this.pendingEndPointEvent = event;
			try {
				if (callListener != null)
					callListener.registerUserFailed();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString());
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onEvent(SipCallEvent event) {
		SipEventType eventType = event.getEventType();
		Log.d(LOG_TAG, "onEvent  SipCallEvent: " + eventType.toString());

		if (SipCallEvent.CALL_SETUP.equals(eventType)) {
			currentCall = event.getSource();
			Log.d(LOG_TAG, "Setting currentCall");
			if (callListener != null) {
				Log.d(LOG_TAG, "currentCall.getNetworkConnection(null): "
						+ currentCall.getNetworkConnection(null));
				callListener.callSetup(currentCall.getNetworkConnection(null));
			}
		} else if (SipCallEvent.CALL_TERMINATE.equals(eventType)) {
			Log.d(LOG_TAG, "Call Terminate");
			if (callListener != null)
				callListener.callTerminate();
		} else if (SipCallEvent.CALL_REJECT.equals(eventType)) {
			Log.d(LOG_TAG, "Call Reject");
			if (callListener != null)
				callListener.callReject();
		} else if (SipCallEvent.CALL_CANCEL.equals(eventType)) {
			Log.d(LOG_TAG, "Call Cancel");
			if (callListener != null)
				callListener.callCancel();
		} else if (SipCallEvent.CALL_ERROR.equals(eventType)) {
			Log.d(LOG_TAG, "Call Error");

		}
	}

	@Override
	public void aceptCall() throws Exception {
		// SipCall sipCall = pendingEndPointEvent.getCallSource();
		// sipCall.addListener(this);
		// sipCall.accept();
		if (incomingCall != null)
			incomingCall.accept();
	}

	@Override
	public void reject() throws Exception {
		pendingEndPointEvent.getCallSource().reject();
	}

	@Override
	public void call(String remoteURI) throws Exception {
		Log.d(LOG_TAG, "calling..." + remoteURI);
		currentCall = endPoint.dial(remoteURI, Direction.DUPLEX, this);
	}

	@Override
	public void hang() {
		Log.d(LOG_TAG, "hanging...");
		if (currentCall != null)
			try {
				currentCall.hangup();
			} catch (ServerInternalErrorException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void cancel() {
		Log.d(LOG_TAG, "canceling...");
		if (currentCall != null)
			try {
				currentCall.cancel();
			} catch (ServerInternalErrorException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void addListener(CallListener listener) {
		callListener = listener;
	}

	@Override
	public void removeListener(CallListener listener) {
		callListener = null;
	}

}
