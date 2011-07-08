package com.tikal.videocall;

import com.tikal.media.AudioInfo;
import com.tikal.media.VideoInfo;
import com.tikal.media.audio.AudioCapture;
import com.tikal.media.audio.AudioReceive;
import com.tikal.media.camera.CameraCapture;
import com.tikal.media.camera.CameraReceive;
import com.tikal.softphone.R;
import com.tikal.softphone.SoftPhone;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class VideoCallService extends Service {
	private final String LOG_TAG = "VideoCallService";

	
	private AudioCapture audioCapture;
	private AudioReceive audioReceive;
	private CameraCapture cameraCapture;
	private CameraReceive cameraReceive;
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(LOG_TAG, "On Start = " + intent.getExtras().toString());
		Bundle extras = intent.getExtras();
		
		
		VideoInfo vi = (VideoInfo) extras.getSerializable("VideoInfo");
		AudioInfo ai = (AudioInfo) extras.getSerializable("AudioInfo");

		String sdpVideo = (String) extras.getSerializable("SdpVideo");
		String sdpAudio = (String) extras.getSerializable("SdpAudio");
		
		Intent videoCallIntent = new Intent(this, VideoCall.class);
		videoCallIntent.putExtra("VideoInfo", vi);
		videoCallIntent.putExtra("AudioInfo", ai);
		videoCallIntent.putExtra("SdpVideo", sdpVideo);
		videoCallIntent.putExtra("SdpAudio", sdpAudio);
		videoCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(videoCallIntent);

	}
	
	
	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "On Destroy"); 
		super.onDestroy();
	}
	

}
