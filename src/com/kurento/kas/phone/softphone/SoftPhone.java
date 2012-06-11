package com.kurento.kas.phone.softphone;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.controlcontacts.ControlContacts;
import com.kurento.kas.phone.exception.ErrorReporter;
import com.kurento.kas.phone.historycall.HistoryCall;
import com.kurento.kas.phone.historycall.ListViewHistoryItem;
import com.kurento.kas.phone.media.MediaControlOutgoing;
import com.kurento.kas.phone.preferences.Connection_Preferences;
import com.kurento.kas.phone.preferences.Video_Preferences;
import com.kurento.kas.phone.shared.Actions;
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.testutils.SoftphoneController;

public class SoftPhone extends Activity implements ServiceUpdateUIListener,
		OnClickListener {
	private static final String LOG_TAG = SoftPhone.class.getName();

	private final int MEDIA_CONTROL_OUTGOING = 0;
	private final int SHOW_PREFERENCES = 1;
	private final int PICK_CONTACT_REQUEST = 2;
	private final int FIRST_REGISTER_PREFERENCES = 3;
	private final int FIRST_REGISTER_MEDIA_PREFERENCES = 5;
	private final int FIRST_REGISTER_CONNECTION_PREFERENCES = 6;
	private final int FINISH_REGISTER_PREFERENCES = 7;
	private final int HISTORY_CALL = 4;

	private Button connection, wifi, _3g, video, buttonCall, buttonContacts;

	private boolean isCreated = false;
	private ProgressDialog dialogWait;
	private Intent intentService;

	private String info_connect, info_video;

	private Controller controller;
	private boolean isRegister = false;
	private boolean isExit = false;
	private ErrorReporter errorReporter;

	private ControlContacts controlcontacts = new ControlContacts(this);

	private static SoftphoneController softphoneController;
	private static String localAddressTest = null;

	private IntentFilter intentFilter;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(LOG_TAG, "Action = " + action);
			if (Actions.REGISTER_USER_SUCESSFUL.equals(action)) {
				Log.d(LOG_TAG, "softphoneController : " + softphoneController);
				if (softphoneController != null)
					softphoneController.onEvent(action);
				connection.setBackgroundResource(R.drawable.connect_icon);
			} else if (Actions.REGISTER_USER_FAIL.equals(action)
					|| Actions.UNREGISTER_USER_SUCESSFUL.equals(action)) {
				if (softphoneController != null)
					softphoneController.onEvent(action);
				connection.setBackgroundResource(R.drawable.disconnect_icon);
			} else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
				NetworkInfo ni = intent.getExtras()
						.getParcelable("networkInfo");
				Log.d(LOG_TAG, "Connection Type: " + ni.getType() + "; State:"
						+ ni.getState());
				switch (ni.getType()) {
				case ConnectivityManager.TYPE_WIFI:
					if (ni.getState().equals(NetworkInfo.State.CONNECTED)) {
						wifi.setBackgroundResource(R.drawable.wifi_on_120);
						if (controller != null && controller.getUa() == null)
							controller.connectionHasChanged();
						else if (controller != null)
							controller.networkChanged();
						controller.mediaHasChanged();
					} else {
						wifi.setBackgroundResource(R.drawable.wifi_off_120);
						connection
								.setBackgroundResource(R.drawable.disconnect_icon);
						if (controller != null && controller.getIsCall())
							controller.hang();
					}
					break;
				case ConnectivityManager.TYPE_MOBILE:
					if (ni.getState().equals(NetworkInfo.State.CONNECTED)) {
						_3g.setBackgroundResource(R.drawable.icon_3g_on_120);
						if (controller != null && controller.getUa() == null)
							controller.connectionHasChanged();
						else if (controller != null)
							controller.networkChanged();
						controller.mediaHasChanged();
					} else {
						_3g.setBackgroundResource(R.drawable.icon_3g_off_120);
						connection
								.setBackgroundResource(R.drawable.disconnect_icon);
						if (controller != null && controller.getIsCall())
							controller.hang();
					}
					break;
				default:
					break;
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setViewById();

		intentFilter = new IntentFilter();
		isCreated = true;
		errorReporter = new ErrorReporter();
		errorReporter.Init(getApplicationContext());

		// Start BD
		// TODO This code must changed from other class that manages this
		SQLiteDatabase db = (SQLiteDatabase) ApplicationContext.contextTable
				.get("db");

		if (db == null)
			db = HistoryCall.openOrCreateBD(getApplicationContext());

		ApplicationContext.contextTable.put("db", db);

		// if (Connection_Preferences
		// .getConnectionPreferences(getApplicationContext()) == null) {
		// /* First Register */
		// Intent first_register = new Intent(SoftPhone.this, Register.class);
		// startActivityForResult(first_register, FIRST_REGISTER_PREFERENCES);
		// } else {
		// if (controller == null)
		// createController();
		// else
		// serviceAndFilters();
		// }
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		/*
		 * With this, I can control if some activity has been created and the
		 * user has pushed the home button and to start that activity.
		 */
		// if ((Intent) ApplicationContext.contextTable.get("outgoingCall") !=
		// null) {
		// Intent i = (Intent) ApplicationContext.contextTable
		// .get("outgoingCall");
		// startActivityForResult(i, MEDIA_CONTROL_OUTGOING);
		// } else if ((Intent)
		// ApplicationContext.contextTable.get("incomingCall") != null) {
		// Intent i = (Intent) ApplicationContext.contextTable
		// .get("incomingCall");
		// startActivity(i);
		// } else if ((Intent) ApplicationContext.contextTable.get("videoCall")
		// != null) {
		// Intent i = (Intent) ApplicationContext.contextTable
		// .get("videoCall");
		// startActivity(i);
		// }
	}

	private void createController() {
		dialogWait = ProgressDialog.show(SoftPhone.this, "", "Please wait ...",
				true, true, new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						Log.d(LOG_TAG, "Cancel Dialog");
						if (dialogWait != null && dialogWait.isShowing())
							dialogWait.dismiss();
					}
				});

		new Thread(new Runnable() {
			public void run() {
				try {
					controller = new Controller(getApplicationContext());
					// Only for test
					if (localAddressTest != null)
						controller.setLocalAddress(localAddressTest);
					controller.configureController();
					ApplicationContext.contextTable.put("controller",
							controller);
					Log.d(LOG_TAG, "controller completed");
					serviceAndFilters();
					if (dialogWait != null && dialogWait.isShowing())
						dialogWait.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void serviceAndFilters() {
		intentService = (Intent) ApplicationContext.contextTable
				.get("intentService");
		if (intentService == null) {
			intentService = new Intent(this, SoftPhoneService.class);
			ApplicationContext.contextTable.put("intentService", intentService);
			startService(intentService);
		}
		if (intentFilter != null) {
			intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			intentFilter.addAction(Actions.REGISTER_USER_SUCESSFUL);
			intentFilter.addAction(Actions.REGISTER_USER_FAIL);
			intentFilter.addAction(Actions.UNREGISTER_USER_SUCESSFUL);
			intentFilter.addAction(Actions.UNREGISTER_USER_FAIL);
		}
		registerReceiver(mReceiver, intentFilter);
	}

	private void setViewById() {
		connection = (Button) findViewById(R.id.connection_button);
		connection.setOnClickListener(this);
		_3g = (Button) findViewById(R.id.info_3g_button);
		_3g.setOnClickListener(this);
		wifi = (Button) findViewById(R.id.wifi_button);
		wifi.setOnClickListener(this);
		video = (Button) findViewById(R.id.video_button);
		video.setOnClickListener(this);
		buttonContacts = (Button) findViewById(R.id.contacts);
		buttonContacts.setOnClickListener(this);
		buttonCall = (Button) findViewById(R.id.call);
		buttonCall.setOnClickListener(this);

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (isCreated) {
			if (Connection_Preferences
					.getConnectionPreferences(getApplicationContext()) == null) {
				/* First Register */
				Intent first_register = new Intent(SoftPhone.this,
						Register.class);
				startActivityForResult(first_register,
						FIRST_REGISTER_PREFERENCES);
			} else {
				if (controller == null)
					createController();
				else
					serviceAndFilters();
			}
			isCreated = false;
		}

	}

	private synchronized void setIsExit(boolean type) {
		isExit = type;
	}

	private synchronized boolean getIsExit() {
		return isExit;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exitApp();
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		try {
			if (getIsExit()) {
				unregisterReceiver(mReceiver);
				if (controller != null)
					controller.finishUA();
				isRegister = false;

				intentService = (Intent) ApplicationContext.contextTable
						.get("intentService");
				try {
					stopService(intentService);
				} catch (Exception e) {
					Log.e(LOG_TAG,
							"stopService " + e.getMessage() + "; "
									+ e.toString());
				}

				SQLiteDatabase db = (SQLiteDatabase) ApplicationContext.contextTable
						.get("db");
				if (db != null)
					db.close();

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
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		final Dialog dialog = new Dialog(v.getContext());
		switch (v.getId()) {
		case R.id.connection_button:
			dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			info_connect = Connection_Preferences
					.getConnectionPreferencesInfo(getApplicationContext());
			if (controller != null)
				info_connect += "\n\n" + controller.getConnectionType();
			dialog.setContentView(R.layout.info_connection);
			((TextView) dialog.findViewById(R.id.info_connect))
					.setText(info_connect);
			dialog.show();
			break;
		case R.id.video_button:
			dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			info_video = Video_Preferences
					.getMediaPreferencesInfo(getApplicationContext());
			info_video += "\n\n"
					+ Connection_Preferences
							.getConnectionNetPreferenceInfo(getApplicationContext());
			dialog.setContentView(R.layout.info_video);
			((TextView) dialog.findViewById(R.id.info_video))
					.setText(info_video);
			dialog.show();
			break;
		case R.id.contacts:
			try {
				openContacts();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage(), e);
			}
			break;
		case R.id.call:
			Log.d(LOG_TAG, "Call is pushed ");
			Intent history_call = new Intent(SoftPhone.this, HistoryCall.class);
			startActivityForResult(history_call, HISTORY_CALL);
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case HISTORY_CALL:
			if (resultCode == RESULT_OK) {
				if (controller != null) {
					if (!controller.getIsCall()) {
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
					} else {
						try {
							Toast.makeText(this, "Another call in progress",
									Toast.LENGTH_SHORT).show();
						} catch (Exception e) {
							Log.e(LOG_TAG, e.getMessage(), e);
						}
					}
				}
			}
			break;
		case FIRST_REGISTER_PREFERENCES:
			if (resultCode == RESULT_CANCELED) {
				Log.d(LOG_TAG, "Result Cancel");
				finish();
			} else {
				Intent remotePreferences = new Intent(this,
						Connection_Preferences.class);
				startActivityForResult(remotePreferences,
						FIRST_REGISTER_CONNECTION_PREFERENCES);
			}
			break;
		case FIRST_REGISTER_CONNECTION_PREFERENCES:
			Intent remotePreferences = new Intent(this, Video_Preferences.class);
			startActivityForResult(remotePreferences,
					FIRST_REGISTER_MEDIA_PREFERENCES);
			break;
		case FIRST_REGISTER_MEDIA_PREFERENCES:
			if (controller == null)
				createController();
			break;
		case FINISH_REGISTER_PREFERENCES:

			break;
		case SHOW_PREFERENCES:
			if (Video_Preferences.isPreferenceChanged())
				if (controller != null)
					controller.mediaHasChanged();

			if (Connection_Preferences.isPreferenceChanged())
				if (controller != null) {
					dialogWait = ProgressDialog.show(SoftPhone.this, "",
							"Please wait for few seconds...", true);
					new Thread(new Runnable() {
						public void run() {
							try {
								controller.connectionHasChanged();
								if (dialogWait != null)
									dialogWait.dismiss();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).start();
				}

			Video_Preferences.resetChanged();
			Connection_Preferences.resetChanged();
			break;
		case PICK_CONTACT_REQUEST:
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
			break;
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
			exitApp();
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
		case (R.id.menu_register):
			if (controller != null) {
				dialogWait = ProgressDialog.show(SoftPhone.this, "",
						"Please wait ...", true);

				new Thread(new Runnable() {
					public void run() {
						try {
							controller.connectionHasChanged();
							ApplicationContext.contextTable.put("controller",
									controller);
							Log.d(LOG_TAG, "controller completed");
							if (dialogWait != null)
								dialogWait.dismiss();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			} else {
				createController();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void exitApp() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"You won't receive more calls. Are you sure you want to exit?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setIsExit(true);

								finish();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void searchCallContact(String uri) {
		String textRemoteUri = uri;
		String remoteURI = "sip:";
		remoteURI += textRemoteUri;
		Integer idContact;
		idContact = controlcontacts.getId(textRemoteUri);

		call(remoteURI, idContact);
	}

	private void openContacts() {
		Intent intentContacts = new Intent(Intent.ACTION_PICK,
				ContactsContract.Contacts.CONTENT_URI);
		startActivityForResult(intentContacts, PICK_CONTACT_REQUEST);
	}

	public void call(String remoteURI, Integer id) {
		if (controller != null && controller.getUa() != null) {
			try {
				Intent mediaIntent = new Intent(SoftPhone.this,
						MediaControlOutgoing.class);
				mediaIntent.putExtra("Id", id);
				mediaIntent.putExtra("Uri", remoteURI);
				startActivityForResult(mediaIntent, MEDIA_CONTROL_OUTGOING);
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage(), e);
			}
		} else
			Toast.makeText(this, "You must be register", Toast.LENGTH_SHORT)
					.show();
	}

	@Override
	public void update(Message message) {

	}

	public static void setSoftphoneController(SoftphoneController sController,
			String localAddress) {
		softphoneController = sController;
		localAddressTest = localAddress;

	}
}
