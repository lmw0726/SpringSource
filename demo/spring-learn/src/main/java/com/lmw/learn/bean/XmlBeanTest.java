package com.lmw.learn.bean;

import com.lmw.learn.bean.person.Boss;
import com.lmw.learn.bean.person.Employee;
import org.springframework.beans.factory.config.BeanDefinition;
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
		Person person = factory.getBean("employee2", Employee.class);
		System.out.println(person);
		BeanDefinition beanDefinition = factory.getBeanDefinition("person");
		Object value = beanDefinition.getAttribute("special_key");
		System.out.println("元数据为："+value);

		Boss boss = (Boss)factory.getBean("boss");
		System.out.println(boss);
	}
}
