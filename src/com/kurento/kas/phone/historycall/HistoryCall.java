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
package com.kurento.kas.phone.historycall;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
import com.kurento.kas.phone.softphone.R;

public class HistoryCall extends ListActivity {
	/** Called when the activity is first created. */
	private ListViewAdapter listViewAdapter;
	private static final String LOG_TAG = "HistoryCall";
	private static final String DB = "DBHistoryCall";
	private SQLiteDatabase db = null;

	private SQLiteDatabase openOrCreateBD() {

		db = openOrCreateDatabase(DB + ".db",
				SQLiteDatabase.CREATE_IF_NECESSARY, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);

		try {

			ListViewHistoryItem l = new ListViewHistoryItem();

			@SuppressWarnings("rawtypes")
			Class progClass = l.getClass();

			String sqlData = "";
			Field[] fields = progClass.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				String type = fields[i].getType().getSimpleName();
				String name = fields[i].getName();
				if ((i + 1) == fields.length) {
					sqlData += name + " " + type;
				} else {
					sqlData += name + " " + type + ", ";
				}
			}
			sqlData += ");";
			String sqlCreate = "CREATE TABLE " + DB
					+ " (idTable INTEGER PRIMARY KEY AUTOINCREMENT,";
			sqlCreate += sqlData;
			Log.d(LOG_TAG, "onCreate " + sqlCreate);
			db.execSQL(sqlCreate);
		} catch (Exception e) {
			Log.d(LOG_TAG, "Ya está creada ");
		}
		return db;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history_call);

		SQLiteDatabase db = openOrCreateBD();

		@SuppressWarnings("unchecked")
		ArrayList<ListViewHistoryItem> items = (ArrayList<ListViewHistoryItem>) ApplicationContext.contextTable
				.get("itemsHistory");

		listViewAdapter = new ListViewAdapter(this,
				R.layout.listview_history_call,
				new ArrayList<ListViewHistoryItem>());
		items = null;
		if (items == null) {
			items = new ArrayList<ListViewHistoryItem>();

			if (db != null) {
				Cursor cur = db.query(DB, null, null, null, null, null, "idTable DESC");
				Log.d(LOG_TAG, "Cursos = " + cur.getCount());
				cur.moveToFirst();
				while (cur.isAfterLast() == false) {

					Boolean mType = false;
					mType = (cur.getString(cur
							.getColumnIndex("type")).equals("1")) ? true
							: false;
					ListViewHistoryItem item = new ListViewHistoryItem(
							cur.getInt(cur.getColumnIndex("id")),
							cur.getString(cur.getColumnIndex("uri")),
							cur.getString(cur.getColumnIndex("name")),
							mType,
							cur.getString(cur.getColumnIndex("date")));
					items.add(item);
					
					listViewAdapter.add(item);
					cur.moveToNext();
				}
			}
		} 
		setListAdapter(listViewAdapter);

		ApplicationContext.contextTable.put("itemsHistory", items);
		ApplicationContext.contextTable.put("listViewAdapter", listViewAdapter);
		ApplicationContext.contextTable.put("db", db);

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

		final Button buttonCall = (Button) findViewById(R.id.buttonCallHistory);
		buttonCall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!textRemoteUri.getText().toString().equals("")) {
					String remoteURI = textRemoteUri.getText().toString();
					Intent resultIntent = new Intent();
					resultIntent.putExtra("contact", remoteURI);
					resultIntent.putExtra("type", "new");
					setResult(RESULT_OK, resultIntent);
					finish();
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
				db.delete(DB, null, null);
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

			holder.uri.setText(listViewItem.getUri());
			holder.date.setText("   " + listViewItem.getDate());
			Log.d(LOG_TAG, "Type = " + listViewItem.getType());
			holder.icon.setImageBitmap(listViewItem.getType() ? mIconIn
					: mIconOut);
			return v;
		}
	}
}