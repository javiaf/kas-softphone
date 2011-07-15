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

public class VideoRecorder extends RecorderBase implements VideoRx,
		SurfaceHolder.Callback {
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

	public synchronized void setVideoSurfaceRx(View videoReceiveSurface,
			int displayWidth, int displayHeight) {
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

	public synchronized View getVideoSurfaceRx() {
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
	public synchronized void putVideoFrameRx(int[] rgb, int width, int height) {
		if (!isRecording)
			return;

		if (rgb == null || rgb.length == 0){
			Log.d(LOG_TAG, "RGB is null o length = 0");
			return;
		}

		try {
			if (mSurfaceReceive == null)
				return;

			canvas = mSurfaceReceive.lockCanvas(null);
			try {

				if (canvas == null) {
					Log.d(LOG_TAG, "canvas is null");
					return;
				}

				Bitmap srcBitmap = Bitmap.createBitmap(rgb, width, height,
						Bitmap.Config.ARGB_8888);
				RectF dirty2 = new RectF(0, 0, screenWidth, screenHeight);

				canvas.drawBitmap(srcBitmap, null, dirty2, null);

			} finally {
				mSurfaceReceive.unlockCanvasAndPost(canvas);
			}
		} catch (IllegalArgumentException e) {
			Log.e(LOG_TAG, "Exception: " + e.toString());
//			e.printStackTrace();
		} catch (OutOfResourcesException e) {
			Log.e(LOG_TAG, "Exception: " + e.toString());
//			e.printStackTrace();
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

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

		Log.d(LOG_TAG, "surface Changed = W: " + width + "; H: " + height);
		mSurfaceReceive = holder.getSurface();
		if (width < height) {
			this.screenWidth = width;
			this.screenHeight = height * 3 / 4;
		} else {
			this.screenWidth = width * 1 / 2;
			this.screenHeight = height * 3 / 4;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(LOG_TAG, "surfaceCreated");
		mSurfaceReceive = holder.getSurface();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(LOG_TAG, "surfaceDestroyed");

	}

}
