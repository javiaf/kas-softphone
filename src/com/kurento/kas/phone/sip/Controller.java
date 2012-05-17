package com.kurento.kas.phone.sip;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.mscontrol.join.JoinableStream.StreamType;
import com.kurento.commons.sip.agent.EndPointFactory;
import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.agent.UaFactory;
import com.kurento.commons.sip.util.SipConfig;
import com.kurento.commons.ua.Call;
import com.kurento.commons.ua.CallListener;
import com.kurento.commons.ua.EndPoint;
import com.kurento.commons.ua.EndPointListener;
import com.kurento.commons.ua.UA;
import com.kurento.commons.ua.UaStun;
import com.kurento.commons.ua.event.CallEvent;
import com.kurento.commons.ua.event.CallEventEnum;
import com.kurento.commons.ua.event.EndPointEvent;
import com.kurento.commons.ua.exception.ServerInternalErrorException;
import com.kurento.commons.ua.timer.KurentoUaTimer;
import com.kurento.commons.ua.timer.KurentoUaTimerTask;
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

public class Controller implements EndPointListener, CallListener, IPhone,
		CallNotifier, KurentoUaTimer {
	public final static String LOG = Controller.class.getName();

	private Context context;
	private UaStun ua = null;
	private SipConfig sipConfig;
	private EndPoint endPoint = null;;
	private EndPointEvent pendingEndPointEvent;
	private Call currentCall;
	private Call incomingCall;
	private int expires = 3600;

	private Boolean isCall;

	private SoftphoneCallListener callListener;

	private InetAddress localAddress;
	private String username, password, domain, ipServer, stunHost, transport;
	private int portServer, localPort, stunPort;
	private boolean keep_alive;
	private long keep_delay;
	private MediaSessionAndroid mediaSession;
	private Map<String, String> connectionParams;
	private Map<String, String> stunParams;
	private Parameters mediaPrefereces;
	private Map<String, Object> mediaNetPreferences;

	public Controller(Context context) {
		this.context = context;
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

			sipConfig.setLocalPort(localPort);
			sipConfig.setProxyAddress(ipServer);
			sipConfig.setProxyPort(portServer);
			sipConfig.setStunServerAddress(stunHost);
			sipConfig.setStunServerPort(stunPort);
			sipConfig.setEnableKeepAlive(keep_alive);
			sipConfig.setKeepAlivePeriod(keep_delay);
			sipConfig.setTransport(transport);
			sipConfig.setTimer(this);

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
			endPoint = EndPointFactory.getInstance(username, domain, password,
					expires, ua, this);
		} catch (Exception e) {
			Log.e(LOG, e.getMessage(), e);
			return;
		}
		setIsCall(false);
	}

	private synchronized void setIsCall(Boolean isCall) {
		this.isCall = isCall;
	}

	private synchronized Boolean getIsCall() {
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
		if (ua != null)
			ua.reconfigure();
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
			setIsCall(true);
			incomingCall.accept();
		}
	}

	@Override
	public void reject() throws Exception {
		setIsCall(false);
		pendingEndPointEvent.getCallSource().reject();
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
		}
	}

	@Override
	public void hang() {
		setIsCall(false);
		if (currentCall != null)
			try {
				currentCall.hangup();
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
				currentCall.cancel();
			} catch (ServerInternalErrorException e) {
				Log.e(LOG, e.getMessage(), e);
			}
	}

	@Override
	public void onEvent(CallEvent event) {
		CallEventEnum eventType = event.getEventType();
		Log.d(LOG, "onEvent  SipCallEvent: " + eventType.toString());

		if (CallEvent.CALL_SETUP.equals(eventType)) {
			currentCall = event.getSource();
			setIsCall(true);
			Log.d(LOG, "Setting currentCall");
			if (callListener != null) {
				// ApplicationContext.contextTable.put("callDirection",
				// currentCall.getMediaTypesModes());

				ApplicationContext.contextTable.put("audioJoinable",
						currentCall.getJoinable(StreamType.audio));
				ApplicationContext.contextTable.put("videoJoinable",
						currentCall.getJoinable(StreamType.video));

				callListener.callSetup();
				Intent iOutgoingClose = new Intent();
				iOutgoingClose.setAction(Actions.OUTGOING_CALL_CLOSE);
				context.sendBroadcast(iOutgoingClose);
			}
		} else if (CallEvent.CALL_TERMINATE.equals(eventType)) {
			setIsCall(false);
			Log.d(LOG, "Call Terminate");
			if (callListener != null)
				callListener.callTerminate();
		} else if (CallEvent.CALL_REJECT.equals(eventType)) {
			setIsCall(false);
			Log.d(LOG, "Call Reject");
			Intent iOutgoingClose = new Intent();
			iOutgoingClose.setAction(Actions.CALL_REJECT);
			context.sendBroadcast(iOutgoingClose);
			// if (callListener != null)
			// callListener.callReject();
		} else if (CallEvent.CALL_CANCEL.equals(eventType)) {
			setIsCall(false);
			Log.d(LOG, "Call Cancel");
			if (callListener != null)
				callListener.callCancel();
		} else if (CallEvent.CALL_ERROR.equals(eventType)) {
			setIsCall(false);
			Intent iOutgoingClose = new Intent();
			iOutgoingClose.setAction(Actions.CALL_ERROR);
			context.sendBroadcast(iOutgoingClose);

			// Log.d(LOG, "Call Error");
			// if (callListener != null)
			// callListener.callReject();
		} else if (CallEvent.MEDIA_NOT_SUPPORTED.equals(eventType)) {

		} else if (CallEvent.MEDIA_RESOURCE_NOT_AVAILABLE.equals(eventType)) {

		}
	}

	@Override
	public void onEvent(EndPointEvent event) {
		Log.d(LOG, "OnEndPointEvent " + event.getEventType().toString());
		Intent i = new Intent();

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
			this.pendingEndPointEvent = event;
			incomingCall = pendingEndPointEvent.getCallSource();
			incomingCall.addListener(this);

			try {
				if (getIsCall()) {
					Log.d(LOG, "Send reject because I've received an INVITE");
					reject();
				} else {
					setIsCall(true);
					Log.d(LOG, "I've received an INVITE");
					Log.d(LOG, "Me llama Uri: "
							+ event.getCallSource().getRemoteUri());
					if (callListener != null)
						callListener.incomingCall(event.getCallSource()
								.getRemoteUri());
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

	// Implement KurentoTimerUA

	private Map<KurentoUaTimerTask, AlarmTask> taskTable = new HashMap<KurentoUaTimerTask, AlarmTask>();

	private class AlarmTask extends KurentoUaTimerTask {
		private KurentoUaTimerTask task;
		private long delay;
		private long period;
		private AlarmManager alarmManager;
		private Context context;
		private PendingIntent pendingIntent;

		protected AlarmTask(KurentoUaTimerTask task, long delay, long period,
				Context context) {
			this.task = task;
			this.delay = delay;
			this.period = period;
			this.context = context;

			Log.d(LOG, "New AlarmTask. Delay : " + delay + ". Period: "
					+ period);

			// TODO Change the SipEndPointImpl by EndPoint
			RegisterService.setEndpoint((SipEndPointImpl) endPoint);
			alarmManager = (AlarmManager) this.context
					.getSystemService(Context.ALARM_SERVICE);
			Intent myIntent = new Intent(this.context, RegisterService.class);
			pendingIntent = PendingIntent.getService(this.context, 0, myIntent,
					0);
		}

		@Override
		public void run() {
			Log.d(LOG, "Run AlarmTask");
			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					delay, period, pendingIntent);
			// // Start Alarm?
			// task.run();
		}

		public void cancel() {
			Log.d(LOG, "Cancel AlarmTask");
			if (alarmManager != null)
				alarmManager.cancel(pendingIntent);
		}
	}

	@Override
	public void cancel(KurentoUaTimerTask kurentoTask) {
		Log.d(LOG, "Cancel " + kurentoTask);
		AlarmTask task;
		if ((task = taskTable.get(kurentoTask)) != null) {
			Log.d(LOG, "task.cancel ");
			task.cancel();
		}
	}

	@Override
	public void purge() {
		// TODO Auto-generated method stub

	}

	@Override
	public void schedule(KurentoUaTimerTask kurentoTask, Date when, long period) {
		// TODO Auto-generated method stub

	}

	@Override
	public void schedule(KurentoUaTimerTask kurentoTask, long delay, long period) {
		AlarmTask task;
		Log.d(LOG, "Schedule. " + kurentoTask + ".Delay : " + delay
				+ ". Period: " + period);
		if ((task = taskTable.get(kurentoTask)) == null) {
			Log.d(LOG, "New kurentoTask");
			task = new AlarmTask(kurentoTask, 0, period, context);
			taskTable.put(kurentoTask, task);
		}
		task.run();
	}

	@Override
	public void schedule(KurentoUaTimerTask kurentoTask, Date when) {
		// TODO Auto-generated method stub

	}

	@Override
	public void schedule(KurentoUaTimerTask kurentoTask, long delay) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scheduleAtFixedRate(KurentoUaTimerTask kurentoTask, long delay,
			long period) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scheduleAtFixedRate(KurentoUaTimerTask kurentoTask, Date when,
			long period) {
		// TODO Auto-generated method stub

	}

}
