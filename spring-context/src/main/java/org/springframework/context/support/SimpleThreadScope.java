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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 一个简单的基于线程的{@link Scope}实现。
 *
 * <p><b>注意：</b>这个线程作用域在常见上下文中不是默认注册的。
 * 相反，您需要通过在设置中将其显式分配给作用域键，可以通过
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory#registerScope}
 * 或通过{@link org.springframework.beans.factory.config.CustomScopeConfigurer} bean。
 *
 * <p>{@code SimpleThreadScope} <em>不清理</em> 与之关联的任何对象。
 * 因此，在Web环境中，通常最好使用一个请求绑定的作用域实现，比如{@code org.springframework.web.context.request.RequestScope}，
 * 它实现了作用域属性的完整生命周期（包括可靠的销毁）。
 *
 * <p>有关支持销毁回调的基于线程的{@code Scope}的实现，请参阅
 * <a href="https://www.springbyexample.org/examples/custom-thread-scope-module.html">Spring by Example</a>。
 *
 * <p>感谢Eugene Kuleshov提交了线程作用域的原型！
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see org.springframework.web.context.request.RequestScope
 * @since 3.0
 */
public class SimpleThreadScope implements Scope {

	private static final Log logger = LogFactory.getLog(SimpleThreadScope.class);

	/**
	 * 线程范围的{@code ThreadLocal}，用于存储作用域对象。
	 * 使用{@code NamedThreadLocal}，初始化为一个空的{@code HashMap}。
	 */
	private final ThreadLocal<Map<String, Object>> threadScope =
			new NamedThreadLocal<Map<String, Object>>("SimpleThreadScope") {
				@Override
				protected Map<String, Object> initialValue() {
					return new HashMap<>();
				}
			};


	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		// 获取当前线程的对象集合
		Map<String, Object> scope = this.threadScope.get();

		// 注意: 请勿使用Map::computeIfAbsent修改以下内容。有关详细信息，
		// 见 https://github.com/spring-projects/spring-framework/issues/25801.
		// 从对象集合中获取特定名称的对象
		Object scopedObject = scope.get(name);

		// 如果对象不存在，则通过 objectFactory 创建对象，并放入线程范围的集合中
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			scope.put(name, scopedObject);
		}

		// 返回线程范围内的对象
		return scopedObject;
	}

	@Override
	@Nullable
	public Object remove(String name) {
		Map<String, Object> scope = this.threadScope.get();
		return scope.remove(name);
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		logger.warn("SimpleThreadScope does not support destruction callbacks. " +
				"Consider using RequestScope in a web environment.");
	}

	@Override
	@Nullable
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		return Thread.currentThread().getName();
	}

}
