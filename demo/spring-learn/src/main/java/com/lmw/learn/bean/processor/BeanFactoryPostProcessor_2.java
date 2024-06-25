package com.lmw.learn.bean.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

/**
 * BeanFactoryPostProcessor 测试
 *
 * @author LaiMingWei
 * @version 1.0.0
 * @date 2024-01-29 12:47
 */
public class BeanFactoryPostProcessor_2 implements BeanFactoryPostProcessor, Ordered {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("调用 BeanFactoryPostProcessor_2 ...");

		// 获取指定的 BeanDefinition
		BeanDefinition bd = beanFactory.getBeanDefinition("studentService");

		MutablePropertyValues pvs = bd.getPropertyValues();

		pvs.addPropertyValue("age", 18);
	}


	@Override
	public int getOrder() {
		return 2;
	}
}
