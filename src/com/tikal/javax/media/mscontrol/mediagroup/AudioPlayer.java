package com.tikal.javax.media.mscontrol.mediagroup;

import java.net.URI;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.resource.RTC;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioPlayer extends PlayerBase {

	private static final String LOG_TAG = "AudioPlayer";
	private int frequency = 44100;
	private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private AudioRecord audioRecord;
	private short[] buffer;
	int frameSize;

	AudioCapture audioCapture;

	public AudioPlayer(MediaGroupBase parent, int sampleRate, int frameSize)
			throws MsControlException {
		super(parent);

		this.frameSize = frameSize;
		Log.d(LOG_TAG, "frameSize = " + frameSize);

		frequency = sampleRate;
		Log.d(LOG_TAG, "Frequency = " + frequency);
		int minBufferSize = AudioRecord.getMinBufferSize(frequency,
				channelConfiguration, audioEncoding);

		int bufferSize = calculateBufferSize(minBufferSize, this.frameSize);

		buffer = new short[bufferSize];
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
				channelConfiguration, audioEncoding, bufferSize);

		audioCapture = new AudioCapture();
	}

	/**
	 * 
	 * @param minBufferSize
	 * @param frameSizeEncode
	 * @return the size, where: size % frameSizeEncode = 0 and size >=
	 *         minBufferSize
	 */
	private int calculateBufferSize(int minBufferSize, int frameSizeEncode) {
		int finalSize = frameSizeEncode;
		while (finalSize < minBufferSize)
			finalSize += frameSizeEncode;
		return finalSize;
	}

//	private void releaseAudioRecord() {
//		Log.d(LOG_TAG, "ReleaseAudio");
//		if (audioRecord != null) {
//			audioRecord.stop();
//			audioRecord.release();
//			audioRecord = null;
//		}
//	}

	@Override
	public void play(URI[] arg0, RTC[] arg1, Parameters arg2)
			throws MsControlException {
		// TODO Auto-generated method stub

	}

	@Override
	public void play(URI arg0, RTC[] arg1, Parameters arg2)
			throws MsControlException {
		audioCapture.start();
	}

	@Override
	public void stop(boolean arg0) {
		audioCapture.stopAudio();
	}

	private class AudioCapture extends Thread {

		@Override
		public void run() {
			startRecording();
		}

		public void stopAudio() {
			interrupt();
		}

		private void stopRecording() {
			if (audioRecord != null)
				audioRecord.stop();
		}

		private int readFully(short[] audioData, int sizeInShorts) {
			if (audioRecord == null)
				return -1;

			int shortsRead = 0;
			int shortsLess = sizeInShorts;
			while (shortsRead < sizeInShorts) {
				int read = audioRecord.read(audioData, shortsRead, shortsLess);
				shortsRead += read;
				shortsLess -= read;
			}
			return shortsRead;
		}

		private void startRecording() {
			Log.d(LOG_TAG, "Start Recording");
			audioRecord.startRecording();
			try {
				while (!isInterrupted()) {
					int bufferReadResult = readFully(buffer, frameSize);
					for (Joinable j : getContainer().getJoinees(Direction.SEND))
						if (j instanceof AudioSink)
							((AudioSink) j).putAudioSamples(buffer,
									bufferReadResult);
					for (Joinable j : getContainer().getJoinees(
							Direction.DUPLEX))
						if (j instanceof AudioSink)
							((AudioSink) j).putAudioSamples(buffer,
									bufferReadResult);
				}
				stopRecording();
			} catch (Throwable t) {
				Log.e(LOG_TAG, "Error al grabar:" + t.toString());
			}
		}
	}

}
