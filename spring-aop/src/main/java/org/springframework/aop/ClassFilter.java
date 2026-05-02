/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop;

/**
 * 将切点或引介的匹配限制到给定目标类集合的过滤器。
 *
 * <p>可作为 {@link Pointcut} 的一部分使用，也可用于
 * {@link IntroductionAdvisor} 的整体目标定位。
 *
 * <p>此接口的具体实现通常应提供适当的
 * {@link Object#equals(Object)} 和 {@link Object#hashCode()} 实现，
 * 以便允许过滤器用于缓存场景 &mdash; 例如 CGLIB 生成的代理。
 *
 * @author Rod Johnson
 * @see Pointcut
 * @see MethodMatcher
 */
@FunctionalInterface
public interface ClassFilter {

	/**
	 * 切点是否应应用于给定接口或目标类？
	 * @param clazz 候选目标类
	 * @return 通知是否应应用于给定目标类
	 */
	boolean matches(Class<?> clazz);


	/**
	 * 匹配所有类的规范 ClassFilter 实例。
	 */
	ClassFilter TRUE = TrueClassFilter.INSTANCE;

}
