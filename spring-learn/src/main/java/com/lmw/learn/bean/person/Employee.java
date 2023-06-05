package com.lmw.learn.bean.person;

import com.lmw.learn.bean.Person;

import java.math.BigDecimal;

/**
 * 雇员类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/06/05 22:22
 */
public class Employee extends Person {
	/**
	 * 技能名称
	 */
	private String skillName;
	/**
	 * 月薪
	 */
	private BigDecimal salary;

	public String getSkillName() {
		return skillName;
	}

	public void setSkillName(String skillName) {
		this.skillName = skillName;
	}

	public BigDecimal getSalary() {
		return salary;
	}

	public void setSalary(BigDecimal salary) {
		this.salary = salary;
	}

	@Override
	public String toString() {
		return "Employee{" + '\'' +
				"age=" + super.getAge() + '\'' +
				", name='" + super.getName() + '\'' +
				", skillName='" + skillName + '\'' +
				", salary=" + salary +
				'}';
	}
}
