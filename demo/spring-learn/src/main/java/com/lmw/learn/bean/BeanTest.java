package com.lmw.learn.bean;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Bean测试类
 *
 * @author MingWei
 * @version 1.0
 * @date 2022/8/17 23:22
 */
public class BeanTest {
	public static void main(String[] args) {
		// 获取注解配置应用上下文
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BeanConfig.class);
		// 获取BeanService对象
		BeanService bean = context.getBean(BeanService.class);
		System.out.println(bean.getClass().getName());
	}
}
