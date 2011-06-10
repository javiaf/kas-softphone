package com.tikal.softphone;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tikal.android.media.AudioCodec;
import com.tikal.android.media.VideoCodec;
import com.tikal.applicationcontext.ApplicationContext;
import com.tikal.media.AudioInfo;
import com.tikal.media.IRTPMedia;
import com.tikal.media.MediaControl;
import com.tikal.media.RTPInfo;
import com.tikal.media.VideoInfo;
import com.tikal.media.format.SessionSpec;
import com.tikal.media.format.SpecTools;
import com.tikal.preferences.Connection_Preferences;
import com.tikal.preferences.Video_Preferences;
import com.tikal.sip.Controller;

import com.tikal.videocall.VideoCall;

public class SoftPhone extends Activity implements IRTPMedia, IPhoneGUI {
	private static int MEDIA_CONTROL = 0;
	private static int SHOW_PREFERENCES = 1;
	private static int VIDEO_CALL = 2;
	static final int PICK_CONTACT_REQUEST = 3;

	private static final String LOG_TAG = "SoftPhone";

	private VideoInfo vi;
	private AudioInfo ai;
	private String localUser;
	private String localRealm;
	private String proxyIP;
	private int proxyPort;

	private Handler handler = new Handler();

	private TextView text;

	private Controller controller;

