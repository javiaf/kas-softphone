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
package com.kurento.kas.phone.historycall;


public class ListViewHistoryItem {
	
	private Integer id;
	private String uri;
	private String name;
	private Boolean type; //True: in; False: out
	private String date;

	public ListViewHistoryItem(){
		
	}
	public ListViewHistoryItem(Integer id, String uri, String name, Boolean type, String date) {
		super();
		this.setId(id);
		this.uri = uri;
		this.name = name;
		this.type = type;
		this.setDate(date);
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setType(Boolean type) {
		this.type = type;
	}

	public Boolean getType() {
		return type;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDate() {
		return date;
	}
}
