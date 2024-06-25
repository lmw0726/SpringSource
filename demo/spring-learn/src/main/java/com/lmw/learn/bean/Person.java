package com.lmw.learn.bean;

/**
 * XML实体类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/05/18 22:54
 */
public class Person {
	private int age;
	private String name;

	/**
	 * 下属
	 */
	private Person subordinate;

	public int getAge() {
		return age;
	}

	public String getName() {
		return name;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Person getSubordinate() {
		return subordinate;
	}

	public void setSubordinate(Person subordinate) {
		this.subordinate = subordinate;
	}

	@Override
	public String toString() {
		return "Person{" +
				"age=" + age +
				", name='" + name + '\'' +
				", subordinate=" + subordinate +
				'}';
	}
}
