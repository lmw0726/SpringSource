package com.lmw.learn.bean.properties;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 构造参数测试类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 14:18
 */
public class PropertyTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:properties/Properties.xml");
		Flight train = (Flight) context.getBean("flight");
		System.out.println(train);
	}
}
