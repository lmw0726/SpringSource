/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.wiring;

import org.springframework.lang.Nullable;

/**
 * 策略接口，由能够根据新实例化的 bean 对象解析 bean 名称信息的对象实现。
 * 对此接口的 {@link #resolveWiringInfo} 方法的调用将由相关具体切面中的
 * AspectJ 切点驱动。
 *
 * <p>元数据解析策略可以是可插拔的。一个不错的默认实现是
 * {@link ClassNameBeanWiringInfoResolver}，它使用完全限定类名作为 bean 名称。
 *
 * @author Rod Johnson
 * @since 2.0
 * @see BeanWiringInfo
 * @see ClassNameBeanWiringInfoResolver
 * @see org.springframework.beans.factory.annotation.AnnotationBeanWiringInfoResolver
 */
public interface BeanWiringInfoResolver {

	/**
	 * 为给定的 bean 实例解析 BeanWiringInfo。
	 * @param beanInstance 要解析信息的 bean 实例
	 * @return BeanWiringInfo，如果未找到则返回 {@code null}
	 */
	@Nullable
	BeanWiringInfo resolveWiringInfo(Object beanInstance);

}
