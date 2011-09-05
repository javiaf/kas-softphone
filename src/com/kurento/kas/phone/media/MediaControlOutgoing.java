package com.kurento.kas.phone.media;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
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

public class MediaControlOutgoing extends Activity {
	private static final String LOG_TAG = "MediaControlOutgoing";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_call_outgoingcall);

		Bundle extras = getIntent().getExtras();
		String uri = (String) extras.getSerializable("Uri");
		Integer id = (Integer) extras.getSerializable("Id");
	
		TextView text = (TextView) findViewById(R.id.outgoing_sip);
		text.setText(uri);
		

		ImageView imageCall = (ImageView) findViewById(R.id.image_call);

		ControlContacts controlcontacts = new ControlContacts(this);
		
		String name = uri;
		if (id != -1) 
			name = controlcontacts.getName(id);
		
		Log.d(LOG_TAG, "Media Control Outgoing Created; uri = " + uri
				+ " id = " + id + "; Name = " + name);
	
		Bitmap bm = controlcontacts.getPhoto(id);
		
		

		if (bm != null) {

			imageCall.setImageBitmap(bm);
		}

		
		
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

		String dateS = date.get(Calendar.HOUR_OF_DAY)
				+ ":" + tMinute + " " + tDay + "/" + tMonth + "/"
				+ date.get(Calendar.YEAR);
		
		items.add(new ListViewHistoryItem(id, onlyUri[1], name, false, dateS));

		Log.d(LOG_TAG, "items size = " + items.size());
		ApplicationContext.contextTable.put("itemsHistory", items);

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
				// setResult(RESULT_OK);
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
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy");
	}

}
