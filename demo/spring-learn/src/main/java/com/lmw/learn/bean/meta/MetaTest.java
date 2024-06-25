package com.lmw.learn.bean.meta;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;

import java.nio.charset.StandardCharsets;

/**
 * Spring元数据测试
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 12:47
 */
public class MetaTest {
	public static void main(String[] args) {
		//获取资源
		ClassPathResource resource = new ClassPathResource("meta/meta.xml");
		//获取BeanFactory
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		//根据BeanFactory创建BeanDefinitionReader对象，该Reader对象为资源的解析器
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		//将资源转为UTF-8格式，消除中文乱码问题
		EncodedResource encodedResource = new EncodedResource(resource, StandardCharsets.UTF_8);
		//装载资源
		reader.loadBeanDefinitions(encodedResource);
		//获取bean定义
		BeanDefinition beanDefinition = factory.getBeanDefinition("person");
		//获取元数据的值
		Object value = beanDefinition.getAttribute("special_key");
		System.out.println("元数据为：" + value);
	}
}
