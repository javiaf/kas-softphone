package com.kurento.kas.phone.sip;

import java.awt.Dimension;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;

import android.util.Log;

import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.sdp.enums.MediaType;
import com.kurento.commons.sdp.enums.Mode;
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
import com.kurento.kas.mscontrol.networkconnection.NetIF;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
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
			NetIF netIF, Map<MediaType, Mode> callDirectionMap, Integer maxBW,
			Integer maxFR, Integer gopSize, Integer maxQueueSize,
			String proxyIP, int proxyPort, String localUser, String localRealm)
			throws Exception {

		Parameters params = new ParametersImpl();
		params.put(MediaSessionAndroid.NET_IF, netIF);
		params.put(MediaSessionAndroid.LOCAL_ADDRESS, localAddress);
		params.put(MediaSessionAndroid.MAX_BANDWIDTH, maxBW);

		params.put(MediaSessionAndroid.STREAMS_MODES, callDirectionMap);
		params.put(MediaSessionAndroid.AUDIO_CODECS, audioCodecs);
		params.put(MediaSessionAndroid.VIDEO_CODECS, videoCodecs);

		params.put(MediaSessionAndroid.FRAME_SIZE, null);
		params.put(MediaSessionAndroid.MAX_FRAME_RATE, maxFR);
		params.put(MediaSessionAndroid.GOP_SIZE, gopSize);
		params.put(MediaSessionAndroid.FRAMES_QUEUE_SIZE, maxQueueSize);

		Log.d(LOG_TAG, "createMediaSession...");
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
		endPoint = ua.registerEndPoint(localUser, localRealm, null, 3600, this);
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

				String cad = "";
				for (MediaType mediaType : event.getCallSource()
						.getMediaTypesModes().keySet()) {
					cad += mediaType
							+ "\t"
							+ event.getCallSource().getMediaTypesModes()
									.get(mediaType) + "\n";
				}
				Log.d(LOG_TAG, "cad: " + cad);

				ApplicationContext.contextTable.put("callDirection", event
						.getCallSource().getMediaTypesModes());

				String getCallSource = (String) ApplicationContext.contextTable
						.get("getCallSource");

				if (getCallSource == null) {
					ApplicationContext.contextTable.put("getCallSource", event
							.getCallSource().toString());
					Log.d(LOG_TAG, "Me llama Uri: "
							+ event.getCallSource().getRemoteUri());
					if (callListener != null)
						callListener.incomingCall(event.getCallSource()
								.getRemoteUri());
				} else {
					reject();
				}
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
		if (SipEndPointEvent.REGISTER_USER_FAIL.equals(eventType)
				|| SipEndPointEvent.REGISTER_USER_NOT_FOUND.equals(eventType)
				|| SipEndPointEvent.SERVER_INTERNAL_ERROR.equals(eventType)) {
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
		ApplicationContext.contextTable.remove("getCallSource");
		pendingEndPointEvent.getCallSource().reject();
	}

	@Override
	public void call(String remoteURI) throws Exception {
		Log.d(LOG_TAG, "calling..." + remoteURI);
		currentCall = endPoint.dial(remoteURI, this);
	}

	@Override
	public void hang() {
		Log.d(LOG_TAG, "hanging...");
		if (currentCall != null)
			try {
				ApplicationContext.contextTable.remove("getCallSource");
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
				ApplicationContext.contextTable.remove("getCallSource");
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
