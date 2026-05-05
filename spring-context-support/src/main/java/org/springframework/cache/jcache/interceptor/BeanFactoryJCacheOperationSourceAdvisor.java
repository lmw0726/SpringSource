/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cache.jcache.interceptor;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.lang.Nullable;

/**
 * 由 {@link JCacheOperationSource} 驱动的 Advisor，
 * 用于为可缓存方法包含一个缓存通知 Bean。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
public class BeanFactoryJCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	@Nullable
	private JCacheOperationSource cacheOperationSource;

	private final JCacheOperationSourcePointcut pointcut = new JCacheOperationSourcePointcut() {
		@Override
		protected JCacheOperationSource getCacheOperationSource() {
			return cacheOperationSource;
		}
	};


	/**
	 * 设置用于查找缓存属性的缓存操作属性源。
	 * 通常它应与缓存拦截器本身上设置的源引用相同。
	 */
	public void setCacheOperationSource(JCacheOperationSource cacheOperationSource) {
		this.cacheOperationSource = cacheOperationSource;
	}

	/**
	 * 设置此切点要使用的 {@link org.springframework.aop.ClassFilter}。
	 * 默认值为 {@link org.springframework.aop.ClassFilter#TRUE}。
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
