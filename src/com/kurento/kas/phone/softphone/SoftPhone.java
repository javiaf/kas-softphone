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
package com.kurento.kas.phone.softphone;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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
import android.widget.Toast;

import com.kurento.commons.sdp.enums.MediaType;
import com.kurento.commons.sdp.enums.Mode;
import com.kurento.kas.media.codecs.AudioCodecType;
import com.kurento.kas.media.codecs.VideoCodecType;
import com.kurento.kas.mscontrol.networkconnection.NetIF;
import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.controlcontacts.ControlContacts;
import com.kurento.kas.phone.historycall.HistoryCall;
import com.kurento.kas.phone.historycall.ListViewHistoryItem;
import com.kurento.kas.phone.media.MediaControlOutgoing;
import com.kurento.kas.phone.network.NetworkIP;
import com.kurento.kas.phone.preferences.Connection_Preferences;
import com.kurento.kas.phone.preferences.Video_Preferences;
import com.kurento.kas.phone.sip.Controller;

import de.javawi.jstun.attribute.MessageAttributeException;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.test.*;
import de.javawi.jstun.util.UtilityException;

public class SoftPhone extends Activity implements ServiceUpdateUIListener {
	private final int MEDIA_CONTROL_OUTGOING = 0;
	private final int SHOW_PREFERENCES = 1;
	private final int PICK_CONTACT_REQUEST = 2;
	private final int FIRST_REGISTER_PREFERENCES = 3;
	private final int HISTORY_CALL = 4;

	private static final String LOG_TAG = "SoftPhone";

	private ArrayList<AudioCodecType> audioCodecs;
	private ArrayList<VideoCodecType> videoCodecs;
	private Map<MediaType, Mode> callDirectionMap;
	private InetAddress localAddress;
	private InetAddress publicAddress;
	private InetAddress lAddressNew;
	private int localPort = 6060;
	private int publicPort;
	private NetIF netIF;
	// ConnectivityManager ConnectManager;
	ConnectivityManager connManager;
	NetworkInfo ni;

	private String localUser;
	private String localRealm;
	private String localPassword;
	private String proxyIP;
	private int proxyPort;

	private Integer max_BW;
	private Integer max_FR;
	private Integer gop_size;
	private Integer max_queue;

	private String info_connect;

	private String info_wifi = "Not connected";
	private String info_3g = "Not connected";
	private String info_video;
	private String info_audio_aux;
	private String info_video_aux;
	private String info_call_type;

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

		netIF = null;
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
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		SoftPhoneService.setUpdateListener(this);

		netIF = null;
		connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		ni = connManager.getActiveNetworkInfo();
		if (ni != null) {
			if (intent.getData() == null)
				return;
			checkCallIntent(intent);
		} else {
			Log.e(LOG_TAG, "Network interface unable.");
			Toast.makeText(SoftPhone.this,
					"SoftPhone: Please enable any network interface.",
					Toast.LENGTH_SHORT).show();
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
				info_video = "Codecs: \n\n" + info_video_aux + "\n\n"
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

	private synchronized void setIsExit(boolean type) {
		isExit = type;
	}

	private synchronized boolean getIsExit() {
		return isExit;
	}

	@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "On Destroy");
		try {
			if (getIsExit()) {
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
			info_video_aux = "Video Codec:\n H263";
		}
		if (settings.getBoolean("MPEG4_CODEC", false)) {
			selectedVideoCodecs.add(VideoCodecType.MPEG4);
			info_video_aux = "Video Codec:\n MPEG4";
		}
		if (settings.getBoolean("H264_CODEC", false)) {
			selectedVideoCodecs.add(VideoCodecType.H264);
			info_video_aux = "Video Codec:\n H264";
		}

		return selectedVideoCodecs;
	}

