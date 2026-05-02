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

package org.springframework.aop.framework.autoproxy.target;

import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;

/**
 * 为每个定义为 "lazy-init" 的 bean 强制使用 {@link LazyInitTargetSource}
 * 的 {@code TargetSourceCreator}。这将导致为这些 bean 中的每一个创建代理，
 * 允许获取对此类 bean 的引用而无需实际初始化目标 bean 实例。
 *
 * <p>要注册为自动代理创建器的自定义 {@code TargetSourceCreator}，
 * 与特定 bean 的自定义拦截器结合使用，或者仅用于创建延迟初始化代理。
 * 例如，作为 XML 应用程序上下文定义中的自动检测的基础设施 bean：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator"&gt;
 *   &lt;property name="beanNames" value="*" /&gt; &lt;!-- apply to all beans --&gt;
 *   &lt;property name="customTargetSourceCreators"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator" /&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myLazyInitBean" class="mypackage.MyBeanClass" lazy-init="true"&gt;
 *   &lt;!-- ... --&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2
 * @see org.springframework.beans.factory.config.BeanDefinition#isLazyInit
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator
 */
public class LazyInitTargetSourceCreator extends AbstractBeanFactoryBasedTargetSourceCreator {

	@Override
	protected boolean isPrototypeBased() {
		return false;
	}

	@Override
	@Nullable
	protected AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName) {

		if (getBeanFactory() instanceof ConfigurableListableBeanFactory) {
			BeanDefinition definition =
					((ConfigurableListableBeanFactory) getBeanFactory()).getBeanDefinition(beanName);
			if (definition.isLazyInit()) {
				return new LazyInitTargetSource();
			}
		}
		return null;
	}

}
