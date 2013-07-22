package com.kurento.kas.phone.sip;

import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kurento.kas.call.Call;
import com.kurento.kas.call.CallDialingHandler;
import com.kurento.kas.call.CallEstablishedHandler;
import com.kurento.kas.call.CallRingingHandler;
import com.kurento.kas.call.CallTerminatedHandler;
import com.kurento.kas.call.DialingCall;
import com.kurento.kas.call.EstablishedCall;
import com.kurento.kas.call.RingingCall;
import com.kurento.kas.call.RingingCall.RejectCode;
import com.kurento.kas.call.TerminatedCall;
import com.kurento.kas.conference.Conference;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.preferences.Connection_Preferences;
import com.kurento.kas.phone.preferences.Keys_Preferences;
import com.kurento.kas.phone.shared.Actions;
import com.kurento.kas.phone.softphone.CallNotifier;
import com.kurento.kas.phone.softphone.IPhone;
import com.kurento.kas.phone.softphone.SoftphoneCallListener;
import com.kurento.kas.sip.ua.KurentoSipException;
import com.kurento.kas.sip.ua.SipUA;
import com.kurento.kas.ua.ErrorHandler;
import com.kurento.kas.ua.KurentoException;
import com.kurento.kas.ua.Register;
import com.kurento.kas.ua.RegisterHandler;
import com.kurento.kas.ua.UA;

public class Controller implements IPhone, CallNotifier {
	public final static String LOG_TAG = Controller.class.getSimpleName();

	private Context context;
	private UA ua = null;
	private Register reg;

	private DialingCall dialingCall;
	private RingingCall incomingCall;
	private EstablishedCall establishedCall;
	private int expires = 3600;

	private Boolean isCall = false;

	private SoftphoneCallListener callListener;

	// You must call configureController after
	public Controller(Context context) {
		this.context = context;
	}

	public void configureController() {
		reconfigureController();
	}

