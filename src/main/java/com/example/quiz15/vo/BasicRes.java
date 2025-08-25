package com.example.quiz15.vo;

public class BasicRes {
	private int code;
	private String message;

	public BasicRes(int code, String message) {
		super();
		this.code = code;
		this.message = message;
	}

	public BasicRes() {
		super();
		// TODO Auto-generated constructor stub
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
