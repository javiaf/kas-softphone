package com.tikal.softphone;

/**
 * 
 * @author Miguel París Díaz
 *
 */
public interface IPhoneGUI {
	
	public void inviteReceived(String uri);
	public void registerSucessful();
	public void registerFailed();
}
