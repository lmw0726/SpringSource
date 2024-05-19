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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.w3c.dom.Element;

import java.util.List;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} 解析 {@code interceptors} 元素，
 * 用于注册一组 {@link MappedInterceptor} 定义。
 *
 * @author Keith Donald
 * @since 3.0
 */
class InterceptorsBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext context) {
		// 将包含组件推送到上下文中，其中包含元素的标签名和来源
		context.pushContainingComponent(new CompositeComponentDefinition(element.getTagName(), context.extractSource(element)));

		// 初始化路径匹配器引用
		RuntimeBeanReference pathMatcherRef = null;
		if (element.hasAttribute("path-matcher")) {
			// 如果有 path-matcher 属性，则设置路径匹配器引用
			pathMatcherRef = new RuntimeBeanReference(element.getAttribute("path-matcher"));
		}

		// 获取拦截器元素列表
		List<Element> interceptors = DomUtils.getChildElementsByTagName(element, "bean", "ref", "interceptor");
		// 遍历拦截器列表
		for (Element interceptor : interceptors) {
			// 创建拦截器的根Bean定义
			RootBeanDefinition mappedInterceptorDef = new RootBeanDefinition(MappedInterceptor.class);
			// 设置属性源
			mappedInterceptorDef.setSource(context.extractSource(interceptor));
			// 设置角色为基础设施角色
			mappedInterceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			ManagedList<String> includePatterns = null;
			ManagedList<String> excludePatterns = null;
			Object interceptorBean;
			// 如果拦截器元素的本地名称是 "interceptor"
			if ("interceptor".equals(interceptor.getLocalName())) {
				// 获取包含模式和排除模式
				includePatterns = getIncludePatterns(interceptor, "mapping");
				excludePatterns = getIncludePatterns(interceptor, "exclude-mapping");
				// 获取 bean 元素或 bean 引用元素
				Element beanElem = DomUtils.getChildElementsByTagName(interceptor, "bean", "ref").get(0);
				// 解析拦截器 bean 元素
				interceptorBean = context.getDelegate().parsePropertySubElement(beanElem, null);
			} else {
				// 解析拦截器元素
				interceptorBean = context.getDelegate().parsePropertySubElement(interceptor, null);
			}
			// 设置拦截器的构造函数参数
			// 第一个构造参数为 包含模式
			mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, includePatterns);
			// 第二个构造参数为 排除模式
			mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(1, excludePatterns);
			// 第三个构造参数为 拦截器
			mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(2, interceptorBean);

			if (pathMatcherRef != null) {
				// 如果存在路径匹配器引用，则设置到拦截器定义中
				mappedInterceptorDef.getPropertyValues().add("pathMatcher", pathMatcherRef);
			}

			// 注册拦截器定义，并获取生成的 bean 名称
			String beanName = context.getReaderContext().registerWithGeneratedName(mappedInterceptorDef);
			// 注册组件
			context.registerComponent(new BeanComponentDefinition(mappedInterceptorDef, beanName));
		}

		// 弹出并注册包含组件
		context.popAndRegisterContainingComponent();
		// 返回 null
		return null;
	}

	private ManagedList<String> getIncludePatterns(Element interceptor, String elementName) {
		// 根据拦截器元素和元素名称获取 子元素列表
		List<Element> paths = DomUtils.getChildElementsByTagName(interceptor, elementName);
		// 创建一个 ManagedList 对象，用于保存路径模式
		ManagedList<String> patterns = new ManagedList<>(paths.size());
		// 遍历路径元素列表
		for (Element path : paths) {
			// 将路径元素的 "path" 属性添加到模式列表中
			patterns.add(path.getAttribute("path"));
		}
		// 返回路径模式列表
		return patterns;
	}

}
