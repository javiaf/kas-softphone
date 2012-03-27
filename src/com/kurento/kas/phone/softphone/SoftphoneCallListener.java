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
package com.kurento.kas.phone.softphone;

/**
 * 
 * @author Miguel París Díaz
 * 
 */
public interface SoftphoneCallListener {

	public void incomingCall(String uri);

	public void registerUserSucessful();

	public void registerUserFailed();

	public void callSetup();

	public void callTerminate();

	public void callReject();

	public void callCancel();
}
