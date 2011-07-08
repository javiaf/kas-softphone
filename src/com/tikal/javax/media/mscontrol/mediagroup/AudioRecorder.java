package com.tikal.javax.media.mscontrol.mediagroup;

import java.net.URI;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.resource.RTC;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.tikal.android.media.rx.AudioRx;
import com.tikal.android.media.rx.MediaRx;

public class AudioRecorder extends RecorderBase implements AudioRx {

	private static final String LOG_TAG = "AudioReceive";

	private int frequency = 44100;// 8000;// 11025;
	private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private AudioTrack audioTrack;
	private int buffer_min;
	private int streamType;
	
	private boolean isRecording = false;

	public AudioRecorder(MediaGroupBase parent, int sampleRate,
			int streamType) throws MsControlException {
		super(parent);
		
		frequency = sampleRate;
		Log.d(LOG_TAG, "Frequency = " + frequency);
		buffer_min = AudioTrack.getMinBufferSize(frequency,
				channelConfiguration, audioEncoding);
		
		audioTrack = new AudioTrack(streamType, frequency,
				channelConfiguration, audioEncoding, buffer_min,
				AudioTrack.MODE_STREAM);
	}

	public void release() {
		Log.d(LOG_TAG, "Release");
		MediaRx.stopAudioRx();
		if (audioTrack != null)
			audioTrack.release();
	}

	public void setStreamType(int streamType) {
		this.streamType = streamType;

	}

	public int getStreamType() {
		return streamType;
	}

	@Override
	public void putAudioSamplesRx(byte[] audio, int length) {
		if (isRecording && audioTrack != null)
			audioTrack.write(audio, 0, length);
	}
	
	@Override
	public void record(URI arg0, RTC[] arg1, Parameters arg2)
			throws MsControlException {
		audioTrack.play();
		isRecording = true;
	}

	@Override
	public void stop() {
		audioTrack.stop();
		isRecording = false;
	}

}
