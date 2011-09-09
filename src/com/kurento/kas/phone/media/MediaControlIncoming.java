package com.kurento.kas.phone.media;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.controlcontacts.ControlContacts;
import com.kurento.kas.phone.historycall.ListViewHistoryItem;
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.softphone.R;
import com.kurento.kas.phone.softphone.ServiceUpdateUIListener;
import com.kurento.kas.phone.softphone.SoftPhone;
import com.kurento.kas.phone.softphone.SoftPhoneService;

public class MediaControlIncoming extends Activity implements
		ServiceUpdateUIListener {
	private static final String LOG_TAG = "MediaControlIncoming";

	private NotificationManager mNotificationMgr;
	private final static int NOTIF_CALLING_IN = 4;
	private final static int NOTIF_CALLING_OUT = 3;
	private final static int NOTIF_VIDEOCALL = 2;
	private final static int NOTIF_SOFTPHONE = 1;

	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "Calling ...";
	private String notificationTitleSoft = "KurentoPhone";

	private ControlContacts controlcontacts = new ControlContacts(this);
	Vibrator vibrator;
	Controller controller = (Controller) ApplicationContext.contextTable
			.get("controller");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_call_incomingcall);
		Log.d(LOG_TAG, "Media Control Incoming Created");

		mNotificationMgr = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotif = new Notification(R.drawable.sym_call_incoming,
				notificationTitle, System.currentTimeMillis());
		mNotif.flags |= Notification.FLAG_ONGOING_EVENT;

		notifIntent = new Intent(this, MediaControlIncoming.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitle, "",
				mNotifContentIntent);

		mNotificationMgr.notify(NOTIF_CALLING_IN, mNotif);
		mNotificationMgr.cancel(NOTIF_SOFTPHONE);

		SoftPhoneService.setUpdateListener(this);

		Bundle extras = getIntent().getExtras();

		if (extras == null)
			extras = (Bundle) ApplicationContext.contextTable.get("extrasIn");
		else
			ApplicationContext.contextTable.put("extrasIn", extras);

		String uri = (String) extras.getSerializable("Uri");

		TextView text = (TextView) findViewById(R.id.incoming_sip);
		text.setText(uri);

		String[] sipArray = uri.split(":");
		String sipUri = "";
		Log.d(LOG_TAG, "SipArray = " + sipArray.length);
		if (sipArray.length > 1)
			sipUri = sipArray[1];
		else
			sipUri = sipArray[0];

		Log.d(LOG_TAG, "sipUri = " + sipUri);
		Integer idContact = controlcontacts.getId(sipUri);
		String name = sipUri;
		if (idContact != -1)
			name = controlcontacts.getName(idContact);

		Log.d(LOG_TAG, "idContact = " + idContact + "; name = " + name);
		if (!idContact.equals("")) {
			ImageView imageCall = (ImageView) findViewById(R.id.image_call);
			Bitmap bm = controlcontacts.getPhoto(idContact);
			if (bm != null) {
				imageCall.setImageBitmap(bm);
			}
		}

		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		long[] pattern = { 0, 1000, 2000, 3000 };

		vibrator.vibrate(pattern, 1);

		@SuppressWarnings("unchecked")
		ArrayList<ListViewHistoryItem> items = (ArrayList<ListViewHistoryItem>) ApplicationContext.contextTable
				.get("itemsHistory");

		if (items == null)
			items = new ArrayList<ListViewHistoryItem>();

		Calendar date = new GregorianCalendar();
		Integer minute = date.get(Calendar.MINUTE);
		Integer day = date.get(Calendar.DAY_OF_MONTH);
		Integer month = date.get(Calendar.MONTH) + 1;

		String tMinute = String.valueOf(minute);
		String tDay = String.valueOf(day);
		String tMonth = String.valueOf(month);

		if (minute < 10)
			tMinute = "0" + minute;
		if (day < 10)
			tDay = "0" + day;
		if (month < 10)
			tMonth = "0" + tMonth;

		String dateS = date.get(Calendar.HOUR_OF_DAY) + ":" + tMinute + " "
				+ tDay + "/" + tMonth + "/" + date.get(Calendar.YEAR);

		SQLiteDatabase db = (SQLiteDatabase) ApplicationContext.contextTable
				.get("db");
		if (db != null) {
			if (db.isOpen()) {
				ContentValues nValue = new ContentValues();
				nValue.put("id", idContact);
				nValue.put("date", dateS);
				nValue.put("uri", sipUri);
				nValue.put("name", name);
				nValue.put("type", true);
				db.insert("DBHistoryCall", null, nValue);
				ApplicationContext.contextTable.put("db", db);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LOG_TAG, "onStart");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(LOG_TAG, "onRestart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume");

		final Button buttonCall = (Button) findViewById(R.id.button_call_accept);
		buttonCall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Call Accepted " + RESULT_OK);
				vibrator.cancel();
				if (controller != null) {
					try {
						controller.aceptCall();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				finish();
			}
		});

		final Button buttonReject = (Button) findViewById(R.id.button_call_reject);
		buttonReject.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Call Canceled");
				vibrator.cancel();
				if (controller != null) {
					try {
						controller.reject();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				finish();
			}
		});

	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "onStop");
		vibrator.cancel();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "onPause");
		vibrator.cancel();
	}

	@Override
	protected void onDestroy() {

		vibrator.cancel();
		mNotificationMgr.cancel(NOTIF_CALLING_IN);
		mNotif = new Notification(R.drawable.icon, notificationTitleSoft,
				System.currentTimeMillis());

		notifIntent = new Intent(this, SoftPhone.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitleSoft, "",
				mNotifContentIntent);

		mNotificationMgr.notify(NOTIF_SOFTPHONE, mNotif);
		Log.d(LOG_TAG, "onDestroy");
		super.onDestroy();

	}

	@Override
	public void update(Message message) {
		Log.d(LOG_TAG, "Message = " + message);
		if (message.getData().containsKey("Call")) {
			if (message.getData().getString("Call").equals("Cancel")) {
				finish();
			}
		}

	}

}
