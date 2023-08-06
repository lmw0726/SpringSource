package com.lmw.learn.bean.custom;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * 用户命名空间处理器
 *
 * @author LMW
 * @version 1.0
 * @date 2023/8/6 21:56
 */
public class UserNamespaceHandler extends NamespaceHandlerSupport {
	@Override
	public void init() {
		registerBeanDefinitionParser("user", new UserDefinitionParser());
//		registerBeanDefinitionParser("user", new UserBeanDefinitionParser(User.class));
	}
}
