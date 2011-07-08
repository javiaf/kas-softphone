package com.tikal.javax.media.mscontrol.mediagroup;

public interface AudioSink {

	public void putAudioSamples(short[] in_buffer, int in_size);
	
}
