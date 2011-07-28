package com.tikal.videocall;

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

import com.tikal.android.mscontrol.ParametersImpl;
import com.tikal.android.mscontrol.mediacomponent.AudioRecorderComponent;
import com.tikal.android.mscontrol.mediacomponent.MediaComponentAndroid;
import com.tikal.applicationcontext.ApplicationContext;
import com.tikal.mscontrol.MsControlException;
import com.tikal.mscontrol.MediaSession;
import com.tikal.mscontrol.Parameters;
import com.tikal.mscontrol.join.Joinable.Direction;
import com.tikal.mscontrol.join.JoinableStream.StreamType;
import com.tikal.mscontrol.mediacomponent.MediaComponent;
import com.tikal.mscontrol.networkconnection.NetworkConnection;
import com.tikal.sip.Controller;
import com.tikal.softphone.R;
import com.tikal.softphone.ServiceUpdateUIListener;
import com.tikal.softphone.SoftPhone;

public class VideoCallService extends Service {
	private final String LOG_TAG = "VideoCallService";

	MediaComponent audioPlayerComponent = null;
	MediaComponent audioRecorderComponent = null;
	

	private NotificationManager mNotificationMgr;
	private final static int NOTIF_VIDEOCALL = 2;
	private final static int NOTIF_SOFTPHONE = 1;
	
	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "VideoCall";
	
	private Intent videoCallIntent;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		mNotificationMgr = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotif = new Notification(R.drawable.ic_jog_dial_answer, notificationTitle,
				System.currentTimeMillis());
		mNotif.flags |= Notification.FLAG_ONGOING_EVENT;

		notifIntent = new Intent(this, VideoCall.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitle, "",
				mNotifContentIntent);

		mNotificationMgr.notify(NOTIF_VIDEOCALL, mNotif);
		mNotificationMgr.cancel(NOTIF_SOFTPHONE);
		
		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");
		if (controller == null) {
			Log.e(LOG_TAG, "controller is NULL");
			return;
		}
		MediaSession mediaSession = controller.getMediaSession();

		try {
			audioPlayerComponent = mediaSession.createMediaComponent(MediaComponentAndroid.AUDIO_PLAYER, Parameters.NO_PARAMETER);
			
			Parameters params = new ParametersImpl();
			params.put(AudioRecorderComponent.STREAM_TYPE, AudioManager.STREAM_MUSIC);
			audioRecorderComponent = mediaSession.createMediaComponent(MediaComponentAndroid.AUDIO_RECORDER, params);
		} catch (MsControlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ApplicationContext.contextTable.put("audioPlayerComponent", audioPlayerComponent);
		ApplicationContext.contextTable.put("audioRecorderComponent", audioRecorderComponent);
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
			// TODO Auto-generated catch block
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
		mNotif = new Notification(R.drawable.icon, notificationTitle,
				System.currentTimeMillis());
		
		notifIntent = new Intent(this, SoftPhone.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitle, "",
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
