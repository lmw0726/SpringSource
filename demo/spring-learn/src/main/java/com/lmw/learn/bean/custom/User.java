package com.lmw.learn.bean.custom;

/**
 * 用户类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 21:49
 */
public class User {
	private String id;
	private String userName;
	private String email;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return "User{" +
				"id='" + id + '\'' +
				", userName='" + userName + '\'' +
				", email='" + email + '\'' +
				'}';
	}
}
