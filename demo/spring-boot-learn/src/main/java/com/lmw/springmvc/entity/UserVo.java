package com.lmw.springmvc.entity;

import java.io.Serializable;

/**
 * 用户视图类
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-17 22:06
 */
public class UserVo implements Serializable {
	private static final long serialVersionUID = 4018065500473006708L;

	private String name;
	private Integer age;

	public String getName() {
		return name;
	}

	public Integer getAge() {
		return age;
	}

	public UserVo setName(String name) {
		this.name = name;
		return this;
	}

	public UserVo setAge(Integer age) {
		this.age = age;
		return this;
	}
}
