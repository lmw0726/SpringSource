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

package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * {@code TargetSource} 用于获取 AOP 调用的当前"目标对象"，
 * 如果没有任何环绕通知选择自行终止拦截器链，则该目标对象将通过反射进行调用。
 *
 * <p>如果 {@code TargetSource} 是"静态"的，它将始终返回相同的目标对象，
 * 从而允许 AOP 框架进行优化。动态目标源则可以支持池化、热替换等功能。
 *
 * <p>应用程序开发者通常不需要直接使用 {@code TargetSource}：
 * 这是一个 AOP 框架接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface TargetSource extends TargetClassAware {

	/**
	 * 返回此 {@link TargetSource} 所返回的目标对象的类型。
	 * <p>可以返回 {@code null}，但在某些特定用法下，
	 * {@code TargetSource} 可以仅依赖预定义的目标类来正常工作。
	 * @return 此 {@link TargetSource} 所返回的目标对象的类型
	 */
	@Override
	@Nullable
	Class<?> getTargetClass();

	/**
	 * 所有对 {@link #getTarget()} 的调用是否都会返回相同的对象？
	 * <p>如果是，则不需要调用 {@link #releaseTarget(Object)}，
	 * 并且 AOP 框架可以缓存 {@link #getTarget()} 的返回值。
	 * @return 如果目标对象是不可变的，则返回 {@code true}
	 * @see #getTarget
	 */
	boolean isStatic();

	/**
	 * 返回目标实例。在 AOP 框架调用 AOP 方法调用的"目标"之前立即调用。
	 * @return 包含连接点的目标对象，如果没有实际的目标实例则返回 {@code null}
	 * @throws Exception 如果无法解析目标对象
	 */
	@Nullable
	Object getTarget() throws Exception;

	/**
	 * 释放通过 {@link #getTarget()} 方法获取的给定目标对象（如果有的话）。
	 * @param target 通过调用 {@link #getTarget()} 获取的对象
	 * @throws Exception 如果无法释放该对象
	 */
	void releaseTarget(Object target) throws Exception;

}
