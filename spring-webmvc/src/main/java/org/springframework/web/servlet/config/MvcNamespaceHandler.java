/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config;

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * 用于 Spring MVC 配置命名空间的 {@link NamespaceHandler}。
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Sebastien Deleuze
 * @since 3.0
 */
public class MvcNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		// 注册 annotation-driven 的 Bean定义解析器
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenBeanDefinitionParser());
		// 注册 default-servlet-handler 的 Bean定义解析器
		registerBeanDefinitionParser("default-servlet-handler", new DefaultServletHandlerBeanDefinitionParser());
		// 注册 interceptors 的 Bean定义解析器
		registerBeanDefinitionParser("interceptors", new InterceptorsBeanDefinitionParser());
		// 注册 resources 的 Bean定义解析器
		registerBeanDefinitionParser("resources", new ResourcesBeanDefinitionParser());
		// 注册 view-controller 的 Bean定义解析器
		registerBeanDefinitionParser("view-controller", new ViewControllerBeanDefinitionParser());
		// 注册 redirect-view-controller 的 Bean定义解析器
		registerBeanDefinitionParser("redirect-view-controller", new ViewControllerBeanDefinitionParser());
		// 注册 status-controller 的 Bean定义解析器
		registerBeanDefinitionParser("status-controller", new ViewControllerBeanDefinitionParser());
		// 注册 view-resolvers 的 Bean定义解析器
		registerBeanDefinitionParser("view-resolvers", new ViewResolversBeanDefinitionParser());
		// 注册 tiles-configurer 的 Bean定义解析器
		registerBeanDefinitionParser("tiles-configurer", new TilesConfigurerBeanDefinitionParser());
		// 注册 freemarker-configurer 的 Bean定义解析器
		registerBeanDefinitionParser("freemarker-configurer", new FreeMarkerConfigurerBeanDefinitionParser());
		// 注册 groovy-configurer 的 Bean定义解析器
		registerBeanDefinitionParser("groovy-configurer", new GroovyMarkupConfigurerBeanDefinitionParser());
		// 注册 script-template-configurer 的 Bean定义解析器
		registerBeanDefinitionParser("script-template-configurer", new ScriptTemplateConfigurerBeanDefinitionParser());
		// 注册 cors 的 Bean定义解析器
		registerBeanDefinitionParser("cors", new CorsBeanDefinitionParser());
	}

}
