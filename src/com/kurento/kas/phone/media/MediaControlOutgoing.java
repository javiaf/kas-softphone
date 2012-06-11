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

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.kurento.kas.phone.applicationcontext.ApplicationContext;
import com.kurento.kas.phone.controlcontacts.ControlContacts;
import com.kurento.kas.phone.shared.Actions;
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.softphone.R;
import com.kurento.kas.phone.softphone.SoftPhone;

public class MediaControlOutgoing extends Activity {
	private static final String LOG_TAG = MediaControlOutgoing.class.getName();

	private NotificationManager mNotificationMgr;
	private final static int NOTIF_CALLING_OUT = 3;
	private final static int NOTIF_SOFTPHONE = 1;

	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "Calling ...";
	private String notificationTitleSoft = "KurentoPhone";

	private Handler mHandler = new Handler();
	private ImageButton buttonReject;

	private Boolean isCanceled = false;
	private Boolean isRejectOrError = false;

	MediaPlayer mPlayer;

	private IntentFilter intentFilter;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			TextView outgoing_call = (TextView) findViewById(R.id.outgoing_call);
			if (Actions.OUTGOING_CALL_CLOSE.equals(action)) {
				if (!isRejectOrError) {
					outgoing_call.setTextColor(Color.RED);
					outgoing_call.setText("Call Finish");
					outgoing_call.setTextSize(20);
					finishHandler((long) 2500);
				}
			} else if (Actions.CALL_REJECT.equals(action)) {
				outgoing_call.setTextColor(Color.RED);
				outgoing_call.setText("Decline call");
				outgoing_call.setTextSize(20);
				isRejectOrError = true;
				finishHandler((long) 2500);
			} else if (Actions.CALL_BUSY.equals(action)) {
				outgoing_call.setTextColor(Color.RED);
				outgoing_call.setText("Busy line");
				outgoing_call.setTextSize(20);
				isRejectOrError = true;
				finishHandler((long) 2500);
			} else if (Actions.CALL_ERROR.equals(action)) {
				Bundle extras = intent.getExtras();
				String message = "Call Error\n";
				if (extras != null)
					message = message + extras.getString("message");
				outgoing_call.setTextColor(Color.RED);
				outgoing_call.setText(message);
				outgoing_call.setTextSize(20);
				isRejectOrError = true;
				finishHandler((long) 5000);
			} else if (Actions.MEDIA_NOT_SUPPORTED.equals(action)) {
				outgoing_call.setTextColor(Color.RED);
				outgoing_call.setText("Media Not Supported");
				outgoing_call.setTextSize(20);
				isRejectOrError = true;
				finishHandler((long) 2500);
			} else if (Actions.USER_NOT_FOUND.equals(action)) {
				outgoing_call.setTextColor(Color.RED);
				outgoing_call.setText("User Not Found");
				outgoing_call.setTextSize(20);
				isRejectOrError = true;
				finishHandler((long) 2500);
			} else if (Actions.CALL_RINGING.equals(action)) {
				// Play sound
				if (mPlayer == null)
					mPlayer = MediaPlayer.create(getApplicationContext(),
							R.raw.tone_call);
				if (mPlayer != null && !mPlayer.isPlaying()) {
					mPlayer.setLooping(true);
					mPlayer.start();
				}
			} else if (Actions.CURRENT_CALL_OK.equals(action)) {
				buttonReject = (ImageButton) findViewById(R.id.button_call_reject);
				buttonReject.setVisibility(View.VISIBLE);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.control_call_outgoingcall);

		buttonReject = (ImageButton) findViewById(R.id.button_call_reject);
		buttonReject.setVisibility(View.INVISIBLE);

		intentFilter = new IntentFilter();

		if (intentFilter != null) {
			intentFilter.addAction(Actions.CALL_REJECT);
			intentFilter.addAction(Actions.CALL_ERROR);
			intentFilter.addAction(Actions.OUTGOING_CALL_CLOSE);
			intentFilter.addAction(Actions.MEDIA_NOT_SUPPORTED);
			intentFilter.addAction(Actions.USER_NOT_FOUND);
			intentFilter.addAction(Actions.CALL_RINGING);
			intentFilter.addAction(Actions.CALL_BUSY);
			intentFilter.addAction(Actions.CURRENT_CALL_OK);
		}
		registerReceiver(mReceiver, intentFilter);

		mNotificationMgr = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotif = new Notification(R.drawable.sym_call_outgoing,
				notificationTitle, System.currentTimeMillis());
		mNotif.flags |= Notification.FLAG_ONGOING_EVENT;

