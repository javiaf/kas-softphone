package com.tikal.media.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.Surface;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.tikal.android.media.rx.MediaRx;
import com.tikal.android.media.rx.VideoPlayer;
import com.tikal.media.VideoInfo;

public class CameraReceive implements Runnable, VideoPlayer {
	/* Implementará la interfaz definida para realizar las llamadas a FFMPEG */
	private static final String LOG_TAG = "CameraReceive";

	private SurfaceView mVideoReceiveView;
	private SurfaceHolder mHolderReceive;
	private Surface mSurfaceReceive;
	private View video_receive_surface;
	
	private int screenWidth;
	private int screenHeight;
	
	private String sdp;
	

	private Canvas canvas = new Canvas();

	Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

	public CameraReceive(View surface,  VideoInfo videoInfo, String sdp) {
		Log.d(LOG_TAG, "CameraReceive Created");
		setVideo_receive_surface(surface);
		mVideoReceiveView = (SurfaceView) video_receive_surface;
		mHolderReceive = mVideoReceiveView.getHolder();
		mSurfaceReceive = mHolderReceive.getSurface();
	
		screenWidth = videoInfo.getScreenWidth() ;
		screenHeight = videoInfo.getScreenHeight() * 3/4;
		this.sdp = sdp;
		Log.d(LOG_TAG, "ScreenWidth : " + screenWidth + "; ScreenHeigh : " + screenHeight);

	}


	@Override
	public void run() {
		StartReceiving();
	}

	private void StartReceiving() {
		Log.d(LOG_TAG, "Start Camera Receiving:" + sdp);
		MediaRx.startVideoRx(sdp,this);
//		audioTrack.play();

	}

	

	public void setVideo_receive_surface(View video_receive_surface) {
		this.video_receive_surface = video_receive_surface;
	}

	public View getVideo_receive_surface() {
		return video_receive_surface;
	}

	public void release() {
		Log.d(LOG_TAG, "Release");
		MediaRx.stopVideoRx();
		Log.d(LOG_TAG, "ok");
	}


	@Override
	public void putVideoFrameRx(int[] rgb, int width, int height) {
		if (rgb == null || rgb.length == 0)
			return;
		try {
			// Log.d(LOG_TAG,"PutFrame");
			canvas = mSurfaceReceive.lockCanvas(null);

			if (canvas == null) {
				//Log.e(LOG_TAG, "Canvas is null");
				return;
			}
			
		
			
			Bitmap srcBitmap = Bitmap.createBitmap(rgb, width, height,
					Bitmap.Config.ARGB_8888);
			RectF dirty2 = new RectF(0, 0, screenWidth, screenHeight);

			// canvas.drawRGB(255,255,255);
			// canvas.drawCircle(50, 50, 10, paint);

			canvas.drawBitmap(srcBitmap, null, dirty2, null);

			mSurfaceReceive.unlockCanvasAndPost(canvas);
			// Log.d(LOG_TAG,"putFrame");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_TAG, "Exception: " + e.toString());
			e.printStackTrace();
		} catch (OutOfResourcesException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_TAG, "Exception: " + e.toString());
			e.printStackTrace();
		}

	}
}
