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

package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * 用于提供 AspectJ 切面实例的接口。
 * 与 Spring 的 bean 工厂解耦。
 *
 * <p>扩展 {@link org.springframework.core.Ordered} 接口，
 * 以表达链中底层切面的顺序值。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory#getBean
 */
public interface AspectInstanceFactory extends Ordered {

	/**
	 * 创建此工厂的切面实例。
	 * @return 切面实例（绝不为 {@code null}）
	 */
	Object getAspectInstance();

	/**
	 * 暴露此工厂使用的切面类加载器。
	 * @return 切面类加载器（引导类加载器则返回 {@code null}）
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	@Nullable
	ClassLoader getAspectClassLoader();

}
