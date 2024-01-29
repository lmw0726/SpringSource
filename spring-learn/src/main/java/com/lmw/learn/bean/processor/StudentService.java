package com.lmw.learn.bean.processor;

/**
 * TODO
 *
 * @author LaiMingWei
 * @version 1.0.0
 * @date 2024-01-29 12:49
 */
public class StudentService {
	private String name;
	private int age;

	public String getName() {
		return name;
	}

	public StudentService setName(String name) {
		this.name = name;
		return this;
	}

	public int getAge() {
		return age;
	}

	public StudentService setAge(int age) {
		this.age = age;
		return this;
	}
}