		notifIntent = new Intent(this, MediaControlOutgoing.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitle, "",
				mNotifContentIntent);

		mNotificationMgr.notify(NOTIF_CALLING_OUT, mNotif);
		mNotificationMgr.cancel(NOTIF_SOFTPHONE);

		Bundle extras = getIntent().getExtras();

		if (extras == null)
			extras = (Bundle) ApplicationContext.contextTable.get("extrasOut");
		else
			ApplicationContext.contextTable.put("extrasOut", extras);

		final String uri = (String) extras.getSerializable("Uri");
		Integer id = (Integer) extras.getSerializable("Id");

		TextView text = (TextView) findViewById(R.id.outgoing_sip);
		text.setText(uri);

		ImageView imageCall = (ImageView) findViewById(R.id.image_call);

		ControlContacts controlcontacts = new ControlContacts(this);

		String name = uri;
		if (id != -1)
			name = controlcontacts.getName(id);

		Bitmap bm = controlcontacts.getPhoto(id);

		if (bm != null) {
			imageCall.setImageBitmap(controlcontacts.getRefelection(bm));
		} else {
			imageCall.setImageBitmap(controlcontacts
					.getRefelection(BitmapFactory.decodeResource(
							getResources(), R.drawable.image_call)));
		}
		Log.d(LOG_TAG, "Media Control Outgoing Created; uri = " + uri
				+ " id = " + id + "; Name = " + name);

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
				nValue.put("id", id);
				nValue.put("date", dateS);
				nValue.put("uri", uri.substring(4));
				nValue.put("name", name);
				nValue.put("type", false);
				db.insert("DBHistoryCall", null, nValue);

				ApplicationContext.contextTable.put("db", db);
			}
		}
		// // Play sound
		// mPlayer = MediaPlayer.create(this, R.raw.tone_call);

		final Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					controller.call(uri);
				} catch (Exception e) {
					TextView outgoing_call = (TextView) findViewById(R.id.outgoing_call);
					outgoing_call.setTextColor(Color.RED);
					outgoing_call.setText("Problem with call" + e.getMessage());
					outgoing_call.setTextSize(20);
					isRejectOrError = true;
					finishHandler((long) 2500);
				}
			}
		}).start();
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		ApplicationContext.contextTable.put("outgoingCall", getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	private void cancel() {
		Log.d(LOG_TAG, "Call Canceled...");
		Controller controller = (Controller) ApplicationContext.contextTable
				.get("controller");
		Log.d(LOG_TAG, "controller: " + controller);
		ApplicationContext.contextTable.remove("outgoingCall");
		try {
			controller.cancel();
			finishHandler((long) 2500);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		isCanceled = false;

		// mPlayer.setLooping(true);
		// mPlayer.start();

		final DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		final ImageButton buttonReject = (ImageButton) findViewById(R.id.button_call_reject);
		final LayoutParams buttonRejectParams = buttonReject.getLayoutParams();
		buttonReject.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE: {
					if (v.equals(buttonReject)) {
						int x = (int) event.getRawX() - 120;

						if ((x < dm.widthPixels / 2 - 20) && (x > -70)) {
							int a = x - buttonReject.getLeft();
							buttonReject.layout(buttonReject.getLeft() + a,
									buttonReject.getTop(),
									buttonReject.getRight() + a,
									buttonReject.getBottom());

						} else if ((x > dm.widthPixels / 2) && (!isCanceled)) {
							cancel();
							buttonReject.setVisibility(View.INVISIBLE);
							isCanceled = true;
						}
					}
				}
					break;
				case MotionEvent.ACTION_UP:
					buttonReject.setLayoutParams(buttonRejectParams);
					break;
				}
				return true;
			}
		});
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			cancel();
		}
		return true;
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mNotificationMgr.cancel(NOTIF_CALLING_OUT);
		mNotif = new Notification(R.drawable.icon, notificationTitleSoft,
				System.currentTimeMillis());

		notifIntent = new Intent(this, SoftPhone.class);
		mNotifContentIntent = PendingIntent
				.getActivity(this, 0, notifIntent, 0);
		mNotif.setLatestEventInfo(this, notificationTitleSoft, "",
				mNotifContentIntent);
		mNotificationMgr.notify(NOTIF_SOFTPHONE, mNotif);
		if (mPlayer != null)
			mPlayer.stop();
		Log.d(LOG_TAG, "onDestroy");
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}

	private void finishHandler(long time) {
		if (mPlayer != null)
			mPlayer.stop();
		mHandler.postDelayed(new Runnable() {
			public void run() {
				finish();
			}
		}, time);
	}

}
