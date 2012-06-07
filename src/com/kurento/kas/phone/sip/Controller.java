package com.kurento.kas.phone.sip;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.mscontrol.join.JoinableStream.StreamType;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.sip.agent.NetworkListener;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.agent.UaImpl;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.Call;
import com.kurento.commons.ua.CallListener;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.EndPointListener;
import com.kurento.commons.ua.TerminateReason;
import com.kurento.commons.ua.UA;
import com.kurento.commons.ua.UaStun;
import com.kurento.commons.ua.event.CallEvent;
import com.kurento.commons.ua.event.CallEventEnum;
import com.kurento.commons.ua.event.EndPointEvent;
import com.kurento.commons.ua.exception.ServerInternalErrorException;
import com.kurento.kas.mscontrol.MSControlFactory;
import com.kurento.kas.mscontrol.MediaSessionAndroid;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.network.NetworkIP;
import com.kurento.kas.phone.preferences.Connection_Preferences;
import com.kurento.kas.phone.preferences.Keys_Preferences;
import com.kurento.kas.phone.preferences.Video_Preferences;
import com.kurento.kas.phone.shared.Actions;
import com.kurento.kas.phone.softphone.CallNotifier;
import com.kurento.kas.phone.softphone.IPhone;
import com.kurento.kas.phone.softphone.SoftphoneCallListener;
import com.kurento.kas.phone.testutils.ControllerController;

