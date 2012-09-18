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
package com.kurento.kas.phone.videocall;

import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.kurento.commons.config.Parameters;
import com.kurento.commons.config.Value;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.softphone.R;
import com.kurento.kas.phone.softphone.ServiceUpdateUIListener;
import com.kurento.kas.phone.softphone.SoftPhone;
import com.kurento.mediaspec.Direction;
import com.kurento.mediaspec.MediaType;
import com.kurento.mscontrol.commons.MsControlException;
import com.kurento.mscontrol.commons.join.Joinable;
import com.kurento.mscontrol.kas.MediaSessionAndroid;
import com.kurento.mscontrol.kas.MsControlFactoryAndroid;
import com.kurento.mscontrol.kas.mediacomponent.MediaComponentAndroid;

public class VideoCallService extends Service {
	private final String LOG_TAG = VideoCallService.class.getName();

	MediaComponentAndroid audioPlayerComponent = null;
	MediaComponentAndroid audioRecorderComponent = null;

	private NotificationManager mNotificationMgr;
	private final static int NOTIF_VIDEOCALL = 2;
	private final static int NOTIF_SOFTPHONE = 1;

	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "VideoCall";
	private String notificationTitleSoft = "KurentoPhone";

	private Map<MediaType, Direction> callDirectionMap;
	private Intent videoCallIntent;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOG_TAG, "VideoCallService Create");

		mNotificationMgr = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotif = new Notification(R.drawable.ic_jog_dial_call,
				notificationTitle, System.currentTimeMillis());
		mNotif.flags |= Notification.FLAG_ONGOING_EVENT;

		notifIntent = new Intent(this, VideoCall.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitle, "",
				mNotifContentIntent);

		mNotificationMgr.notify(NOTIF_VIDEOCALL, mNotif);
		mNotificationMgr.cancel(NOTIF_SOFTPHONE);

		callDirectionMap = (Map<MediaType, Direction>) ApplicationContext.contextTable
				.get("callDirection");
		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");
		if (callDirectionMap == null) {
			Log.e(LOG_TAG, "direccion is NULL");
			return;
		}
		if (controller == null) {
			Log.e(LOG_TAG, "controller is NULL");
			return;
		}

		MediaSessionAndroid mediaSession = controller.getMediaSession();

		try {
			Direction audioMode = callDirectionMap.get(MediaType.AUDIO);

			if ((audioMode != null)
					&& (Direction.SENDONLY.equals(audioMode) || Direction.SENDRECV
							.equals(audioMode))) {
				ApplicationContext.contextTable.put("mute", false);
				audioPlayerComponent = mediaSession.createMediaComponent(
						MediaComponentAndroid.AUDIO_PLAYER,
						Parameters.NO_PARAMETERS);
				ApplicationContext.contextTable.put("audioPlayerComponent",
						audioPlayerComponent);
			}

			if ((audioMode != null)
					&& (Direction.RECVONLY.equals(audioMode) || Direction.SENDRECV
							.equals(audioMode))) {
				// speaker = false, audioRecorderComponent was created as
				// STREAM_MUSIC
				// speaker = true, audioRecorderComponent was created as
				// STREAM_VOICE_CALL
				ApplicationContext.contextTable.put("speaker", false);
				Parameters params = MsControlFactoryAndroid.createParameters();
				params.put(MediaComponentAndroid.STREAM_TYPE,
						new Value<Integer>(AudioManager.STREAM_MUSIC));
				audioRecorderComponent = mediaSession.createMediaComponent(
						MediaComponentAndroid.AUDIO_RECORDER, params);
				ApplicationContext.contextTable.put("audioRecorderComponent",
						audioRecorderComponent);
			}
		} catch (MsControlException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		Joinable audioJoinable = (Joinable) ApplicationContext.contextTable
				.get("audioJoinable");

		videoCallIntent = new Intent(this, VideoCall.class);
		videoCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(videoCallIntent);

		try {

			if (audioPlayerComponent != null) {
				audioPlayerComponent.join(Joinable.Direction.SEND,
						audioJoinable);
				audioPlayerComponent.start();
			}

			if (audioRecorderComponent != null) {
				audioRecorderComponent.join(Joinable.Direction.RECV,
						audioJoinable);
				audioRecorderComponent.start();
			}

		} catch (MsControlException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "On Destroy");

		mNotificationMgr.cancel(NOTIF_VIDEOCALL);
		mNotif = new Notification(R.drawable.icon, notificationTitleSoft,
				System.currentTimeMillis());

		notifIntent = new Intent(this, SoftPhone.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitleSoft, "",
				mNotifContentIntent);

		mNotificationMgr.notify(NOTIF_SOFTPHONE, mNotif);

		audioRecorderComponent = (MediaComponentAndroid) ApplicationContext.contextTable
				.get("audioRecorderComponent");
		audioPlayerComponent = (MediaComponentAndroid) ApplicationContext.contextTable
				.get("audioPlayerComponent");

		if (audioPlayerComponent != null)
			audioPlayerComponent.stop();

		if (audioRecorderComponent != null)
			audioRecorderComponent.stop();

		ApplicationContext.contextTable.remove("videoCall");

		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("Call", "Terminate");
		msg.setData(b);
		handler.sendMessage(msg);

		super.onDestroy();
	}

	public static ServiceUpdateUIListener UI_UPDATE_LISTENER_VIDEOCALL;

	public static void setUpdateListener(ServiceUpdateUIListener l) {
		UI_UPDATE_LISTENER_VIDEOCALL = l;
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(LOG_TAG, "Message = " + msg.getData());
			if (UI_UPDATE_LISTENER_VIDEOCALL != null)
				UI_UPDATE_LISTENER_VIDEOCALL.update(msg);
		}
	};

}
