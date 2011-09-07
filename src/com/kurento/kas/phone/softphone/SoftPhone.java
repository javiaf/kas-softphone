package com.kurento.kas.phone.softphone;

import java.net.InetAddress;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kurento.kas.media.AudioCodecType;
import com.kurento.kas.media.VideoCodecType;
import com.kurento.kas.mscontrol.networkconnection.ConnectionType;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.controlcontacts.ControlContacts;
import com.kurento.kas.phone.historycall.HistoryCall;
import com.kurento.kas.phone.historycall.ListViewHistoryItem;
import com.kurento.kas.phone.media.MediaControlOutgoing;
import com.kurento.kas.phone.network.NetworkIP;
import com.kurento.kas.phone.preferences.Connection_Preferences;
import com.kurento.kas.phone.preferences.Video_Preferences;
import com.kurento.kas.phone.sip.Controller;

public class SoftPhone extends Activity implements ServiceUpdateUIListener {
	private final int MEDIA_CONTROL_OUTGOING = 0;
	private final int SHOW_PREFERENCES = 1;
	private final int PICK_CONTACT_REQUEST = 2;
	private final int FIRST_REGISTER_PREFERENCES = 3;
	private final int HISTORY_CALL = 4;

	private static final String LOG_TAG = "SoftPhone";

	private ArrayList<AudioCodecType> audioCodecs;
	private ArrayList<VideoCodecType> videoCodecs;
	private InetAddress localAddress;
	private ConnectionType connectionType;
	// ConnectivityManager ConnectManager;
	ConnectivityManager connManager;
	NetworkInfo ni;

	private String localUser;
	private String localRealm;
	private String proxyIP;
	private int proxyPort;
	private String info_connect;
	private String info_wifi = "Not connected";
	private String info_3g = "Not connected";
	private String info_video;
	private String info_audio_aux;
	private String info_video_aux;

	private ProgressDialog dialog;
	private Intent intentService;
	private TelephonyManager signalManager;

	private Handler handler = new Handler();
	private ControlContacts controlcontacts = new ControlContacts(this);

	private TextView text;
	private TextView textUser;
	private TextView textServer;
	private Button connection;
	private Button wifi;
	private Button _3g;
	private Button video;

	private Controller controller;
	private boolean isRegister = false;

	private boolean isExit = false;

	// =(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
	//
	// String sNetworkType = "No Activate";
	// /*Control para sólo transmitir cuando tengamos conexión si es false*/
	// boolean backgroundEnabled = ConnectManager.getBackgroundDataSetting();
	//
	// NetworkInfo activeNetwork = ConnectManager.getActiveNetworkInfo();

	/** Called when the activity is first created. */
	/* Cycle Life */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		PreferenceManager.setDefaultValues(this, R.layout.video_preferences,
				true);
		SoftPhoneService.setUpdateListener(this);

		// PowerManager pm = (PowerManager)
		// getSystemService(Context.POWER_SERVICE);
		// PowerManager.WakeLock wl =
		// pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		// wl.acquire();
		//
		// wl.acquire(tiempo);

		/* If first time */
		controller = (Controller) ApplicationContext.contextTable
				.get("controller");

		connectionType = null;
		connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		ni = connManager.getActiveNetworkInfo();

		if (ni != null) {

			if (initControllerUAFromSettings()) {

				if (controller == null) {
					Log.d(LOG_TAG, "Controller is null");
					register();
				} else {
					Log.d(LOG_TAG, "Controller not is null");
					try {
						if ((Boolean) ApplicationContext.contextTable
								.get("isRegister"))
							registerSucessful();
					} catch (Exception e) {
						isRegister = false;
						ApplicationContext.contextTable.put("isRegister",
								isRegister);
					}
				}
				// Estoy registrado?
				checkCallIntent(getIntent());
			} else {
				/* First Register */
				Intent first_register = new Intent(SoftPhone.this,
						Register.class);
				startActivityForResult(first_register,
						FIRST_REGISTER_PREFERENCES);
			}
		} else {
			Log.e(LOG_TAG, "Network interface unable.");
			Toast.makeText(SoftPhone.this,
					"SoftPhone: Please enable any network interface.",
					Toast.LENGTH_SHORT).show();

			// finish();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Log.d(LOG_TAG, "onNewIntent Is first or is call??");
		
		SoftPhoneService.setUpdateListener(this);

		connectionType = null;
		connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		ni = connManager.getActiveNetworkInfo();
		if (ni != null) {
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
		} else {
			Log.e(LOG_TAG, "Network interface unable.");
			Toast.makeText(SoftPhone.this,
					"SoftPhone: Please enable any network interface.",
					Toast.LENGTH_SHORT).show();
			// TODO: revisar el tema de los iconos
			// finish();
		}

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
		Log.d(LOG_TAG, "On Restart");
		super.onRestart();
	}

