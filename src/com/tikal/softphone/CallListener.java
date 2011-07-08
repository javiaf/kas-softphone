package com.tikal.softphone;

/**
 * 
 * @author Miguel París Díaz
 *
 */
public interface CallListener {
	
	public void incomingCall(String uri);
	public void registerUserSucessful();
	public void registerUserFailed();
	
	public void callSetup();
	public void callTerminate();
	public void callReject();
}
