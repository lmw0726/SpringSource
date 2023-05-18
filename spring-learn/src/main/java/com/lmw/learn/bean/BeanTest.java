package com.lmw.learn.bean;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Bean测试类
 *
 * @author MingWei
 * @version 1.0
 * @date 2022/8/17 23:22
 */
public class BeanTest {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BeanConfig.class);
		BeanService bean = context.getBean(BeanService.class);
		System.out.println(bean.getClass().getName());
	}
}
