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

package org.springframework.aop.framework.autoproxy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * 通过名称列表识别要代理 bean 的自动代理创建器。
 * 检查直接匹配、"xxx*" 和 "*xxx" 匹配。
 *
 * <p>有关配置细节，请参阅父类 AbstractAutoProxyCreator 的 javadoc。
 * 通常，你会通过 "interceptorNames" 属性指定要应用于所有已识别 bean 的
 * 拦截器名称列表。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 10.10.2003
 * @see #setBeanNames
 * @see #isMatch
 * @see #setInterceptorNames
 * @see AbstractAutoProxyCreator
 */
@SuppressWarnings("serial")
public class BeanNameAutoProxyCreator extends AbstractAutoProxyCreator {

	private static final String[] NO_ALIASES = new String[0];

	@Nullable
	private List<String> beanNames;


	/**
	 * 设置应自动使用代理包装的 bean 名称。
	 * 名称可以通过以 "*" 结尾指定要匹配的前缀，例如 "myBean,tx*"
	 * 将匹配名为 "myBean" 的 bean，以及所有名称以 "tx" 开头的 bean。
	 * <p><b>注意：</b>对于 FactoryBean，只会代理 FactoryBean 创建的对象。
	 * 自 Spring 2.0 起应用此默认行为。如果打算代理 FactoryBean 实例本身
	 * （这是少见用例，但属于 Spring 1.2 的默认行为），请指定包含 factory-bean
	 * 前缀 "&amp;" 的 FactoryBean bean 名称：例如 "&amp;myFactoryBean"。
	 * @see org.springframework.beans.factory.FactoryBean
	 * @see org.springframework.beans.factory.BeanFactory#FACTORY_BEAN_PREFIX
	 */
	public void setBeanNames(String... beanNames) {
		Assert.notEmpty(beanNames, "'beanNames' must not be empty");
		this.beanNames = new ArrayList<>(beanNames.length);
		for (String mappedName : beanNames) {
			this.beanNames.add(StringUtils.trimWhitespace(mappedName));
		}
	}


	/**
	 * 如果 bean 名称匹配已配置受支持名称列表中的某个名称，
	 * 则委托给 {@link AbstractAutoProxyCreator#getCustomTargetSource(Class, String)}，
	 * 否则返回 {@code null}。
	 * @since 5.3
	 * @see #setBeanNames(String...)
	 */
	@Override
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		return (isSupportedBeanName(beanClass, beanName) ?
				super.getCustomTargetSource(beanClass, beanName) : null);
	}

	/**
	 * 如果 bean 名称匹配已配置受支持名称列表中的某个名称，
	 * 则将其识别为要代理的 bean。
	 * @see #setBeanNames(String...)
	 */
	@Override
	@Nullable
	protected Object[] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {

		return (isSupportedBeanName(beanClass, beanName) ?
				PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS : DO_NOT_PROXY);
	}

	/**
	 * 确定给定 bean 类的 bean 名称是否匹配
	 * 已配置受支持名称列表中的某个名称。
	 * @param beanClass 要进行 advising 的 bean 类
	 * @param beanName bean 的名称
	 * @return 如果支持给定 bean 名称，则为 {@code true}
	 * @see #setBeanNames(String...)
	 */
	private boolean isSupportedBeanName(Class<?> beanClass, String beanName) {
		if (this.beanNames != null) {
			boolean isFactoryBean = FactoryBean.class.isAssignableFrom(beanClass);
			for (String mappedName : this.beanNames) {
				if (isFactoryBean) {
					if (!mappedName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
						continue;
					}
					mappedName = mappedName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
				}
				if (isMatch(beanName, mappedName)) {
					return true;
				}
			}

			BeanFactory beanFactory = getBeanFactory();
			String[] aliases = (beanFactory != null ? beanFactory.getAliases(beanName) : NO_ALIASES);
			for (String alias : aliases) {
				for (String mappedName : this.beanNames) {
					if (isMatch(alias, mappedName)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 确定给定 bean 名称是否匹配映射名称。
	 * <p>默认实现检查 "xxx*"、"*xxx" 和 "*xxx*" 匹配，
	 * 以及直接相等。可以在子类中重写。
	 * @param beanName 要检查的 bean 名称
	 * @param mappedName 已配置名称列表中的名称
	 * @return 名称是否匹配
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String beanName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, beanName);
	}

}
