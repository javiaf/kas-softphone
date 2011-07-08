package com.tikal.softphone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.tikal.media.AudioInfo;
import com.tikal.media.MediaControlIncoming;
import com.tikal.media.VideoInfo;
import com.tikal.sip.Controller;
import com.tikal.videocall.VideoCall;

public class MyService extends Service implements IPhoneGUI {
	private static final String LOG_TAG = "MyService";
	MediaPlayer player;
	private VideoInfo vi;
	private AudioInfo ai;
	private String localUser;
	private String localRealm;
	private String proxyIP;
	private int proxyPort;

	private static int MEDIA_CONTROL_INCOMING = 0;
	private static int MEDIA_CONTROL_OUTGOING = 1;
	private static int SHOW_PREFERENCES = 2;
	private static int VIDEO_CALL = 3;
	static final int PICK_CONTACT_REQUEST = 4;

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
	private static final int IC_LEVEL_OFFLINE = 3;

	public static void setUpdateListener(ServiceUpdateUIListener l) {
		UI_UPDATE_LISTENER = l;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		// Toast.makeText(this, "My Service Created", Toast.LENGTH_LONG).show();
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
		// Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
		Log.d(LOG_TAG, "onDestroy");
		mNotificationMgr.cancel(NOTIF_ID);
		// player.stop();
	}
	Intent mediaIntent;
	@Override
	public void onStart(Intent intent, int startid) {
		// Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();
		Log.d(LOG_TAG, "onStart");

		// initControllerUAFromSettings();
		//
		// if (controller == null)
		// register();
		//
		
//		mediaIntent = new Intent(this, MediaControlIncoming.class);
		
		

	}

	@Override
	public void inviteReceived(String uri) {
		Log.d(LOG_TAG, "Invite received");
		// Intent mediaIntent = new Intent(this,
		// MediaControlIncoming.class);
		// mediaIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		// mediaIntent.putExtra("Uri", uri);
		// startActivityForResult(mediaIntent, MEDIA_CONTROL_INCOMING);
		// Log.d(LOG_TAG, "Media Control Started");
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("Call", "Invite");
		b.putString("Contact", uri);
		msg.setData(b);

		handler.sendMessage(msg);

		// startActivity(new Intent().setClass(this, MediaControlIncoming.class)
		// .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("Uri", uri));
	}

	@Override
	public void rejectReceived() {
		Log.d(LOG_TAG, "Call Reject Received");
		// finishActivity(MEDIA_CONTROL_OUTGOING);
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("Call", "Reject");
		msg.setData(b);
		handler.sendMessage(msg);
	}

	@Override
	public void registerSucessful() {
		Log.d(LOG_TAG, "Register Sucessful");
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("Register", "Sucessful");
		msg.setData(b);

		handler.sendMessage(msg);

		// handler.post(new Runnable() {
		//
		// @Override
		// public void run() {
		// SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		// SoftPhone.this.text.setTextSize(20);
		// SoftPhone.this.text.setTextColor(Color.GREEN);
		// SoftPhone.this.text.setText("Register Sucessful");
		// }
		// });

	}

	@Override
	public void registerFailed() {
		Log.d(LOG_TAG, "Register Failed");
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("Register", "Failed");
		msg.setData(b);
		handler.sendMessage(msg);
		// handler.post(new Runnable() {
		//
		// @Override
		// public void run() {
		// SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		// SoftPhone.this.text.setTextSize(20);
		// SoftPhone.this.text.setTextColor(Color.RED);
		// SoftPhone.this.text.setText("Register Failed");
		// }
		// });

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
