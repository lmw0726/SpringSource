package com.lmw.learn.bean.qualifier;

import com.lmw.learn.bean.Person;

/**
 * 学生类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 15:53
 */
public class Student extends Person {
	private int grade;

	private Person teacher;

	public int getGrade() {
		return grade;
	}

	public void setGrade(int grade) {
		this.grade = grade;
	}

	public Person getTeacher() {
		return teacher;
	}

	public void setTeacher(Person teacher) {
		this.teacher = teacher;
	}

	@Override
	public String toString() {
		return "Student{" +
				"grade=" + grade +
				", teacher=" + teacher +
				'}';
	}
}
