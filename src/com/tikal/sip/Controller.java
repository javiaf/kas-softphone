package com.tikal.sip;

import javax.media.mscontrol.join.Joinable.Direction;

import android.util.Log;

import com.tikal.media.AudioInfo;
import com.tikal.media.IRTPMedia;
import com.tikal.media.VideoInfo;
import com.tikal.networkconnection.NetworkConnectionFactoryImpl;
import com.tikal.networkconnection.NetworkIP;
import com.tikal.sip.agent.UaFactory;
import com.tikal.sip.event.SipCallEvent;
import com.tikal.sip.event.SipEndPointEvent;
import com.tikal.sip.event.SipEventType;
import com.tikal.sip.exception.ServerInternalErrorException;
import com.tikal.sip.util.SipConfig;
import com.tikal.softphone.IPhone;
import com.tikal.softphone.IPhoneGUI;

public class Controller implements SipEndPointListener, SipCallListener, IPhone {

	public final static String LOG_TAG = "CONTROLLER";

	private IPhoneGUI phoneGUI;
	private UA ua = null;
	private SipEndPoint endPoint = null;;
	private SipEndPointEvent pendingEndPointEvent;
	private SipCall currentCall;

	public UA getUa() {
		return ua;
	}

	public Controller(IPhoneGUI phoneGUI, IRTPMedia rtpMedia) {
		this.phoneGUI = phoneGUI;
		setUp(rtpMedia);
	}
	
	public Controller(IPhoneGUI phoneGUI, IRTPMedia rtpMedia, VideoInfo vi,
			AudioInfo ai, String proxyIP, int proxyPort, String localUser,
			String localRealm) throws Exception {
		this.phoneGUI = phoneGUI;
		setUp(rtpMedia);
		initUA(rtpMedia, vi, ai, proxyIP, proxyPort, localUser, localRealm);
	}

	private void setUp(IRTPMedia rtpMedia) {
		try {
			// PatternLayout layout = new PatternLayout("%d - %-5p - %m%n");
			// Logger logger = Logger.getRootLogger();
			// logger.addAppender(new ConsoleAppender(layout));
			// logger.setLevel(Level.TRACE);

			// UaFactory
			// .setNetworkConnectionFactory(new NetworkConnectionFactoryImpl(
			// rtpMedia));
		} catch (Exception e) {
			Log.e(LOG_TAG, "UaFactory initialization error", e);
		}
	}

	public void initUA(IRTPMedia rtpMedia, VideoInfo vi, AudioInfo ai,
			String proxyIP, int proxyPort, String localUser, String localRealm)
			throws Exception {
		UaFactory.setNetworkConnectionFactory(new NetworkConnectionFactoryImpl(
				rtpMedia, vi, ai));

		SipConfig config = new SipConfig();
		//TODO : CONTROLAR QUE LA WIFI ESTÉ APAGADA. POR SI DEVUELVE NULL EN getHostAddress()
		config.setLocalAddress(NetworkIP.getLocalAddress().getHostAddress());
		config.setLocalPort(6060);
		config.setProxyAddress(proxyIP);
		config.setProxyPort(proxyPort);

		Log.d(LOG_TAG, "CONFIGURATION User Agent: " + config);

		if (ua != null) {
			ua.terminate();
			Log.d(LOG_TAG, "UA Terminate");
		}
		// Log.d(LOG_TAG, "UA: " + ua.toString());

		ua = UaFactory.getInstance(config);

		register(localUser, localRealm);
	}

	public void finishUA() throws Exception {
		if (ua != null)
			ua.terminate();
		Log.d(LOG_TAG, "FinishUA");
	}

	private void register(String localUser, String localRealm) throws Exception {
		Log.d(LOG_TAG, "localUser: " + localUser + "; localReal: " + localRealm);
		endPoint = ua.registerEndPoint(localUser, localRealm, 3600, this);
	}

	@Override
	public void onEvent(SipEndPointEvent event) {
		SipEventType eventType = event.getEventType();
		Log.d(LOG_TAG, "onEvent  SipEndPointEvent: " + eventType.toString());

		if (SipEndPointEvent.INCOMING_CALL.equals(eventType)) {
			this.pendingEndPointEvent = event;
			try {

				Log.d(LOG_TAG, "Call Source: "
						+ event.getCallSource().toString());
				Log.d(LOG_TAG, "Me llama Uri: "
						+ event.getCallSource().getRemoteUri());

				phoneGUI.inviteReceived(event.getCallSource().getRemoteUri());

				// this.aceptCall();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString());
				e.printStackTrace();
			}
		}
		if (SipEndPointEvent.REGISTER_USER_SUCESSFUL.equals(eventType)) {
			this.pendingEndPointEvent = event;
			try {
				phoneGUI.registerSucessful();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString());
				e.printStackTrace();
			}
		}
		if (SipEndPointEvent.REGISTER_USER_FAIL.equals(eventType)) {
			this.pendingEndPointEvent = event;
			try {
				phoneGUI.registerFailed();
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
		} else if (SipCallEvent.CALL_TERMINATE.equals(eventType)) {
			Log.d(LOG_TAG, "Call Terminate");

		} else if (SipCallEvent.CALL_REJECT.equals(eventType)) {
			Log.d(LOG_TAG, "Call Reject");
			phoneGUI.rejectReceived();
		} else if (SipCallEvent.CALL_ERROR.equals(eventType)) {
			Log.d(LOG_TAG, "Call Error");
			
		}
	}

	@Override
	public void aceptCall() throws Exception {
		SipCall sipCall = pendingEndPointEvent.getCallSource();
		sipCall.addListener(this);
		sipCall.accept();
	}

	@Override
	public void reject() throws Exception {
		pendingEndPointEvent.getCallSource().reject();
	}

	@Override
	public void call(String remoteURI) throws Exception {
		Log.d(LOG_TAG, "calling...");
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

}
