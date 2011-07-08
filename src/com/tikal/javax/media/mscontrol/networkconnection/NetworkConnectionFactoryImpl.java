package com.tikal.javax.media.mscontrol.networkconnection;



import javax.media.mscontrol.networkconnection.NetworkConnection;

import com.tikal.media.AudioInfo;
import com.tikal.media.VideoInfo;
import com.tikal.sip.exception.ServerInternalErrorException;


/**
 * 
 * @author Miguel París Díaz
 * 
 */
public class NetworkConnectionFactoryImpl implements NetworkConnectionFactory {
	private VideoInfo vi;
	private AudioInfo ai;

	public NetworkConnectionFactoryImpl(VideoInfo vi, AudioInfo ai) {
		this.vi = vi;
		this.ai = ai;
	}

	public NetworkConnection getInstance() throws ServerInternalErrorException {
		return new NetworkConnectionImpl(vi, ai);
	}
}
