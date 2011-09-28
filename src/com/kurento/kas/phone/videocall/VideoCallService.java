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

import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.mscontrol.join.Joinable.Direction;
import com.kurento.commons.mscontrol.join.JoinableStream.StreamType;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.sdp.enums.MediaType;
import com.kurento.commons.sdp.enums.Mode;
import com.kurento.kas.mscontrol.MSControlFactory;
import com.kurento.kas.mscontrol.MediaSessionAndroid;
import com.kurento.kas.mscontrol.mediacomponent.MediaComponentAndroid;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.softphone.R;
import com.kurento.kas.phone.softphone.ServiceUpdateUIListener;
import com.kurento.kas.phone.softphone.SoftPhone;

public class VideoCallService extends Service {
	private final String LOG_TAG = "VideoCallService";

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

	private Map<MediaType, Mode> callDirectionMap;
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

		mNotif = new Notification(R.drawable.ic_jog_dial_answer,
				notificationTitle, System.currentTimeMillis());
		mNotif.flags |= Notification.FLAG_ONGOING_EVENT;

		notifIntent = new Intent(this, VideoCall.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitle, "",
				mNotifContentIntent);

		mNotificationMgr.notify(NOTIF_VIDEOCALL, mNotif);
		mNotificationMgr.cancel(NOTIF_SOFTPHONE);

		callDirectionMap = (Map<MediaType, Mode>) ApplicationContext.contextTable
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
			Mode audioMode = callDirectionMap.get(MediaType.AUDIO);

			if ((audioMode != null)
					&& (Mode.SENDONLY.equals(audioMode) || Mode.SENDRECV
							.equals(audioMode))) {
				audioPlayerComponent = mediaSession.createMediaComponent(
						MediaComponentAndroid.AUDIO_PLAYER,
						Parameters.NO_PARAMETER);
				ApplicationContext.contextTable.put("audioPlayerComponent",
						audioPlayerComponent);
			}

			if ((audioMode != null)
					&& (Mode.RECVONLY.equals(audioMode) || Mode.SENDRECV
							.equals(audioMode))) {
				Parameters params = MSControlFactory.createParameters();
				params.put(MediaComponentAndroid.STREAM_TYPE,
						AudioManager.STREAM_MUSIC);
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

		NetworkConnection nc = (NetworkConnection) ApplicationContext.contextTable
				.get("networkConnection");
		if (nc == null) {
			Log.e(LOG_TAG, "networkConnection is NULL");
			return;
		}

		try {

			if (audioPlayerComponent != null) {
				audioPlayerComponent.join(Direction.SEND,
						nc.getJoinableStream(StreamType.audio));
				audioPlayerComponent.start();
			}

			if (audioRecorderComponent != null) {
				audioRecorderComponent.join(Direction.RECV,
						nc.getJoinableStream(StreamType.audio));
				audioRecorderComponent.start();
			}

		} catch (MsControlException e) {
			e.printStackTrace();
		}

		videoCallIntent = new Intent(this, VideoCall.class);
		videoCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(videoCallIntent);
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

		if (audioPlayerComponent != null)
			audioPlayerComponent.stop();

		if (audioRecorderComponent != null)
			audioRecorderComponent.stop();

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
			UI_UPDATE_LISTENER_VIDEOCALL.update(msg);
		}
	};

}
