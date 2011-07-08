package com.tikal.javax.media.mscontrol.join;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableContainer;

import com.tikal.android.media.rx.AudioRx;
import com.tikal.android.media.tx.MediaTx;
import com.tikal.javax.media.mscontrol.mediagroup.AudioSink;

public class AudioJoinableStreamImpl extends JoinableStreamBase implements
		AudioSink, AudioRx {

	private static final String LOG_TAG = "AudioJoinableStream";

	public AudioJoinableStreamImpl(JoinableContainer container, StreamType type) {
		super(container, type);
	}

	@Override
	public void putAudioSamples(short[] in_buffer, int in_size) {
		MediaTx.putAudioSamples(in_buffer, in_size);
	}

	@Override
	public void putAudioSamplesRx(byte[] audio, int length) {
		try {
			for (Joinable j : getJoinees(Direction.SEND))
				if (j instanceof AudioRx)
					((AudioRx) j).putAudioSamplesRx(audio, length);
			for (Joinable j : getJoinees(Direction.DUPLEX))
				if (j instanceof AudioRx)
					((AudioRx) j).putAudioSamplesRx(audio, length);
		} catch (MsControlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
