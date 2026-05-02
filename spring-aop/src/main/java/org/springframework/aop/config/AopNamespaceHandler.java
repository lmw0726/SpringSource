/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code aop} 命名空间的 {@code NamespaceHandler}。
 *
 * <p>为 {@code <aop:config>} 标签提供
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}。
 * {@code config} 标签可以包含嵌套的 {@code pointcut}、{@code advisor}
 * 和 {@code aspect} 标签。
 *
 * <p>{@code pointcut} 标签允许使用简单语法创建命名的
 * {@link AspectJExpressionPointcut} bean：
 * <pre class="code">
 * &lt;aop:pointcut id=&quot;getNameCalls&quot; expression=&quot;execution(* *..ITestBean.getName(..))&quot;/&gt;
 * </pre>
 *
 * <p>使用 {@code advisor} 标签可以配置一个 {@link org.springframework.aop.Advisor}，
 * 并让它自动应用到 {@link org.springframework.beans.factory.BeanFactory}
 * 中所有相关的 bean。{@code advisor} 标签同时支持内联和引用的
 * {@link org.springframework.aop.Pointcut Pointcuts}：
 *
 * <pre class="code">
 * &lt;aop:advisor id=&quot;getAgeAdvisor&quot;
 *     pointcut=&quot;execution(* *..ITestBean.getAge(..))&quot;
 *     advice-ref=&quot;getAgeCounter&quot;/&gt;
 *
 * &lt;aop:advisor id=&quot;getNameAdvisor&quot;
 *     pointcut-ref=&quot;getNameCalls&quot;
 *     advice-ref=&quot;getNameCounter&quot;/&gt;</pre>
 *
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AopNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * 为 '{@code config}'、'{@code spring-configured}'、'{@code aspectj-autoproxy}'
	 * 和 '{@code scoped-proxy}' 标签注册
	 * {@link BeanDefinitionParser BeanDefinitionParsers}。
	 */
	@Override
	public void init() {
		// 在 2.0 XSD 以及 2.5+ XSD 中
		registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
		registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
		registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());

		// 仅在 2.0 XSD 中：在 2.5+ 中已移至 context 命名空间
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
	}

}
