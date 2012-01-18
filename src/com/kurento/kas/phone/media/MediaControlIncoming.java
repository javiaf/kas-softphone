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
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
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
	private final static int NOTIF_SOFTPHONE = 1;

	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "Calling ...";
	private String notificationTitleSoft = "KurentoPhone";

	private Boolean isAccepted = false;
	private Boolean isRejected = false;
	
	MediaPlayer mPlayer;

	private ControlContacts controlcontacts = new ControlContacts(this);
	Vibrator vibrator;
	Controller controller = (Controller) ApplicationContext.contextTable
			.get("controller");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_call_incomingcall);
		Log.d(LOG_TAG, "Media Control Incoming Created");
		
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD 
					| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

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
				imageCall.setImageBitmap(controlcontacts.getRefelection(bm));
			}else{
				imageCall.setImageBitmap(controlcontacts.getRefelection(BitmapFactory.decodeResource(getResources(),R.drawable.image_call)));
			}
		}

		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		mPlayer = MediaPlayer.create(this, R.raw.tone_call);
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
				nValue.put("uri", uri.substring(4));
				nValue.put("name", name);
				nValue.put("type", true);
				db.insert("DBHistoryCall", null, nValue);
				ApplicationContext.contextTable.put("db", db);
			}
		}
	}

	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		ApplicationContext.contextTable.put("incomingCall", getIntent());
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

	private void reject() {
		vibrator.cancel();
		if (controller != null) {
			try {
				controller.reject();
				ApplicationContext.contextTable.remove("incomingCall");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume");

		isAccepted = false;
		isRejected = false;
		mPlayer.setLooping(true);
		mPlayer.start();

		final DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		final ImageButton buttonCall = (ImageButton) findViewById(R.id.button_call_accept);
		buttonCall.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE: {
					if (v.equals(buttonCall)) {
						int x = (int) event.getRawX() - 120;

						if ((x < dm.widthPixels / 4 - 70) && (x > -70)) {
							int a = x - buttonCall.getLeft();
							buttonCall.layout(buttonCall.getLeft() + a,
									buttonCall.getTop(), buttonCall.getRight()
											+ a, buttonCall.getBottom());

						} else if ((x > dm.widthPixels / 4) && (!isAccepted)) {
							vibrator.cancel();
							if (mPlayer != null)
								mPlayer.stop();
							if (controller != null) {
								try {
									controller.aceptCall();
									ApplicationContext.contextTable
											.remove("incomingCall");
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							isAccepted = true;
							finish();
						}
					}
				}
					break;
				}
				return true;

			}
		});

		final ImageButton buttonReject = (ImageButton) findViewById(R.id.button_call_reject);
		buttonReject.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE: {
					if (v.equals(buttonReject)) {
						int x = (int) event.getRawX() - 120;

						if ((x < dm.widthPixels - 100)
								&& (x > dm.widthPixels / 2)) {
							int a = x - buttonReject.getLeft();
							buttonReject.layout(buttonReject.getLeft() + a,
									buttonReject.getTop(),
									buttonReject.getRight() + a,
									buttonReject.getBottom());

						} else if ((x < dm.widthPixels / 2) && (!isRejected)) {
							Log.d(LOG_TAG, "Call Canceled");
							isRejected = true;
							if (mPlayer != null)
								mPlayer.stop();
							reject();
						}
					}
				}
					break;
				}
				return true;

			}
		});

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			reject();
		}
		return true;
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
		if (mPlayer != null)
			mPlayer.stop();
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
