package com.kurento.kas.phone.softphone;

import com.kurento.commons.mscontrol.networkconnection.NetworkConnection;
import com.kurento.commons.mscontrol.join.Joinable.Direction;
/**
 * 
 * @author Miguel París Díaz
 * 
 */
public interface CallListener {

	public void incomingCall(String uri);

	public void registerUserSucessful();

	public void registerUserFailed();

	public void callSetup(NetworkConnection networkConnection, Direction direction);

	public void callTerminate();

	public void callReject();

	public void callCancel();
}
