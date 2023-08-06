package com.lmw.learn.bean.custom;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 自定义标签测试
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 22:06
 */
public class CustomTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("custom/User.xml");
		User user = (User) context.getBean("user");
		System.out.println(user);
	}
}
