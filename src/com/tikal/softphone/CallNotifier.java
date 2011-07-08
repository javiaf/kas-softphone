package com.tikal.softphone;


public interface CallNotifier {

	public void addListener(CallListener listener);
	public void removeListener(CallListener listener);
}
