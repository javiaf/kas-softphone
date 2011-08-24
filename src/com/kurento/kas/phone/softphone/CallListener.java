package com.kurento.kas.phone.softphone;

import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;

/**
 * 
 * @author Miguel París Díaz
 * 
 */
public interface CallListener {

	public void incomingCall(String uri);

	public void registerUserSucessful();

	public void registerUserFailed();

	public void callSetup(NetworkConnection networkConnection);

	public void callTerminate();

	public void callReject();

	public void callCancel();
}
