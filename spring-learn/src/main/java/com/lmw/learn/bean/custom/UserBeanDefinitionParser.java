package com.lmw.learn.bean.custom;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * 用户定义解析器，BeanDefinitionParser实现的自定义标签。
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 22:44
 */
public class UserBeanDefinitionParser implements BeanDefinitionParser {
	private Class<?> beanClass;

	public UserBeanDefinitionParser(Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setBeanClass(beanClass);
		beanDefinition.setLazyInit(false);
		element.getAttribute("id");
		String id = element.getAttribute("id");
		String userName = element.getAttribute("userName");
		String email = element.getAttribute("email");
		MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
		if (StringUtils.hasText(id)) {
			propertyValues.addPropertyValue("id", id);
		}

		if (StringUtils.hasText(userName)) {
			propertyValues.addPropertyValue("userName", userName);
		}

		if (StringUtils.hasText(email)) {
			propertyValues.addPropertyValue("email", email);
		}
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		registry.registerBeanDefinition("user", beanDefinition);
		return beanDefinition;
	}
}
