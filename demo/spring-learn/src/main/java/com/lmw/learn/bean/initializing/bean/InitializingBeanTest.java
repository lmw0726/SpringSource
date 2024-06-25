package com.lmw.learn.bean.initializing.bean;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * 初始化bean测试
 *
 * @author LMW
 * @version 1.0
 * @date 2024-01-25 21:24
 */
public class InitializingBeanTest implements InitializingBean {
	private String name;

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("InitializingBeanTest 初始化中...");
		this.name = "init 2";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("initializing/bean/initializingBean.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);

		InitializingBeanTest initializingBeanTest = (InitializingBeanTest) factory.getBean("initializingBeanTest");
		System.out.println("name：" + initializingBeanTest.getName());
	}
}
