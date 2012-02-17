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
package com.kurento.kas.phone.softphone;

import java.util.ArrayList;

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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.media.MediaControlIncoming;
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.videocall.VideoCallService;

public class SoftPhoneService extends Service implements SoftphoneCallListener {
	private static final String LOG_TAG = "SoftPhoneService";

	private NotificationManager mNotificationMgr;
	private final static int NOTIF_SOFTPHONE = 1;
	private final static int NOTIF_VIDEOCALL = 1;

	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "KurentoPhone";
	private static final int IC_LEVEL_ORANGE = 0;

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
	public void callSetup(NetworkConnection networkConnection) {
		ApplicationContext.contextTable.put("networkConnection",
				networkConnection);

		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("finishActivity", "MEDIA_CONTROL_OUTGOING");
		msg.setData(b);
		handler.sendMessage(msg);

		videoCallIntent = new Intent(this, VideoCallService.class);
		videoCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Log.d(LOG_TAG, "Start Service " + videoCallIntent);
		startService(videoCallIntent);

	}

	@Override
	public void callTerminate() {
		Log.d(LOG_TAG, "callTerminate: " + videoCallIntent);
		if (videoCallIntent != null)
			stopService(videoCallIntent);
	}

	@Override
	public void callReject() {
		Log.d(LOG_TAG, "Call Reject Received or INVITE sent");
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("finishActivity", "MEDIA_CONTROL_OUTGOING");
		msg.setData(b);
		handler.sendMessage(msg);

		ApplicationContext.contextTable.remove("incomingCall");

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
