package com.kurento.kas.phone.exception;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.kurento.kas.phone.preferences.Connection_Preferences;
import com.kurento.kas.phone.preferences.Keys_Preferences;
import com.kurento.kas.phone.preferences.Video_Preferences;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class ErrorReporter implements Thread.UncaughtExceptionHandler {
	private String LOG = ErrorReporter.class.getName();
	String VersionName;
	String PackageName;
	String FilePath;
	String PhoneModel;
	String AndroidVersion;
	String Board;
	String Brand;
	// String CPU_ABI;
	String Device;
	String Display;
	String FingerPrint;
	String Host;
	String ID;
	String Manufacturer;
	String Model;
	String Product;
	String Tags;
	long Time;
	String Type;
	String User;
	String connectionPreferences, mediaPreferences;
	String usernameKphone;

	private Thread.UncaughtExceptionHandler PreviousHandler;
	private static ErrorReporter S_mInstance;
	private Context CurContext;

	public void Init(Context context) {
		Log.d(LOG, "init");
		PreviousHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(this);
		RecoltInformations(context);
		CurContext = context;
	}

	public long getAvailableInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize;
	}

	public long getTotalInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return totalBlocks * blockSize;
	}

	void RecoltInformations(Context context) {
		PackageManager pm = context.getPackageManager();
		Log.d(LOG, "Recolt Information");
		try {
			PackageInfo pi;
			// Version
			pi = pm.getPackageInfo(context.getPackageName(), 0);
			VersionName = pi.versionName;
			// Package name
			PackageName = pi.packageName;
			// Files dir for storing the stack traces
			FilePath = context.getFilesDir().getAbsolutePath();
			// Device model
			PhoneModel = android.os.Build.MODEL;
			// Android version
			AndroidVersion = android.os.Build.VERSION.RELEASE;

			Board = android.os.Build.BOARD;
			Brand = android.os.Build.BRAND;
			// CPU_ABI = android.os.Build.;
			Device = android.os.Build.DEVICE;
			Display = android.os.Build.DISPLAY;
			FingerPrint = android.os.Build.FINGERPRINT;
			Host = android.os.Build.HOST;
			ID = android.os.Build.ID;
			// Manufacturer = android.os.Build.;
			Model = android.os.Build.MODEL;
			Product = android.os.Build.PRODUCT;
			Tags = android.os.Build.TAGS;
			Time = android.os.Build.TIME;
			Type = android.os.Build.TYPE;
			User = android.os.Build.USER;

			Map<String, String> connectionParams;
			connectionParams = Connection_Preferences
					.getConnectionPreferences(context);
			if (connectionParams != null)
				usernameKphone = connectionParams
						.get(Keys_Preferences.SIP_LOCAL_USERNAME);

			connectionPreferences = Connection_Preferences
					.getConnectionPreferencesInfo(context)
					+ "\n\n"
					+ Connection_Preferences
							.getConnectionNetPreferenceInfo(context);

			mediaPreferences = Video_Preferences
					.getMediaPreferencesInfo(context);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			Log.d(LOG, "ErrorReporter: " + e.getMessage());
		}
	}

	public String CreateInformationString() {
		String ReturnVal = "";
		ReturnVal += "Version : " + VersionName;
		ReturnVal += "\n";
		ReturnVal += "Package : " + PackageName;
		ReturnVal += "\n";
		ReturnVal += "FilePath : " + FilePath;
		ReturnVal += "\n";
		ReturnVal += "Phone Model" + PhoneModel;
		ReturnVal += "\n";
		ReturnVal += "Android Version : " + AndroidVersion;
		ReturnVal += "\n";
		ReturnVal += "Board : " + Board;
		ReturnVal += "\n";
		ReturnVal += "Brand : " + Brand;
		ReturnVal += "\n";
		ReturnVal += "Device : " + Device;
		ReturnVal += "\n";
		ReturnVal += "Display : " + Display;
		ReturnVal += "\n";
		ReturnVal += "Finger Print : " + FingerPrint;
		ReturnVal += "\n";
		ReturnVal += "Host : " + Host;
		ReturnVal += "\n";
		ReturnVal += "ID : " + ID;
		ReturnVal += "\n";
		ReturnVal += "Model : " + Model;
		ReturnVal += "\n";
		ReturnVal += "Product : " + Product;
		ReturnVal += "\n";
		ReturnVal += "Tags : " + Tags;
		ReturnVal += "\n";
		ReturnVal += "Time : " + Time;
		ReturnVal += "\n";
		ReturnVal += "Type : " + Type;
		ReturnVal += "\n";
		ReturnVal += "User : " + User;
		ReturnVal += "\n";
		ReturnVal += "Total Internal memory : " + getTotalInternalMemorySize();
		ReturnVal += "\n";
		ReturnVal += "Available Internal memory : "
				+ getAvailableInternalMemorySize();
		ReturnVal += "\n";

		return ReturnVal;
	}

	private String CreatePreferencesString() {
		String ReturnVal = "";

		ReturnVal = connectionPreferences;
		ReturnVal += "\n====\n\n";
		ReturnVal += mediaPreferences;
		ReturnVal += "\n";

		return ReturnVal;
	}

	public void uncaughtException(Thread t, Throwable e) {
		// Cancel the notifications
		NotificationManager mNotificationMgr = (NotificationManager) CurContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// Cancel the notifications
		if (mNotificationMgr != null) {
			mNotificationMgr.cancel(4);
			mNotificationMgr.cancel(3);
			mNotificationMgr.cancel(2);
		}
		Log.d(LOG, "unCaughtException");
		String Report = "";
		Date CurDate = new Date();
		Report += "Error Report collected on : " + CurDate.toString();
		Report += "\n";
		Report += "\n";
		Report += "Informations :";
		Report += "\n";
		Report += "==============";
		Report += "\n";
		Report += "\n";
		Report += CreateInformationString();

		Report += "\n\n";
		Report += "K-Phone Configuration: \n";
		Report += "==============\n\n";
		Report += CreatePreferencesString();
		Report += "======= \n";
		Report += "\n\n";
		Report += "Stack : \n";
		Report += "======= \n";
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		e.printStackTrace(printWriter);
		String stacktrace = result.toString();
		Report += stacktrace;

		Report += "\n";
		Report += "Cause : \n";
		Report += "======= \n";

		// If the exception was thrown in a background thread inside
		// AsyncTask, then the actual exception can be found with getCause
		Throwable cause = e.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			Report += result.toString();
			cause = cause.getCause();
		}
		printWriter.close();
		Report += "****  End of current Report ***";
		SaveAsFile(Report);
		// SendErrorMail( Report );
		CheckErrorAndSendMail(CurContext);

		PreviousHandler.uncaughtException(t, e);

	}

	public static ErrorReporter getInstance() {
		if (S_mInstance == null)
			S_mInstance = new ErrorReporter();
		return S_mInstance;
	}

	private void UploadError(Context _context, String ErrorContent) {
		RequestParams params = new RequestParams();
		try {

			HttpEntity entityGroup = new StringEntity(usernameKphone, "UTF8");
			params.put("phoneid", entityGroup.getContent(), "phoneid",
					"text/plain");

			HttpEntity entityUser = new StringEntity(ErrorContent, "UTF8");
			params.put("data", entityUser.getContent(), "data", "text/plain");

			HttpEntity entity = params.getEntity();

			AsyncHttpClient httpClient = new AsyncHttpClient();

			String urlServer = "http://193.147.51.24:8080/log-upload/save";

			httpClient.post(_context, urlServer, entity, entity
					.getContentType().getValue(),
					new AsyncHttpResponseHandler() {
						@Override
						public void onSuccess(String content) {
							Log.d(LOG, "Upload Ok");
						}

						@Override
						public void onFailure(Throwable error, String content) {
							super.onFailure(error, content);

							Log.e(LOG, "Failure:" + content, error);
						}

					});
		} catch (Exception e) {
			Log.e(LOG, "UploadError. " + e.getMessage(), e);
		}
	}

	private void SendErrorMail(Context _context, String ErrorContent) {
		Log.d(LOG, "SendErrorMail");
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		String subject = "Subject:";
		String body = "\n\n" + ErrorContent + "\n\n";
		sendIntent.putExtra(Intent.EXTRA_EMAIL,
				new String[] { "raulbenitezmejias@gmail.com" });
		sendIntent.putExtra(Intent.EXTRA_TEXT, body);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		sendIntent.setType("message/rfc822");
		sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		_context.startActivity(sendIntent);
	}

	private void SaveAsFile(String ErrorContent) {
		try {
			Random generator = new Random();
			int random = generator.nextInt(99999);
			String FileName = "stack-" + random + ".stacktrace";
			FileOutputStream trace = CurContext.openFileOutput(FileName,
					Context.MODE_PRIVATE);
			trace.write(ErrorContent.getBytes());
			trace.close();
		} catch (IOException ioe) {
			// ...
		}
	}

	private String[] GetErrorFileList() {
		File dir = new File(FilePath + "/");
		// Try to create the files folder if it doesn't exist
		dir.mkdir();
		// Filter for ".stacktrace" files
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".stacktrace");
			}
		};
		return dir.list(filter);
	}

	private boolean bIsThereAnyErrorFile() {
		return GetErrorFileList().length > 0;
	}

	public void CheckErrorAndSendMail(Context _context) {
		Log.d(LOG, "CheckErrorAndSendMail");
		try {
			if (bIsThereAnyErrorFile()) {
				Log.d(LOG, "bIsThereAndErrorFile");
				String WholeErrorText = "";
				String[] ErrorFileList = GetErrorFileList();
				int curIndex = 0;
				// We limit the number of crash reports to send ( in order not
				// to be too slow )
				final int MaxSendMail = 5;
				for (String curString : ErrorFileList) {
					if (curIndex++ <= MaxSendMail) {
						WholeErrorText += "New Trace collected :\n";
						WholeErrorText += "=====================\n ";
						String filePath = FilePath + "/" + curString;
						BufferedReader input = new BufferedReader(
								new FileReader(filePath));
						String line;
						while ((line = input.readLine()) != null) {
							WholeErrorText += line + "\n";
						}
						input.close();
					}

					// DELETE FILES !!!!
					File curFile = new File(FilePath + "/" + curString);
					curFile.delete();
				}
				UploadError(_context, WholeErrorText);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}