package com.lmw.springmvc.entity;

/**
 * 性别枚举
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-30 22:09
 */
public enum Gender {
	MALE("男"),
	FEMAILE("女");
	private String source;

	Gender(String source) {
		this.source = source;
	}

	public String getSource() {
		return source;
	}
}
