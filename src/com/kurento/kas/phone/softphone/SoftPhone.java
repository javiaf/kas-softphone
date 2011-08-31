package com.kurento.kas.phone.softphone;

import java.net.InetAddress;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kurento.kas.media.AudioCodecType;
import com.kurento.kas.media.VideoCodecType;
import com.kurento.kas.mscontrol.networkconnection.ConnectionType;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.controlcontacts.ControlContacts;
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

	private ProgressDialog dialog;
	private Intent intentService;
	private TelephonyManager signalManager;

	private Handler handler = new Handler();
	private ControlContacts controlcontacts = new ControlContacts(this);

	private TextView text;
	private TextView textUser;
	private TextView textServer;

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
		// TODO Auto-generated method stub
		super.onNewIntent(intent);

		Log.d(LOG_TAG, "onNewIntent Is first or is call??");

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

		final EditText textRemoteUri = (EditText) findViewById(R.id.textRemoteUri);
		textRemoteUri.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					// Perform action on key press
					Toast.makeText(SoftPhone.this, textRemoteUri.getText(),
							Toast.LENGTH_SHORT).show();
					String remoteURI = "sip:";
					remoteURI += textRemoteUri.getText().toString();
					Integer idContact;
					idContact = controlcontacts.getId(textRemoteUri.getText()
							.toString());

					Log.d(LOG_TAG, "remoteURI: " + remoteURI + " IdContact = "
							+ idContact);
					call(remoteURI, idContact);
					return true;
				}
				return false;
			}
		});

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
			SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
			SoftPhone.this.text.setTextSize(20);
			SoftPhone.this.text.setTextColor(Color.WHITE);
			SoftPhone.this.text.setText("Connecting...");
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
			isExit = true;
			// try {
			// stopService(intentService);
			// } catch (Exception e) {
			// Log.e(LOG_TAG,
			// "stopService " + e.getMessage() + "; " + e.toString());
			// }
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
			SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
			SoftPhone.this.text.setTextSize(20);
			SoftPhone.this.text.setTextColor(Color.WHITE);
			SoftPhone.this.text.setText("Connecting...");
			register();
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

		SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		SoftPhone.this.text.setTextSize(20);
		SoftPhone.this.text.setTextColor(Color.GREEN);
		SoftPhone.this.text.setText("Register Sucessful");

		isRegister = true;
		ApplicationContext.contextTable.put("isRegister", isRegister);

	}

	public void registerFailed() {
		Log.d(LOG_TAG, "Register Failed");
		SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		SoftPhone.this.text.setTextSize(20);
		SoftPhone.this.text.setTextColor(Color.RED);
		SoftPhone.this.text.setText("Register Failed");

		isRegister = false;
		ApplicationContext.contextTable.put("isRegister", isRegister);
	}

	public void notRegister() {
		SoftPhone.this.text = (TextView) findViewById(R.id.textRegister);
		SoftPhone.this.text.setTextSize(20);
		SoftPhone.this.text.setTextColor(Color.BLUE);
		SoftPhone.this.text.setText("Not Register, please register.");

		isRegister = false;
		ApplicationContext.contextTable.put("isRegister", isRegister);
	}

	private ArrayList<VideoCodecType> getVideoCodecsFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		ArrayList<VideoCodecType> selectedVideoCodecs = new ArrayList<VideoCodecType>();
		if (settings.getBoolean("H263_CODEC", false))
			selectedVideoCodecs.add(VideoCodecType.H263);
		if (settings.getBoolean("MPEG4_CODEC", false))
			selectedVideoCodecs.add(VideoCodecType.MPEG4);
		if (settings.getBoolean("H264_CODEC", false))
			selectedVideoCodecs.add(VideoCodecType.H264);

		return selectedVideoCodecs;
	}

	private ArrayList<AudioCodecType> getAudioCodecsFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		ArrayList<AudioCodecType> selectedAudioCodecs = new ArrayList<AudioCodecType>();
		if (settings.getBoolean("AMR_AUDIO_CODEC", false))
			selectedAudioCodecs.add(AudioCodecType.AMR);
		if (settings.getBoolean("MP2_AUDIO_CODEC", false))
			selectedAudioCodecs.add(AudioCodecType.MP2);
		if (settings.getBoolean("AAC_AUDIO_CODEC", false))
			selectedAudioCodecs.add(AudioCodecType.AAC);

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

			this.textUser = (TextView) findViewById(R.id.textUser);
			this.textUser.setText("User: " + localUser + "@" + localRealm);

			this.textServer = (TextView) findViewById(R.id.textServer);
			this.textServer.setText("Server: " + proxyIP + ":" + proxyPort);
			
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
			
			// TODO: cambiar el nuevo botón que indica que la red esta mal.
			// finish();
			return false;
		}

	}

	public static Context getContext() {
		return getContext();
	}

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
					if (lAddress != null)
						if (lAddressNew.equals(lAddress))
							isAddressEqual = true;
						else
							isAddressEqual = false;
					isNetworking = true;
					break;
				case -1: // Disconneted
					Log.d(LOG_TAG, "No Activate");
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
