/*
 * libjingle
 * Copyright 2013, Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.kurento.kas.phone.media;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.VideoRenderer.I420Frame;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

/**
 * A GLSurfaceView{,.Renderer} that efficiently renders YUV frames from local &
 * remote VideoTracks using the GPU for CSC. Clients will want to call the
 * constructor, setSize() and updateFrame() as appropriate, but none of the
 * other public methods of this class are of interest to clients (only to system
 * classes).
 */
public class VideoStreamView extends GLSurfaceView implements
		GLSurfaceView.Renderer {

	private static final Logger log = LoggerFactory
			.getLogger(VideoStreamView.class.getSimpleName());

	private ViewGroup parent;
	private Preview preview;

	private int viewWidth = -1;
	private int viewHeight = -1;
	private int remoteWidth = -1;
	private int remoteHeight = -1;
	private int localWidth = -1;
	private int localHeight = -1;

	/** Identify which of the two video streams is being addressed. */
	public static enum Endpoint {
		LOCAL, REMOTE
	};

	private final static String TAG = "VideoStreamsView";
	// [0] are local Y,U,V, [1] are remote Y,U,V.
	private int[][] yuvTextures = { { -1, -1, -1 }, { -1, -1, -1 } };
	private int posLocation = -1;
	private long lastFPSLogTime = System.nanoTime();
	private long numFramesSinceLastLog = 0;
	private FramePool framePool = new FramePool();

	public VideoStreamView(ViewGroup v) {
		super(v.getContext());
		parent = v;

		preview = new Preview(this);
		preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		parent.addView(preview);

		setEGLConfigChooser(false); // Don't need a depth buffer.
		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(RENDERMODE_WHEN_DIRTY);
	}

	/** Queue |frame| to be uploaded. */
	public void queueFrame(final Endpoint stream, I420Frame frame) {
		// Paying for the copy of the YUV data here allows CSC and painting time
		// to get spent on the render thread instead of the UI thread.
		abortUnless(framePool.validateDimensions(frame), "Frame too large!");
		final I420Frame frameCopy = framePool.takeFrame(frame).copyFrom(frame);
		queueEvent(new Runnable() {
			public void run() {
				updateFrame(stream, frameCopy);
			}
		});
	}

	// Upload the planes from |frame| to the textures owned by this View.
	private void updateFrame(Endpoint stream, I420Frame frame) {
		log.debug("updateFrame endpoint: " + stream);
		int[] textures = yuvTextures[stream == Endpoint.LOCAL ? 0 : 1];
		texImage2D(frame, textures);
		framePool.returnFrame(frame);
		requestRender();
	}

	private synchronized void resize() {
		log.debug("resize");
		if (viewWidth == -1 || viewHeight == -1)
			return;

		GLES20.glViewport(0, 0, viewWidth, viewHeight);

		if (localWidth != -1 && localHeight != -1) {
			int[] textures = yuvTextures[0];
			GLES20.glGenTextures(3, textures, 0);
			for (int i = 0; i < 3; ++i) {
				int w = i == 0 ? localWidth : localWidth / 2;
				int h = i == 0 ? localHeight : localHeight / 2;
				GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
				GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
						GLES20.GL_LUMINANCE, w, h, 0, GLES20.GL_LUMINANCE,
						GLES20.GL_UNSIGNED_BYTE, null);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			}
		}

		if (remoteWidth != -1 && remoteHeight != -1) {
			int[] textures = yuvTextures[1];
			GLES20.glGenTextures(3, textures, 0);
			for (int i = 0; i < 3; ++i) {
				int w = i == 0 ? remoteWidth : remoteWidth / 2;
				int h = i == 0 ? remoteHeight : remoteHeight / 2;
				GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
				GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
						GLES20.GL_LUMINANCE, w, h, 0, GLES20.GL_LUMINANCE,
						GLES20.GL_UNSIGNED_BYTE, null);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			}
		}

		checkNoGLES2Error();
	}

	/** Inform this View of the dimensions of frames coming from |stream|. */
	public synchronized void setSize(Endpoint stream, int width, int height) {
		log.debug("setSize endpoint: " + stream);
		// Generate 3 texture ids for Y/U/V and place them into |textures|,
		// allocating enough storage for |width|x|height| pixels.
		if (Endpoint.LOCAL.equals(stream)) {
			localWidth = width;
			localHeight = height;
		} else {
			remoteWidth = width;
			remoteHeight = height;
		}

		resize();
	}

	@Override
	public synchronized void onSurfaceChanged(GL10 unused, int width, int height) {
		log.debug("onSurfaceChanged: " + width + "x" + height);
		viewWidth = width;
		viewHeight = height;
		resize();
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		drawRectangle(yuvTextures[1], remoteVertices);
		drawRectangle(yuvTextures[0], localVertices);
		++numFramesSinceLastLog;
		long now = System.nanoTime();
		if (lastFPSLogTime == -1 || now - lastFPSLogTime > 1e9) {
			double fps = numFramesSinceLastLog / ((now - lastFPSLogTime) / 1e9);
			Log.d(TAG, "Rendered FPS: " + fps);
			lastFPSLogTime = now;
			numFramesSinceLastLog = 1;
		}
		checkNoGLES2Error();
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		int program = GLES20.glCreateProgram();
		addShaderTo(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_STRING, program);
		addShaderTo(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_STRING, program);

		GLES20.glLinkProgram(program);
		int[] result = new int[] { GLES20.GL_FALSE };
		result[0] = GLES20.GL_FALSE;
		GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, result, 0);
		abortUnless(result[0] == GLES20.GL_TRUE,
				GLES20.glGetProgramInfoLog(program));
		GLES20.glUseProgram(program);

		GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "y_tex"), 0);
		GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_tex"), 1);
		GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "v_tex"), 2);

		// Actually set in drawRectangle(), but queried only once here.
		posLocation = GLES20.glGetAttribLocation(program, "in_pos");

		int tcLocation = GLES20.glGetAttribLocation(program, "in_tc");
		GLES20.glEnableVertexAttribArray(tcLocation);
		GLES20.glVertexAttribPointer(tcLocation, 2, GLES20.GL_FLOAT, false, 0,
				textureCoords);

		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		checkNoGLES2Error();
	}

	// Wrap a float[] in a direct FloatBuffer using native byte order.
	private static FloatBuffer directNativeFloatBuffer(float[] array) {
		FloatBuffer buffer = ByteBuffer.allocateDirect(array.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		buffer.put(array);
		buffer.flip();
		return buffer;
	}

	// Upload the YUV planes from |frame| to |textures|.
	private void texImage2D(I420Frame frame, int[] textures) {
		for (int i = 0; i < 3; ++i) {
			ByteBuffer plane = frame.yuvPlanes[i];
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
			int w = i == 0 ? frame.width : frame.width / 2;
			int h = i == 0 ? frame.height : frame.height / 2;
			abortUnless(w == frame.yuvStrides[i], frame.yuvStrides[i] + "!="
					+ w);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
					w, h, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
					plane);
		}
		checkNoGLES2Error();
	}

	// Draw |textures| using |vertices| (X,Y coordinates).
	private void drawRectangle(int[] textures, FloatBuffer vertices) {
		for (int i = 0; i < 3; ++i) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
		}

		GLES20.glVertexAttribPointer(posLocation, 2, GLES20.GL_FLOAT, false, 0,
				vertices);
		GLES20.glEnableVertexAttribArray(posLocation);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		checkNoGLES2Error();
	}

	// Compile & attach a |type| shader specified by |source| to |program|.
	private static void addShaderTo(int type, String source, int program) {
		int[] result = new int[] { GLES20.GL_FALSE };
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, source);
		GLES20.glCompileShader(shader);
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, result, 0);
		abortUnless(result[0] == GLES20.GL_TRUE,
				GLES20.glGetShaderInfoLog(shader) + ", source: " + source);
		GLES20.glAttachShader(program, shader);
		GLES20.glDeleteShader(shader);
		checkNoGLES2Error();
	}

	// Poor-man's assert(): die with |msg| unless |condition| is true.
	private static void abortUnless(boolean condition, String msg) {
		if (!condition) {
			throw new RuntimeException(msg);
		}
	}

	// Assert that no OpenGL ES 2.0 error has been raised.
	private static void checkNoGLES2Error() {
		int error = GLES20.glGetError();
		abortUnless(error == GLES20.GL_NO_ERROR, "GLES20 error: " + error);
	}

	// Remote image should span the full screen.
	private static final FloatBuffer remoteVertices = directNativeFloatBuffer(new float[] {
			-1, 1, -1, -1, 1, 1, 1, -1 });

	// Local image should be thumbnailish.
	private static final FloatBuffer localVertices = directNativeFloatBuffer(new float[] {
			0.4f, -0.4f, 0.4f, -0.95f, 0.95f, -0.4f, 0.95f, -0.95f });

	// Texture Coordinates mapping the entire texture.
	private static final FloatBuffer textureCoords = directNativeFloatBuffer(new float[] {
			0, 0, 0, 1, 1, 0, 1, 1 });

	// Pass-through vertex shader.
	private static final String VERTEX_SHADER_STRING = "varying vec2 interp_tc;\n"
			+ "\n"
			+ "attribute vec4 in_pos;\n"
			+ "attribute vec2 in_tc;\n"
			+ "\n"
			+ "void main() {\n"
			+ "  gl_Position = in_pos;\n"
			+ "  interp_tc = in_tc;\n" + "}\n";

	// YUV to RGB pixel shader. Loads a pixel from each plane and pass through
	// the
	// matrix.
	private static final String FRAGMENT_SHADER_STRING = "precision mediump float;\n"
			+ "varying vec2 interp_tc;\n"
			+ "\n"
			+ "uniform sampler2D y_tex;\n"
			+ "uniform sampler2D u_tex;\n"
			+ "uniform sampler2D v_tex;\n"
			+ "\n"
			+ "void main() {\n"
			+ "  float y = texture2D(y_tex, interp_tc).r;\n"
			+ "  float u = texture2D(u_tex, interp_tc).r - .5;\n"
			+ "  float v = texture2D(v_tex, interp_tc).r - .5;\n"
			+
			// CSC according to http://www.fourcc.org/fccyvrgb.php
			"  gl_FragColor = vec4(y + 1.403 * v, "
			+ "                      y - 0.344 * u - 0.714 * v, "
			+ "                      y + 1.77 * u, 1);\n" + "}\n";

	private class Preview extends ViewGroup {
		private VideoStreamView view;

		Preview(VideoStreamView v) {
			super(v.getContext());
			this.view = v;
			addView(view);
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			log.debug("onLayout " + changed + " - " + l + " - " + t + " - " + r
					+ " - " + b);
			try {
				view.layout(l, t, r, b);
			} catch (Throwable th) {
				log.error("Error resizing", th);
			}
		}
	}

}