	private ArrayList<AudioCodecType> getAudioCodecsFromSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		ArrayList<AudioCodecType> selectedAudioCodecs = new ArrayList<AudioCodecType>();
		if (settings.getBoolean("AMR_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.AMR);
			info_audio_aux = "Audio Codec:\n AMR";
		}
		if (settings.getBoolean("MP2_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.MP2);
			info_audio_aux = "Audio Codec:\n MP2";
		}
		if (settings.getBoolean("AAC_AUDIO_CODEC", false)) {
			selectedAudioCodecs.add(AudioCodecType.AAC);
			info_audio_aux = "Audio Codec:\n AAC";
		}

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

	private boolean getPreferences() {
		try {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());
			localUser = settings.getString("LOCAL_USERNAME", "");
			localPassword = settings.getString("LOCAL_PASSWORD", "");
			localRealm = settings.getString("LOCAL_DOMAIN", "");
			proxyIP = settings.getString("PROXY_IP", "");
			proxyPort = Integer.parseInt(settings.getString("PROXY_PORT", "0"));

			if (localUser.equals("") || localRealm.equals("")
					|| proxyIP.equals("") || proxyPort == 0)
				return false;

			info_connect = "Connecting ... \n\n User: \n " + localUser + "@"
					+ localRealm + "\n\n Server:\n " + proxyIP + ":"
					+ proxyPort;

			try {
				max_BW = Integer.parseInt(settings.getString("MAX_BW", ""));
			} catch (NumberFormatException e) {
				max_BW = null;
			}

			try {
				max_FR = Integer.parseInt(settings.getString("MAX_FR", ""));
			} catch (NumberFormatException e) {
				max_FR = null;
			}

			try {
				gop_size = Integer.parseInt(settings.getString("GOP_SIZE", ""));
			} catch (NumberFormatException e) {
				gop_size = null;
			}

			try {
				max_queue = Integer.parseInt(settings.getString("QUEUE_SIZE",
						""));
			} catch (NumberFormatException e) {
				max_queue = null;
			}

			callDirectionMap = getCallDirectionMapFromSettings();

			info_call_type += "\n\nMax BW:\n" + max_BW + "\n\nMax FR:\n"
					+ max_FR + "\n\nGOP Size:\n" + gop_size
					+ "\n\nMax Queue:\n" + max_queue;

			ApplicationContext.contextTable.put("callDirection",
					callDirectionMap);

			this.audioCodecs = getAudioCodecsFromSettings();
			this.videoCodecs = getVideoCodecsFromSettings();

			return true;
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error in parse preferences.");
			e.printStackTrace();
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
					netIF = NetIF.WIFI;
				else if ("MOBILE".equalsIgnoreCase(conType))
					netIF = NetIF.MOBILE;

				this.localAddress = NetworkIP.getLocalAddress();
				publicAddress = localAddress;
				if (isNewIp())
					update_stun();
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
			return false;
		}

	}

	private void initUA() {
		try {

			controller.initUA(audioCodecs, videoCodecs, localAddress,
					localPort, publicAddress, publicPort, netIF,
					callDirectionMap, max_BW, max_FR, gop_size, max_queue,
					proxyIP, proxyPort, localUser, localPassword, localRealm);
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
		} else if (message.getData().containsKey("Call")) {
			if (message.getData().getString("Call").equals("Reject")) {
				Log.d(LOG_TAG, "cALL rEJECT");

			}
		}

	}

	private boolean isNewIp() {

		InetAddress lAddress;
		lAddressNew = NetworkIP.getLocalAddress();
		lAddress = (InetAddress) ApplicationContext.contextTable
				.get("localAddress");

		if (lAddress != null)
			if (lAddress.equals(lAddressNew))
				return false;
		return true;
	}

	private final PhoneStateListener signalListener = new PhoneStateListener() {

		public void onDataConnectionStateChanged(int state) {
			if (!getIsExit()) {
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
				boolean isNetworking = false;
				switch (networkType) {
				case ConnectivityManager.TYPE_WIFI: // Disconnected
					// Register() with new ip.
					Log.d(LOG_TAG, "Connection OK, Register... WIFI");

					wifi.setBackgroundResource(R.drawable.wifi_on_120);
					_3g.setBackgroundResource(R.drawable.icon_3g_off_120);
					info_wifi = "Wifi enable. \n IP: \n " + lAddressNew;
					info_3g = "Not connected";
					isNetworking = true;
					break;
				case ConnectivityManager.TYPE_MOBILE: // Connecting
					// Register() with new ip.
					Log.d(LOG_TAG, "Connection OK, Register...MOBILE");
					ApplicationContext.contextTable.put("isNetworking", true);

					wifi.setBackgroundResource(R.drawable.wifi_off_120);
					_3g.setBackgroundResource(R.drawable.icon_3g_on_120);
					info_wifi = "Not connected";
					info_3g = "3G enable. \n IP: \n " + lAddressNew;

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
					Log.d(LOG_TAG, "****IsNetwoking ; isNewIp = " + isNewIp());
					if (isNewIp()) {
						update_stun();
						try {
							if (controller != null) {
								controller.finishUA();
								controller = null;
							}
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
						registerFailed();
					} catch (Exception e) {

					}
				}
			}
		}
	};

	private void update_stun() {
		DiscoveryTest test = new DiscoveryTest(localAddress, localPort,
				"stun.sipgate.net", 10000);
		try {
			DiscoveryInfo info = test.test();
			publicAddress = info.getPublicIP();
			publicPort = info.getPublicPort();

			Log.d(LOG_TAG, "Private IP:" + localAddress + ":" + localPort
					+ "\nPublic IP: " + publicAddress + ":" + publicPort);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessageAttributeParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessageHeaderParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UtilityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessageAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
