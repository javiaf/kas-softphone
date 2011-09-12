package com.kurento.kas.phone.softphone;

import java.util.ArrayList;

import com.kurento.kas.phone.softphone.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.media.MediaControlIncoming;
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.videocall.VideoCallService;
import com.kurento.commons.mscontrol.join.Joinable.Direction;

public class SoftPhoneService extends Service implements CallListener {
	private static final String LOG_TAG = "SoftPhoneService";

	private NotificationManager mNotificationMgr;
	private final static int NOTIF_SOFTPHONE = 1;
	private final static int NOTIF_VIDEOCALL = 1;

	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "KurentoPhone";
	private static final int IC_LEVEL_ORANGE = 0;
	/*
	 * private static final int IC_LEVEL_GREEN=1; private static final int
	 * IC_LEVEL_RED=2;
	 */

	private Intent videoCallIntent;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.d(LOG_TAG, "onCreate");

		mNotificationMgr = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotif = new Notification(R.drawable.icon, notificationTitle,
				System.currentTimeMillis());
		mNotif.iconLevel = IC_LEVEL_ORANGE;
		mNotif.flags |= Notification.FLAG_ONGOING_EVENT;

		notifIntent = new Intent(this, SoftPhone.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitle, "",
				mNotifContentIntent);

		mNotificationMgr.notify(NOTIF_SOFTPHONE, mNotif);

		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");

		if (controller != null)
			controller.addListener(this);
		else
			Log.e(LOG_TAG, "Controller is null, not addListener");

		Log.e(LOG_TAG, "onCreate OK");
	}

	// private void sendNotification(int level, String text) {
	// if (mNotif != null) {
	// // mNotif.iconLevel = level;
	// mNotif.when = System.currentTimeMillis();
	//
	// mNotif.setLatestEventInfo(this, notificationTitle, text,
	// mNotifContentIntent);
	// mNotificationMgr.notify(NOTIF_ID, mNotif);
	// Log.d(LOG_TAG, "sendNotification = " + text);
	// } else
	// Log.d(LOG_TAG, "mNotif == null");
	//
	// }

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "onDestroy");
		mNotificationMgr.cancel(NOTIF_SOFTPHONE);
		mNotificationMgr.cancel(NOTIF_VIDEOCALL);
		if (UI_UPDATE_LISTENERS != null)
			UI_UPDATE_LISTENERS.clear();
		try {
			stopService(videoCallIntent);
		} catch (Exception e) {
		}

	}

	Intent mediaIntent;

	@Override
	public void incomingCall(String uri) {
		Log.d(LOG_TAG, "Invite received");
		Intent mediaIntent = new Intent(this, MediaControlIncoming.class);
		mediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mediaIntent.putExtra("Uri", uri);
		startActivity(mediaIntent);
	}

	@Override
	public void registerUserSucessful() {
		Log.d(LOG_TAG, "Register Sucessful");
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("Register", "Sucessful");
		msg.setData(b);

		handler.sendMessage(msg);
	}

	@Override
	public void registerUserFailed() {
		Log.d(LOG_TAG, "Register Failed");
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("Register", "Failed");
		msg.setData(b);

		handler.sendMessage(msg);
	}

	@Override
	public void callSetup(NetworkConnection networkConnection,
			Direction direction) {
		ApplicationContext.contextTable.put("networkConnection",
				networkConnection);

		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("finishActivity", "MEDIA_CONTROL_OUTGOING");
		msg.setData(b);
		handler.sendMessage(msg);

		videoCallIntent = new Intent(this, VideoCallService.class);
		ApplicationContext.contextTable.put("callDirectionRemote", direction);
		videoCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Log.d(LOG_TAG, "Start Service " + videoCallIntent);
		startService(videoCallIntent);

	}

	@Override
	public void callTerminate() {
		Log.d(LOG_TAG, "callTerminate: " + videoCallIntent);
		stopService(videoCallIntent);
	}

	@Override
	public void callReject() {
		Log.d(LOG_TAG, "Call Reject Received");
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("finishActivity", "MEDIA_CONTROL_OUTGOING");
		msg.setData(b);
		handler.sendMessage(msg);
		msg = new Message();
		b = new Bundle();
		b.putString("Call", "Reject");
		msg.setData(b);
		handler.sendMessage(msg);
	}

	@Override
	public void callCancel() {
		Log.d(LOG_TAG, "Call Cancel Received");
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("Call", "Cancel");
		msg.setData(b);
		handler.sendMessage(msg);
	}

	public static ArrayList<ServiceUpdateUIListener> UI_UPDATE_LISTENERS = new ArrayList<ServiceUpdateUIListener>();

	public static void setUpdateListener(ServiceUpdateUIListener l) {
		if (!UI_UPDATE_LISTENERS.contains(l))
			UI_UPDATE_LISTENERS.add(l);
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(LOG_TAG, "Message = " + msg.getData());
			for (ServiceUpdateUIListener l : UI_UPDATE_LISTENERS) {
				l.update(msg);
			}
		}
	};

}
