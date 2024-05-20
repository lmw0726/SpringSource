/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} 用于解析以下 MVC 命名空间元素：
 * <ul>
 * <li>{@code <view-controller>}
 * <li>{@code <redirect-view-controller>}
 * <li>{@code <status-controller>}
 * </ul>
 *
 * <p>所有元素都将注册一个 {@link org.springframework.web.servlet.mvc.ParameterizableViewController ParameterizableViewController}，
 * 并且所有控制器都使用 {@link org.springframework.web.servlet.handler.SimpleUrlHandlerMapping SimpleUrlHandlerMapping} 进行映射。
 *
 * @author Keith Donald
 * @author Christian Dupuis
 * @author Rossen Stoyanchev
 * @since 3.0
 */
class ViewControllerBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * 处理器映射Bean名称
	 */
	private static final String HANDLER_MAPPING_BEAN_NAME =
			"org.springframework.web.servlet.config.viewControllerHandlerMapping";

	@Override
	@SuppressWarnings("unchecked")
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		// 获取源
		Object source = parserContext.extractSource(element);

		// 注册 SimpleUrlHandlerMapping 用于视图控制器
		BeanDefinition hm = registerHandlerMapping(parserContext, source);

		// 确保 BeanNameUrlHandlerMapping 和默认的 HandlerAdapters 不被“关闭”
		MvcNamespaceUtils.registerDefaultComponents(parserContext, source);

		// 创建视图控制器 bean 定义
		RootBeanDefinition controller = new RootBeanDefinition(ParameterizableViewController.class);
		// 设置源
		controller.setSource(source);

		HttpStatus statusCode = null;
		if (element.hasAttribute("status-code")) {
			// 如果有 status-code 属性，则获取 状态码
			int statusValue = Integer.parseInt(element.getAttribute("status-code"));
			statusCode = HttpStatus.valueOf(statusValue);
		}

		String name = element.getLocalName();
		switch (name) {
			case "view-controller":
				// 如果是 view-controller 标签
				if (element.hasAttribute("view-name")) {
					// 如果有 view-name 属性，则获取并设置 视图名称
					controller.getPropertyValues().add("viewName", element.getAttribute("view-name"));
				}
				if (statusCode != null) {
					// 设置状态码
					controller.getPropertyValues().add("statusCode", statusCode);
				}
				break;
			case "redirect-view-controller":
				// 如果是 redirect-view-controller 标签，获取并设置 重定向视图
				controller.getPropertyValues().add("view", getRedirectView(element, statusCode, source));
				break;
			case "status-controller":
				// 如果是 status-controller 标签，获取并设置 状态码
				controller.getPropertyValues().add("statusCode", statusCode);
				// 设置仅有状态码
				controller.getPropertyValues().add("statusOnly", true);
				break;
			default:
				// 不应该发生...
				throw new IllegalStateException("Unexpected tag name: " + name);
		}
		// 获取URL映射
		Map<String, BeanDefinition> urlMap = (Map<String, BeanDefinition>) hm.getPropertyValues().get("urlMap");
		if (urlMap == null) {
			// 如果URL映射不存在，则创建空的URL映射，并添加到urlMap属性名中
			urlMap = new ManagedMap<>();
			hm.getPropertyValues().add("urlMap", urlMap);
		}
		// 添加视图控制器
		urlMap.put(element.getAttribute("path"), controller);

		return null;
	}

	private BeanDefinition registerHandlerMapping(ParserContext context, @Nullable Object source) {
		if (context.getRegistry().containsBeanDefinition(HANDLER_MAPPING_BEAN_NAME)) {
			// 如果 处理器映射Bean名称 被注册为Bean定义
			// 返回 处理器映射Bean名称 的 bean 定义
			return context.getRegistry().getBeanDefinition(HANDLER_MAPPING_BEAN_NAME);
		}

		// 创建一个SimpleUrlHandlerMapping 类型的 根Bean定义实例
		RootBeanDefinition beanDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		// 设置 bean 定义的角色为基础设施角色
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 将 处理器映射Bean名称 和 上面的根Bean定义 注册为Bean定义
		context.getRegistry().registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME, beanDef);
		// 将 处理器映射Bean名称 和 上面的根Bean定义 组成的Bean组件定义 注册为Bean组件
		context.registerComponent(new BeanComponentDefinition(beanDef, HANDLER_MAPPING_BEAN_NAME));

		// 设置 bean 定义的源
		beanDef.setSource(source);
		// 设置 bean 定义的 order 属性为 "1"
		beanDef.getPropertyValues().add("order", "1");
		// 设置 bean 定义的 pathMatcher 属性
		beanDef.getPropertyValues().add("pathMatcher", MvcNamespaceUtils.registerPathMatcher(null, context, source));
		// 设置 bean 定义的 urlPathHelper 属性
		beanDef.getPropertyValues().add("urlPathHelper", MvcNamespaceUtils.registerUrlPathHelper(null, context, source));
		// 注册 CORS 配置
		RuntimeBeanReference corsConfigurationsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
		// 设置 bean 定义的 corsConfigurations 属性
		beanDef.getPropertyValues().add("corsConfigurations", corsConfigurationsRef);

		// 返回 bean 定义
		return beanDef;
	}

	private RootBeanDefinition getRedirectView(Element element, @Nullable HttpStatus status, @Nullable Object source) {
		// 创建一个新的 RootBeanDefinition 对象，用于 RedirectView 类
		RootBeanDefinition redirectView = new RootBeanDefinition(RedirectView.class);
		// 设置 bean 定义的源
		redirectView.setSource(source);
		// 设置 bean定义 的第一个构造函数参数，即重定向的 URL
		redirectView.getConstructorArgumentValues().addIndexedArgumentValue(0, element.getAttribute("redirect-url"));

		if (status != null) {
			// 如果存在 Http状态码，则设置 bean定义 的 状态码
			redirectView.getPropertyValues().add("statusCode", status);
		}

		// 如果元素具有 "context-relative" 属性
		if (element.hasAttribute("context-relative")) {
			// 设置 bean定义 的 contextRelative 属性为元素的 "context-relative" 属性值
			redirectView.getPropertyValues().add("contextRelative", element.getAttribute("context-relative"));
		} else {
			// 否则，默认设置 bean定义 的 contextRelative 属性为 true
			redirectView.getPropertyValues().add("contextRelative", true);
		}

		// 如果元素具有 "keep-query-params" 属性
		if (element.hasAttribute("keep-query-params")) {
			// 设置 bean定义 的 propagateQueryParams 属性为元素的 "keep-query-params" 属性值
			redirectView.getPropertyValues().add("propagateQueryParams", element.getAttribute("keep-query-params"));
		}

		// 返回bean 定义
		return redirectView;
	}

}
