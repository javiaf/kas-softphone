package com.kurento.kas.phone.softphone;

/**
 * 
 * @author Miguel París Díaz
 *
 */

import com.kurento.commons.mscontrol.join.Joinable.Direction;
public interface IPhone {

	public void aceptCall() throws Exception;
	public void reject() throws Exception;
	
	public void call(String remoteURI, Direction direction) throws Exception;
	public void hang();
	
	public void cancel();
}
