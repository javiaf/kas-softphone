package com.kurento.kas.phone.softphone;

/**
 * 
 * @author Miguel París Díaz
 *
 */

public interface IPhone {

	public void aceptCall() throws Exception;
	public void reject() throws Exception;
	
	public void call(String remoteURI) throws Exception;
	public void hang();
	
	public void cancel();
}
