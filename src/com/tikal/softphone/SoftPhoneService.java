package com.tikal.softphone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.tikal.applicationcontext.ApplicationContext;
import com.tikal.media.AudioInfo;
import com.tikal.media.MediaControlIncoming;
import com.tikal.media.VideoInfo;
import com.tikal.sip.Controller;
import com.tikal.videocall.VideoCallService;

public class SoftPhoneService extends Service implements CallListener {
	private static final String LOG_TAG = "SoftPhoneService";

	private NotificationManager mNotificationMgr;
	private final static int NOTIF_ID = 1;

	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "Softphone";
	private static final int IC_LEVEL_ORANGE = 0;
	/*
	 * private static final int IC_LEVEL_GREEN=1; private static final int
	 * IC_LEVEL_RED=2;
	 */

	private Intent videoCallIntent;

	public static void setUpdateListener(ServiceUpdateUIListener l) {
		UI_UPDATE_LISTENER = l;
	}

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

		mNotificationMgr.notify(NOTIF_ID, mNotif);

		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");

		if (controller != null)
			controller.addListener(this);
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
		mNotificationMgr.cancel(NOTIF_ID);
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
	public void callSetup() {
		Log.d(LOG_TAG, "startRTPMedia");
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("finishActivity", "MEDIA_CONTROL_OUTGOING");
		msg.setData(b);
		handler.sendMessage(msg);

		videoCallIntent = new Intent(this, VideoCallService.class);
		videoCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startService(videoCallIntent);
	}

	@Override
	public void callTerminate() {
		Log.d(LOG_TAG, "callTerminate");
		stopService(videoCallIntent);
	}

	@Override
	public void callReject() {
		Log.d(LOG_TAG, "Call Reject Received");
		// finishActivity(MEDIA_CONTROL_OUTGOING);
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("Call", "Reject");
		msg.setData(b);
		handler.sendMessage(msg);
	}

	public static ServiceUpdateUIListener UI_UPDATE_LISTENER;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(LOG_TAG, "Message = " + msg.getData());
			UI_UPDATE_LISTENER.update(msg);

		}
	};

}
