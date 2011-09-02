package com.kurento.kas.phone.softphone;

import java.util.Calendar;

public class ListViewHistoryItem {
	
	private Integer id;
	private String uri;
	private String name;
	private Boolean type; //True: in; False: out
	private Calendar date;


	public ListViewHistoryItem(Integer id, String uri, String name, Boolean type, Calendar date) {
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

	public void setDate(Calendar date) {
		this.date = date;
	}

	public Calendar getDate() {
		return date;
	}
}
