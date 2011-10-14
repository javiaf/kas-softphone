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
			int localPort, NetIF netIF, Map<MediaType, Mode> callDirectionMap,
			Integer maxBW, Integer maxFR, Integer gopSize,
			Integer maxQueueSize, String proxyIP, int proxyPort,
			String localUser, String localPassword, String localRealm,
			String stunHost, Integer stunPort) throws Exception {

		Parameters params = MSControlFactory.createParameters();
		params.put(MediaSessionAndroid.NET_IF, netIF);
		params.put(MediaSessionAndroid.LOCAL_ADDRESS, localAddress);
		params.put(MediaSessionAndroid.MAX_BANDWIDTH, maxBW);

		params.put(MediaSessionAndroid.STREAMS_MODES, callDirectionMap);
		params.put(MediaSessionAndroid.AUDIO_CODECS, audioCodecs);
		params.put(MediaSessionAndroid.VIDEO_CODECS, videoCodecs);

		params.put(MediaSessionAndroid.FRAME_HEIGHT, null);
		params.put(MediaSessionAndroid.FRAME_WIDTH, null);
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

		Log.d(LOG_TAG, "CONFIGURATION User Agent: " + sipConfig + " Stun :"
				+ stunHost + ":" + stunPort);

		if (ua != null) {
			ua.terminate();
			Log.d(LOG_TAG, "UA Terminate");
		}

		ua = UaFactory.getInstance(sipConfig);

		register(localUser, localPassword, localRealm);
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
				ApplicationContext.contextTable.put("callDirection",
						currentCall.getMediaTypesModes());
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
			if (callListener != null)
				callListener.callReject();
		}
	}

	@Override
	public void aceptCall() throws Exception {
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
