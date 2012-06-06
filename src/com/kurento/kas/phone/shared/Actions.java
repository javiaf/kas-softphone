package com.kurento.kas.phone.shared;

import com.kurento.commons.ua.event.CallEvent;
import com.kurento.commons.ua.event.EndPointEvent;

public class Actions {
	public final static String REGISTER_USER_SUCESSFUL = EndPointEvent.REGISTER_USER_SUCESSFUL
			.toString();
	public final static String REGISTER_USER_FAIL = EndPointEvent.REGISTER_USER_FAIL
			.toString();
	public final static String REGISTER_USER_NOT_FOUND = EndPointEvent.REGISTER_USER_NOT_FOUND
			.toString();
	public final static String UNREGISTER_USER_SUCESSFUL = EndPointEvent.UNREGISTER_USER_SUCESSFUL
			.toString();
	public final static String UNREGISTER_USER_FAIL = EndPointEvent.UNREGISTER_USER_FAIL
			.toString();

	public final static String INCOMING_CALL = EndPointEvent.INCOMING_CALL
			.toString();
	public final static String OUTGOING_CALL = "OUTGOING_CALL";
	public final static String OUTGOING_CALL_CLOSE = "OUTGOING_CALL_CLOSE";
	public final static String CURRENT_CALL_OK = "CURRENT_CALL_OK";

	public final static String VIDEOCALL_CLOSE = "VIDEOCALL_CLOSE";

	public final static String CALL_SETUP = CallEvent.CALL_SETUP.toString();
	public final static String CALL_TERMINATE = CallEvent.CALL_TERMINATE
			.toString();
	public final static String CALL_REJECT = CallEvent.CALL_REJECT.toString();
	public final static String CALL_BUSY = CallEvent.CALL_BUSY.toString();
	public final static String CALL_ERROR = CallEvent.CALL_ERROR.toString();
	public final static String CALL_CANCEL = CallEvent.CALL_CANCEL.toString();
	public final static String CALL_RINGING = CallEvent.CALL_RINGING.toString();
	public final static String MEDIA_NOT_SUPPORTED = CallEvent.MEDIA_NOT_SUPPORTED
			.toString();
	public final static String USER_NOT_FOUND = CallEvent.USER_NOT_FOUND
			.toString();

}
