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
package com.kurento.kas.phone.sip;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;

import javax.sql.rowset.spi.SyncResolver;

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
import com.kurento.kas.media.codecs.AudioCodecType;
import com.kurento.kas.media.codecs.VideoCodecType;
import com.kurento.kas.mscontrol.MSControlFactory;
import com.kurento.kas.mscontrol.MediaSessionAndroid;
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

	private Boolean isCall;

	private CallListener callListener;

	private MediaSessionAndroid mediaSession;

	private synchronized void setIsCall(Boolean isCall) {
		this.isCall = isCall;
		Log.w(LOG_TAG, "--- setIsCall -> " + this.isCall);
	}

	private synchronized Boolean getIsCall() {
		Log.w(LOG_TAG, "--- getIsCall -> " + this.isCall);
		return this.isCall;
	}

	public UA getUa() {
		return ua;
	}

	public MediaSessionAndroid getMediaSession() {
		return mediaSession;
	}

	public void initUA(ArrayList<AudioCodecType> audioCodecs,
			ArrayList<VideoCodecType> videoCodecs, InetAddress localAddress,
			Integer localPort, NetIF netIF,
			Map<MediaType, Mode> callDirectionMap, Integer maxBW,
			Integer maxFR, Integer gopSize, Integer maxQueueSize,
			Integer width, Integer height, String proxyIP, int proxyPort,
			String localUser, String localPassword, String localRealm,
			String stunHost, Integer stunPort) throws Exception {

		Boolean isInitUA = false;
		Boolean isStunOk = true;

		Parameters params = MSControlFactory.createParameters();
		params.put(MediaSessionAndroid.NET_IF, netIF);
		params.put(MediaSessionAndroid.LOCAL_ADDRESS, localAddress);
		params.put(MediaSessionAndroid.MAX_BANDWIDTH, maxBW);

		params.put(MediaSessionAndroid.STREAMS_MODES, callDirectionMap);
		params.put(MediaSessionAndroid.AUDIO_CODECS, audioCodecs);
		params.put(MediaSessionAndroid.VIDEO_CODECS, videoCodecs);

		params.put(MediaSessionAndroid.FRAME_WIDTH, width);
		params.put(MediaSessionAndroid.FRAME_HEIGHT, height);
		params.put(MediaSessionAndroid.MAX_FRAME_RATE, maxFR);
		params.put(MediaSessionAndroid.GOP_SIZE, gopSize);
		params.put(MediaSessionAndroid.FRAMES_QUEUE_SIZE, maxQueueSize);

		params.put(MediaSessionAndroid.STUN_HOST, stunHost);
		params.put(MediaSessionAndroid.STUN_PORT, stunPort);

		Log.d(LOG_TAG, "createMediaSession...");
		mediaSession = MSControlFactory.createMediaSession(params);
		Log.d(LOG_TAG, "mediaSession: " + this.mediaSession);
		UaFactory.setMediaSession(mediaSession);

		SipConfig sipConfig = new SipConfig();
		sipConfig.setLocalAddress(localAddress.getHostAddress());
		sipConfig.setLocalPort(localPort);
		sipConfig.setProxyAddress(proxyIP);
		sipConfig.setProxyPort(proxyPort);
		sipConfig.setStunAddress(stunHost);
		sipConfig.setStunPort(stunPort);

		ApplicationContext.contextTable.put("isStunOk", isStunOk);
		Integer trying = 0;
		while (!isInitUA) {
			try {
				if (ua != null) {
					ua.terminate();
					Log.d(LOG_TAG, "UA Terminate");
				}
				ua = UaFactory.getInstance(sipConfig);
				localPort = ua.getLocalPort();
				isInitUA = true;
				ApplicationContext.contextTable.put("localPort", localPort);
				Log.d(LOG_TAG, "CONFIGURATION User Agent: " + sipConfig);
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString() + ". Looking for a free port.");
				if (localPort != 0)
					localPort++;
				trying++;
				sipConfig.setLocalPort(localPort);
				ua = null;
				if ((trying > 5)
						|| (!e.toString().contains("Address already in use"))) {
					Log.e(LOG_TAG, "Break initUA");
					isStunOk = false;
					ApplicationContext.contextTable.put("isStunOk", isStunOk);
					return;
				}
			}
		}

		register(localUser, localPassword, localRealm);
		setIsCall(false);
	}

	public boolean isRegister() {
		return (endPoint != null);
	}

	public void finishUA() throws Exception {
		if (ua != null)
			ua.terminate();
		Log.d(LOG_TAG, "FinishUA");
	}

	private void register(String localUser, String localPassword,
			String localRealm) throws Exception {
		Log.d(LOG_TAG, "localUser: " + localUser + "; localReal: " + localRealm);
		endPoint = ua.registerEndPoint(localUser, localRealm, localPassword,
				3600, this);
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

				if (getIsCall()) {
					Log.d(LOG_TAG,
							"Send reject because I've received an INVITE");
					reject();
				} else {
					setIsCall(true);
					Log.d(LOG_TAG, "I've received an INVITE");
					Log.d(LOG_TAG, "Me llama Uri: "
							+ event.getCallSource().getRemoteUri());
					if (callListener != null)
						callListener.incomingCall(event.getCallSource()
								.getRemoteUri());
				}

				// String getCallSource = (String)
				// ApplicationContext.contextTable
				// .get("getCallSource");
				//
				// if (getCallSource == null) {
				// Log.d(LOG_TAG, "I've received an INVITE");
				// ApplicationContext.contextTable.put("getCallSource", event
				// .getCallSource().toString());
				// Log.d(LOG_TAG, "Me llama Uri: "
				// + event.getCallSource().getRemoteUri());
				// if (callListener != null)
				// callListener.incomingCall(event.getCallSource()
				// .getRemoteUri());
				// } else {
				// Log.d(LOG_TAG,
				// "Send reject because I've received an INVITE");
				// reject();
				// }
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString());
				e.printStackTrace();
			}
		}
		if (SipEndPointEvent.REGISTER_USER_SUCESSFUL.equals(eventType)) {
			this.pendingEndPointEvent = event;
			try {
				ApplicationContext.contextTable.put("isRegister", true);

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
				ApplicationContext.contextTable.put("isRegister", false);
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
			setIsCall(true);
			Log.d(LOG_TAG, "Setting currentCall");
			if (callListener != null) {
				ApplicationContext.contextTable.put("callDirection",
						currentCall.getMediaTypesModes());
				callListener.callSetup(currentCall.getNetworkConnection(null));
			}
		} else if (SipCallEvent.CALL_TERMINATE.equals(eventType)) {
			setIsCall(false);
			Log.d(LOG_TAG, "Call Terminate");
			if (callListener != null)
				callListener.callTerminate();
			// ApplicationContext.contextTable.remove("getCallSource");
		} else if (SipCallEvent.CALL_REJECT.equals(eventType)) {
			setIsCall(false);
			Log.d(LOG_TAG, "Call Reject");
			if (callListener != null)
				callListener.callReject();

			// ApplicationContext.contextTable.remove("getCallSource");
		} else if (SipCallEvent.CALL_CANCEL.equals(eventType)) {
			setIsCall(false);
			Log.d(LOG_TAG, "Call Cancel");
			if (callListener != null)
				callListener.callCancel();
			// ApplicationContext.contextTable.remove("getCallSource");
		} else if (SipCallEvent.CALL_ERROR.equals(eventType)) {
			setIsCall(false);
			Log.d(LOG_TAG, "Call Error");
			if (callListener != null)
				callListener.callReject();
			// ApplicationContext.contextTable.remove("getCallSource");
		} else if (SipCallEvent.MEDIA_NOT_SUPPORTED.equals(eventType)) {

		} else if (SipCallEvent.MEDIA_RESOURCE_NOT_AVAILABLE.equals(eventType)) {

		}
	}

	@Override
	public void aceptCall() throws Exception {
		if (incomingCall != null) {
			setIsCall(true);
			incomingCall.accept();
		}
	}

	@Override
	public void reject() throws Exception {
		Log.d(LOG_TAG, "***Send a REJECT");
		setIsCall(false);
		// ApplicationContext.contextTable.remove("getCallSource");
		pendingEndPointEvent.getCallSource().reject();
	}

	@Override
	public void call(String remoteURI) throws Exception {

		if (getIsCall()) {
			Log.d(LOG_TAG,
					"You cannot send an INVITE because exist a Call Active");
			callListener.callReject();// TODO: Review
		} else {
			setIsCall(true);
			Log.d(LOG_TAG, "***Send an INVITE to " + remoteURI);
			currentCall = endPoint.dial(remoteURI, this);

			// ApplicationContext.contextTable.put("getCallSource",
			// currentCall.toString());
		}
		// String getCallSource = (String) ApplicationContext.contextTable
		// .get("getCallSource");
		//
		// if (getCallSource == null) {
		// currentCall = endPoint.dial(remoteURI, this);
		// Log.d(LOG_TAG, "***Send an INVITE to " + remoteURI);
		// ApplicationContext.contextTable.put("getCallSource",
		// currentCall.toString());
		// } else {
		// Log.d(LOG_TAG,
		// "You cannot send an INVITE because exist a Call Active");
		// callListener.callReject();// TODO: Review
		// }
	}

	@Override
	public void hang() {
		Log.d(LOG_TAG, "***Send a HANG");
		// ApplicationContext.contextTable.remove("getCallSource");
		setIsCall(false);
		if (currentCall != null)
			try {
				currentCall.hangup();
			} catch (ServerInternalErrorException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void cancel() {
		Log.d(LOG_TAG, "***Send a CANCEL");
		// ApplicationContext.contextTable.remove("getCallSource");
		setIsCall(false);
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