	@Override
	protected void onStart() {
		Log.d(LOG_TAG, "On Start");
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "On Resume");

		/* Listener for change of networking */

		signalManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		signalManager.listen(signalListener,
				PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

		video = (Button) findViewById(R.id.video_button);
		video.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final Dialog dialog = new Dialog(v.getContext());
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				info_video = "Codecs: \n " + info_video_aux + "\n "
						+ info_audio_aux;
				dialog.setContentView(R.layout.info_video);
				((TextView) dialog.findViewById(R.id.info_video))
						.setText(info_video);
				dialog.show();
			}
		});

		wifi = (Button) findViewById(R.id.wifi_button);
		wifi.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final Dialog dialog = new Dialog(v.getContext());
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.info_wifi);
				((TextView) dialog.findViewById(R.id.info_wifi))
						.setText(info_wifi);
				dialog.show();
			}
		});

		_3g = (Button) findViewById(R.id.info_3g_button);
		_3g.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final Dialog dialog = new Dialog(v.getContext());
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.info_3g);
				((TextView) dialog.findViewById(R.id.info_3g)).setText(info_3g);
				dialog.show();
			}
		});

		connection = (Button) findViewById(R.id.connection_button);
		connection.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final Dialog dialog = new Dialog(v.getContext());
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.info_connection);
				((TextView) dialog.findViewById(R.id.info_connect))
						.setText(info_connect);
				dialog.show();
			}
		});

		final Button buttonCall = (Button) findViewById(R.id.call);
		buttonCall.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent history_call = new Intent(SoftPhone.this,
						HistoryCall.class);
				startActivityForResult(history_call, HISTORY_CALL);
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
		Log.d(LOG_TAG, "On Pause");
		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "On Stop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "On Destroy");
		try {
			if (isExit) {
				signalManager.listen(signalListener,
						PhoneStateListener.LISTEN_NONE);
				if (controller != null)
					controller.finishUA();
				isRegister = false;
				ApplicationContext.contextTable.put("isRegister", isRegister);

				intentService = (Intent) ApplicationContext.contextTable
						.get("intentService");
				try {
					stopService(intentService);
				} catch (Exception e) {
					Log.e(LOG_TAG,
							"stopService " + e.getMessage() + "; "
									+ e.toString());
				}

				// // Save DB
				SQLiteDatabase db = (SQLiteDatabase) ApplicationContext.contextTable
						.get("db");

				// Si hemos abierto correctamente la base de datos

				if (db.isOpen()) {
					// Insertamos 5 usuarios de ejemplo

					// Creamos el registro a insertar como objeto ContentValues

					@SuppressWarnings("unchecked")
					ArrayList<ListViewHistoryItem> items = (ArrayList<ListViewHistoryItem>) ApplicationContext.contextTable
							.get("itemsHistory");
					db.delete("DBHistoryCall", null, null);

					for (int i = 0; i < items.size(); i++) {
						ContentValues nValue = new ContentValues();
						nValue.put("id", items.get(i).getId());
						nValue.put("date", items.get(i).getDate());
						nValue.put("uri", items.get(i).getUri());
						nValue.put("name", items.get(i).getName());
						nValue.put("type", items.get(i).getType());
						db.insert("DBHistoryCall", null, nValue);
						Log.d(LOG_TAG, "************insert data");
					}

					// Cerramos la base de datos
					db.close();
				} else
					Log.d(LOG_TAG, "************data base closed");

				ApplicationContext.contextTable.clear();
				Log.d(LOG_TAG, " FinishUA");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	/**** End Cycle Life */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == HISTORY_CALL) {
			if (resultCode == RESULT_OK) {
				String type = data.getStringExtra("type");
				String uri = "";
				if (type.equals("new")) {
					uri = data.getStringExtra("contact");
					searchCallContact(uri);
				} else if (type.equals("history")) {
					@SuppressWarnings("unchecked")
					ArrayList<ListViewHistoryItem> items = (ArrayList<ListViewHistoryItem>) ApplicationContext.contextTable
							.get("itemsHistory");
					int id = data.getIntExtra("positionContact", -1);
					if (id != -1) {
						uri = items.get(id).getUri();
						searchCallContact(uri);
					}
				} else if (type.equals("openContacts")) {
					Log.d(LOG_TAG, "OPEN CONTACTS");
					openContacts();
				}
			}
		}

		if (requestCode == FIRST_REGISTER_PREFERENCES) {
			if (resultCode == RESULT_OK)
				Log.d(LOG_TAG, "Result Ok");
			else if (resultCode == RESULT_CANCELED) {
				Log.d(LOG_TAG, "Result Cancel");
				finish();
			}
		}

		if (requestCode == SHOW_PREFERENCES) {
			/*
			 * CARGAR LAS PREFERENCIAS
			 */

			connection.setBackgroundResource(R.drawable.connecting_icon);
			info_connect = "Connection .... \n\n User: \n " + localUser + "@"
					+ localRealm + "\n\n Server:\n " + proxyIP + ":"
					+ proxyPort;
			isRegister = false;
			ApplicationContext.contextTable.put("isRegister", isRegister);
			// dialog = ProgressDialog.show(SoftPhone.this, "",
			// "Connecting ...");

			Log.d(LOG_TAG, "Reconfigure Preferences");
			if (initControllerUAFromSettings())
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

	private void searchCallContact(String uri) {
		String textRemoteUri = uri;
		String remoteURI = "sip:";
		remoteURI += textRemoteUri;
		Integer idContact;
		idContact = controlcontacts.getId(textRemoteUri);

		Log.d(LOG_TAG, "remoteURI: " + remoteURI + " IdContact = " + idContact);
		call(remoteURI, idContact);
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
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to exit?")
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									isExit = true;
									finish();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
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
		case (R.id.menu_about):
			final Dialog dialog = new Dialog(this);
			dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.aboutbox);
			dialog.show();

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void register() {
		if (controller == null)
			controller = new Controller();
		intentService = (Intent) ApplicationContext.contextTable
				.get("intentService");
		if (intentService == null) {
			intentService = new Intent(this, SoftPhoneService.class);
			ApplicationContext.contextTable.put("intentService", intentService);
			startService(intentService);

			Log.d(LOG_TAG, "StartService");
			SoftPhoneService.setUpdateListener(this);
		}

		if (initControllerUAFromSettings())
			initUA();
	}

	public void registerSucessful() {
		Log.d(LOG_TAG, "Register Sucessful");
		SoftPhone.this.connection = (Button) findViewById(R.id.connection_button);
		connection.setBackgroundResource(R.drawable.connect_icon);

		info_connect = "The connection is ok. \n\n User: \n " + localUser + "@"
				+ localRealm + "\n\n Server:\n " + proxyIP + ":" + proxyPort;
		// if (connection != null)
		// SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		// SoftPhone.this.text.setTextSize(20);
		// SoftPhone.this.text.setTextColor(Color.GREEN);
		// SoftPhone.this.text.setText("Register Sucessful");

		isRegister = true;
		ApplicationContext.contextTable.put("isRegister", isRegister);

	}

	public void registerFailed() {
		Log.d(LOG_TAG, "Register Failed");

		info_connect = "The connection is failed. \n\n User: \n " + localUser
				+ "@" + localRealm + "\n\n Server:\n " + proxyIP + ":"
				+ proxyPort;
		SoftPhone.this.connection = (Button) findViewById(R.id.connection_button);
		connection.setBackgroundResource(R.drawable.disconnect_icon);
		// SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		// SoftPhone.this.text.setTextSize(20);
		// SoftPhone.this.text.setTextColor(Color.RED);
		// SoftPhone.this.text.setText("Register Failed");

		isRegister = false;
		ApplicationContext.contextTable.put("isRegister", isRegister);
	}

	public void notRegister() {
		registerFailed();
	}

	private ArrayList<VideoCodecType> getVideoCodecsFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		ArrayList<VideoCodecType> selectedVideoCodecs = new ArrayList<VideoCodecType>();
		if (settings.getBoolean("H263_CODEC", false)) {
			selectedVideoCodecs.add(VideoCodecType.H263);
			info_video_aux = "Codec de Video:\n H263";
		}
		if (settings.getBoolean("MPEG4_CODEC", false)) {
			selectedVideoCodecs.add(VideoCodecType.MPEG4);
			info_video_aux = "Codec de Video:\n MPEG4";
		}
		if (settings.getBoolean("H264_CODEC", false)) {
			selectedVideoCodecs.add(VideoCodecType.H264);
			info_video_aux = "Codec de Video:\n H264";
		}

		return selectedVideoCodecs;
	}

	private ArrayList<AudioCodecType> getAudioCodecsFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		ArrayList<AudioCodecType> selectedAudioCodecs = new ArrayList<AudioCodecType>();
		if (settings.getBoolean("AMR_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.AMR);
			info_audio_aux = "Codec de Audio:\n AMR";
		}
		if (settings.getBoolean("MP2_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.MP2);
			info_audio_aux = "Codec de Audio:\n MP2";
		}
		if (settings.getBoolean("AAC_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.AAC);
			info_audio_aux = "Codec de Audio:\n AAC";
		}

		return selectedAudioCodecs;
	}

	private boolean getPreferences() {
		try {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());
			localUser = settings.getString("LOCAL_USERNAME", "");
			localRealm = settings.getString("LOCAL_DOMAIN", "");
			proxyIP = settings.getString("PROXY_IP", "");
			proxyPort = Integer.parseInt(settings.getString("PROXY_PORT", ""));

			info_connect = "Connecting ... \n\n User: \n " + localUser + "@"
					+ localRealm + "\n\n Server:\n " + proxyIP + ":"
					+ proxyPort;

			this.audioCodecs = getAudioCodecsFromSettings();
			this.videoCodecs = getVideoCodecsFromSettings();

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean initControllerUAFromSettings() {
		try {
			if (getPreferences()) {

				// Controlar si ni == null .
				ni = connManager.getActiveNetworkInfo();
				String conType = ni.getTypeName();

				if ("WIFI".equalsIgnoreCase(conType))
					connectionType = ConnectionType.WIFI;
				else if ("MOBILE".equalsIgnoreCase(conType))
					connectionType = ConnectionType.MOBILE;

				this.localAddress = NetworkIP.getLocalAddress();
				ApplicationContext.contextTable.put("localAddress",
						localAddress);

				return true;
			} else
				return false;
		} catch (Exception e) {
			Log.e(LOG_TAG, "Network interface unable.");
			Toast.makeText(SoftPhone.this,
					"SoftPhone: Please enable any network interface.",
					Toast.LENGTH_SHORT).show();
			// TODO: CAMBIAR ICONOS DE RED
			return false;
		}

	}

	// public static Context getContext() {
	// Context context = SoftPhone.getContext();
	// return Context;
	// }

	private void initUA() {
		try {
			Log.d(LOG_TAG, "LocalUser : " + localUser + "; localReal : "
					+ localRealm + " proxyIP: " + proxyIP + "; localPort : "
					+ proxyPort + " ConnectionType = " + connectionType);

			controller.initUA(audioCodecs, videoCodecs, localAddress,
					connectionType, proxyIP, proxyPort, localUser, localRealm);
			ApplicationContext.contextTable.put("controller", controller);
			Log.e(LOG_TAG, "put controller in context");
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
			e.printStackTrace();
		}
	}

	@Override
	public void update(Message message) {
		Log.d(LOG_TAG, "Message : " + message.getData());

		if (message.getData().containsKey("Register")) {
			if (message.getData().getString("Register").equals("Sucessful")) {
				registerSucessful();
			} else if (message.getData().getString("Register").equals("Failed")) {
				registerFailed();
			}
		} else if (message.getData().containsKey("finishActivity")) {
			if (message.getData().getString("finishActivity")
					.equals("MEDIA_CONTROL_OUTGOING")) {
				Log.d(LOG_TAG, "Finish Activity MEDIA_CONTROL_OUTGOING");
				finishActivity(MEDIA_CONTROL_OUTGOING);
			}
		}

	}

	private final PhoneStateListener signalListener = new PhoneStateListener() {

		public void onDataConnectionStateChanged(int state) {
			if (!isExit) {
				ConnectivityManager ConnectManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

				String sNetworkType = "No Activate";
				/*
				 * Control para sólo transmitir cuando tengamos conexión si es
				 * false
				 */
				boolean backgroundEnabled = ConnectManager
						.getBackgroundDataSetting();
				int networkType = -1;
				NetworkInfo activeNetwork = ConnectManager
						.getActiveNetworkInfo();
				if (activeNetwork != null) {
					networkType = activeNetwork.getType();
				}
				boolean isAddressEqual = false;
				boolean isNetworking = false;
				InetAddress lAddressNew;
				InetAddress lAddress;

				switch (networkType) {
				case ConnectivityManager.TYPE_WIFI: // Disconnected
					// Register() with new ip.
					Log.d(LOG_TAG, "Connection OK, Register... WIFI");
					lAddressNew = NetworkIP.getLocalAddress();
					lAddress = (InetAddress) ApplicationContext.contextTable
							.get("localAddress");
					wifi.setBackgroundResource(R.drawable.wifi_on_120);
					_3g.setBackgroundResource(R.drawable.icon_3g_off_120);
					info_wifi = "Wifi enable. \n IP: \n " + lAddressNew;
					info_3g = "Not connected";
					if (lAddress != null)
						if (lAddressNew.equals(lAddress))
							isAddressEqual = true;
						else
							isAddressEqual = false;
					isNetworking = true;
					break;
				case ConnectivityManager.TYPE_MOBILE: // Connecting
					// Register() with new ip.
					Log.d(LOG_TAG, "Connection OK, Register...MOBILE");
					ApplicationContext.contextTable.put("isNetworking", true);
					lAddressNew = NetworkIP.getLocalAddress();
					lAddress = (InetAddress) ApplicationContext.contextTable
							.get("localAddress");

					wifi.setBackgroundResource(R.drawable.wifi_off_120);
					_3g.setBackgroundResource(R.drawable.icon_3g_on_120);
					info_wifi = "Not connected";
					info_3g = "3G enable. \n IP: \n " + lAddressNew;
					if (lAddress != null)
						if (lAddressNew.equals(lAddress))
							isAddressEqual = true;
						else
							isAddressEqual = false;
					isNetworking = true;
					break;
				case -1: // Disconneted
					Log.d(LOG_TAG, "No Activate");
					wifi.setBackgroundResource(R.drawable.wifi_off_120);
					_3g.setBackgroundResource(R.drawable.icon_3g_off_120);
					info_wifi = "Not connected";
					info_3g = "Not connected";
					isNetworking = false;
					break;
				default:
					break;
				}

				if (isNetworking) {
					// Destruir Service and UA
					Log.d(LOG_TAG, "****IsNetwoking ; IsAdressEqual = "
							+ isAddressEqual);
					if (!isAddressEqual) {
						controller = null;
						isRegister = false;
						ApplicationContext.contextTable.put("isRegister",
								isRegister);

						intentService = (Intent) ApplicationContext.contextTable
								.get("intentService");
						try {
							stopService(intentService);
						} catch (Exception e) {
							Log.e(LOG_TAG, "stopService " + e.getMessage()
									+ "; " + e.toString());
						}
						Log.d(LOG_TAG, "All Destroy");
						ApplicationContext.contextTable.clear();
						// Registar
						if (initControllerUAFromSettings()) {
							Log.d(LOG_TAG, "****Register on new networking");
							register();
						}
					}
				} else {
					try {
						if ((Boolean) ApplicationContext.contextTable
								.get("isRegister")) {
							// Destruir
							if (controller != null)
								try {
									controller.finishUA();
									controller = null;
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							isRegister = false;
							ApplicationContext.contextTable.put("isRegister",
									isRegister);

							intentService = (Intent) ApplicationContext.contextTable
									.get("intentService");
							try {
								stopService(intentService);
							} catch (Exception e) {
								Log.e(LOG_TAG, "stopService " + e.getMessage()
										+ "; " + e.toString());
							}
							ApplicationContext.contextTable.clear();
							Log.d(LOG_TAG, "All Destroy");
						}
					} catch (Exception e) {

					}
				}
			}
		}
	};

}