	public void reconfigureController() {
		try {
			ua = new SipUA(context);
			addHandlers();

			Map<String, String> connectionParams = Connection_Preferences
					.getConnectionPreferences(this.context);
			String username = connectionParams
					.get(Keys_Preferences.SIP_LOCAL_USERNAME);
			String domain = connectionParams
					.get(Keys_Preferences.SIP_LOCAL_DOMAIN);
			reg = new Register("sip:" + username + "@" + domain);
			ua.register(reg);
		} catch (KurentoSipException e) {
			Log.e(LOG_TAG, "Exception: " + e.getMessage(), e);
			return;
		} catch (KurentoException e) {
			Log.e(LOG_TAG, "Exception: " + e.getMessage(), e);
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

	public boolean isRegister() {
		return true; // TODO: complete
	}

	public void finishUA() throws Exception {
		if (ua != null)
			ua.terminate();
		Log.d(LOG_TAG, "FinishUA");
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
		incomingCall.reject(RejectCode.DECLINE);
	}

	@Override
	public void call(String remoteURI) throws Exception {
		if (getIsCall()) {
			Log.d(LOG_TAG,
					"You cannot send an INVITE because exist a Call Active");
			callListener.callReject();
		} else {
			setIsCall(true);
			Log.d(LOG_TAG, "Send an INVITE to " + remoteURI);
			dialingCall = ua.dial(reg.getUri(), remoteURI);
			Intent iOutgoingDial = new Intent();
			iOutgoingDial.setAction(Actions.CURRENT_CALL_OK);
			context.sendBroadcast(iOutgoingDial);
		}
	}

	@Override
	public void hang() {
		setIsCall(false);
		if (establishedCall != null)
			establishedCall.hangup();
		if (callListener != null)
			callListener.callTerminate();
	}

	@Override
	public void cancel() {
		Log.d(LOG_TAG, "***Send a CANCEL");
		setIsCall(false);
		if (dialingCall != null)
			dialingCall.cancel();
	}

	private void addHandlers() {
		ua.setErrorHandler(new ErrorHandler() {

			@Override
			public void onUAError(UA ua, KurentoException exception) {
				Log.i(LOG_TAG, "onUAError");
			}

			@Override
			public void onConfError(Conference conference,
					KurentoException exception) {
				Log.i(LOG_TAG, "onConfError");
			}

			@Override
			public void onCallError(Call call, KurentoException exception) {
				Log.i(LOG_TAG, "onCallError");
				setIsCall(false);
				establishedCall = null;

				Intent iOutgoingClose = new Intent();
				iOutgoingClose.setAction(Actions.OUTGOING_CALL_CLOSE);
				context.sendBroadcast(iOutgoingClose);
			}
		});

		ua.setRegisterHandler(new RegisterHandler() {

			@Override
			public void onUserOnline(Register register) {
				Log.i(LOG_TAG, "onUserOnline");
				Intent i = new Intent();
				ApplicationContext.contextTable.put("isRegister", true);
				i.setAction(Actions.REGISTER_USER_SUCESSFUL);
				context.sendBroadcast(i);
			}

			@Override
			public void onUserOffline(Register register) {
				Log.i(LOG_TAG, "onUserOffline");
				Intent i = new Intent();
				ApplicationContext.contextTable.put("isRegister", false);
				i.setAction(Actions.REGISTER_USER_SUCESSFUL);
				context.sendBroadcast(i);
			}

			@Override
			public void onAuthenticationFailure(Register register) {
				Log.i(LOG_TAG, "onAuthenticationFailure");
				Intent i = new Intent();
				ApplicationContext.contextTable.put("isRegister", false);
				i.setAction(Actions.REGISTER_USER_FAIL);
				context.sendBroadcast(i);
			}

			@Override
			public void onRegisterError(Register register,
					KurentoException exception) {
				Log.i(LOG_TAG, "onRegisterError");
				Intent i = new Intent();
				ApplicationContext.contextTable.put("isRegister", false);
				i.setAction(Actions.REGISTER_USER_FAIL);
				context.sendBroadcast(i);
			}
		});

		ua.setCallDialingHandler(new CallDialingHandler() {

			@Override
			public void onRemoteRinging(DialingCall dialingCall) {
				Log.i(LOG_TAG, "onRemoteRinging");
				Intent iIncomingCall = new Intent();
				iIncomingCall.setAction(Actions.CALL_RINGING);
				context.sendBroadcast(iIncomingCall);
			}
		});

		ua.setCallEstablishedHandler(new CallEstablishedHandler() {

			@Override
			public void onEstablished(EstablishedCall call) {
				Log.i(LOG_TAG, "onEstablished");
				setIsCall(true);
				establishedCall = call;
				Log.d(LOG_TAG, "Setting currentCall");
				if (callListener != null) {
					ApplicationContext.contextTable.put("call", call);
					callListener.callSetup();
				}
			}
		});

		ua.setCallRingingHandler(new CallRingingHandler() {

			@Override
			public void onRinging(RingingCall ringinCall) {
				Log.i(LOG_TAG, "onRinging");
				synchronized (this) {
					if (getIsCall()) {
						Log.d(LOG_TAG,
								"Send reject because I've received an INVITE");
						ringinCall.reject(RejectCode.BUSY);
					} else {
						setIsCall(true);
						incomingCall = ringinCall;
						if (callListener != null)
							callListener.incomingCall(ringinCall.getRemoteUri());
					}
				}
			}
		});

		ua.setCallTerminatedHander(new CallTerminatedHandler() {

			@Override
			public void onTerminated(TerminatedCall terminatedCall) {
				Log.i(LOG_TAG, "onTerminate");
				setIsCall(false);
				establishedCall = null;

				if (callListener != null)
					callListener.callTerminate();

				Intent iOutgoingClose = new Intent();
				iOutgoingClose.setAction(Actions.OUTGOING_CALL_CLOSE);
				context.sendBroadcast(iOutgoingClose);
			}
		});
	}

}
