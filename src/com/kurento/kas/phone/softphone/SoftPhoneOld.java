/*
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
package com.kurento.kas.phone.softphone;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.kurento.commons.sdp.enums.MediaType;
import com.kurento.commons.sdp.enums.Mode;
import com.kurento.kas.media.codecs.AudioCodecType;
import com.kurento.kas.media.codecs.VideoCodecType;
import com.kurento.kas.mscontrol.networkconnection.NetIF;
import com.kurento.kas.mscontrol.networkconnection.PortRange;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.controlcontacts.ControlContacts;
import com.kurento.kas.phone.historycall.ListViewHistoryItem;
import com.kurento.kas.phone.network.NetworkIP;
import com.kurento.kas.phone.preferences.Connection_Preferences;
import com.kurento.kas.phone.preferences.Video_Preferences;
import com.kurento.kas.phone.sip.Controller;

import de.javawi.jstun.test.DiscoveryInfo;

public class SoftPhoneOld extends Activity implements ServiceUpdateUIListener {
	private final int MEDIA_CONTROL_OUTGOING = 0;
	private final int SHOW_PREFERENCES = 1;
	private final int PICK_CONTACT_REQUEST = 2;
	private final int FIRST_REGISTER_PREFERENCES = 3;
	private final int HISTORY_CALL = 4;

	private static final String LOG_TAG = "SoftPhone";

	private ArrayList<AudioCodecType> audioCodecs;
	private PortRange audioPortRange;
	private ArrayList<VideoCodecType> videoCodecs;
	private PortRange videoPortRange;
	private Map<MediaType, Mode> callDirectionMap;
	private InetAddress localAddress;
	private InetAddress publicAddress;
	private InetAddress lAddressNew;
	private int localPort = 0; // The stun server will give a random port
	private int publicPort;
	private NetIF netIF;

	private String stunHost = "stun.xten.com";
	private int stunPort = 3478;

	// ConnectivityManager ConnectManager;
	ConnectivityManager connManager;
	NetworkInfo ni;

	private String localUser;
	private String localRealm;
	private String localPassword;
	private String proxyIP;
	private int proxyPort;

	private long keepAliveDelay;
	private boolean keepAliveEnable;
	private String transport;

	private Integer max_BW;
	private Integer max_delay;
	private Integer cameraFacing; // Camera.CameraInfo.CAMERA_FACING_X
	private String camera = "Camera Back";
	private Integer max_FR;
	private Integer gop_size;
	private Integer max_queue;
	private Integer width;
	private Integer height;

	private String info_connect;

	private int type_network;
	private String info_wifi = "Not connected";
	private String info_3g = "Not connected";
	private String info_network = "";
	private String info_transport = "";
	private String info_video;
	private String info_audio_aux;
	private String info_video_aux;
	private String info_size_video;
	private String info_call_type;

	private ProgressDialog dialog;
	private ProgressDialog dialogWait;
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
	private boolean isUseStun = true;

	private Controller controller;
	private boolean isRegister = false;

	private boolean isExit = false;

	int sipPortMin;
	int sipPortMax;

	private static final int SIP_PORT_MIN_DEF = 6060;
	private static final int SIP_PORT_MAX_DEF = 10000;

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

		if ((Boolean) ApplicationContext.contextTable.get("isRegister") == null)
			ApplicationContext.contextTable.put("isRegister", false);

		/* If first time */
		controller = (Controller) ApplicationContext.contextTable
				.get("controller");

		netIF = null;
		connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		ni = connManager.getActiveNetworkInfo();

		if (ni != null) {
			if (initControllerUAFromSettings()) {
				if (controller == null) {
					register();
				} else {
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
				// Am I registered?
				checkCallIntent(getIntent());
			} else {
				/* First Register */
				// Intent first_register = new Intent(SoftPhone.this,
				// Register.class);
				// startActivityForResult(first_register,
				// FIRST_REGISTER_PREFERENCES);
			}
		} else {
			Log.e(LOG_TAG, "Network interface unable.");
			// Toast.makeText(SoftPhone.this,
			// "SoftPhone: Please enable any network interface.",
			// Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		/*
		 * With this, I can control if some activity has been created and the
		 * user has pushed the home button and to start that activity.
		 */
		if ((Intent) ApplicationContext.contextTable.get("outgoingCall") != null) {
			Intent i = (Intent) ApplicationContext.contextTable
					.get("outgoingCall");
			startActivityForResult(i, MEDIA_CONTROL_OUTGOING);
		} else if ((Intent) ApplicationContext.contextTable.get("incomingCall") != null) {
			Intent i = (Intent) ApplicationContext.contextTable
					.get("incomingCall");
			startActivity(i);
		} else if ((Intent) ApplicationContext.contextTable.get("videoCall") != null) {
			Intent i = (Intent) ApplicationContext.contextTable
					.get("videoCall");
			startActivity(i);
		}
		netIF = null;
		connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		ni = connManager.getActiveNetworkInfo();
		if (ni != null) {
			if (intent.getData() == null)
				return;
			checkCallIntent(intent);
		} else {
			// Toast.makeText(SoftPhone.this,
			// "SoftPhone: Please enable any network interface.",
			// Toast.LENGTH_SHORT).show();
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

				// if (sip != null) {
				// Toast.makeText(SoftPhone.this, name + ". SIP:" + sip,
				// Toast.LENGTH_SHORT).show();
				// call("sip:" + sip, idContact);
				// } else
				// Toast.makeText(SoftPhone.this, name + ". No tiene SIP:",
				// Toast.LENGTH_SHORT).show();
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
				info_video = info_transport + "\nCodecs: \n\n"
						+ info_size_video + "\n\n" + info_video_aux + "\n\n"
						+ info_audio_aux + " \n\n" + info_call_type;
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
				if (type_network == ConnectivityManager.TYPE_WIFI)
					info_wifi = (String) ApplicationContext.contextTable
							.get("info_network");
				else
					info_wifi = "Not connected";
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
				if (type_network == ConnectivityManager.TYPE_MOBILE)
					info_3g = (String) ApplicationContext.contextTable
							.get("info_network");
				else
					info_3g = "Not connected";
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
				// Intent history_call = new Intent(SoftPhone.this,
				// HistoryCall.class);
				// startActivityForResult(history_call, HISTORY_CALL);
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

	private synchronized void setIsExit(boolean type) {
		isExit = type;
	}

	private synchronized boolean getIsExit() {
		return isExit;
	}

	@Override
	protected void onDestroy() {
		try {
			if (getIsExit()) {
				intentService = (Intent) ApplicationContext.contextTable
						.get("intentService");
				try {
					stopService(intentService);
				} catch (Exception e) {
					Log.e(LOG_TAG,
							"stopService " + e.getMessage() + "; "
									+ e.toString());
				}

				signalManager.listen(signalListener,
						PhoneStateListener.LISTEN_NONE);
				if (controller != null)
					controller.finishUA();
				isRegister = false;

				if (ApplicationContext.contextTable != null) {
					ApplicationContext.contextTable.put("isRegister",
							isRegister);
					ApplicationContext.contextTable.clear();
				}
				if (dialogWait != null)
					dialogWait.dismiss();
				Log.d(LOG_TAG, " FinishUA");
			}

		} catch (Exception e) {
			// e.printStackTrace();
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

			connection.setBackgroundResource(R.drawable.disconnect_icon);
			info_connect = "Connection .... \n\n User: \n " + localUser + "@"
					+ localRealm + "\n\n Server:\n " + proxyIP + ":"
					+ proxyPort;
			isRegister = false;
			ApplicationContext.contextTable.put("isRegister", isRegister);

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

				// if (sip != null) {
				// Toast.makeText(SoftPhone.this, name + ", SIP:" + sip,
				// Toast.LENGTH_SHORT).show();
				// call("sip:" + sip, id);
				// } else
				// Toast.makeText(SoftPhone.this, name + ", No tiene SIP:",
				// Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void searchCallContact(String uri) {
		String textRemoteUri = uri;
		String remoteURI = "sip:";
		remoteURI += textRemoteUri;
		Integer idContact;
		idContact = controlcontacts.getId(textRemoteUri);

		call(remoteURI, idContact);
	}

	private void call(String remoteURI, Integer id) {
		if (controller != null) {
			if (controller.getUa() == null)
				initControllerUAFromSettings();

			try {
				// Intent mediaIntent = new Intent(SoftPhone.this,
				// MediaControlOutgoing.class);
				// mediaIntent.putExtra("Id", id);
				// mediaIntent.putExtra("Uri", remoteURI);
				// startActivityForResult(mediaIntent, MEDIA_CONTROL_OUTGOING);

				final String rUri = remoteURI;
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							controller.call(rUri);
						} catch (Exception e) {
							Log.e(LOG_TAG, "Fail in thread for call");
							e.printStackTrace();
						}
					}
				}).start();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.toString());
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
									setIsExit(true);

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
			controller = new Controller(getApplicationContext());
		intentService = (Intent) ApplicationContext.contextTable
				.get("intentService");
		// SoftPhoneService.setUpdateListener(this);
		if (intentService == null) {
			intentService = new Intent(this, SoftPhoneService.class);
			ApplicationContext.contextTable.put("intentService", intentService);
			startService(intentService);
		}
		if (initControllerUAFromSettings())
			initUA();
	}

	public void registerSucessful() {
		// SoftPhone.this.connection = (Button)
		// findViewById(R.id.connection_button);
		connection.setBackgroundResource(R.drawable.connect_icon);

		info_connect = "The connection is ok. \n\n User: \n " + localUser + "@"
				+ localRealm + "\n\n Server:\n " + proxyIP + ":" + proxyPort;
		isRegister = true;
		ApplicationContext.contextTable.put("isRegister", isRegister);

		if (!stunHost.equals("")) {
			DiscoveryInfo stunInfo = (DiscoveryInfo) ApplicationContext.contextTable
					.get("stunInfo");
			if (stunInfo == null)
				info_connect += "\n\n Not Stun Info";
			else
				info_connect += "\n\n " + stunInfo;
		} else
			info_connect += "\n\n Not use Stun";

	}

	public void registerFailed() {
		info_connect = "The connection is failed. \n\n User: \n " + localUser
				+ "@" + localRealm + "\n\n Server:\n " + proxyIP + ":"
				+ proxyPort;
		// SoftPhone.this.connection = (Button)
		// findViewById(R.id.connection_button);
		connection.setBackgroundResource(R.drawable.disconnect_icon);

		isRegister = false;
		ApplicationContext.contextTable.put("isRegister", isRegister);

		DiscoveryInfo stunInfo = (DiscoveryInfo) ApplicationContext.contextTable
				.get("stunInfo");
		if (stunInfo == null)
			info_connect += "\n\n Not Stun Info";
		else
			info_connect += "\n\n " + stunInfo;
	}

	public void notRegister() {
		registerFailed();
	}

	private ArrayList<VideoCodecType> getVideoCodecsFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		ArrayList<VideoCodecType> selectedVideoCodecs = new ArrayList<VideoCodecType>();
		info_video_aux = "Video Codec: ";
		String codec = "";
		if (settings.getBoolean("H263_CODEC", false)) {
			selectedVideoCodecs.add(VideoCodecType.H263);
			codec += "\n H263";
		}
		if (settings.getBoolean("MPEG4_CODEC", false)) {
			selectedVideoCodecs.add(VideoCodecType.MPEG4);
			codec += "\n MPEG4";
		}
		if (settings.getBoolean("H264_CODEC", false)) {
			selectedVideoCodecs.add(VideoCodecType.H264);
			codec += "\n H264";
		}
		info_video_aux += codec;

		return selectedVideoCodecs;
	}

	private ArrayList<AudioCodecType> getAudioCodecsFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		ArrayList<AudioCodecType> selectedAudioCodecs = new ArrayList<AudioCodecType>();
		info_audio_aux = "Audio Codec:";
		String codec = "";
		if (settings.getBoolean("AMR_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.AMR);
			codec += "\n AMR";
		}
		if (settings.getBoolean("MP2_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.MP2);
			codec += "\n MP2";
		}
		if (settings.getBoolean("AAC_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.AAC);
			codec += "\n AAC";
		}
		if (settings.getBoolean("PCMU_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.PCMU);
			codec += "\n PCMU";
		}
		if (settings.getBoolean("PCMA_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.PCMA);
			codec += "\n PCMA";
		}
		info_audio_aux += codec;
		return selectedAudioCodecs;
	}

	private Map<MediaType, Mode> getCallDirectionMapFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		Map<MediaType, Mode> callDirection = new HashMap<MediaType, Mode>();

		String videoDirection = settings.getString("CALL_VIDEO_DIRECTION",
				"SEND/RECEIVE");
		if (videoDirection.equals("SEND ONLY")) {
			callDirection.put(MediaType.VIDEO, Mode.SENDONLY);
			info_call_type = "Call Video Direction:\n SEND ONLY\n";
		}
		if (videoDirection.equals("RECEIVE ONLY")) {
			callDirection.put(MediaType.VIDEO, Mode.RECVONLY);
			info_call_type = "Call Video Direction:\n RECEIVE ONLY\n";
		}
		if (videoDirection.equals("SEND/RECEIVE")) {
			callDirection.put(MediaType.VIDEO, Mode.SENDRECV);
			info_call_type = "Call Video Direction:\n SEND/RECEIVE\n";
		}

		String audioDirection = settings.getString("CALL_AUDIO_DIRECTION",
				"SEND/RECEIVE");
		if (audioDirection.equals("SEND ONLY")) {
			callDirection.put(MediaType.AUDIO, Mode.SENDONLY);
			info_call_type += "Call Audio Direction:\n SEND ONLY";
		}
		if (audioDirection.equals("RECEIVE ONLY")) {
			callDirection.put(MediaType.AUDIO, Mode.RECVONLY);
			info_call_type += "Call Audio Direction:\n RECEIVE ONLY";
		}
		if (audioDirection.equals("SEND/RECEIVE")) {
			callDirection.put(MediaType.AUDIO, Mode.SENDRECV);
			info_call_type += "Call Audio Direction:\n SEND/RECEIVE";
		}

		return callDirection;
	}

	private void getStunFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		String stunHostAux = settings.getString("STUN_LIST", "stun.xten.com");

		// if (localPort == 0)
		// localPort = (int) Math.floor(Math.random()
		// * (sipPortMin - sipPortMax + 1) + sipPortMax);
		if (stunHostAux.equals("-")) {
			stunHostAux = settings.getString("STUN_HOST", "-");
			if (!stunHostAux.equals("-")) {
				stunHost = stunHostAux;
				stunPort = Integer.parseInt(settings.getString(
						"STUN_HOST_PORT", "3478"));
			} else {
				stunHost = "";
				stunPort = 0;
			}
		} else {
			stunHost = stunHostAux;
			stunPort = 3478;
		}
	}

	private boolean getPreferences() {
		try {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());
			localUser = settings.getString("LOCAL_USERNAME", "");
			localPassword = settings.getString("LOCAL_PASSWORD", "");
			localRealm = settings.getString("LOCAL_DOMAIN", "");

			try {
				sipPortMin = Integer.parseInt(settings.getString(
						"SIP_MIN_LOCAL_PORT",
						Integer.toString(SIP_PORT_MIN_DEF)));
			} catch (NumberFormatException e) {
				sipPortMin = SIP_PORT_MIN_DEF;
			}

			try {
				sipPortMax = Integer.parseInt(settings.getString(
						"SIP_MAX_LOCAL_PORT",
						Integer.toString(SIP_PORT_MAX_DEF)));
			} catch (NumberFormatException e) {
				sipPortMax = SIP_PORT_MAX_DEF;
			}

			localPort = sipPortMin;

			proxyIP = settings.getString("PROXY_IP", "");
			proxyPort = Integer.parseInt(settings.getString("PROXY_PORT", "0"));

			getStunFromSettings();

			if (localUser.equals("") || localRealm.equals("")
					|| proxyIP.equals("") || proxyPort == 0)
				return false;

			info_connect = "Connecting ... \n\n User: \n " + localUser + "@"
					+ localRealm + "\n\n Server:\n " + proxyIP + ":"
					+ proxyPort;

			try {
				keepAliveDelay = settings.getLong("KEEP_DELAY", 10000);
				keepAliveEnable = settings.getBoolean("KEEP_ALIVE", false);
				transport = settings.getString("TRANSPORT", "UDP");
				Log.d(LOG_TAG, "New params ok");
			} catch (Exception e) {
				keepAliveDelay = 10000;
				keepAliveEnable = false;
				transport = "UDP";
				Log.e(LOG_TAG, "Exception : ", e);
			}

			info_transport = "Transport: \nKeep Alive Enable:\n"
					+ keepAliveEnable + "\nKeep Alive Delay(ms):\n"
					+ keepAliveDelay + "\nTransport:\n" + transport + "\n";
			try {
				String size = settings.getString("VIDEO_SIZE", "352x288");
				String sizes[] = size.split("x");
				width = Integer.parseInt(sizes[0]);
				height = Integer.parseInt(sizes[1]);
			} catch (NumberFormatException e) {
				width = 352;
				height = 288;
			}
			info_size_video = "Size:\n" + width + "x" + height;
			try {
				max_BW = Integer.parseInt(settings.getString("MAX_BW", ""));
			} catch (NumberFormatException e) {
				max_BW = null;
			}
			try {
				max_delay = Integer.parseInt(settings.getString("MAX_DELAY",
						"200"));
			} catch (NumberFormatException e) {
				max_delay = 200;
			}

			try {
				Boolean camera_t = settings.getBoolean("CAMERA_FRONT", false);
				if (camera_t) {
					cameraFacing = 1; // Camera Front
					camera = "Camera Front";
				} else {
					cameraFacing = 0; // Camera Back
					camera = "Camera Back";
				}
			} catch (NumberFormatException e) {
				cameraFacing = 0;
			}

			ApplicationContext.contextTable.put("cameraFacing", cameraFacing);
			try {
				max_FR = Integer.parseInt(settings.getString("MAX_FR", "15"));
			} catch (NumberFormatException e) {
				max_FR = 15;
			}

			try {
				gop_size = Integer
						.parseInt(settings.getString("GOP_SIZE", "6"));
			} catch (NumberFormatException e) {
				gop_size = 6;
			}

			try {
				max_queue = Integer.parseInt(settings.getString("QUEUE_SIZE",
						"2"));
			} catch (NumberFormatException e) {
				max_queue = 2;
			}

			callDirectionMap = getCallDirectionMapFromSettings();

			ApplicationContext.contextTable.put("callDirection",
					callDirectionMap);

			try {
				int minVideoPort = Integer.parseInt(settings.getString(
						"MIN_VIDEO_LOCAL_PORT", "0"));
				int maxVideoPort = Integer.parseInt(settings.getString(
						"MAX_VIDEO_LOCAL_PORT", "0"));
				Log.d(LOG_TAG, "minVideoPort: " + minVideoPort
						+ " maxVideoPort: " + maxVideoPort);
				videoPortRange = new PortRange(minVideoPort, maxVideoPort);
			} catch (Exception e) {
				videoPortRange = null;
				Log.d(LOG_TAG, "videoPortRange is null: " + e.toString());
			}

			try {
				int minAudioPort = Integer.parseInt(settings.getString(
						"MIN_AUDIO_LOCAL_PORT", "0"));
				int maxAudioPort = Integer.parseInt(settings.getString(
						"MAX_AUDIO_LOCAL_PORT", "0"));
				Log.d(LOG_TAG, "minAudioPort: " + minAudioPort
						+ " maxAudioPort: " + maxAudioPort);
				audioPortRange = new PortRange(minAudioPort, maxAudioPort);
			} catch (Exception e) {
				audioPortRange = null;
				Log.d(LOG_TAG, "audioPortRange is null: " + e.toString());
			}

			this.audioCodecs = getAudioCodecsFromSettings();
			this.videoCodecs = getVideoCodecsFromSettings();

			return true;
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error in parse preferences." + e.toString());
			return false;
		}
	}

	private boolean initControllerUAFromSettings() {
		try {
			if (getPreferences()) {

				// Controlar si ni == null .
				ni = connManager.getActiveNetworkInfo();
				String conType = ni.getTypeName();

				if ("WIFI".equalsIgnoreCase(conType)) {
					netIF = NetIF.WIFI;
					type_network = ConnectivityManager.TYPE_WIFI;
					if (max_BW == null)
						max_BW = 3000000;
				} else if ("MOBILE".equalsIgnoreCase(conType)) {
					netIF = NetIF.MOBILE;
					type_network = ConnectivityManager.TYPE_MOBILE;
					if (max_BW == null)
						max_BW = 384000;
				}

				info_call_type += "\n\nMax BW:\n" + max_BW + "\n\nMax Delay:\n"
						+ max_delay + "\n\nCamera:\n" + camera
						+ "\n\nMax FR:\n" + max_FR + "\n\nGOP Size:\n"
						+ gop_size + "\n\nMax Queue:\n" + max_queue;

				this.localAddress = NetworkIP.getLocalAddress();
				publicAddress = localAddress;
				publicPort = localPort;
				if (localAddress != null) {
					info_network = "IP Private: \n "
							+ localAddress.getHostAddress() + ":" + localPort;
				} else {
					info_network = "Problems with your IP. Review the configuration.";
				}
				ApplicationContext.contextTable.put("info_network",
						info_network);
				ApplicationContext.contextTable.put("localAddress",
						localAddress);

				return true;
			} else
				return false;
		} catch (Exception e) {
			Log.e(LOG_TAG, "Network interface unable.");
			// Toast.makeText(SoftPhone.this,
			// "SoftPhone: Please enable any network interface.",
			// Toast.LENGTH_SHORT).show();
			return true;
		}

	}

	private void initUA() {

		Log.d(LOG_TAG, "Init UA ....");
		try {
			notRegister();
			// dialogWait = ProgressDialog.show(SoftPhone.this, "",
			// "Please wait for few seconds...", true);
		} catch (Exception e) {

		}
		new Thread(new Runnable() {
			public void run() {
				try {
					// controller
					// .initUA(audioCodecs, audioPortRange, videoCodecs,
					// videoPortRange, localAddress, localPort,
					// netIF, callDirectionMap, max_BW, max_delay,
					// max_FR, gop_size, max_queue, width, height,
					// proxyIP, proxyPort, localUser,
					// localPassword, localRealm, stunHost,
					// stunPort, keepAliveDelay, keepAliveEnable,
					// transport, getApplicationContext());
					Boolean isStun = (Boolean) ApplicationContext.contextTable
							.get("isStunOk");
					if (isStun != null) {
						if (!isStun) {
							String stunInfo = (String) ApplicationContext.contextTable
									.get("stunError");
							if (stunInfo == null)
								stunInfo = "Unknow";
							info_connect += "\n -- Problem with Stun: \n "
									+ stunInfo + " --";
						}
					}

				} catch (Exception e) {
					Log.e(LOG_TAG, "Init UA : " + e.toString());
					if (localAddress != null)
						info_network = "IP Private: \n "
								+ localAddress.getHostAddress() + ":"
								+ localPort;
					else
						info_network = "Not connected";
				}
				if (dialogWait != null)
					dialogWait.dismiss();
				Integer localPortAux = (Integer) ApplicationContext.contextTable
						.get("localPort");

				if (localPortAux != null)
					info_network = "IP Private: \n "
							+ localAddress.getHostAddress() + ":"
							+ localPortAux;
				ApplicationContext.contextTable.put("info_network",
						info_network);
			}
		}).start();
		ApplicationContext.contextTable.put("controller", controller);
	}

	@Override
	public void update(Message message) {
		if (message.getData().containsKey("Register")) {
			if (message.getData().getString("Register").equals("Sucessful")) {
				registerSucessful();
			} else if (message.getData().getString("Register").equals("Failed")) {
				registerFailed();
			}
		} else if (message.getData().containsKey("finishActivity")) {
			if (message.getData().getString("finishActivity")
					.equals("MEDIA_CONTROL_OUTGOING")) {
				ApplicationContext.contextTable.remove("outgoingCall");
				finishActivity(MEDIA_CONTROL_OUTGOING);
			}
		} else if (message.getData().containsKey("Call")) {
			if (message.getData().getString("Call").equals("Reject")) {
			}
		}
	}

	private boolean isNewIp() {
		InetAddress lAddress;
		lAddressNew = NetworkIP.getLocalAddress();
		lAddress = (InetAddress) ApplicationContext.contextTable
				.get("localAddress");
		if (lAddress != null) {
			if (lAddress.equals(lAddressNew))
				return false;
			ApplicationContext.contextTable.put("localAddress", lAddress);
		}
		return true;
	}

	private final PhoneStateListener signalListener = new PhoneStateListener() {

		public void onDataConnectionStateChanged(int state) {
			if (!getIsExit()) {
				ConnectivityManager ConnectManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

				int networkType = -1;
				NetworkInfo activeNetwork = ConnectManager
						.getActiveNetworkInfo();
				if (activeNetwork != null) {
					networkType = activeNetwork.getType();
				}
				boolean isNetworking = false;
				switch (networkType) {
				case ConnectivityManager.TYPE_WIFI: // Disconnected
					// Register() with new ip.
					Log.d(LOG_TAG, "Connection OK, Register... WIFI");

					wifi.setBackgroundResource(R.drawable.wifi_on_120);
					_3g.setBackgroundResource(R.drawable.icon_3g_off_120);
					info_3g = "Not connected";
					info_wifi = (String) ApplicationContext.contextTable
							.get("info_network");
					type_network = ConnectivityManager.TYPE_WIFI;
					isNetworking = true;
					break;
				case ConnectivityManager.TYPE_MOBILE: // Connecting
					// Register() with new ip.
					Log.d(LOG_TAG, "Connection OK, Register...MOBILE");
					ApplicationContext.contextTable.put("isNetworking", true);
					info_wifi = "Not connected";
					info_3g = (String) ApplicationContext.contextTable
							.get("info_network");
					wifi.setBackgroundResource(R.drawable.wifi_off_120);
					_3g.setBackgroundResource(R.drawable.icon_3g_on_120);
					type_network = ConnectivityManager.TYPE_MOBILE;
					isNetworking = true;
					break;
				case -1: // Disconneted
					Log.d(LOG_TAG, "No Activate");
					wifi.setBackgroundResource(R.drawable.wifi_off_120);
					_3g.setBackgroundResource(R.drawable.icon_3g_off_120);
					info_wifi = "Not connected";
					info_3g = "Not connected";
					type_network = -1;
					isNetworking = false;
					break;
				default:
					break;
				}

				if (isNetworking) {
					// Destruir UA
					if (isNewIp()) {
						try {
							if (controller != null) {
								controller.finishUA();
							}
						} catch (Exception e1) {
							Log.e(LOG_TAG,
									"Exception (New ip) when controller.finishUA. "
											+ e1.toString());
						}

						isRegister = false;
						ApplicationContext.contextTable.put("isRegister",
								isRegister);

						intentService = (Intent) ApplicationContext.contextTable
								.get("intentService");

						ApplicationContext.contextTable.clear();
						// Registar
						if (initControllerUAFromSettings()) {
							Log.d(LOG_TAG, "****Register with new network = "
									+ localAddress.getHostAddress());
							register();
						}
						if (localAddress != null) {
							info_network = "IP Private: \n "
									+ localAddress.getHostAddress() + ":"
									+ localPort;
							ApplicationContext.contextTable.put("info_network",
									info_network);
						} else {
							Log.w(LOG_TAG, "LocalAddress is Null");
						}
					}
				} else {
					try {
						if ((Boolean) ApplicationContext.contextTable
								.get("isRegister")) {
							if (controller != null)
								try {
									controller.finishUA();
								} catch (Exception e1) {
									Log.e(LOG_TAG,
											"Exception (not connection) when controller.finishUA. "
													+ e1.toString());
								}
							isRegister = false;
							ApplicationContext.contextTable.put("isRegister",
									isRegister);

							intentService = (Intent) ApplicationContext.contextTable
									.get("intentService");

							ApplicationContext.contextTable.clear();
						}
						registerFailed();
					} catch (Exception e) {

					}
				}
			}
		}
	};

}