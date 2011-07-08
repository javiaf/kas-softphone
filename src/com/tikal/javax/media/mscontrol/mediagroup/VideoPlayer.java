package com.tikal.javax.media.mscontrol.mediagroup;

import java.net.URI;
import java.util.List;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.resource.RTC;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class VideoPlayer extends PlayerBase implements SurfaceHolder.Callback,
		PreviewCallback {
	private static final String LOG_TAG = "VideoPlayer";

	private SurfaceView mVideoView;
	private SurfaceHolder mHolder;

	private Camera mCamera;
	private View videoSurfaceTx;

	private int width;
	private int height;

	public void setVideoSurfaceTx(View surface) {
		this.videoSurfaceTx = surface;
		if (videoSurfaceTx != null) {
			mVideoView = (SurfaceView) videoSurfaceTx;
			mHolder = mVideoView.getHolder();
			mHolder.addCallback(this);
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
	}

	public View getVideoSurfaceTx() {
		return videoSurfaceTx;
	}

	public VideoPlayer(VideoMediaGroup parent, View surface, int width,
			int height) throws MsControlException {
		super(parent);

		this.videoSurfaceTx = surface;
		this.width = width;
		this.height = height;
	}

	private Camera openFrontFacingCameraGingerbread() {
		int cameraCount = 0;
		Camera cam = null;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				try {
					cam = Camera.open(camIdx);
				} catch (RuntimeException e) {
					Log.e(LOG_TAG,
							"Camera failed to open: " + e.getLocalizedMessage());
				}
			}
		}

		return cam;
	}

	private void startRecording() {
		Log.d(LOG_TAG, "Start Camera Capturing");

		Log.d(LOG_TAG, " Versión SDK " + VERSION.SDK_INT);

		if (mCamera == null) {
			if (VERSION.SDK_INT < 9) {
				mCamera = Camera.open();
			} else
				mCamera = openFrontFacingCameraGingerbread();
		}

		Log.i(LOG_TAG, "Camera: W:"
				+ mCamera.getParameters().getPreviewSize().width + "; H:"
				+ mCamera.getParameters().getPreviewSize().height);

		Camera.Parameters parameters = mCamera.getParameters();

		// parameters.set("camera-id", 2);
		// mCamera.setParameters(parameters);

		List<Size> sizes = parameters.getSupportedPreviewSizes();

		// Parameters
		Log.d("FPS",
				"getPreviewFrameRate(): " + parameters.getPreviewFrameRate());
		Log.d("FPS",
				"getSupportedPreviewFrameRates(): "
						+ parameters.getSupportedPreviewFrameRates());

		String cad = "";
		// Video Preferences is support?
		boolean isSupport = false;
		for (int i = 0; i < sizes.size(); i++) {
			cad += sizes.get(i).width + " x " + sizes.get(i).height + "\n";
			if ((width == sizes.get(i).width)
					&& (height == sizes.get(i).height)) {
				isSupport = true;
				break;
			}
		}

		Log.d("FPS", "getPreviewSize: " + parameters.getPreviewSize().width
				+ " x " + parameters.getPreviewSize().height);
		Log.d("FPS", "getSupportedPreviewSizes:\n" + cad);

		Log.d("FPS", "getPreviewFormat(): " + parameters.getPreviewFormat());
		Log.d("FPS",
				"getSupportedPreviewFormats(): "
						+ parameters.getSupportedPreviewFormats());

		// frame_rate = parameters.getPreviewFrameRate();
		if (!isSupport) {
			width = sizes.get(0).width;
			height = sizes.get(0).height;
		}
		parameters.setPreviewSize(width, height);
		// parameters.setRotation(180);
		mCamera.setParameters(parameters);

		parameters = mCamera.getParameters();

		Log.d("FPS",
				"Despues del Set getPreviewSize: "
						+ parameters.getPreviewSize().width + " x "
						+ parameters.getPreviewSize().height);

		mCamera.setPreviewCallback(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

		// Parameters params = mCamera.getParameters();
		if (mCamera != null) {
			int degrees = 90;

			mCamera.setDisplayOrientation(degrees);
			Camera.Parameters parameters = mCamera.getParameters();

			parameters.setRotation(degrees);
			// params.setPreviewSize(width, height);
			mCamera.setParameters(parameters);

			mCamera.startPreview();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {

			if (mCamera == null) {
				if (VERSION.SDK_INT < 9) {
					mCamera = Camera.open();
				} else
					mCamera = openFrontFacingCameraGingerbread();
			}
			Log.d(LOG_TAG, " mCamera opened " + mCamera.toString());
			// mCamera.setPreviewCallback(this);
			// mCamera = Camera.open();
			if (mCamera != null) {
				Log.d(LOG_TAG, "Surface Create");
				mCamera.setPreviewDisplay(holder);
			} else
				Log.w(LOG_TAG, "Not Surface Create");

			// startRecording();
		} catch (Exception e) {
			Log.e(LOG_TAG, "Exception : " + e.toString());
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			mCamera.stopPreview();
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (data == null)
			return;
		// Send video to subscribers
		try {
			for (Joinable j : getContainer().getJoinees(Direction.SEND))
				if (j instanceof VideoSink)
					((VideoSink) j).putVideoFrame(data);
			for (Joinable j : getContainer().getJoinees(Direction.DUPLEX))
				if (j instanceof VideoSink)
					((VideoSink) j).putVideoFrame(data);
		} catch (MsControlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void play(URI[] arg0, RTC[] arg1, Parameters arg2)
			throws MsControlException {
		// TODO Auto-generated method stub
	}

	@Override
	public void play(URI arg0, RTC[] arg1, Parameters arg2)
			throws MsControlException {
		if (videoSurfaceTx != null) {
			mVideoView = (SurfaceView) videoSurfaceTx;
			mHolder = mVideoView.getHolder();
			mHolder.addCallback(this);
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		startRecording();
	}

	@Override
	public void stop(boolean arg0) {
		// TODO incomplete
		Log.d(LOG_TAG, "Release");
		// mCamera.stopPreview();
		// mCamera.release();
		mCamera = null;
	}

}
