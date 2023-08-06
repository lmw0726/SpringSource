package com.lmw.learn.bean.qualifier;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Qualifier标签测试
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 15:54
 */
public class QualifierTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("qualifier/Qualifier.xml");
		Student student = (Student) context.getBean("student");
		System.out.println(student);
	}
}
