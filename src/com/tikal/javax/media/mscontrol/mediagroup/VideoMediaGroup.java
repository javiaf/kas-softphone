package com.tikal.javax.media.mscontrol.mediagroup;

import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalGenerator;

import android.view.View;

import com.tikal.android.media.rx.VideoRx;

public class VideoMediaGroup extends MediaGroupBase implements VideoRx {

	private VideoPlayer videoPlayer = null;
	private VideoRecorder videoRecorder = null;

	public VideoMediaGroup(Configuration<MediaGroup> predefinedConfig,
			View surfaceTx, int width, int height, View surfaceRx,
			int displayWidth, int displayHeight) throws MsControlException {

		if (PLAYER.equals(predefinedConfig))
			videoPlayer = new VideoPlayer(this, surfaceTx, width, height);
		else if (PLAYER_RECORDER_SIGNALDETECTOR.equals(predefinedConfig)) {
			videoPlayer = new VideoPlayer(this, surfaceTx, width, height);
			videoRecorder = new VideoRecorder(this, surfaceRx, displayWidth,
					displayHeight);
		}
	}

	public void setSurfaceTx(View surfaceTx) {
		videoPlayer.setVideoSurfaceTx(surfaceTx);
	}

	public void setSurfaceRx(View surfaceRx, int displayWidth, int displayHeight) {
		videoRecorder.setVideoSurfaceRx(surfaceRx, displayWidth, displayHeight);
	}

	@Override
	public Player getPlayer() throws MsControlException {
		return videoPlayer;
	}

	@Override
	public Recorder getRecorder() throws MsControlException {
		return videoRecorder;
	}

	@Override
	public SignalDetector getSignalDetector() throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SignalGenerator getSignalGenerator() throws MsControlException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stop() {
		videoPlayer.stop(true);
		videoRecorder.stop();
	}

	@Override
	public void putVideoFrameRx(int[] rgb, int width, int height) {
		videoRecorder.putVideoFrameRx(rgb, width, height);
	}

}
