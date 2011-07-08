package com.tikal.softphone;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
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
import com.tikal.controlcontacts.ControlContacts;
import com.tikal.media.AudioInfo;
import com.tikal.media.MediaControlOutgoing;
import com.tikal.media.VideoInfo;
import com.tikal.preferences.Connection_Preferences;
import com.tikal.preferences.Video_Preferences;
import com.tikal.sip.Controller;

public class SoftPhone extends Activity implements ServiceUpdateUIListener {
	private final int MEDIA_CONTROL_OUTGOING = 0;
	private final int SHOW_PREFERENCES = 1;
	private final int PICK_CONTACT_REQUEST = 2;

	private static final String LOG_TAG = "SoftPhone";

	private VideoInfo vi;
	private AudioInfo ai;
	private String localUser;
	private String localRealm;
	private String proxyIP;
	private int proxyPort;

	private ProgressDialog dialog;
	private Intent intentService;

	private Handler handler = new Handler();
	private ControlContacts controlcontacts = new ControlContacts(this);

	private TextView text;

	private Controller controller;

	/** Called when the activity is first created. */
	/* Cycle Life */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		PreferenceManager.setDefaultValues(this, R.layout.video_preferences,
				true);

		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		// PowerManager.WakeLock wl =
		// pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
		// LOG_TAG);
		// WakeLock wl = mPowerManager.newWakeLock(
		// PowerManager.ACQUIRE_CAUSES_WAKEUP
		// |PowerManager.ON_AFTER_RELEASE
		// |PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
		// "incoming_call");

		// wl.acquire(tiempo);

		initControllerUAFromSettings();
		if (controller == null)
			register();
		// Estoy registrado?
		checkCallIntent(getIntent());

	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		if (intent.getData() == null)
			return;
		// Log.d(LOG_TAG, "onNewIntent Tlf: " +
		// intent.getData().getSchemeSpecificPart() + "; Action: " +
		// intent.getData().getScheme());
		// if (intent.getData().getScheme().equalsIgnoreCase("tel")){
		// try {
		// Intent callIntent = new Intent(Intent.ACTION_DIAL);
		// callIntent.setData(Uri.parse("tel:" +
		// intent.getData().getSchemeSpecificPart()));
		// startActivity(callIntent);
		// } catch (ActivityNotFoundException activityException) {
		// Log.e("dialing-example", "Call failed", activityException);
		// }
		//
		//
		//
		// }

