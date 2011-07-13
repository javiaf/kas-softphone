package com.tikal.javax.media.mscontrol.mediagroup;

import java.net.URI;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.resource.RTC;

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

import com.tikal.android.media.rx.VideoRx;

public class VideoRecorder extends RecorderBase implements VideoRx {
	private static final String LOG_TAG = "VideoRecorder";

	private SurfaceView mVideoReceiveView;
	private SurfaceHolder mHolderReceive;
	private Surface mSurfaceReceive;
	private View videoSurfaceRx;

	private int screenWidth;
	private int screenHeight;

	private Canvas canvas = new Canvas();

	Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

	private boolean isRecording = false;

	public void setVideoSurfaceRx(View videoReceiveSurface, int displayWidth,
			int displayHeight) {
		this.videoSurfaceRx = videoReceiveSurface;
		mVideoReceiveView = (SurfaceView) videoSurfaceRx;
		mHolderReceive = mVideoReceiveView.getHolder();
		mSurfaceReceive = mHolderReceive.getSurface();

		Log.d(LOG_TAG, "W: " + displayWidth + "; H: " + displayHeight);

		if (displayWidth < displayHeight) {
			this.screenWidth = displayWidth;
			this.screenHeight = displayHeight * 3 / 4;
		} else {
			this.screenWidth = displayWidth * 1 / 2;
			this.screenHeight = displayHeight * 3 / 4;
		}
	}

	public View getVideoSurfaceRx() {
		return videoSurfaceRx;
	}

	public VideoRecorder(MediaGroupBase parent, View surface, int displayWidth,
			int displayHeight) throws MsControlException {
		super(parent);

		Log.d(LOG_TAG, "CameraReceive Created");
		this.videoSurfaceRx = surface;
		this.screenWidth = displayWidth;
		this.screenHeight = displayHeight * 3 / 4;

		if (surface != null) {
			mVideoReceiveView = (SurfaceView) videoSurfaceRx;
			mHolderReceive = mVideoReceiveView.getHolder();
			mSurfaceReceive = mHolderReceive.getSurface();
		}
	}

	@Override
	public void putVideoFrameRx(int[] rgb, int width, int height) {
		if (!isRecording)
			return;

		if (rgb == null || rgb.length == 0)
			return;

		try {
			if (mSurfaceReceive == null)
				return;
			canvas = mSurfaceReceive.lockCanvas(null);
			if (canvas == null)
				return;

			Bitmap srcBitmap = Bitmap.createBitmap(rgb, width, height,
					Bitmap.Config.ARGB_8888);
			RectF dirty2 = new RectF(0, 0, screenWidth, screenHeight);

			canvas.drawBitmap(srcBitmap, null, dirty2, null);
			
			if (mSurfaceReceive == null)
				return;
			mSurfaceReceive.unlockCanvasAndPost(canvas);
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

	@Override
	public void record(URI arg0, RTC[] arg1, Parameters arg2)
			throws MsControlException {
		isRecording = true;
	}

	@Override
	public void stop() {
		isRecording = false;
	}

}
