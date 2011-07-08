package com.tikal.media;

import java.util.ArrayList;

import com.tikal.android.media.VideoCodec;

/**
 * 
 * @author Miguel París Díaz
 * 
 */
public class VideoInfo {

	public final static String LOG_TAG = "VI";

	private static final long serialVersionUID = -7370416054677263349L;

	public static final String MODE_RECORD = "MODE_RECORD";
	public static final String MODE_SEND_RTP = "MODE_SEND_RTP";

	private int frame_rate;
	private int width;
	private int height;
	private int codecID;
	private int payloadType;
	ArrayList<Integer> supportedCodecsID;
	private String out;
	private String mode;

	public int getFrame_rate() {
		return frame_rate;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getCodecID() {
		return codecID;
	}

	public ArrayList<Integer> getSupportedCodecsID() {
		return supportedCodecsID;
	}

	public String getMode() {
		return mode;
	}

	public String getOut() {
		return out;
	}

	public void setCodecID(int codecID) {
		this.codecID = codecID;
	}

	public void setSupportedCodecsID(ArrayList<Integer> supportedCodecsID) {
		this.supportedCodecsID = supportedCodecsID;
	}

	public void setOut(String out) {
		this.out = out;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public int getPayloadType() {
		return payloadType;
	}

	public void setPayloadType(int payload_type) {
		this.payloadType = payload_type;
	}

	public VideoInfo(int frame_rate, int width, int height,
			ArrayList<Integer> supportedCodecsID, String codecName, String out,
			String mode) {
		this.frame_rate = frame_rate;
		this.width = width;
		this.height = height;
		VideoCodec c = VideoCodec.getInstance();
		try {
			this.codecID = c.getCodecId(codecName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.payloadType = 96;
		this.supportedCodecsID = supportedCodecsID;
		this.out = out;
		this.mode = mode;
	}

}
