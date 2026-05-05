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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 单个{@code 条件}，必须{@linkplain #matches 匹配}才能注册组件。
 *
 * <p>条件会在bean定义即将注册之前立即检查，并且可以基于在该时刻能够确定的任何标准
 * 自由否决注册。
 *
 * <p>条件必须遵循与{@link BeanFactoryPostProcessor}相同的限制，
 * 并且注意永远不要与bean实例交互。对于与{@code @Configuration} bean交互的更细粒度的条件控制，
 * 请考虑实现{@link ConfigurationCondition}接口。
 *
 * @author Phillip Webb
 * @since 4.0
 * @see ConfigurationCondition
 * @see Conditional
 * @see ConditionContext
 */
@FunctionalInterface
public interface Condition {

	/**
	 * 确定条件是否匹配。
	 * @param context 条件上下文
	 * @param metadata 正在检查的{@link org.springframework.core.type.AnnotationMetadata 类}
	 * 或{@link org.springframework.core.type.MethodMetadata 方法}的元数据
	 * @return 如果条件匹配且组件可以注册则返回{@code true}，
	 * 如果要否决被注解组件的注册则返回{@code false}
	 */
	boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);

}
