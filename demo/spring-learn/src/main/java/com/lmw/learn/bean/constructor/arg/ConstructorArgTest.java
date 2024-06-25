package com.lmw.learn.bean.constructor.arg;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 构造参数测试类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 14:18
 */
public class ConstructorArgTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:constructor.arg/ConstructorArg.xml");
		Train train = (Train) context.getBean("train");
		System.out.println(train);
	}
}
