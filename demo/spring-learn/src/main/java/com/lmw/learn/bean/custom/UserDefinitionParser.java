package com.lmw.learn.bean.custom;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * 用户定义解析器
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 21:53
 */
public class UserDefinitionParser extends AbstractSingleBeanDefinitionParser {
	@Override
	protected Class<?> getBeanClass(Element element) {
		return User.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String id = element.getAttribute("id");
		String userName = element.getAttribute("userName");
		String email = element.getAttribute("email");

		if (StringUtils.hasText(id)) {
			builder.addPropertyValue("id", id);
		}

		if (StringUtils.hasText(userName)) {
			builder.addPropertyValue("userName", userName);
		}

		if (StringUtils.hasText(email)) {
			builder.addPropertyValue("email", email);
		}
	}

}
