package com.lmw.learn.bean.processor;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * BeanFactoryPostProcessor 测试
 *
 * @author LaiMingWei
 * @version 1.0.0
 * @date 2024-01-29 12:51
 */
public class BeanFactoryPostProcessorTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("processor/Processor2.xml");
		StudentService studentService = (StudentService) context.getBean("studentService");
		System.out.println("Student name=" + studentService.getName() + " age:" + studentService.getAge());
	}
}
