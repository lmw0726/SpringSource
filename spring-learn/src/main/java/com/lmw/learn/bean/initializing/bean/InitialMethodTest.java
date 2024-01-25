package com.lmw.learn.bean.initializing.bean;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * Init-method参数测试
 *
 * @author LMW
 * @version 1.0
 * @date 2024-01-25 21:51
 */
public class InitialMethodTest {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOtherName() {
		System.out.println("init-method 执行中...");
		this.name = "init 3";
	}

	public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("initializing/bean/initMethod.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);

		InitialMethodTest initializingBeanTest = (InitialMethodTest) factory.getBean("initialMethodTest");
		System.out.println("name：" + initializingBeanTest.getName());
	}
}
