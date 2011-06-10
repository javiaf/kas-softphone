package com.tikal.networkconnection;



import javax.media.mscontrol.networkconnection.NetworkConnection;

import com.tikal.media.AudioInfo;
import com.tikal.media.IRTPMedia;
import com.tikal.media.NetworkConnectionFactory;
import com.tikal.media.VideoInfo;
import com.tikal.sip.exception.ServerInternalErrorException;


/**
 * 
 * @author Miguel París Díaz
 * 
 */
public class NetworkConnectionFactoryImpl implements NetworkConnectionFactory {

	private IRTPMedia rtpMedia;
	private VideoInfo vi;
	private AudioInfo ai;

	public NetworkConnectionFactoryImpl(IRTPMedia rtpMedia, VideoInfo vi, AudioInfo ai) {
		this.rtpMedia = rtpMedia;
		this.vi = vi;
		this.ai = ai;
	}

	public NetworkConnection getInstance() throws ServerInternalErrorException {
		return new NetworkConnectionImpl(rtpMedia, vi, ai);
	}
}
