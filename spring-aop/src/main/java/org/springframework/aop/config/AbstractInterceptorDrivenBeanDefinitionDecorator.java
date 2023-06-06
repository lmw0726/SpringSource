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

package org.springframework.aop.config;

import java.util.List;

import org.w3c.dom.Node;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base implementation for
 * {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator BeanDefinitionDecorators}
 * wishing to add an {@link org.aopalliance.intercept.MethodInterceptor interceptor}
 * to the resulting bean.
 *
 * <p>This base class controls the creation of the {@link ProxyFactoryBean} bean definition
 * and wraps the original as an inner-bean definition for the {@code target} property
 * of {@link ProxyFactoryBean}.
 *
 * <p>Chaining is correctly handled, ensuring that only one {@link ProxyFactoryBean} definition
 * is created. If a previous {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator}
 * already created the {@link org.springframework.aop.framework.ProxyFactoryBean} then the
 * interceptor is simply added to the existing definition.
 *
 * <p>Subclasses have only to create the {@code BeanDefinition} to the interceptor that
 * they wish to add.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see org.aopalliance.intercept.MethodInterceptor
 * @since 2.0
 */
public abstract class AbstractInterceptorDrivenBeanDefinitionDecorator implements BeanDefinitionDecorator {

	@Override
	public final BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definitionHolder, ParserContext parserContext) {
		//获取bean定义注册
		BeanDefinitionRegistry registry = parserContext.getRegistry();

		//获取根bean名称-将是生成的代理工厂bean的名称
		//获取bean名称
		String existingBeanName = definitionHolder.getBeanName();
		//获取bean定义
		BeanDefinition targetDefinition = definitionHolder.getBeanDefinition();
		//原来的bean名称+.TARGET以及原来的bean定义构建成新的BeanDefinitionHolder实例
		BeanDefinitionHolder targetHolder = new BeanDefinitionHolder(targetDefinition, existingBeanName + ".TARGET");

		//委托给拦截器定义的子类
		BeanDefinition interceptorDefinition = createInterceptorDefinition(node);

		// 生成名称并注册拦截器
		String interceptorName = existingBeanName + '.' + getInterceptorNameSuffix(interceptorDefinition);
		//注册bean拦截器
		BeanDefinitionReaderUtils.registerBeanDefinition(
				new BeanDefinitionHolder(interceptorDefinition, interceptorName), registry);

		BeanDefinitionHolder result = definitionHolder;

		if (!isProxyFactoryBeanDefinition(targetDefinition)) {
			//如果目标bean定义不是代理工厂bean定义
			// 创建代理bean定义
			RootBeanDefinition proxyDefinition = new RootBeanDefinition();
			//创建代理工厂bean定义
			proxyDefinition.setBeanClass(ProxyFactoryBean.class);
			proxyDefinition.setScope(targetDefinition.getScope());
			proxyDefinition.setLazyInit(targetDefinition.isLazyInit());
			// 设置目标类
			proxyDefinition.setDecoratedDefinition(targetHolder);
			proxyDefinition.getPropertyValues().add("target", targetHolder);
			// 创建拦截器名称列表
			proxyDefinition.getPropertyValues().add("interceptorNames", new ManagedList<String>());
			// 从原始bean定义复制自动装配设置
			proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
			proxyDefinition.setPrimary(targetDefinition.isPrimary());
			if (targetDefinition instanceof AbstractBeanDefinition) {
				proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
			}
			// 将其包装在带有bean名称的BeanDefinitionHolder中
			result = new BeanDefinitionHolder(proxyDefinition, existingBeanName);
		}
		//添加拦截器名称到列表
		addInterceptorNameToList(interceptorName, result.getBeanDefinition());
		return result;
	}

	@SuppressWarnings("unchecked")
	private void addInterceptorNameToList(String interceptorName, BeanDefinition beanDefinition) {
		List<String> list = (List<String>) beanDefinition.getPropertyValues().get("interceptorNames");
		Assert.state(list != null, "Missing 'interceptorNames' property");
		list.add(interceptorName);
	}

	private boolean isProxyFactoryBeanDefinition(BeanDefinition existingDefinition) {
		return ProxyFactoryBean.class.getName().equals(existingDefinition.getBeanClassName());
	}

	protected String getInterceptorNameSuffix(BeanDefinition interceptorDefinition) {
		String beanClassName = interceptorDefinition.getBeanClassName();
		//如果拦截器定义的bean类名不为空，获取类的简称，并返回第一个字符小写的名称。否则返回空字符串
		return (StringUtils.hasLength(beanClassName) ?
				StringUtils.uncapitalize(ClassUtils.getShortName(beanClassName)) : "");
	}

	/**
	 * 子类应该实现此方法，以返回他们希望应用于正在装饰的bean的拦截器的 {@code BeanDefinition}。
	 */
	protected abstract BeanDefinition createInterceptorDefinition(Node node);

}
