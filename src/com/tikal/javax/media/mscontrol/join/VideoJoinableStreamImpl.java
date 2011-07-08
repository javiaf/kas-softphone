package com.tikal.javax.media.mscontrol.join;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableContainer;

import com.tikal.android.media.rx.VideoRx;
import com.tikal.android.media.tx.MediaTx;
import com.tikal.javax.media.mscontrol.mediagroup.VideoSink;

public class VideoJoinableStreamImpl extends JoinableStreamBase implements
		VideoSink, VideoRx {

	private static final String LOG_TAG = "VideoJoinableStream";

	public VideoJoinableStreamImpl(JoinableContainer container, StreamType type) {
		super(container, type);
	}

	@Override
	public void putVideoFrame(byte[] frame) {
		MediaTx.putVideoFrame(frame);
	}

	@Override
	public void putVideoFrameRx(int[] rgb, int width, int height) {
		try {
			for (Joinable j : getJoinees(Direction.SEND))
				if (j instanceof VideoRx)
					((VideoRx) j).putVideoFrameRx(rgb, width, height);
			for (Joinable j : getJoinees(Direction.DUPLEX))
				if (j instanceof VideoRx)
					((VideoRx) j).putVideoFrameRx(rgb, width, height);
		} catch (MsControlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
