package com.lmw.learn.bean.processor;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * BeanPostProcessor测试
 *
 * @author LaiMingWei
 * @version 1.0.0
 * @date 2024-01-23 12:39
 */
public class BeanPostProcessorTest {
	public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("processor/Processor.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);

//		MyBeanPostProcessor myBeanPostProcessor = new MyBeanPostProcessor();
//		factory.addBeanPostProcessor(myBeanPostProcessor);

		MyBeanPostProcessor test = (MyBeanPostProcessor) factory.getBean("myBeanPostProcessor");
		test.display();
	}
}
