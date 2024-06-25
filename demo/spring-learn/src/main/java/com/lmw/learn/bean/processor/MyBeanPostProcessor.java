package com.lmw.learn.bean.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * BeanPostProcessor测试
 *
 * @author LaiMingWei
 * @version 1.0.0
 * @date 2024-01-23 12:36
 */
public class MyBeanPostProcessor implements BeanPostProcessor {
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("Bean [ " + beanName + " ] 开始初始化");
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("Bean [ " + beanName + " ] 完成初始化");
		return bean;
	}

	public void display() {
		System.out.println("Hello BeanPostProcessor!");
	}
}
