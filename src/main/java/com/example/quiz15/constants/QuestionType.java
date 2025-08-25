package com.example.quiz15.constants;

public enum QuestionType {

	SINGLE("Single"),
	MULIT("Multi"),
	TEXT("Text");
	
	private String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	private QuestionType(String type) {
		this.type = type;
	}

}
