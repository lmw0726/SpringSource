package com.lmw.learn.bean;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;

import java.nio.charset.StandardCharsets;

/**
 * XML Bean测试类
 *
 * @author LMW
 * @version 1.0
 * @date 2023/05/18 22:56
 */
public class XmlBeanTest {
	public static void main(String[] args) {
		//获取资源
		ClassPathResource resource = new ClassPathResource("bean.xml");

		//获取BeanFactory
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		//根据BeanFactory创建BeanDefinitionReader对象，该Reader对象为资源的解析器
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		//将资源转为UTF-8格式，消除中文乱码问题
		EncodedResource encodedResource = new EncodedResource(resource, StandardCharsets.UTF_8);
		//装载资源
		reader.loadBeanDefinitions(encodedResource);
		//获取Bean对象
		Person person = factory.getBean("person", Person.class);
		System.out.println(person);
	}
}
