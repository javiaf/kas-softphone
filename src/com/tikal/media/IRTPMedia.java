package com.tikal.media;

import com.tikal.media.format.SessionSpec;

/**
 * 
 * @author Miguel París Díaz
 * 
 */
public interface IRTPMedia {

	public void startRTPMedia(RTPInfo rtpInfo, SessionSpec sdp);

	public void releaseRTPMedia();
}
