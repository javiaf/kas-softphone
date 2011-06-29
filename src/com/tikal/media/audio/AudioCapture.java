package com.tikal.media.audio;

import com.tikal.media.AudioInfo;
import com.tikal.android.media.tx.MediaTx;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioCapture extends Thread {
	/* Implementará la interfaz definida para realizar las llamadas a FFMPEG */
	private static final String LOG_TAG = "AudioCapture";
	private int frequency =  44100;
	private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private AudioRecord audioRecord;
	private short[] buffer;
	int frameSize;
	int ret = 0;

	public AudioCapture(AudioInfo audioInfo) {
		Log.d(LOG_TAG,
				"AudioCapture Created: audioInfo.getOut: " + audioInfo.getOut()
						+ " ; getCodectID:" + audioInfo.getCodecID()
						+ " ; getPayLoadType:" + audioInfo.getPayloadType());

		frameSize = MediaTx.initAudio(audioInfo.getOut() + "?local_port=2323",
				audioInfo.getCodecID(), audioInfo.getSample_rate(),
				audioInfo.getBit_rate(), audioInfo.getPayloadType());

		Log.d(LOG_TAG, "initAudio returns frameSize: " + frameSize);
		Log.d(LOG_TAG,
				"getOut:" + audioInfo.getOut() + "; getCodecID:"
						+ audioInfo.getCodecID() + "; getSample_rate:"
						+ audioInfo.getSample_rate() + " getBitRate:"
						+ audioInfo.getBit_rate() + "; getPayLoadType:"
						+ audioInfo.getPayloadType());

		if (frameSize < 0) {
			MediaTx.finishAudio();
			return;
		}
		frequency =  audioInfo.getSample_rate();
		Log.d(LOG_TAG,
				"Frequency = " + frequency);
		int minBufferSize = AudioRecord.getMinBufferSize(frequency,
				channelConfiguration, audioEncoding);

		int bufferSize = calculateBufferSize(minBufferSize, frameSize);
		Log.d(LOG_TAG, "bufferSize: " + bufferSize);

		buffer = new short[bufferSize];

		// Create a new AudioRecord.
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
				channelConfiguration, audioEncoding, bufferSize);

		Log.d(LOG_TAG, "AudioRecord Create");
	}

	@Override
	public void run() {
		StartRecording();
	}

	public void release() {
		interrupt();
	}

	private void releaseAudio() {
		Log.d(LOG_TAG, "ReleaseAudio");
		MediaTx.finishAudio();
		Log.d(LOG_TAG, "MediaTx.finishAudio");
		if (audioRecord != null) {
			audioRecord.stop();
			audioRecord.release();
			audioRecord = null;
			Log.d(LOG_TAG, "AudioRecord, stop, release and null");
		}
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

	private void StartRecording() {
		Log.d(LOG_TAG, "Start Recording");
		audioRecord.startRecording();
		try {
			while (!isInterrupted()) {

				int bufferReadResult = readFully(buffer, frameSize);
				ret = MediaTx.putAudioSamples(buffer, bufferReadResult);
				if (ret < 0)
					break;
//				this.sleep(20);
			}
			releaseAudio();
		} catch (Throwable t) {
			Log.e(LOG_TAG, "Error al grabar:" + t.toString());
		}
	}

}
