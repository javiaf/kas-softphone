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
package com.kurento.kas.phone.controlcontacts;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.util.Log;

public class ControlContacts {
	private static final String LOG_TAG = "ControlContacts";

	private Context c;

	public ControlContacts(Context c) {
		super();
		this.c = c;
	}

	/**
	 * 
	 * @param id
	 *            Contact Id
	 * @return Contact Name
	 */
	public String getName(Integer id) {

		Log.d(LOG_TAG, "getName Id:" + id);
		int contact_id = -1;
		String name = "";
		contact_id = id;

		Cursor pidcursor = c.getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI,
				new String[] { ContactsContract.Contacts.DISPLAY_NAME },
				ContactsContract.Contacts._ID + "=" + contact_id, null, null);
		if (pidcursor != null) {
			if (pidcursor.moveToFirst()) {
				name = pidcursor.getString(0);
			}
		}
		Log.d(LOG_TAG, "Name: " + name);
		return name;

	}

	/**
	 * 
	 * @param data
	 *            Intent with all data
	 * @return Contact Name
	 */
	public String getName(Intent data) {
		Cursor cursor = c.getContentResolver().query(data.getData(), null,
				null, null, null);

		String name = null;

		if (cursor.moveToFirst()) {
			int nameIdx = cursor
					.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);

			name = cursor.getString(nameIdx);
		}
		Log.d(LOG_TAG, "Name: " + name);
		if (name != null)
			return name;
		else
			return null;
	}

	/**
	 * 
	 * @param sipUri
	 *            sipUri contact
	 * @return Contact Id
	 */
	public Integer getId(String sipUri) {

		Integer idContact = -1;
		String sipUriContact = "";

		String whereIm = ContactsContract.Data.MIMETYPE + " = ?";
		String[] whereParametersIm = new String[] { ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE };

		Cursor contactsIm = c.getContentResolver().query(
				ContactsContract.Data.CONTENT_URI, null, whereIm,
				whereParametersIm, null);

		while (contactsIm.moveToNext()) {

			sipUriContact = contactsIm.getString(contactsIm
					.getColumnIndexOrThrow(ContactsContract.Data.DATA1));
			if (sipUri.equals(sipUriContact)) {
				int idIdx = contactsIm
						.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
				idContact = contactsIm.getInt(idIdx);

				break;
			}
		}
		contactsIm.close();
		return idContact;
	}

	/**
	 * 
	 * @param data
	 *            Intent with all data
	 * @return Contact Id
	 */
	public Integer getId(Intent data) {
		Cursor cursor = c.getContentResolver().query(data.getData(), null,
				null, null, null);

		Integer id = -1;
		if (cursor.moveToFirst()) {
			int idIdx = cursor
					.getColumnIndexOrThrow(ContactsContract.Contacts._ID);

			id = cursor.getInt(idIdx);
		}
		Log.d(LOG_TAG, "ID: " + id);
		if (id != null)
			return id;
		else
			return -1;
	}

	/**
	 * 
	 * @param data
	 *            Intent with all data
	 * @return Contact SipUri
	 */
	public String getSip(Intent data) {

		Cursor cursor = c.getContentResolver().query(data.getData(), null,
				null, null, null);

		String id = null;
		String sip = null;

		if (cursor.moveToFirst()) {
			int idIdx = cursor
					.getColumnIndexOrThrow(ContactsContract.Contacts._ID);

			id = cursor.getString(idIdx);

			String whereIm = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND "
					+ ContactsContract.Data.MIMETYPE + " = ?";

			String[] whereParametersIm = new String[] { id,
					ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE };

			Cursor contactsIm = c.getContentResolver().query(
					ContactsContract.Data.CONTENT_URI, null, whereIm,
					whereParametersIm, null);

			if (contactsIm.moveToFirst()) {
				sip = contactsIm.getString(contactsIm
						.getColumnIndexOrThrow(ContactsContract.Data.DATA1));
			}
			contactsIm.close();
		}
		if (sip != null) {
			return sip;
		} else
			return null;
	}

	/**
	 * 
	 * @param id
	 *            Contact Id
	 * @return Contact Photo
	 */
	public Bitmap getPhoto(Integer id) {

		Log.d(LOG_TAG, "getPhoto Id:" + id);
		byte[] photo = null;
		Bitmap bm = null;
		int contact_id = -1;
		int photo_id = -1;
		// Cursor cursor =
		// getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
		// new String[] { ContactsContract.CommonDataKinds.Phone.CONTACT_ID },
		// ContactsContract.Data.DATA1 + "=" + "'desi@urjc.es'", null, null);
		// if (cursor != null) {
		// if (cursor.moveToFirst()) {
		// contact_id = cursor.getInt(0);
		//
		// }
		// }
		// Cursor cursor =
		// getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
		// new String[] { ContactsContract.CommonDataKinds.Phone.CONTACT_ID },
		// ContactsContract.CommonDataKinds.Phone.NUMBER + "=" + phone, null,
		// null);
		// if (cursor != null) {
		// if (cursor.moveToFirst()) {
		// contact_id = cursor.getInt(0);
		//
		// }
		// }
		//
		if (id != -1){
			contact_id = id;

			Cursor pidcursor = c.getContentResolver().query(
					ContactsContract.Contacts.CONTENT_URI,
					new String[] { ContactsContract.Contacts.PHOTO_ID },
					ContactsContract.Contacts._ID + "=" + contact_id, null,
					null);
			if (pidcursor != null) {
				if (pidcursor.moveToFirst()) {
					photo_id = pidcursor.getInt(0);
				}
			}

			Cursor pcursor = c.getContentResolver().query(
					ContactsContract.Data.CONTENT_URI,
					new String[] { ContactsContract.Data.DATA15 },
					ContactsContract.Data._ID + "=" + photo_id, null, null);
			if (pcursor.moveToFirst()) {
				photo = pcursor.getBlob(0);
			}
			if (photo != null) {
				bm = BitmapFactory.decodeByteArray(photo, 0, photo.length);
			}
		}
		else Log.d(LOG_TAG, "Id is null, not contatc");
		return bm;
	}

	public void setC(Context c) {
		this.c = c;
	}

	public Context getC() {
		return c;
	}

	public String General(Intent data) {

		Cursor cursor = c.getContentResolver().query(data.getData(), null,
				null, null, null);

		String id = null;
		String name = null;
		String number = null;
		String note = null;
		String sip = null;
		String imType = "";
		if (cursor.moveToFirst()) {
			int nameIdx = cursor
					.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
			int idIdx = cursor
					.getColumnIndexOrThrow(ContactsContract.Contacts._ID);

			id = cursor.getString(idIdx);
			name = cursor.getString(nameIdx);

			Log.e(LOG_TAG, "Value de nameIdx:" + nameIdx + " ID User: " + id);

			// Leer las notas del usuario si tine

			String[] columns = new String[] { ContactsContract.CommonDataKinds.Note.NOTE };
			String where = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND "
					+ ContactsContract.Data.MIMETYPE + " = ?";
			String[] whereParameters = new String[] { id,
					ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE };

			Cursor contacts = c.getContentResolver().query(
					ContactsContract.Data.CONTENT_URI, null, where,
					whereParameters, null);

			Log.e(LOG_TAG, "Valor de contacts:" + contacts.getCount());
			;
			String rv = null;
			if (contacts.moveToFirst()) {
				Log.e(LOG_TAG,
						"Valor de las colunas:" + contacts.getColumnCount());
				for (int i = 0; i < contacts.getColumnCount(); i++)
					Log.e(LOG_TAG, "Name:" + contacts.getColumnName(i)
							+ "; ValueIm = " + contacts.getString(i));

				note = contacts.getString(contacts
						.getColumnIndexOrThrow(ContactsContract.Data.DATA1));
			}
			contacts.close();

			// Leer las IM del usuario si tine

			String[] columnsIm = new String[] { ContactsContract.CommonDataKinds.Im.PROTOCOL };

			String whereIm = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND "
					+ ContactsContract.Data.MIMETYPE + " = ?";
			String[] whereParametersIm = new String[] { id,
					ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE };

			Cursor contactsIm = c.getContentResolver().query(
					ContactsContract.Data.CONTENT_URI, null, whereIm,
					whereParametersIm, null);
			Log.e(LOG_TAG, "Valor de contacts:" + contactsIm.getCount());
			;
			String rvIm = null;
			if (contactsIm.moveToFirst()) {
				Log.e(LOG_TAG,
						"Valor de las colunas:" + contactsIm.getColumnCount());
				for (int i = 0; i < contactsIm.getColumnCount(); i++)
					Log.e(LOG_TAG, "NameIm:" + contactsIm.getColumnName(i)
							+ "; ValueIm = " + contactsIm.getString(i));
				rvIm = " Protocolo: "
						+ contactsIm
								.getString(contactsIm
										.getColumnIndexOrThrow(ContactsContract.Data.DATA5))
						+ " Custom: "
						+ contactsIm
								.getString(contactsIm
										.getColumnIndexOrThrow(ContactsContract.Data.DATA6))
						+ " Data: "
						+ contactsIm
								.getString(contactsIm
										.getColumnIndexOrThrow(ContactsContract.Data.DATA1));

				sip = contactsIm.getString(contactsIm
						.getColumnIndexOrThrow(ContactsContract.Data.DATA1));
			}
			contactsIm.close();

			Log.e(LOG_TAG, "Valor de nameIdx:" + nameIdx + " ID: " + id
					+ " IM: " + rvIm);

		}
		if (sip != null) {
			return sip;
		} else
			return null;
	}
}
