package com.lmw.learn.bean.aware;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Aware接口测试
 *
 * @author LMW
 * @version 1.0
 * @date 2024-01-22 21:51
 */
public class AwareTest {
public static void main(String[] args) {

//	ClassPathResource resource = new ClassPathResource("aware/aware.xml");
//	DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
//	XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
//	reader.loadBeanDefinitions(resource);
//
//	MyApplicationAware applicationAware = (MyApplicationAware) factory.getBean("myApplicationAware");
//	applicationAware.display();

	ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:aware/aware.xml");
	MyApplicationAware applicationAware = (MyApplicationAware) context.getBean("myApplicationAware");
	applicationAware.display();
}


}
