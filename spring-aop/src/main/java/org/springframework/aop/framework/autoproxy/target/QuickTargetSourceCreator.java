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

package org.springframework.aop.framework.autoproxy.target;

import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.aop.target.CommonsPool2TargetSource;
import org.springframework.aop.target.PrototypeTargetSource;
import org.springframework.aop.target.ThreadLocalTargetSource;
import org.springframework.lang.Nullable;

/**
 * 便捷的 TargetSourceCreator，使用 bean 名称前缀创建三种
 * 众所周知的 TargetSource 类型之一：
 * <ul>
 * <li>: CommonsPool2TargetSource</li>
 * <li>% ThreadLocalTargetSource</li>
 * <li>! PrototypeTargetSource</li>
 * </ul>
 *
 * @author Rod Johnson
 * @author Stephane Nicoll
 * @see org.springframework.aop.target.CommonsPool2TargetSource
 * @see org.springframework.aop.target.ThreadLocalTargetSource
 * @see org.springframework.aop.target.PrototypeTargetSource
 */
public class QuickTargetSourceCreator extends AbstractBeanFactoryBasedTargetSourceCreator {

	/**
	 * CommonsPool2TargetSource 前缀。
	 */
	public static final String PREFIX_COMMONS_POOL = ":";

	/**
	 * ThreadLocalTargetSource 前缀。
	 */
	public static final String PREFIX_THREAD_LOCAL = "%";

	/**
	 * PrototypeTargetSource 前缀。
	 */
	public static final String PREFIX_PROTOTYPE = "!";

	@Override
	@Nullable
	protected final AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName) {

		if (beanName.startsWith(PREFIX_COMMONS_POOL)) {
			CommonsPool2TargetSource cpts = new CommonsPool2TargetSource();
			cpts.setMaxSize(25);
			return cpts;
		}
		else if (beanName.startsWith(PREFIX_THREAD_LOCAL)) {
			return new ThreadLocalTargetSource();
		}
		else if (beanName.startsWith(PREFIX_PROTOTYPE)) {
			return new PrototypeTargetSource();
		}
		else {
			// 没有匹配。不要创建自定义目标源。
			return null;
		}
	}

}
