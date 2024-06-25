package com.lmw.learn.bean.life.cycle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * Bean生命周期演示
 *
 * @author LMW
 * @version 1.0
 * @date 2024-01-25 23:11
 */
public class LifeCycleBean implements BeanNameAware, BeanFactoryAware, BeanClassLoaderAware, BeanPostProcessor,
		InitializingBean, DisposableBean {
	private String test;

	public LifeCycleBean() {
		System.out.println("构造函数调用...");
	}

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		System.out.println("属性注入....");
		this.test = test;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		System.out.println("BeanClassLoaderAware 被调用...");
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		System.out.println("BeanFactoryAware 被调用...");
	}

	@Override
	public void setBeanName(String name) {
		System.out.println("BeanNameAware 被调用...");
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("BeanPostProcessor postProcessBeforeInitialization 被调用...");
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("BeanPostProcessor postProcessAfterInitialization 被调用...");
		return bean;
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("DisposableBean destroy 被调动...");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("InitializingBean afterPropertiesSet 被调动...");
	}

	public void initMethod(){
		System.out.println("init-method 被调用...");
	}

	public void destroyMethdo(){
		System.out.println("destroy-method 被调用...");
	}

	public void display(){
		System.out.println("方法调用...");
	}

	public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("life/cycle/lifeCycleBean.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);
		// BeanFactory 容器一定要调用该方法进行 BeanPostProcessor 注册
		factory.addBeanPostProcessor(new LifeCycleBean());

		LifeCycleBean lifeCycleBean = (LifeCycleBean) factory.getBean("lifeCycle");
		lifeCycleBean.display();

		System.out.println("方法调用完成，容器开始关闭....");
		// 关闭容器
		factory.destroySingletons();
	}
}
