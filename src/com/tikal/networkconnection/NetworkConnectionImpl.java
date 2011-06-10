package com.tikal.networkconnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.JoinEventListener;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.AllocationEventListener;
import javaxt.sdp.SdpException;

import android.os.Environment;
import android.util.Log;

import com.tikal.android.media.AudioCodec;
import com.tikal.android.media.VideoCodec;
import com.tikal.media.AudioInfo;
import com.tikal.media.IRTPMedia;
import com.tikal.media.NetworkConnectionBase;
import com.tikal.media.RTPInfo;
import com.tikal.media.VideoInfo;
import com.tikal.media.format.MediaSpec;
import com.tikal.media.format.PayloadSpec;
import com.tikal.media.format.SessionSpec;
import com.tikal.sdp.enums.MediaType;
import com.tikal.sdp.enums.Mode;

/**
 * 
 * @author Miguel París Díaz
 * 
 */
public class NetworkConnectionImpl extends NetworkConnectionBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SessionSpec localSessionSpec;
	private SessionSpec remoteSessionSpec;

	private IRTPMedia rtpMedia;
	private VideoInfo videoInfo;
	private AudioInfo audioInfo;

	public final static String LOG_TAG = "NW";

	@Override
	public void setLocalSessionSpec(SessionSpec arg0) {
		this.localSessionSpec = arg0;
	}

	@Override
	public void setRemoteSessionSpec(SessionSpec arg0) {
		this.remoteSessionSpec = arg0;
	}

	protected NetworkConnectionImpl(IRTPMedia rtpMedia, VideoInfo vi,
			AudioInfo ai) {
		super();
		this.rtpMedia = rtpMedia;
		this.videoInfo = vi;
		this.audioInfo = ai;
	}

	@Override
	public void confirm() throws MsControlException {
		Log.d(LOG_TAG, "start on NCImpl");
		Log.d(LOG_TAG, "remoteSessionSpec:\n" + remoteSessionSpec);
		Log.d(LOG_TAG, "localSessionSpec:\n" + localSessionSpec);

		PrintWriter pw = null;
		try {
			String fileSDP = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/DCIM/local3.sdp";
			File f = new File(fileSDP);
			f.delete();
			pw = new PrintWriter(f);
			pw.print(localSessionSpec.toString());
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			if (pw != null)
				pw.close();
		}

		if (remoteSessionSpec == null)
			// throw new MediaException("SessionSpec corrupt");
			return;

		try {
			rtpMedia.startRTPMedia(new RTPInfo(remoteSessionSpec),
					localSessionSpec);
		} catch (Exception e) {// (NoSuchMediaInfoException e) {
			e.printStackTrace();
			// throw new MediaException(e.getMessage());
			return;
		}
	}

	@Override
	public void release() {
		rtpMedia.releaseRTPMedia();
	}

	private void addPayloadSpec(List<PayloadSpec> videoList, String payloadStr,
			MediaType mediaType, int port) {
		try {
			PayloadSpec payload = new PayloadSpec(payloadStr);
			payload.setMediaType(mediaType);
			payload.setPort(port);
			videoList.add(payload);
		} catch (SdpException e) {
			e.printStackTrace();
		}
	}

	@Override
	public SessionSpec generateSessionSpec() {
		// VIDEO
		List<PayloadSpec> videoList = new Vector<PayloadSpec>();
		if (videoInfo.getSupportedCodecsID()
				.contains(VideoCodec.CODEC_ID_MPEG4))
			addPayloadSpec(videoList, "96 MP4V-ES/90000", MediaType.VIDEO, 2323);
		if (videoInfo.getSupportedCodecsID().contains(VideoCodec.CODEC_ID_H263))
			addPayloadSpec(videoList, "97 H263-1998/90000", MediaType.VIDEO,
					2323);
		if (videoInfo.getSupportedCodecsID().contains(VideoCodec.CODEC_ID_H264))
			addPayloadSpec(videoList, "98 H264/90000", MediaType.VIDEO, 2323);

		MediaSpec videoMedia = new MediaSpec();
		videoMedia.setPayloadList(videoList);

		// AUDIO
		List<PayloadSpec> audioList = new Vector<PayloadSpec>();
		if (audioInfo.getSupportedCodecsID().contains(AudioCodec.CODEC_ID_AMR)) {
			// addPayloadSpec(audioList, "100 AMR/8000/1", MediaType.AUDIO,
			// 3434);
			PayloadSpec audioPayloadAMR = null;
			try {
				audioPayloadAMR = new PayloadSpec("100 AMR/8000/1");
				audioPayloadAMR.setFormatParams("octet-align=1");
				audioPayloadAMR.setMediaType(MediaType.AUDIO);
				audioPayloadAMR.setPort(3434);
			} catch (SdpException e) {
				e.printStackTrace();
			}
			audioList.add(audioPayloadAMR);

		}
		if (audioInfo.getSupportedCodecsID().contains(AudioCodec.CODEC_ID_MP2)) {
			PayloadSpec payloadAudioMP2 = new PayloadSpec();
			payloadAudioMP2.setMediaType(MediaType.AUDIO);
			payloadAudioMP2.setPort(3434);
			payloadAudioMP2.setPayload(14);
			audioList.add(payloadAudioMP2);
		}
		if (audioInfo.getSupportedCodecsID().contains(AudioCodec.CODEC_ID_AAC)) {
			PayloadSpec audioPayloadAAC = null;
			try {
				audioPayloadAAC = new PayloadSpec("101 MPEG4-GENERIC/8000/1");
				audioPayloadAAC
						.setFormatParams("profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3; config=1210");
				audioPayloadAAC.setMediaType(MediaType.AUDIO);
				audioPayloadAAC.setPort(3434);
			} catch (SdpException e) {
				e.printStackTrace();
			}
			audioList.add(audioPayloadAAC);
		}

		MediaSpec audioMedia = new MediaSpec();
		audioMedia.setPayloadList(audioList);

		List<MediaSpec> mediaList = new Vector<MediaSpec>();
		mediaList.add(videoMedia);
		mediaList.add(audioMedia);

		SessionSpec session = new SessionSpec();
		session.setMediaSpec(mediaList);

		session.setOriginAddress(getLocalAddress().getHostAddress().toString());
		session.setRemoteHandler("0.0.0.0");
		session.setSessionName("TestSession");

		return session;
	}

	@Override
	public InetAddress getLocalAddress() {
		return NetworkIP.getLocalAddress();
	}

	@Override
	public void addListener(JoinEventListener arg0) {

	}

	@Override
	public void addListener(AllocationEventListener arg0) {

	}

	@Override
	public Parameters createParameters() {

		return null;
	}

	@Override
	public MediaConfig getConfig() {

		return null;
	}

	@Override
	public JoinableStream getJoinableStream(StreamType arg0) {

		return null;
	}

	@Override
	public JoinableStream[] getJoinableStreams() throws MsControlException {

		return null;
	}

	@Override
	public Joinable[] getJoinees() throws MsControlException {

		return null;
	}

	@Override
	public Joinable[] getJoinees(Direction arg0) throws MsControlException {

		return null;
	}

	@Override
	public Iterator<MediaObject> getMediaObjects() {

		return null;
	}

	@Override
	public <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> arg0) {

		return null;
	}

	@Override
	public MediaSession getMediaSession() {

		return null;
	}

	@Override
	public Parameters getParameters(Parameter[] arg0) {

		return null;
	}

	@Override
	public <R> R getResource(Class<R> arg0) throws MsControlException {

		return null;
	}

	@Override
	public URI getURI() {

		return null;
	}

	@Override
	public void join(Direction arg0, Joinable arg1) throws MsControlException {

	}

	@Override
	public void joinInitiate(Direction arg0, Joinable arg1, Serializable arg2) {

	}

	@Override
	public void removeListener(JoinEventListener arg0) {

	}

	@Override
	public void removeListener(AllocationEventListener arg0) {

	}

	@Override
	public void setParameters(Parameters arg0) {

	}

	@Override
	public void triggerAction(Action arg0) {

	}

	@Override
	public void unjoin(Joinable arg0) throws MsControlException {

	}

	@Override
	public void unjoinInitiate(Joinable arg0, Serializable arg1)
			throws MsControlException {

	}

}
