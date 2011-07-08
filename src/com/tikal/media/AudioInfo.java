package com.tikal.media;

import java.util.ArrayList;

import android.util.Log;

import com.tikal.android.media.AudioCodec;



/**
 * 
 * @author Miguel París Díaz
 * 
 */
public class AudioInfo {

private static final long serialVersionUID = 2763070968508207537L;
	
	public final static String LOG_TAG = "AudioInfo";

	private int sample_rate;
	private int bit_rate;
	private int codecID;
	private int payloadType;
	ArrayList<Integer> supportedCodecsID;
	private String out;
	
	public int frameSize;

	public int getSample_rate() {
		return sample_rate;
	}

	public void setSample_rate(int sample_rate) {
		this.sample_rate = sample_rate;
	}

	public int getBit_rate() {
		return bit_rate;
	}

	public void setBit_rate(int bit_rate) {
		this.bit_rate = bit_rate;
	}

	public int getCodecID() {
		return codecID;
	}

	public void setCodecID(int codecID) {
		this.codecID = codecID;
	}

	public int getPayloadType() {
		return payloadType;
	}

	public void setPayloadType(int payloadType) {
		this.payloadType = payloadType;
	}

	public ArrayList<Integer> getSupportedCodecsID() {
		return supportedCodecsID;
	}

	public void setSupportedCodecsID(ArrayList<Integer> supportedCodecsID) {
		this.supportedCodecsID = supportedCodecsID;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getOut() {
		return out;
	}

	public void setOut(String out) {
		this.out = out;
	}
	
	

	public int getFrameSize() {
		return frameSize;
	}

	public void setFrameSize(int frameSize) {
		this.frameSize = frameSize;
	}

	public AudioInfo(ArrayList<Integer> supportedCodecsID, String codecName,
			String out) {
		this.supportedCodecsID = supportedCodecsID;
		AudioCodec c = AudioCodec.getInstance();
		try {
			this.codecID = c.getCodecId(codecName);
			this.sample_rate = c.getSampleRate(this.codecID);
			this.bit_rate = c.getBitRate(this.codecID);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
			e.printStackTrace();
		}
		this.payloadType = 97;
		this.out = out;
	}
}
