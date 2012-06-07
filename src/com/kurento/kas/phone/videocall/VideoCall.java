/*
Softphone application for Android. It can make video calls using SIP with different video formats and audio formats.
Copyright (C) 2011 Tikal Technologies

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 3
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kurento.kas.phone.videocall;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.kurento.commons.media.format.MediaSpec;
import com.kurento.commons.media.format.Payload;
import com.kurento.commons.media.format.SessionSpec;
import com.kurento.commons.media.format.enums.MediaType;
import com.kurento.commons.media.format.enums.Mode;
import com.kurento.commons.media.format.payload.PayloadRtp;
import com.kurento.commons.mscontrol.MsControlException;
import com.kurento.commons.mscontrol.Parameters;
import com.kurento.commons.mscontrol.join.Joinable;
import com.kurento.commons.mscontrol.join.Joinable.Direction;
import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.mscontrol.networkconnection.SdpPortManagerException;
import com.kurento.kas.mscontrol.MSControlFactory;
import com.kurento.kas.mscontrol.MediaSessionAndroid;
import com.kurento.kas.mscontrol.mediacomponent.AndroidAction;
import com.kurento.kas.mscontrol.mediacomponent.AndroidInfo;
import com.kurento.kas.mscontrol.mediacomponent.MediaComponentAndroid;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.preferences.Connection_Preferences;
import com.kurento.kas.phone.preferences.VideoCall_Preferences;
import com.kurento.kas.phone.preferences.Video_Preferences;
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.softphone.R;
import com.kurento.kas.phone.softphone.ServiceUpdateUIListener;

public class VideoCall extends Activity implements ServiceUpdateUIListener {
	private static final String LOG_TAG = VideoCall.class.getName();
	private static final int SHOW_PREFERENCES = 1;
	private PowerManager.WakeLock wl;
	private boolean hang = false;
	private Map<MediaType, Mode> callDirectionMap, mediaTypesModes;
	private int cameraFacing = 0;

	private MediaComponentAndroid videoPlayerComponent = null;
	private MediaComponentAndroid videoRecorderComponent = null;
	private MediaComponentAndroid audioRecorderComponent = null;
	private MediaComponentAndroid audioPlayerComponent = null;

	private NetworkConnection nc;

	private Integer audioBW, videoBW;
	private Timer timer;
	private String infoBW;

	private Boolean isOccult = true;
	private WakeLock mWakeLock = null;

	private TextView txt_bandwidth;
	private Handler mHandler;

	private View videoCaptureSurface;

	private Boolean isStarted = true;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		VideoCallService.setUpdateListener(this);
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

		mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, "K-Phone");
		mWakeLock.acquire();

		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		callDirectionMap = (Map<MediaType, Mode>) ApplicationContext.contextTable
				.get("callDirection");
		mediaTypesModes = (Map<MediaType, Mode>) ApplicationContext.contextTable
				.get("mediaTypesModes");

		try {
			cameraFacing = (Integer) ApplicationContext.contextTable
					.get("cameraFacing");
		} catch (Exception e) {
			cameraFacing = 0;
		}

		Mode videoMode = null;
		if (mediaTypesModes != null)
			videoMode = mediaTypesModes.get(MediaType.VIDEO);

		if ((videoMode != null) && (Mode.RECVONLY.equals(videoMode)))
			setContentView(R.layout.videocall_receive);
		else if ((videoMode != null) && (Mode.SENDONLY.equals(videoMode)))
			setContentView(R.layout.videocall_send);
		else if ((videoMode != null) && (Mode.SENDRECV.equals(videoMode)))
			setContentView(R.layout.videocall);
		else {
			setContentView(R.layout.onlycall);
		}

		hang = false;

		audioBW = 0;
		videoBW = 0;
		mHandler = new Handler();
		timer = new Timer();

		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");

		if (controller != null) {
			nc = (NetworkConnection) ApplicationContext.contextTable.get("nc");

			MediaSessionAndroid mediaSession = controller.getMediaSession();

			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			int Orientation = getWindowManager().getDefaultDisplay()
					.getOrientation();
			try {
				txt_bandwidth = (TextView) findViewById(R.id.txt_bandwidth);

				if ((videoMode != null)
						&& (Mode.SENDONLY.equals(videoMode) || Mode.SENDRECV
								.equals(videoMode))) {
					videoCaptureSurface = (View) findViewById(R.id.video_capture_surface);
					Parameters params = MSControlFactory.createParameters();
					params.put(MediaComponentAndroid.PREVIEW_SURFACE,
							videoCaptureSurface);
					params.put(MediaComponentAndroid.DISPLAY_ORIENTATION,
							Orientation);
					params.put(MediaComponentAndroid.CAMERA_FACING,
							cameraFacing);
					videoPlayerComponent = mediaSession.createMediaComponent(
							MediaComponentAndroid.VIDEO_PLAYER, params);
				}
			} catch (MsControlException e) {
				Log.e(LOG_TAG, "MsControl + " + e.toString());
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(LOG_TAG, "Exception " + e.toString());
			}

			try {
				if ((videoMode != null)
						&& (Mode.RECVONLY.equals(videoMode) || Mode.SENDRECV
								.equals(videoMode))) {
					Parameters params = MSControlFactory.createParameters();
					params.put(MediaComponentAndroid.VIEW_SURFACE,
							(View) findViewById(R.id.video_receive_surface));
					params.put(MediaComponentAndroid.DISPLAY_WIDTH,
							dm.widthPixels);
					params.put(MediaComponentAndroid.DISPLAY_HEIGHT,
							dm.heightPixels);
					videoRecorderComponent = mediaSession.createMediaComponent(
							MediaComponentAndroid.VIDEO_RECORDER, params);
				}
			} catch (MsControlException e) {
				Log.e(LOG_TAG, e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage());
			}
		} else
			Log.e(LOG_TAG, "Controller is null");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(LOG_TAG, "OnNewIntent");
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	private void hang() {
		Log.d(LOG_TAG, "Hang ...");
		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");
		if (controller != null) {
			ApplicationContext.contextTable.remove("videoCall");
			controller.hang();
			hang = true;
		}
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "HANG = " + hang);
		if (!hang) {
			startGetMediaInfo();
			// NetworkConnection nc = (NetworkConnection)
			// ApplicationContext.contextTable
			// .get("networkConnection");
			Joinable videoJoinable = (Joinable) ApplicationContext.contextTable
					.get("videoJoinable");
			// Log.e(LOG_TAG, "nc: " + nc);
			// if (nc == null) {
			// Log.e(LOG_TAG, "networkConnection is NULL");
			// return;
			// }
			// TODO: Review. Not do it when the button Camara has been pushed

			try {
				if (videoPlayerComponent != null) {
					videoPlayerComponent.join(Direction.SEND, videoJoinable);
					videoPlayerComponent.start();
				}
				if (videoRecorderComponent != null) {
					videoRecorderComponent.join(Direction.RECV, videoJoinable);
					videoRecorderComponent.start();
				}

			} catch (MsControlException e) {
				Log.e(LOG_TAG, e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage());
			}

			final Button buttonTerminateCall = (Button) findViewById(R.id.button_terminate_call);
			buttonTerminateCall.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					hang();
				}
			});

			final Button buttonMute = (Button) findViewById(R.id.button_mute);

			buttonMute.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					audioPlayerComponent = (MediaComponentAndroid) ApplicationContext.contextTable
							.get("audioPlayerComponent");

					try {
						if (audioPlayerComponent != null) {
							// NetworkConnection nc = (NetworkConnection)
							// ApplicationContext.contextTable
							// .get("networkConnection");
							// if (nc == null) {
							// Log.e(LOG_TAG, "networkConnection is NULL");
							// return;
							// }
							Joinable audioJoinable = (Joinable) ApplicationContext.contextTable
									.get("audioJoinable");

							Boolean mute = (Boolean) ApplicationContext.contextTable
									.get("mute");
							if (mute != null) {
								if (mute)
									audioPlayerComponent.join(Direction.SEND,
											audioJoinable);
								else
									audioPlayerComponent.unjoin(audioJoinable);

								mute = !mute;
								ApplicationContext.contextTable.put("mute",
										mute);
							}
						}
					} catch (MsControlException e) {
						Log.e(LOG_TAG, "Exception Mute. " + e.toString());
						e.printStackTrace();
					}
				}
			});
			try {
				final Button buttonCamera = (Button) findViewById(R.id.button_camera);

				buttonCamera.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Log.d(LOG_TAG, "Button Push");

						// Log.d(LOG_TAG, "VideoPlayercomponent is Stop");
						Controller controller = (Controller) ApplicationContext.contextTable
								.get("controller");
						// NetworkConnection nc = (NetworkConnection)
						// ApplicationContext.contextTable
						// .get("networkConnection");
						Joinable videoJoinable = (Joinable) ApplicationContext.contextTable
								.get("videoJoinable");
						if (videoPlayerComponent != null) {
							videoPlayerComponent.release();
							try {
								videoPlayerComponent.unjoin(videoJoinable);
							} catch (MsControlException e) {
								// TODO Auto-generated catch block
								Log.e(LOG_TAG,
										"Exception unjoin " + e.toString());
								e.printStackTrace();
							}
							videoPlayerComponent = null;

						}
						Log.d(LOG_TAG, "videoPlayercomonent is null, it's Ok");
						if (controller != null) {
							MediaSessionAndroid mediaSession = controller
									.getMediaSession();

							Log.d(LOG_TAG, "Create MediaSession");

							DisplayMetrics dm = new DisplayMetrics();
							getWindowManager().getDefaultDisplay().getMetrics(
									dm);
							int Orientation = getWindowManager()
									.getDefaultDisplay().getOrientation();
							Parameters params = MSControlFactory
									.createParameters();
							params.put(
									MediaComponentAndroid.PREVIEW_SURFACE,
									(View) findViewById(R.id.video_capture_surface));
							params.put(
									MediaComponentAndroid.DISPLAY_ORIENTATION,
									Orientation);
							if (cameraFacing == 0) {
								params.put(MediaComponentAndroid.CAMERA_FACING,
										1);
								cameraFacing = 1;
							} else {
								params.put(MediaComponentAndroid.CAMERA_FACING,
										0);
								cameraFacing = 0;
							}
							ApplicationContext.contextTable.put("cameraFacing",
									cameraFacing);
							try {
								videoPlayerComponent = mediaSession
										.createMediaComponent(
												MediaComponentAndroid.VIDEO_PLAYER,
												params);
								Log.d(LOG_TAG,
										"Create new videoPlayerComponent");
								videoPlayerComponent.join(Direction.SEND,
										videoJoinable);
								videoPlayerComponent.start();
								Log.d(LOG_TAG,
										"Create videoPlayercomponent start");
							} catch (MsControlException e) {
								// TODO Auto-generated catch block
								Log.d(LOG_TAG,
										"Exception button Camera "
												+ e.toString());
								e.printStackTrace();
							}
						}
					}
				});
			} catch (Exception e) {
				Log.d(LOG_TAG, "This button doesn't exist in xml");
			}

			final Button buttonSpeaker = (Button) findViewById(R.id.button_headset);

			buttonSpeaker.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					Controller controller = (Controller) ApplicationContext.contextTable
							.get("controller");
					if (controller == null) {
						Log.e(LOG_TAG, "controller is NULL");
						return;
					}

					// NetworkConnection nc = (NetworkConnection)
					// ApplicationContext.contextTable
					// .get("networkConnection");
					// if (nc == null) {
					// Log.e(LOG_TAG, "networkConnection is NULL");
					// return;
					// }

					Joinable audioJoinable = (Joinable) ApplicationContext.contextTable
							.get("audioJoinable");

					MediaSessionAndroid mediaSession = controller
							.getMediaSession();
					audioRecorderComponent = (MediaComponentAndroid) ApplicationContext.contextTable
							.get("audioRecorderComponent");
					try {
						if (audioRecorderComponent != null) {
							audioRecorderComponent.stop();
							audioRecorderComponent.unjoin(audioJoinable);
						}

						Parameters params = MSControlFactory.createParameters();

						Boolean speaker = (Boolean) ApplicationContext.contextTable
								.get("speaker");
						if (speaker != null) {

							if (speaker)
								params.put(MediaComponentAndroid.STREAM_TYPE,
										AudioManager.STREAM_MUSIC);
							else
								params.put(MediaComponentAndroid.STREAM_TYPE,
										AudioManager.STREAM_VOICE_CALL);

							speaker = !speaker;
							ApplicationContext.contextTable.put("speaker",
									speaker);

							audioRecorderComponent = mediaSession
									.createMediaComponent(
											MediaComponentAndroid.AUDIO_RECORDER,
											params);
							ApplicationContext.contextTable.put(
									"audioRecorderComponent",
									audioRecorderComponent);

							if (audioRecorderComponent != null) {
								audioRecorderComponent.join(Direction.RECV,
										audioJoinable);
								audioRecorderComponent.start();

							}
						}
					} catch (MsControlException e) {
						Log.e(LOG_TAG,
								"Exception change speaker." + e.toString());
						e.printStackTrace();
					}
				}
			});

			final Button btnOccult = (Button) findViewById(R.id.btnOcculPanel);

			btnOccult.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					final TableLayout t = (TableLayout) findViewById(R.id.tableLayout1);

					Animation a = new Animation() {
					};
					if (!isOccult) {
						a = new TranslateAnimation(100, 0, 0, 0);
						a.setDuration(50L);
						a.setInterpolator(new AccelerateDecelerateInterpolator());
						t.setAnimation(a);
						t.startAnimation(a);
						a.setAnimationListener(new AnimationListener() {

							@Override
							public void onAnimationStart(Animation animation) {

								t.layout(t.getLeft() - 100, t.getTop(),
										t.getRight() - 100, t.getBottom());
								btnOccult
										.setBackgroundResource(R.drawable.occult_menu_in);
							}

							@Override
							public void onAnimationEnd(Animation animation) {
								btnOccult
										.setBackgroundResource(R.drawable.occult_menu_out);
							}

							@Override
							public void onAnimationRepeat(Animation animation) {

							}
						});
						isOccult = true;
					} else {
						isOccult = false;
						a = new TranslateAnimation(0, 100, 0, 0);
						a.setDuration(50L);

						a.setInterpolator(new AccelerateDecelerateInterpolator());
						t.setAnimation(a);
						t.startAnimation(a);
						a.setAnimationListener(new AnimationListener() {

							@Override
							public void onAnimationStart(Animation animation) {
								btnOccult
										.setBackgroundResource(R.drawable.occult_menu_out);
							}

							@Override
							public void onAnimationRepeat(Animation animation) {

							}

							@Override
							public void onAnimationEnd(Animation animation) {
								t.layout(t.getLeft() + 100, t.getTop(),
										t.getRight() + 100, t.getBottom());
								btnOccult
										.setBackgroundResource(R.drawable.occult_menu_in);
							}
						});
					}
				}
			});

			if (videoCaptureSurface != null) {
				videoCaptureSurface.setOnTouchListener(new OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if (videoPlayerComponent != null)
							try {
								videoPlayerComponent
										.onAction(AndroidAction.CAMERA_AUTOFOCUS);
							} catch (MsControlException e) {
								Log.e(LOG_TAG, e.getMessage(), e);
							}
						return false;
					}
				});
			}

			Log.e(LOG_TAG, "onResume OK");
		}
	}

	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		ApplicationContext.contextTable.put("videoCall", getIntent());
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
			ApplicationContext.contextTable.put("videoCall", getIntent());
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void finish() {
		Log.d(LOG_TAG, "Finish");
		super.finish();
	}

	@Override
	protected void onPause() {
		Log.d(LOG_TAG, "OnPause");
		if (videoRecorderComponent != null)
			videoRecorderComponent.stop();
		if (videoPlayerComponent != null)
			videoPlayerComponent.stop();
		if (timer != null)
			timer.cancel();
		super.onPause();
	}

	@Override
	protected void onRestart() {
		Log.d(LOG_TAG, "OnRestart");
		super.onRestart();
	}

	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "OnStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "OnDestroy ");
		if (mWakeLock != null)
			mWakeLock.release();
		super.onDestroy();
	}

	/* Menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.videocall_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case (R.id.menu_videocall_preferences):
			Intent videoCallPreferences = new Intent(this,
					VideoCall_Preferences.class);
			startActivityForResult(videoCallPreferences, SHOW_PREFERENCES);
			return true;
		case (R.id.menu_videocall_info):
			Dialog dialog = new Dialog(this);
			dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
			String info_video = Video_Preferences
					.getMediaPreferencesInfo(getApplicationContext());
			info_video += "\n\n"
					+ Connection_Preferences
							.getConnectionNetPreferenceInfo(getApplicationContext());
			dialog.setContentView(R.layout.info_video);

			((TextView) dialog.findViewById(R.id.info_video))
					.setText("MEDIA NEGOCIATE\n" + getInfoSdp()
							+ "\n\n-------\nMEDIA CONFIGURATE\n " + info_video);

			dialog.show();

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SHOW_PREFERENCES) {
			Log.d(LOG_TAG, "Show preferences");
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());
		}
	}

	@Override
	public void update(Message message) {
		Log.d(LOG_TAG, "Message = " + message);
		finish();
	}

	private void startGetMediaInfo() {
		timer.schedule(new TimerTask() {
			public void run() {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						if (videoRecorderComponent != null) {
							try {
								videoBW = (Integer) videoRecorderComponent
										.getInfo(AndroidInfo.BIT_RATE);
							} catch (MsControlException e) {
								videoBW = 0;
							}
							infoBW = "V: " + videoBW;
						} else
							infoBW = "No Video";
						if (audioRecorderComponent != null) {
							try {
								audioBW = (Integer) audioRecorderComponent
										.getInfo(AndroidInfo.BIT_RATE);
							} catch (MsControlException e) {
								audioBW = 0;
							}
							infoBW = infoBW + " ---- A: " + audioBW;
						} else
							infoBW = infoBW + " ---- No Audio";
						txt_bandwidth.setText(infoBW);
					}
				});
			}
		}, 0, 1000);
	}

	private String getInfoSdp() {
		String info = "NC hasn't Info";
		if (nc != null) {
			SessionSpec sessionSpec;
			try {
				sessionSpec = nc.getSdpPortManager()
						.getMediaServerSessionDescription();
				info = "";
				for (MediaSpec media : sessionSpec.getMediaSpecs()) {
					if (media.getTypes().contains(MediaType.AUDIO))
						info += "\nAudio: \n";
					else if (media.getTypes().contains(MediaType.VIDEO))
						info += "\nVideo: \n";

					for (Payload payload : media.getPayloads()) {
						try {
							PayloadRtp infoRtp = payload.getRtp();
							info += infoRtp.toString() + "\n";
						} catch (Exception e) {
							continue;
						}
					}
				}
			} catch (SdpPortManagerException e1) {
				return "NC hasn't Info";
			} catch (MsControlException e1) {
				return "NC hasn't Info";
			}
			return info;
		}
		return "NC hasn't Info";

	}
}
