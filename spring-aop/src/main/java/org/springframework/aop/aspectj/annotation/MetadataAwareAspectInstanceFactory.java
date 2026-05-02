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

package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.AspectInstanceFactory;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory} 的子接口，
 * 返回与 AspectJ 注解类关联的 {@link AspectMetadata}。
 *
 * <p>理想情况下，AspectInstanceFactory 本身会包含此方法，但是因为
 * AspectMetadata 使用仅限 Java 5 的 {@link org.aspectj.lang.reflect.AjType}，
 * 我们需要分离出这个子接口。
 *
 * @author Rod Johnson
 * @since 2.0
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjType
 */
public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

	/**
	 * 返回此工厂切面的 AspectJ AspectMetadata。
	 * @return 切面元数据
	 */
	AspectMetadata getAspectMetadata();

	/**
	 * 返回此工厂的最佳创建互斥锁。
	 * @return 互斥锁对象（如果不使用互斥锁，则可能为 {@code null}）
	 * @since 4.3
	 */
	@Nullable
	Object getAspectCreationMutex();

}
