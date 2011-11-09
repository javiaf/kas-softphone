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
import android.os.Bundle;
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
import com.kurento.kas.phone.softphone.SoftPhone;

public class MediaControlOutgoing extends Activity {
	private static final String LOG_TAG = "MediaControlOutgoing";

	private NotificationManager mNotificationMgr;
	private final static int NOTIF_CALLING_OUT = 3;
	private final static int NOTIF_VIDEOCALL = 2;
	private final static int NOTIF_SOFTPHONE = 1;

	private Notification mNotif;
	private PendingIntent mNotifContentIntent;
	private Intent notifIntent;
	private String notificationTitle = "Calling ...";
	private String notificationTitleSoft = "KurentoPhone";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_call_outgoingcall);
	
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

		String uri = (String) extras.getSerializable("Uri");
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

			imageCall.setImageBitmap(bm);
		}
		Log.d(LOG_TAG, "Media Control Outgoing Created; uri = " + uri
				+ " id = " + id + "; Name = " + name);
		
		@SuppressWarnings("unchecked")
		ArrayList<ListViewHistoryItem> items = (ArrayList<ListViewHistoryItem>) ApplicationContext.contextTable
				.get("itemsHistory");

		String[] onlyUri = uri.split(":");
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
				nValue.put("id", id);
				nValue.put("date", dateS);
				nValue.put("uri", uri.substring(4));
				nValue.put("name", name);
				nValue.put("type", false);
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

		final Button buttonReject = (Button) findViewById(R.id.button_call_reject);
		buttonReject.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "Call Canceled...");
				Controller controller = (Controller) ApplicationContext.contextTable
						.get("controller");
				Log.d(LOG_TAG, "controller: " + controller);
				try {
					controller.cancel();
				} catch (Exception e) {
					e.printStackTrace();
				}
				finish();
			}
		});

	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "onStop");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "onPause");
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
		Log.d(LOG_TAG, "onDestroy");
		super.onDestroy();
	}

}
