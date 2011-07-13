package com.tikal.videocall;

import java.net.URI;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.resource.RTC;

import com.tikal.applicationcontext.ApplicationContext;
import com.tikal.javax.media.mscontrol.mediagroup.AudioMediaGroup;
import com.tikal.javax.media.mscontrol.mediagroup.AudioRecorder;
import com.tikal.javax.media.mscontrol.mediagroup.VideoMediaGroup;
import com.tikal.javax.media.mscontrol.networkconnection.NetworkConnectionImpl;
import com.tikal.media.AudioInfo;
import com.tikal.media.VideoInfo;
import com.tikal.sip.Controller;
import com.tikal.softphone.R;
import com.tikal.softphone.SoftPhone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

public class VideoCallService extends Service {
	private final String LOG_TAG = "VideoCallService";

	AudioMediaGroup audioMediaGroup = null;
	VideoMediaGroup videoMediaGroup = null;
	

	private NotificationManager mNotificationMgr;
	private final static int NOTIF_VIDEOCALL = 2;
	private final static int NOTIF_SOFTPHONE = 1;
	
	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "VideoCall";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// Crear mediagroups y registralos en el context

		/* Notification Video Call Active */

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

		// FIXME controller != null
		NetworkConnectionImpl nc = controller.getNetworkConnection();

		VideoInfo vi = nc.getVideoInfo();
		AudioInfo ai = nc.getAudioInfo();

		String sdpVideo = nc.getSdpVideo();
		String sdpAudio = nc.getSdpAudio();

		// if (!sdpAudio.equals("")) {
		try {
			audioMediaGroup = new AudioMediaGroup(
					MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR,
					ai.getSample_rate(), ai.getFrameSize(), AudioManager.STREAM_MUSIC);
			
		} catch (MsControlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// }
//		Log.d(LOG_TAG, "sdpVideo:\n" + sdpVideo);
		// if (!sdpVideo.equals("")) {
		Log.d(LOG_TAG, "create videoMediaGroup");
		try {
			videoMediaGroup = new VideoMediaGroup(
					MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR, null,
					vi.getWidth(), vi.getHeight(), null, 0, 0);
		} catch (MsControlException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_TAG, "Error on create videoMediaGroup");
			e.printStackTrace();
		}
		// }

		Log.d(LOG_TAG, "videoMediaGroup: " + videoMediaGroup);
		ApplicationContext.contextTable.put("audioMediaGroup", audioMediaGroup);
		ApplicationContext.contextTable.put("videoMediaGroup", videoMediaGroup);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// hacer joins de los mediagroups con el NC
		// start play y recorder

		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");

		NetworkConnectionImpl nc = controller.getNetworkConnection();

		try {
			if (audioMediaGroup != null) {
				audioMediaGroup.join(Direction.DUPLEX,
						nc.getJoinableStream(StreamType.audio));
				Log.d(LOG_TAG, "137");
				audioMediaGroup.getPlayer().play(URI.create(""), RTC.NO_RTC,
						Parameters.NO_PARAMETER);
				Log.d(LOG_TAG, "140");
				audioMediaGroup.getRecorder().record(URI.create(""),
						RTC.NO_RTC, Parameters.NO_PARAMETER);
				Log.d(LOG_TAG, "Audio is OK");
				// Unjoin example
				// audioMediaGroup.stop();
				// audioMediaGroup.unjoin(controller.getNetworkConnection()
				// .getJoinableStream(StreamType.audio));
			}
			
			if (videoMediaGroup != null) {
				videoMediaGroup.join(Direction.DUPLEX,
						nc.getJoinableStream(StreamType.video));
				Log.d(LOG_TAG, "151");
				videoMediaGroup.getPlayer().play(URI.create(""), RTC.NO_RTC,
						Parameters.NO_PARAMETER);
				Log.d(LOG_TAG, "154");
				videoMediaGroup.getRecorder().record(URI.create(""),
						RTC.NO_RTC, Parameters.NO_PARAMETER);

				// Unjoin example
				// videoMediaGroup.stop();
				// videoMediaGroup.unjoin(controller.getNetworkConnection()
				// .getJoinableStream(StreamType.audio));
				Log.d(LOG_TAG, "Video is OK");
			}
		} catch (MsControlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Intent videoCallIntent = new Intent(this, VideoCall.class);
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
		
		if (audioMediaGroup != null)
			audioMediaGroup.stop();

		if (videoMediaGroup != null)
			videoMediaGroup.stop();
		
		
		super.onDestroy();
	}

}
