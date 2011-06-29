package com.tikal.media.camera;

import java.io.IOException;
import java.util.List;

import com.tikal.android.media.VideoCodec;
import com.tikal.android.media.tx.MediaTx;
import com.tikal.media.VideoInfo;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Build.VERSION;
import android.os.Environment;
import android.text.style.LineHeightSpan.WithDensity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class CameraCapture implements Runnable, SurfaceHolder.Callback,
		PreviewCallback {
	/* Implementará la interfaz definida para realizar las llamadas a FFMPEG */
	private static final String LOG_TAG = "CameraCapture";

	private SurfaceView mVideoView;

	private SurfaceHolder mHolder;

	private Camera mCamera;

	private View video_surface;

	// default values
	private int frame_rate;// = 15;// 30;
	private int bit_rate = 4000000;//320000;// 250000;//4000000;
	private int width;// = 352;// 640;//176;//320;
	private int height;// = 288;// 480;//144;//240;
	private int codec = VideoCodec.CODEC_ID_MPEG4;
	private int payload_type = 96;
	private String out =
	// Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/video_grabado-26.01.11-h263.avi";
	"rtp://192.168.1.10:8888";
	private String audioOut = "rtp://192.168.1.10:7777";
	public static final String PRESET_FILE =
	// null;
	Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/DCIM/libx264-ffpreset";

	public CameraCapture(View surface, VideoInfo videoInfo) {
		Log.d(LOG_TAG, "CameraCapture Created");
		setVideo_surface(surface);

		if (videoInfo != null) {
			Log.d(LOG_TAG, "VideoInfo " + videoInfo.getWidth() + " "
					+ videoInfo.getCodecID() + ";" + videoInfo.getPayloadType()
					+ ";" + videoInfo.getOut());
			// frame_rate = vi.getFrame_rate();
			width = videoInfo.getWidth(); //800
			height =  videoInfo.getHeight(); //600
			codec = videoInfo.getCodecID();
			payload_type = videoInfo.getPayloadType();
			out = videoInfo.getOut();
		}

		mVideoView = (SurfaceView) video_surface;
		mHolder = mVideoView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void run() {
		StartRecording();
	}

	
	
	private Camera openFrontFacingCameraGingerbread() 
	{
	    int cameraCount = 0;
	    Camera cam = null;
	    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
	    cameraCount = Camera.getNumberOfCameras();
	    for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
	        Camera.getCameraInfo( camIdx, cameraInfo );
	        if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK ) {
	            try {
	                cam = Camera.open( camIdx );
	            } catch (RuntimeException e) {
	                Log.e(LOG_TAG, "Camera failed to open: " + e.getLocalizedMessage());
	            }
	        }
	    }

	    return cam;
	}
	
	private void StartRecording() {
		Log.d(LOG_TAG, "Start Camera Capturing");
		
		Log.d(LOG_TAG, " Versión SDK " + VERSION.SDK_INT);	
		
		if (mCamera == null){
			if (VERSION.SDK_INT < 9){
				mCamera = Camera.open();
			}else
				mCamera = openFrontFacingCameraGingerbread();
		}
		
		Log.i(LOG_TAG, "Camera: W:"
				+ mCamera.getParameters().getPreviewSize().width + "; H:"
				+ mCamera.getParameters().getPreviewSize().height);

		Camera.Parameters parameters = mCamera.getParameters();
		
//		parameters.set("camera-id", 2);
//		mCamera.setParameters(parameters);

		List<Size> sizes = parameters.getSupportedPreviewSizes();

		// Parameters
		Log.d("FPS",
				"getPreviewFrameRate(): "
						+ parameters.getPreviewFrameRate());
		Log.d("FPS",
				"getSupportedPreviewFrameRates(): "
						+ parameters.getSupportedPreviewFrameRates());

		
		
		String cad = "";
		//Video Preferences is support?
		boolean isSupport = false;
		for (int i = 0; i < sizes.size(); i++){
			cad += sizes.get(i).width + " x " + sizes.get(i).height + "\n";
			if ((width == sizes.get(i).width) && (height == sizes.get(i).height)){
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

		frame_rate = parameters.getPreviewFrameRate();
		if (!isSupport){
			width = sizes.get(0).width;
			height = sizes.get(0).height;
		}
		parameters.setPreviewSize(width, height);
//		parameters.setRotation(180);
		mCamera.setParameters(parameters);
		
		
		parameters = mCamera.getParameters();
		
		Log.d("FPS", "Despues del Set getPreviewSize: " + parameters.getPreviewSize().width
				+ " x " + parameters.getPreviewSize().height);
		
		
		mCamera.setPreviewCallback(this);
		
		Log.d("FPS", "For initVideo -> Width = " + width + ";Height = " + height + "; Frame_rate = " + frame_rate);

		int ret = MediaTx.initVideo(out, width, height, frame_rate, bit_rate,
				codec, payload_type, PRESET_FILE);

		if (ret < 0) {
			Log.d(LOG_TAG, "Error in initVideo");
			MediaTx.finishVideo();
			System.exit(1);
		}

	}

	public void release() {
		Log.d(LOG_TAG, "Release");
		MediaTx.finishVideo();
		// mCamera.stopPreview();
		// mCamera.release();
		mCamera = null;
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

			if (mCamera == null){
				if (VERSION.SDK_INT < 9){
					mCamera = Camera.open();
				}else
					mCamera = openFrontFacingCameraGingerbread();
			}
			Log.d(LOG_TAG, " mCamera opened " + mCamera.toString());
		//	mCamera.setPreviewCallback(this);
			// mCamera = Camera.open();
			if (mCamera != null) {
				Log.d(LOG_TAG, "Surface Create");
				mCamera.setPreviewDisplay(holder);
			} else
				Log.w(LOG_TAG, "Not Surface Create");
			
			StartRecording();
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

	public void setVideo_surface(View view) {
		this.video_surface = view;
	}

	public View getVideo_surface() {
		return video_surface;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (data == null)
			return;
		MediaTx.putVideoFrame(data);
	}

}