	/** Called when the activity is first created. */
	/* Cycle Life */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		PreferenceManager.setDefaultValues(this, R.layout.video_preferences,
				true);
		initControllerUAFromSettings();
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
				"GdC");

		// Estoy registrado?
		checkCallIntent();

	}

	private void checkCallIntent() {
		Intent intent = getIntent();
		if (intent.getData() == null)
			return;

		if (Intent.ACTION_CALL.equalsIgnoreCase(intent.getAction())) {
			Log.d(LOG_TAG, "CALL" + intent.getData().getSchemeSpecificPart());
		} else if (Intent.ACTION_SENDTO.equals(intent.getAction())) {
			Log.d(LOG_TAG, "sip:" + intent.getData().getLastPathSegment());
			if (controller == null)
				register();
			call("sip:" + intent.getData().getLastPathSegment());
		}
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		final Button buttonCall = (Button) findViewById(R.id.simulate_call);
		buttonCall.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (controller.getUa() == null)
					initControllerUAFromSettings();
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(getBaseContext());
				String remoteURI = "sip:"
						+ settings.getString("REMOTE_USERNAME", "pc1") + "@"
						+ settings.getString("REMOTE_DOMAIN", "urjc.es");
				try {
					Log.d(LOG_TAG, "remoteURI: " + remoteURI);
					controller.call(remoteURI);
				} catch (Exception e) {
					Log.e(LOG_TAG, e.toString());
					e.printStackTrace();
				}

				// Intent mediaIntent = new Intent(SoftPhone.this,
				// MediaControl.class);
				// startActivityForResult(mediaIntent, MEDIA_CONTROL);
			}
		});

		final Button buttonContacts = (Button) findViewById(R.id.contacts);
		buttonContacts.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					Intent intentContacts = new Intent(Intent.ACTION_PICK,
							ContactsContract.Contacts.CONTENT_URI);

					startActivityForResult(intentContacts, PICK_CONTACT_REQUEST);
				} catch (Exception e) {
					Log.e("Error Search", e.toString());
				}

			}
		});
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		try {
			controller.finishUA();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**** End Cycle Life */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == MEDIA_CONTROL) {
			if (resultCode == RESULT_OK) {
				Log.d(LOG_TAG, "Accept Call on onActivityResult");
				try {
					controller.aceptCall();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (resultCode == RESULT_CANCELED) {
				Log.d(LOG_TAG, "Rejected Call");
				try {
					controller.reject();
				} catch (Exception e) {

					e.printStackTrace();
				}
				// Meter notificación de cancelación

			} else
				Log.d(LOG_TAG, "Media Control; ResultCode = " + resultCode);
		}

		if (requestCode == VIDEO_CALL) {
			Log.d(LOG_TAG, "Video Call Finish");

		}
		if (requestCode == SHOW_PREFERENCES) {
			/*
			 * CARGAR LAS PREFERENCIAS
			 */
			initControllerUAFromSettings();

		}

		if (requestCode == PICK_CONTACT_REQUEST) {
			if (resultCode == RESULT_OK) {
				// A contact was picked. Here we will just display it
				// to the user.
				try {
					String id = null;
					String name = null;
					String number = null;
					String note = null;
					String sip = null;
					String imType = "";
					/* En Data esta el resultado de pulsar a una persona */
					Cursor cursor = getContentResolver().query(data.getData(),
							null, null, null, null);

					if (cursor.moveToFirst()) {
						int nameIdx = cursor
								.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
						int idIdx = cursor
								.getColumnIndexOrThrow(ContactsContract.Data._ID);

						id = cursor.getString(idIdx);
						name = cursor.getString(nameIdx);

						Log.e(LOG_TAG, "Valor de nameIdx:" + nameIdx + " ID: "
								+ id);

						// Leer las notas del usuario si tine

						String[] columns = new String[] { ContactsContract.CommonDataKinds.Note.NOTE };
						String where = ContactsContract.Data.RAW_CONTACT_ID
								+ " = ? AND " + ContactsContract.Data.MIMETYPE
								+ " = ?";
						String[] whereParameters = new String[] {
								id,
								ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE };

						Cursor contacts = getContentResolver().query(
								ContactsContract.Data.CONTENT_URI, null, where,
								whereParameters, null);
						Log.e(LOG_TAG,
								"Valor de contacts:" + contacts.getCount());
						;
						String rv = null;
						if (contacts.moveToFirst()) {
							Log.e(LOG_TAG,
									"Valor de las colunas:"
											+ contacts.getColumnCount());
							for (int i = 0; i < contacts.getColumnCount(); i++)
								Log.e(LOG_TAG,
										"Name:" + contacts.getColumnName(i)
												+ "; ValueIm = "
												+ contacts.getString(i));

							note = contacts
									.getString(contacts
											.getColumnIndexOrThrow(ContactsContract.Data.DATA1));
						}
						contacts.close();

						// Leer las IM del usuario si tine

						String[] columnsIm = new String[] { ContactsContract.CommonDataKinds.Im.PROTOCOL };

						String whereIm = ContactsContract.Data.RAW_CONTACT_ID
								+ " = ? AND " + ContactsContract.Data.MIMETYPE
								+ " = ?";
						String[] whereParametersIm = new String[] {
								id,
								ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE };

						Cursor contactsIm = getContentResolver().query(
								ContactsContract.Data.CONTENT_URI, null,
								whereIm, whereParametersIm, null);
						Log.e(LOG_TAG,
								"Valor de contacts:" + contactsIm.getCount());
						;
						String rvIm = null;
						if (contactsIm.moveToFirst()) {
							Log.e(LOG_TAG,
									"Valor de las colunas:"
											+ contactsIm.getColumnCount());
							for (int i = 0; i < contactsIm.getColumnCount(); i++)
								Log.e(LOG_TAG,
										"NameIm:" + contactsIm.getColumnName(i)
												+ "; ValueIm = "
												+ contactsIm.getString(i));

							rvIm = "Protocolo: "
									+ contactsIm
											.getString(contactsIm
													.getColumnIndexOrThrow(ContactsContract.Data.DATA5))
									+ " Custom: "
									+ contactsIm
											.getString(contactsIm
													.getColumnIndexOrThrow(ContactsContract.Data.DATA6))
									+ " Data: "
									+ contactsIm
											.getString(contactsIm
													.getColumnIndexOrThrow(ContactsContract.Data.DATA1));
							// sip = contactsIm
							// .getString(contactsIm
							// .getColumnIndexOrThrow(ContactsContract.Data.DATA1));
							sip = contactsIm
									.getString(contactsIm
											.getColumnIndexOrThrow(ContactsContract.Data.DATA1));
						}
						contactsIm.close();

						Log.e(LOG_TAG, "Valor de nameIdx:" + nameIdx + " ID: "
								+ id + " IM: " + rvIm);

					}

					if (sip != null) {
						Toast.makeText(SoftPhone.this, name + ", SIP:" + sip,
								Toast.LENGTH_SHORT).show();
						call("sip:" + sip);

					} else
						Toast.makeText(SoftPhone.this,
								name + ", No tiene SIP:", Toast.LENGTH_SHORT)
								.show();

					// startActivity(intent);
				} catch (Exception e) {
					Log.e("onActivityResult", e.toString());
				}
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);

		Log.d(LOG_TAG, "Se está intentando crear una nueva actividad...");
	}

	private void call(String remoteURI) {
		if (controller.getUa() == null)
			initControllerUAFromSettings();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (remoteURI == null) {
			remoteURI = "sip:" + settings.getString("REMOTE_USERNAME", "pc1")
					+ "@" + settings.getString("REMOTE_DOMAIN", "urjc.es");
		}
		try {
			Log.d(LOG_TAG, "remoteURI: " + remoteURI);
			controller.call(remoteURI);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
			e.printStackTrace();
		}
	}

	/* Menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();

		inflater.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case (R.id.menu_exit):
			finish();
			return true;
		case (R.id.menu_conection_preferences):
			Intent localPreferences = new Intent(this,
					Connection_Preferences.class);
			startActivityForResult(localPreferences, SHOW_PREFERENCES);
			return true;
		case (R.id.menu_video_preferences):
			Intent remotePreferences = new Intent(this, Video_Preferences.class);
			startActivityForResult(remotePreferences, SHOW_PREFERENCES);
			return true;
		case (R.id.menu_register):
			// ****Revisar si hay que comprobar si los datos han cambiado antes
			// de crear o registar de nuevo.
			// text = (TextView)findViewById(R.id.textRegister);
			// text.setText("Registering...");

			register();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void register() {
		controller = new Controller(this, this);
		initControllerUAFromSettings();
		initUA();
	}

	@Override
	public void inviteReceived(String uri) {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "Invite received");
		Intent mediaIntent = new Intent(SoftPhone.this, MediaControl.class);
		mediaIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		mediaIntent.putExtra("Uri", uri);
		startActivityForResult(mediaIntent, MEDIA_CONTROL);
		Log.d(LOG_TAG, "Media Control Started");
	}

	@Override
	public void registerSucessful() {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "Register Sucessful");

		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
				SoftPhone.this.text.setTextSize(20);
				SoftPhone.this.text.setTextColor(Color.GREEN);
				SoftPhone.this.text.setText("Register Sucessful");
			}
		});

	}

	@Override
	public void registerFailed() {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "Register Failed");
		handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
				SoftPhone.this.text.setTextSize(20);
				SoftPhone.this.text.setTextColor(Color.RED);
				SoftPhone.this.text.setText("Register Failed");
			}
		});

	}

	@Override
	public void startRTPMedia(RTPInfo rtpInfo, SessionSpec sdp) { // Sesion Spec
																	// en vez de
																	// String
		// Cuando la llamada está aceptada y se puede enviar audio y video.
		Log.d(LOG_TAG, "startRTPMedia");
		VideoInfo videoInfo = getVideoInfoFromSettings();
		videoInfo.setCodecID(rtpInfo.getVideoCodecId());
		videoInfo.setOut(rtpInfo.getVideoRTPDir());
		videoInfo.setMode(VideoInfo.MODE_SEND_RTP);
		videoInfo.setPayloadType(rtpInfo.getVideoPayloadType());

		AudioInfo audioInfo = getAudioInfoFromSettings();
		audioInfo.setCodecID(rtpInfo.getAudioCodecId());
		audioInfo.setOut(rtpInfo.getAudioRTPDir());
		audioInfo.setPayloadType(rtpInfo.getAudioPayloadType());
		AudioCodec ac = AudioCodec.getInstance();
		audioInfo.setSample_rate(ac.getSampleRate(rtpInfo.getAudioCodecId()));
		audioInfo.setBit_rate(ac.getBitRate(rtpInfo.getAudioCodecId()));

		Log.d(LOG_TAG, "Accept Call: Sdp -> " + sdp.toString());

		String sdpAudio = "";
		String sdpVideo = "";

		if (!SpecTools.filterMediaByType(sdp, "audio").getMediaSpec().isEmpty())
			sdpAudio = SpecTools.filterMediaByType(sdp, "audio").toString();
		if (!SpecTools.filterMediaByType(sdp, "video").getMediaSpec().isEmpty())
			sdpVideo = SpecTools.filterMediaByType(sdp, "video").toString();

		Log.d(LOG_TAG, "Accept Call: SdpAudio -> " + sdpAudio.toString());
		Log.d(LOG_TAG, "Accept Call: SdpVideo -> " + sdpVideo.toString());

		Intent videoCallIntent = new Intent(SoftPhone.this, VideoCall.class);
		videoCallIntent.putExtra("VideoInfo", videoInfo);
		videoCallIntent.putExtra("AudioInfo", audioInfo);
		videoCallIntent.putExtra("SdpVideo", sdpVideo);
		videoCallIntent.putExtra("SdpAudio", sdpAudio);
		startActivityForResult(videoCallIntent, VIDEO_CALL);

	}

	@Override
	public void releaseRTPMedia() {
		Log.d(LOG_TAG, "ReleaseRTPMedia");
		finishActivity(VIDEO_CALL);
	}

	private VideoInfo getVideoInfoFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		int frame_rate = Integer
				.parseInt(settings.getString("FRAME_RATE", "0"));

		// FIXME Comprobar arrays
		String[] arraySizes = getResources()
				.getStringArray(R.array.video_sizes);
		String size = settings.getString("VIDEO_SIZE", arraySizes[0]);
		Log.d(LOG_TAG, "size: " + size);
		int select = -1;
		for (int pos = 0; pos < arraySizes.length; pos++)
			if (arraySizes[pos].equalsIgnoreCase(size)) {
				select = pos;
				break;
			}
		if (select < 0)
			select = 0;
		// FIXME throw Exception if select is -1

		Log.d(LOG_TAG, "select: " + select);
		int width = getResources().getIntArray(R.array.video_width)[select];
		int height = getResources().getIntArray(R.array.video_height)[select];

		String codecName = settings.getString("CODEC_NAME", "-");

		ArrayList<Integer> supportedCodecsID = new ArrayList<Integer>();
		if (settings.getBoolean("H263_CODEC", false))
			supportedCodecsID.add(VideoCodec.CODEC_ID_H263);
		if (settings.getBoolean("MPEG4_CODEC", false))
			supportedCodecsID.add(VideoCodec.CODEC_ID_MPEG4);
		if (settings.getBoolean("H264_CODEC", false))
			supportedCodecsID.add(VideoCodec.CODEC_ID_H264);

		String out = Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/DCIM/"
				+ settings.getString("VIDEO_OUT", "-");

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;

		return new VideoInfo(frame_rate, width, height, supportedCodecsID,
				codecName, out, "", screenWidth, screenHeight);
	}

	private AudioInfo getAudioInfoFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		String codecName = settings.getString("AUDIO_CODEC_NAME", "-");

		ArrayList<Integer> supportedCodecsID = new ArrayList<Integer>();
		if (settings.getBoolean("AMR_AUDIO_CODEC", false))
			supportedCodecsID.add(AudioCodec.CODEC_ID_AMR);
		if (settings.getBoolean("MP2_AUDIO_CODEC", false))
			supportedCodecsID.add(AudioCodec.CODEC_ID_MP2);
		if (settings.getBoolean("AAC_AUDIO_CODEC", false))
			supportedCodecsID.add(AudioCodec.CODEC_ID_AAC);

		String out = Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/DCIM/"
				+ settings.getString("AUDIO_OUT", "-");

		return new AudioInfo(supportedCodecsID, codecName, out);
	}

	private void initControllerUAFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		localUser = settings.getString("LOCAL_USERNAME", "android1");
		localRealm = settings.getString("LOCAL_DOMAIN", "urjc.es");
		proxyIP = settings.getString("PROXY_IP", "193.147.51.17");
		proxyPort = Integer.parseInt(settings.getString("PROXY_PORT", "5060"));
		vi = getVideoInfoFromSettings();
		ai = getAudioInfoFromSettings();
	}

	public static Context getContext() {
		return getContext();
	}

	private void initUA() {
		try {
			Log.d(LOG_TAG, "LocalUser : " + localUser + "; localReal : "
					+ localRealm);
			controller.initUA(this, vi, ai, proxyIP, proxyPort, localUser,
					localRealm);
			ApplicationContext.contextTable.put("controller", controller);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
			e.printStackTrace();
		}
	}

	/* Gestión de Contactos */

	public void Contacts() {

	}
}