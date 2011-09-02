package com.kurento.kas.phone.softphone;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.kurento.kas.phone.applicationcontext.ApplicationContext;

public class HistoryCall extends ListActivity {
	/** Called when the activity is first created. */
	private ListViewAdapter listViewAdapter;
	private static final String LOG_TAG = "HistoryCall";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history_call);

		@SuppressWarnings("unchecked")
		ArrayList<ListViewHistoryItem> items = (ArrayList<ListViewHistoryItem>) ApplicationContext.contextTable
				.get("itemsHistory");

		listViewAdapter = new ListViewAdapter(this,
				R.layout.listview_history_call,
				new ArrayList<ListViewHistoryItem>());

		if (items == null)
			items = new ArrayList<ListViewHistoryItem>();

		for (int i = 0; i < items.size(); i++) {
			listViewAdapter.add(items.get(i));
		}
		setListAdapter(listViewAdapter);

		ApplicationContext.contextTable.put("itemsHistory", items);
		ApplicationContext.contextTable.put("listViewAdapter", listViewAdapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		Log.e(LOG_TAG, "Has seleccionado el elemento " + position);
		Intent resultIntent = new Intent();

		resultIntent.putExtra("type", "history");
		resultIntent.putExtra("positionContact", position);
		setResult(RESULT_OK, resultIntent);
		finish();

	}

	@Override
	protected void onResume() {
		super.onResume();

		final EditText textRemoteUri = (EditText) findViewById(R.id.textRemoteUri);
		textRemoteUri.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					String remoteURI = textRemoteUri.getText().toString();
					Intent resultIntent = new Intent();
					resultIntent.putExtra("contact", remoteURI);
					resultIntent.putExtra("type", "new");
					setResult(RESULT_OK, resultIntent);
					finish();
					return true;
				}
				return false;
			}
		});

		final Button infoRemoteUri = (Button) findViewById(R.id.infoRemoteUri);
		infoRemoteUri.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					Dialog dialog = new Dialog(v.getContext());
					dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
					dialog.setContentView(R.layout.inforemoteuri);
					dialog.show();
				} catch (Exception e) {
					Log.e(LOG_TAG, "Exception " + e.toString());
				}
			}
		});

		final Button buttonContacts = (Button) findViewById(R.id.contactsHistory);
		buttonContacts.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(LOG_TAG, "openContacts");
				Intent resultIntent = new Intent();
				resultIntent.putExtra("type", "openContacts");
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		});

	}

	/* Menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.history_call_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case (R.id.clear_history):
			@SuppressWarnings("unchecked")
			ArrayList<ListViewHistoryItem> items = (ArrayList<ListViewHistoryItem>) ApplicationContext.contextTable
					.get("itemsHistory");
			if (items != null) {
				items.clear();
				ApplicationContext.contextTable.put("itemsHistory", items);
				listViewAdapter.clear();
				setListAdapter(listViewAdapter);
			}
		}
		return super.onOptionsItemSelected(item);
	}

	
	
	// --------
	public class ListViewAdapter extends ArrayAdapter<ListViewHistoryItem> {

		private ArrayList<ListViewHistoryItem> items;
		private ViewHolder holder;
		private Bitmap mIconIn;
		private Bitmap mIconOut;

		class ViewHolder {
			TextView uri;
			ImageView icon;
			TextView date;
		}

		public ListViewAdapter(Context context, int textViewResourceId,
				ArrayList<ListViewHistoryItem> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			mIconIn = BitmapFactory.decodeResource(this.getContext()
					.getResources(), R.drawable.sym_call_incoming);
			mIconOut = BitmapFactory.decodeResource(this.getContext()
					.getResources(), R.drawable.sym_call_outgoing);

			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.listview_history_call, null);

				holder = new ViewHolder();

				holder.uri = (TextView) v.findViewById(R.id.uri);
				holder.icon = (ImageView) v.findViewById(R.id.icon);
				holder.date = (TextView) v.findViewById(R.id.date);
				v.setTag(holder);
			} else {
				holder = (ViewHolder) v.getTag();
			}
			ListViewHistoryItem listViewItem = items.get(position);
		
			Integer minute = listViewItem.getDate().get(Calendar.MINUTE);
			Integer day = listViewItem.getDate().get(Calendar.DAY_OF_MONTH);
			Integer month = listViewItem.getDate().get(Calendar.MONTH) + 1;
			
			String tMinute = String.valueOf(minute);
			String tDay = String.valueOf(day);
			String tMonth = String.valueOf(month);
			
			if (minute < 10) tMinute = "0" + minute;
			if (day < 10) tDay = "0" + day;
			if (month < 10) tMonth = "0" + tMonth;
			
			
			String date = listViewItem.getDate().get(Calendar.HOUR_OF_DAY)
					+ ":" + tMinute + " "
					+ tDay + "/"
					+ tMonth + "/"
					+ listViewItem.getDate().get(Calendar.YEAR);
		
			
			holder.uri.setText(listViewItem.getUri());
			holder.date.setText("   " + date);
			holder.icon.setImageBitmap(listViewItem.getType() ? mIconIn
					: mIconOut);
			return v;
		}
	}
}