public class Controller implements EndPointListener, CallListener, IPhone,
		CallNotifier {
	public final static String LOG = Controller.class.getName();

	private Context context;
	private UaStun ua = null;
	private SipConfig sipConfig;
	private EndPoint endPoint = null;;
	private EndPointEvent pendingEndPointEvent;
	private Call currentCall;
	private Call incomingCall;
	private int expires = 3600;

	private NetworkListener networkListener;

	private Boolean isCall = false;

	private SoftphoneCallListener callListener;

	private InetAddress localAddress;
	// Only use it to do test
	private String localAddressS = null;
	private String username, password, domain, ipServer, stunHost, transport;
	private int portServer, localPort, stunPort;
	private boolean keep_alive;
	private long keep_delay;
	private MediaSessionAndroid mediaSession;
	private Map<String, String> connectionParams;
	private Map<String, String> stunParams;
	private Parameters mediaPrefereces;
	private Map<String, Object> mediaNetPreferences;

	// Only use it to do tests
	private ControllerController controlController;

	// You must call configureController after
	public Controller(Context context) {
		this.context = context;
	}

	public void configureController() {
		reconfigureController();
		networkChanged();
	}

	public void reconfigureController() {
		try {
			this.localAddress = NetworkIP.getLocalAddress();

			this.mediaPrefereces = Video_Preferences
					.getMediaPreferences(context);

			this.mediaSession = MSControlFactory
					.createMediaSession(mediaPrefereces);

			this.mediaNetPreferences = Connection_Preferences
					.getConnectionNetPreferences(context);
			this.keep_alive = (Boolean) mediaNetPreferences
					.get(Keys_Preferences.MEDIA_NET_KEEP_ALIVE);
			this.keep_delay = (Long) (mediaNetPreferences
					.get(Keys_Preferences.MEDIA_NET_KEEP_DELAY));
			this.transport = (String) mediaNetPreferences
					.get(Keys_Preferences.MEDIA_NET_TRANSPORT);

			this.connectionParams = Connection_Preferences
					.getConnectionPreferences(this.context);
			if (connectionParams != null) {
				this.ipServer = connectionParams
						.get(Keys_Preferences.SIP_PROXY_IP);
				this.portServer = Integer.valueOf(connectionParams
						.get(Keys_Preferences.SIP_PROXY_PORT));
				this.username = connectionParams
						.get(Keys_Preferences.SIP_LOCAL_USERNAME);
				this.password = connectionParams
						.get(Keys_Preferences.SIP_LOCAL_PASSWORD);
				this.domain = connectionParams
						.get(Keys_Preferences.SIP_LOCAL_DOMAIN);
				this.localPort = Integer.valueOf(connectionParams
						.get(Keys_Preferences.SIP_MIN_LOCAL_PORT));

			}
			this.stunParams = Connection_Preferences
					.getStunPreferences(context);
			this.stunHost = stunParams.get(Keys_Preferences.STUN_HOST);
			this.stunPort = Integer.valueOf(stunParams
					.get(Keys_Preferences.STUN_HOST_PORT));

			Log.d(LOG, "All params its Ok");
			sipConfig = new SipConfig();

			// Only use it to do tests
			if (localAddressS != null)
				sipConfig.setLocalAddress(localAddressS);
			sipConfig.setLocalPort(localPort);
			sipConfig.setProxyAddress(ipServer);
			sipConfig.setProxyPort(portServer);
			sipConfig.setStunServerAddress(stunHost);
			sipConfig.setStunServerPort(stunPort);
			sipConfig.setEnableKeepAlive(keep_alive);
			sipConfig.setKeepAlivePeriod(keep_delay);
			sipConfig.setTransport(transport);
			sipConfig.setTimer(new AlarmUaTimer(context));

			UaFactory.setMediaSession(mediaSession);
		} catch (MsControlException e) {
			Log.e(LOG, e.getMessage(), e);
			return;
		} catch (Exception ex) {
			Log.e(LOG, ex.getMessage(), ex);
			return;
		}

		UA uaAux;
		try {
			uaAux = UaFactory.getInstance(sipConfig);

			if (uaAux instanceof UaStun)
				ua = (UaStun) uaAux;
			else
				return;
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			networkListener = UaFactory.getNetworkListener(ua);

			Map<String, Object> extra = new HashMap<String, Object>();
			extra.put(UaImpl.SIP_EXPIRES, expires);
			extra.put(UaImpl.SIP_PASWORD, password);
			endPoint = ua.registerEndpoint(username, domain, this, extra);
			// EndPointFactory.getInstance(username, domain, password,
			// expires, ua, this);
		} catch (Exception e) {
			Log.e(LOG, e.getMessage(), e);
			return;
		}
		setIsCall(false);
	}

	private synchronized void setIsCall(Boolean isCall) {
		this.isCall = isCall;
	}

	public synchronized Boolean getIsCall() {
		return this.isCall;
	}

	public UA getUa() {
		return ua;
	}

	public String getConnectionType() {
		try {
			return "Stun:\n" + ua.getConnectionType().toString();
		} catch (Exception e) {
			return "No STUN info";
		}
	}

	public void connectionHasChanged() {
		try {// TODO Review
			Intent i = new Intent();
			ApplicationContext.contextTable.put("isRegister", false);
			i.setAction(Actions.REGISTER_USER_FAIL);
			context.sendBroadcast(i);
			finishUA();
		} catch (Exception e) {
			Log.e(LOG, e.getMessage(), e);
			e.printStackTrace();
		}
		reconfigureController();
		networkChanged();
	}

	public void mediaHasChanged() {
		try {
			mediaSession = MSControlFactory
					.createMediaSession(Video_Preferences
							.getMediaPreferences(context));
			UaFactory.setMediaSession(mediaSession);
		} catch (MsControlException e) {
			Log.e(LOG, e.getMessage(), e);
		}
	}

	public void networkChanged() {
		if (networkListener != null)
			try {
				networkListener.networkReconfigure();
			} catch (ServerInternalErrorException e) {
				Log.e(LOG, "NetworkChanged: " + e.getMessage(), e);
			}
		// if (ua != null)
		// try {
		// ua.reconfigure();
		// } catch (ServerInternalErrorException e) {
		// e.printStackTrace();
		// }
	}

	public MediaSessionAndroid getMediaSession() {
		return mediaSession;
	}

	public boolean isRegister() {
		return (endPoint != null);
	}

	public void finishUA() throws Exception {
		if (ua != null)
			ua.terminate();
		Log.d(LOG, "FinishUA");
	}

	@Override
	public void addListener(SoftphoneCallListener listener) {
		callListener = listener;
	}

	@Override
	public void removeListener(SoftphoneCallListener listener) {
		callListener = null;
	}

	@Override
	public void aceptCall() throws Exception {
		if (incomingCall != null) {
			incomingCall.accept();
		}
	}

	@Override
	public void reject() throws Exception {
		setIsCall(false);
		pendingEndPointEvent.getCallSource().terminate(TerminateReason.DECLINE);
	}

	@Override
	public void call(String remoteURI) throws Exception {
		if (getIsCall()) {
			Log.d(LOG, "You cannot send an INVITE because exist a Call Active");
			callListener.callReject();
		} else {
			setIsCall(true);
			Log.d(LOG, "Send an INVITE to " + remoteURI);
			currentCall = endPoint.dial(remoteURI, this);
			Intent iOutgoingDial = new Intent();
			iOutgoingDial.setAction(Actions.CURRENT_CALL_OK);
			context.sendBroadcast(iOutgoingDial);
		}
	}

	@Override
	public void hang() {
		setIsCall(false);
		if (currentCall != null)
			try {
				currentCall.terminate();
			} catch (ServerInternalErrorException e) {
				Log.e(LOG, e.getMessage(), e);
			}
	}

	@Override
	public void cancel() {
		Log.d(LOG, "***Send a CANCEL");
		setIsCall(false);
		if (currentCall != null)
			try {
				currentCall.terminate();
			} catch (ServerInternalErrorException e) {
				Log.e(LOG, e.getMessage(), e);
			}
	}

	@Override
	public void onEvent(CallEvent event) {
		CallEventEnum eventType = event.getEventType();
		Log.d(LOG, "onEvent  SipCallEvent: " + eventType.toString()
				+ "; Message: " + event.getMessage());

		// Only use it to do tests
		if (controlController != null)
			controlController.onEvent(event);

		if (CallEvent.CALL_SETUP.equals(eventType)) {
			currentCall = event.getSource();
			setIsCall(true);
			Log.d(LOG, "Setting currentCall");
			if (callListener != null) {
				// ApplicationContext.contextTable.put("callDirection",
				// currentCall.getMediaTypesModes());

				NetworkConnection nc = currentCall.getNetworkConnection();
				ApplicationContext.contextTable.put("nc", nc);
				try {
					ApplicationContext.contextTable.put("audioJoinable",
							nc.getJoinableStream(StreamType.audio));
				} catch (MsControlException e) {
					// TODO: handle exception
				}
				try {
					ApplicationContext.contextTable.put("videoJoinable",
							nc.getJoinableStream(StreamType.video));
				} catch (MsControlException e) {
					// TODO: handle exception
				}
				ApplicationContext.contextTable.put("mediaTypesModes",
						currentCall.getMediaTypesModes());

				callListener.callSetup();
			}
		} else if (CallEvent.CALL_TERMINATE.equals(eventType)) {
			setIsCall(false);
			currentCall = null;

			Log.d(LOG, "Call Terminate");
			if (callListener != null)
				callListener.callTerminate();

			Intent iOutgoingClose = new Intent();
			iOutgoingClose.setAction(Actions.OUTGOING_CALL_CLOSE);
			context.sendBroadcast(iOutgoingClose);
		} else if (CallEvent.CALL_REJECT.equals(eventType)) {
			setIsCall(false);
			currentCall = null;
			Log.d(LOG, "Call Reject");
			Intent iOutgoingClose = new Intent();
			iOutgoingClose.setAction(Actions.CALL_REJECT);
			context.sendBroadcast(iOutgoingClose);
			// if (callListener != null)
			// callListener.callReject();
		} else if (CallEvent.CALL_BUSY.equals(eventType)) {
			setIsCall(false);
			currentCall = null;

			Log.d(LOG, "Call Busy");
			Intent iOutgoingClose = new Intent();
			iOutgoingClose.setAction(Actions.CALL_BUSY);
			context.sendBroadcast(iOutgoingClose);
		} else if (CallEvent.CALL_CANCEL.equals(eventType)) {
			setIsCall(false);
			currentCall = null;

			Log.d(LOG, "Call Cancel");
			Intent iIncomingCall = new Intent();
			iIncomingCall.setAction(Actions.CALL_CANCEL);
			context.sendBroadcast(iIncomingCall);
			// if (callListener != null)
			// callListener.callCancel();
		} else if (CallEvent.CALL_ERROR.equals(eventType)) {
			setIsCall(false);
			currentCall = null;

			Intent iOutgoingClose = new Intent();
			iOutgoingClose.setAction(Actions.CALL_ERROR);
			iOutgoingClose.putExtra("message", event.getMessage());
			context.sendBroadcast(iOutgoingClose);

			// Log.d(LOG, "Call Error");
			// if (callListener != null)
			// callListener.callReject();
		} else if (CallEvent.MEDIA_NOT_SUPPORTED.equals(eventType)) {
			Log.d(LOG, "Media Not Supported");
			setIsCall(false);
			currentCall = null;

			Intent iOutgoingClose = new Intent();
			iOutgoingClose.setAction(Actions.MEDIA_NOT_SUPPORTED);
			context.sendBroadcast(iOutgoingClose);

		} else if (CallEvent.USER_NOT_FOUND.equals(eventType)) {
			Log.d(LOG, "User Not Found");
			setIsCall(false);
			currentCall = null;

			Intent iOutgoingClose = new Intent();
			iOutgoingClose.setAction(Actions.USER_NOT_FOUND);
			context.sendBroadcast(iOutgoingClose);
		} else if (CallEvent.MEDIA_RESOURCE_NOT_AVAILABLE.equals(eventType)) {
			setIsCall(false);
			currentCall = null;
		} else if (CallEvent.CALL_RINGING.equals(eventType)) {
			Log.d(LOG, "Ringing ...");
			Intent iIncomingCall = new Intent();
			iIncomingCall.setAction(Actions.CALL_RINGING);
			context.sendBroadcast(iIncomingCall);
		} else
			setIsCall(false);
	}

	@Override
	public void onEvent(EndPointEvent event) {
		Log.d(LOG, "OnEndPointEvent " + event.getEventType().toString());
		Intent i = new Intent();

		// Only use it to do tests
		if (controlController != null)
			controlController.onEvent(event);

		if (event.getEventType().equals(EndPointEvent.REGISTER_USER_SUCESSFUL)) {
			ApplicationContext.contextTable.put("isRegister", true);
			i.setAction(Actions.REGISTER_USER_SUCESSFUL);
			context.sendBroadcast(i);
		} else if (event.getEventType()
				.equals(EndPointEvent.REGISTER_USER_FAIL)) {
			ApplicationContext.contextTable.put("isRegister", false);
			i.setAction(Actions.REGISTER_USER_FAIL);
			context.sendBroadcast(i);
		} else if (event.getEventType().equals(EndPointEvent.INCOMING_CALL)) {
			// this.pendingEndPointEvent = event;
			// incomingCall = pendingEndPointEvent.getCallSource();
			// incomingCall.addListener(this);
			try {
				synchronized (this) {
					if (getIsCall()) {
						Log.d(LOG,
								"Send reject because I've received an INVITE");
						event.getCallSource().terminate(TerminateReason.BUSY);
						// reject();
					} else {
						setIsCall(true);
						this.pendingEndPointEvent = event;
						incomingCall = pendingEndPointEvent.getCallSource();
						incomingCall.addListener(this);

						Log.d(LOG, "I've received an INVITE");
						Log.d(LOG, "Me llama Uri: "
								+ event.getCallSource().getRemoteUri());
						if (callListener != null)
							callListener.incomingCall(event.getCallSource()
									.getRemoteUri());
					}
				}
			} catch (Exception e) {
				Log.e(LOG, e.getMessage(), e);
			}
		} else if (event.getEventType().equals(
				EndPointEvent.UNREGISTER_USER_SUCESSFUL)) {
			i.setAction(Actions.UNREGISTER_USER_SUCESSFUL);
			context.sendBroadcast(i);
		} else if (event.getEventType().equals(
				EndPointEvent.UNREGISTER_USER_FAIL)) {
			i.setAction(Actions.UNREGISTER_USER_FAIL);
			context.sendBroadcast(i);
		}
	}

	// Only use it to do tests
	public void setControlController(ControllerController control) {
		controlController = control;
	}

	// Only use it to do test
	public void setLocalAddress(String localAddressTest) {
		this.localAddressS = localAddressTest;
	}

}
