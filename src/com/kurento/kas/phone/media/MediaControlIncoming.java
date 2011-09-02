package com.kurento.kas.phone.media;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
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
import com.kurento.kas.phone.sip.Controller;
import com.kurento.kas.phone.softphone.ListViewHistoryItem;
import com.kurento.kas.phone.softphone.R;
import com.kurento.kas.phone.softphone.ServiceUpdateUIListener;
import com.kurento.kas.phone.softphone.SoftPhoneService;

public class MediaControlIncoming extends Activity implements
		ServiceUpdateUIListener {
	private static final String LOG_TAG = "MediaControlIncoming";

	private ControlContacts controlcontacts = new ControlContacts(this);
	Vibrator vibrator;
	Controller controller = (Controller) ApplicationContext.contextTable
			.get("controller");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_call_incomingcall);
		Log.d(LOG_TAG, "Media Control Incoming Created");

		SoftPhoneService.setUpdateListener(this);

		Bundle extras = getIntent().getExtras();

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
		items.add(new ListViewHistoryItem(idContact, sipUri, name, true, date));

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
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy");
		vibrator.cancel();
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
