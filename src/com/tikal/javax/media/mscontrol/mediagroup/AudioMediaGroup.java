package com.tikal.javax.media.mscontrol.mediagroup;

import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalGenerator;

import android.media.AudioManager;

import com.tikal.android.media.rx.AudioRx;

public class AudioMediaGroup extends MediaGroupBase implements AudioRx {

	private AudioPlayer audioPlayer = null;
	private AudioRecorder audioRecorder = null;

	public AudioMediaGroup(Configuration<MediaGroup> predefinedConfig,
			int sampleRate, int frameSize) throws MsControlException {
		if (PLAYER.equals(predefinedConfig))
			audioPlayer = new AudioPlayer(this, sampleRate, frameSize);
		else if (PLAYER_RECORDER_SIGNALDETECTOR.equals(predefinedConfig)) {
			audioPlayer = new AudioPlayer(this, sampleRate, frameSize);
			audioRecorder = new AudioRecorder(this, sampleRate,
					AudioManager.STREAM_MUSIC);
		}
	}

	@Override
	public Player getPlayer() throws MsControlException {
		return audioPlayer;
	}

	@Override
	public Recorder getRecorder() throws MsControlException {
		return audioRecorder;
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
		audioPlayer.stop(true);
		audioRecorder.stop();
	}

	@Override
	public void putAudioSamplesRx(byte[] audio, int length) {
		audioRecorder.putAudioSamplesRx(audio, length);
	}

}