		checkCallIntent(intent);
	}

	private void checkCallIntent(Intent intent) {

		if (intent == null || intent.getData() == null)
			return;

		if (Intent.ACTION_CALL.equalsIgnoreCase(intent.getAction())) {
			Log.d(LOG_TAG, " ********* CALL"
					+ intent.getData().getSchemeSpecificPart());
		} else if (Intent.ACTION_SENDTO.equals(intent.getAction())) {
			Log.d(LOG_TAG, "sip:" + intent.getData().getLastPathSegment());
			if (controller == null)
				register();

			try {
				String sip = null;
				String name = "";
				sip = intent.getData().getLastPathSegment();

				Integer idContact = controlcontacts.getId(sip);
				if (idContact != -1)
					name = controlcontacts.getName(idContact);

				if (sip != null) {
					Toast.makeText(SoftPhone.this, name + ". SIP:" + sip,
							Toast.LENGTH_SHORT).show();
					call("sip:" + sip, idContact);
				} else
					Toast.makeText(SoftPhone.this, name + ". No tiene SIP:",
							Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Log.e("onActivityResult", e.toString());
			}
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();

		/**
		 * Control Botones Servicio Quitar
		 */

		// final Button buttonStartService = (Button)
		// findViewById(R.id.buttonStart);
		// buttonStartService.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// // TODO Auto-generated method stub
		// Log.d(LOG_TAG, "++++Start Service");
		//
		// startService(intentService);
		//
		// }
		// });
		// final Button buttonEndService = (Button)
		// findViewById(R.id.buttonStop);
		// buttonEndService.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// // TODO Auto-generated method stub
		// Log.d(LOG_TAG, "++++nStop Service");
		// stopService(intentService);
		// }
		// });

		/**
		 * Fin Control Botones Servicio Quitar
		 */

		final Button buttonCall = (Button) findViewById(R.id.call);
		buttonCall.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (controller != null) {
					if (controller.getUa() == null)
						initControllerUAFromSettings();
					try {
						TextView textRemoteUri = (TextView) findViewById(R.id.textRemoteUri);
						String remoteURI = "sip:";
						if (textRemoteUri.getText().toString()
								.equals("user@host")
								|| textRemoteUri.getText().toString()
										.equals("")) {
							openContacts();

						} else {
							remoteURI += textRemoteUri.getText().toString();
							Integer idContact;
							idContact = controlcontacts.getId(textRemoteUri
									.getText().toString());

							Log.d(LOG_TAG, "remoteURI: " + remoteURI
									+ " IdContact = " + idContact);
							call(remoteURI, idContact);
						}

					} catch (Exception e) {
						Log.e(LOG_TAG, e.toString());
						e.printStackTrace();
					}
				} else
					notRegister();
			}
		});

		final Button buttonContacts = (Button) findViewById(R.id.contacts);
		buttonContacts.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					openContacts();
				} catch (Exception e) {
					Log.e("Error Search", e.toString());
				}
			}
		});
	}

	private void openContacts() {
		Intent intentContacts = new Intent(Intent.ACTION_PICK,
				ContactsContract.Contacts.CONTENT_URI);

		startActivityForResult(intentContacts, PICK_CONTACT_REQUEST);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			controller.finishUA();
			Log.d(LOG_TAG, " FinishUA");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**** End Cycle Life */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == MEDIA_CONTROL_OUTGOING) {
			if (resultCode == RESULT_OK) {
				Log.d(LOG_TAG, "Outgoing: Rejected Call");
				try {
					// TODO
					// ************Aquí iria el Cancel no el Reject
					// controller.reject();
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else
				Log.d(LOG_TAG, "Media Control Outgoing; ResultCode = "
						+ resultCode);
		}

		if (requestCode == SHOW_PREFERENCES) {
			/*
			 * CARGAR LAS PREFERENCIAS
			 */
			SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
			SoftPhone.this.text.setTextSize(20);
			SoftPhone.this.text.setTextColor(Color.WHITE);
			SoftPhone.this.text.setText("Connecting...");

			// dialog = ProgressDialog.show(SoftPhone.this, "",
			// "Connecting ...");

			Log.d(LOG_TAG, "Reconfigure Preferences");
			initControllerUAFromSettings();
			initUA();

		}

		if (requestCode == PICK_CONTACT_REQUEST) {
			if (resultCode == RESULT_OK) {
				Integer id = null;
				String sip = null;
				String name = null;

				id = controlcontacts.getId(data);
				sip = controlcontacts.getSip(data);
				name = controlcontacts.getName(data);

				Log.d(LOG_TAG, "Id: " + id);
				Log.d(LOG_TAG, "Sip: " + sip);
				Log.d(LOG_TAG, "Name: " + name);

				if (sip != null) {
					Toast.makeText(SoftPhone.this, name + ", SIP:" + sip,
							Toast.LENGTH_SHORT).show();
					call("sip:" + sip, id);
				} else
					Toast.makeText(SoftPhone.this, name + ", No tiene SIP:",
							Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void call(String remoteURI, Integer id) {
		if (controller != null) {
			if (controller.getUa() == null)
				initControllerUAFromSettings();

			try {

				controller.call(remoteURI);
				Intent mediaIntent = new Intent(SoftPhone.this,
						MediaControlOutgoing.class);

				mediaIntent.putExtra("Id", id);
				mediaIntent.putExtra("Uri", remoteURI);
				startActivityForResult(mediaIntent, MEDIA_CONTROL_OUTGOING);

				Log.d(LOG_TAG, "Media Control Outgoing Started");
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString());
				e.printStackTrace();
			}
		} else
			notRegister();
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
			stopService(intentService);
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
			register();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void register() {
		if (controller == null)
			controller = new Controller();

		if (intentService == null) {
			intentService = new Intent(this, SoftPhoneService.class);
			startService(intentService);
			SoftPhoneService.setUpdateListener(this);
		}

		// dialog = ProgressDialog.show(SoftPhone.this, "", "Connecting ...");
		initControllerUAFromSettings();
		initUA();
	}

	public void registerSucessful() {
		Log.d(LOG_TAG, "Register Sucessful");

		SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		SoftPhone.this.text.setTextSize(20);
		SoftPhone.this.text.setTextColor(Color.GREEN);
		SoftPhone.this.text.setText("Register Sucessful");

	}

	public void registerFailed() {
		Log.d(LOG_TAG, "Register Failed");
		SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		SoftPhone.this.text.setTextSize(20);
		SoftPhone.this.text.setTextColor(Color.RED);
		SoftPhone.this.text.setText("Register Failed");
	}

	public void notRegister() {
		SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		SoftPhone.this.text.setTextSize(20);
		SoftPhone.this.text.setTextColor(Color.BLUE);
		SoftPhone.this.text.setText("Not Register, please register.");
	}

	private VideoInfo getVideoInfoFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		int frame_rate = Integer
				.parseInt(settings.getString("FRAME_RATE", "0"));

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

		return new VideoInfo(frame_rate, width, height, supportedCodecsID,
				codecName, out, "");
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
					+ localRealm + " proxyIP: " + proxyIP + "; localPort : "
					+ proxyPort);

			controller
					.initUA(vi, ai, proxyIP, proxyPort, localUser, localRealm);
			ApplicationContext.contextTable.put("controller", controller);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
			e.printStackTrace();
		}
	}

	@Override
	public void update(Message message) {
		// TODO Auto-generated method stub
		// if (dialog != null)
		// dialog.dismiss();
		Log.d(LOG_TAG, "Message : " + message.getData());

		if (message.getData().containsKey("Call")) {
			// if (message.getData().getString("Call").equals("Invite")) {
			// inviteReceived(message.getData().getString("Contact"));
			// }
		} else if (message.getData().containsKey("Register")) {
			if (message.getData().getString("Register").equals("Sucessful")) {
				registerSucessful();
			} else if (message.getData().getString("Register").equals("Failed")) {
				registerFailed();
			}
		} else if (message.getData().containsKey("finishActivity")) {
			if (message.getData().getString("finishActivity")
					.equals("MEDIA_CONTROL_OUTGOING")) {
				finishActivity(MEDIA_CONTROL_OUTGOING);
			}
		}

	}

}