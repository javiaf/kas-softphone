package com.tikal.media;

import android.util.Log;

import com.tikal.android.media.AudioCodec;
import com.tikal.android.media.VideoCodec;
import com.tikal.media.format.MediaSpec;
import com.tikal.media.format.SessionSpec;
import com.tikal.sdp.enums.MediaType;




/**
 * 
 * @author Miguel París Díaz
 * 
 */
public class RTPInfo {

	public final static String LOG_TAG = "RTPInfo";

	private String dstIp;

	private int dstVideoPort;
	private int videoCodecId;
	private int videoPayloadType;

	private int dstAudioPort;
	private int audioCodecId;
	private int audioPayloadType;

	public String getDstIp() {
		return dstIp;
	}

	public int getDstVideoPort() {
		return dstVideoPort;
	}

	public int getVideoCodecId() {
		return videoCodecId;
	}

	public int getVideoPayloadType() {
		return videoPayloadType;
	}

	public int getDstAudioPort() {
		return dstAudioPort;
	}

	public int getAudioCodecId() {
		return audioCodecId;
	}

	public int getAudioPayloadType() {
		return audioPayloadType;
	}

	public RTPInfo(SessionSpec se){ //throws NoSuchMediaInfoException {
		try {
			this.dstIp = se.getOriginAddress();

			for (MediaSpec ms : se.getMediaSpec()) {
				Log.d(LOG_TAG, "ms: " + ms.toString());
				if (ms.getMediaType().equals(MediaType.AUDIO)) {
					Log.d(LOG_TAG, "audio");
					this.dstAudioPort = ms.getPort();
					Log.d(LOG_TAG, "dstAudioPort: " + dstAudioPort);
					Log.d(LOG_TAG,
							"encoding name: "
									+ ms.getPayloadList().get(0)
											.getEncodingName());
					AudioCodec c = AudioCodec.getInstance();
					try {
						this.audioCodecId = c.getCodecId(ms.getPayloadList()
								.get(0).getEncodingName());
						Log.d(LOG_TAG, "audioCodecId: " + audioCodecId);
					} catch (Exception e) {
						Log.d(LOG_TAG, e.toString());
						e.printStackTrace();
					}
					this.audioPayloadType = ms.getPayloadList().get(0)
							.getPayload();
				} else if (ms.getMediaType().equals(MediaType.VIDEO)) {
					Log.d(LOG_TAG, "video");
					this.dstVideoPort = ms.getPort();
					Log.d(LOG_TAG, "dstVideoPort: " + dstVideoPort);
					Log.d(LOG_TAG,
							"encoding name: "
									+ ms.getPayloadList().get(0)
											.getEncodingName());
					VideoCodec c = VideoCodec.getInstance();
					try {
						this.videoCodecId = c.getCodecId(ms.getPayloadList()
								.get(0).getEncodingName());
						Log.d(LOG_TAG, "videoCodecId: " + videoCodecId);
					} catch (Exception e) {
						Log.d(LOG_TAG, e.toString());
						e.printStackTrace();
					}
					this.videoPayloadType = ms.getPayloadList().get(0)
							.getPayload();
				}
			}
		} catch (IndexOutOfBoundsException ioobe) {
//			throw new NoSuchMediaInfoException(
//					"No such media info in SessionSpec object");
		}
	}

	public String getVideoRTPDir() {
		return "rtp://" + dstIp + ":" + dstVideoPort;
	}

	public String getAudioRTPDir() {
		return "rtp://" + dstIp + ":" + dstAudioPort;
	}

}
