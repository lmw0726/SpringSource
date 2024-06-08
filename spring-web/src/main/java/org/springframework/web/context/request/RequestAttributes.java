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

package org.springframework.web.context.request;

import org.springframework.lang.Nullable;

/**
 * 抽象类，用于访问与请求相关的属性对象。
 * 支持访问请求范围的属性以及会话范围的属性，并且可选地支持“全局会话”的概念。
 *
 * <p>可以针对任何类型的请求/会话机制实现，特别是针对 servlet 请求。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ServletRequestAttributes
 */
public interface RequestAttributes {

	/**
	 * 指示请求范围的常量。
	 */
	int SCOPE_REQUEST = 0;

	/**
	 * 指示会话范围的常量。
	 * <p>这最好是指本地隔离的会话（如果这种区分可用）。
	 * 否则，它简单地指普通会话。
	 */
	int SCOPE_SESSION = 1;


	/**
	 * 标准请求对象引用的名称："request"。
	 *
	 * @see #resolveReference
	 */
	String REFERENCE_REQUEST = "request";

	/**
	 * 标准会话对象引用的名称："session"。
	 *
	 * @see #resolveReference
	 */
	String REFERENCE_SESSION = "session";


	/**
	 * 返回给定名称的范围属性的值（如果有的话）。
	 *
	 * @param name  属性的名称
	 * @param scope 范围标识符
	 * @return 当前属性值，如果未找到则返回 {@code null}
	 */
	@Nullable
	Object getAttribute(String name, int scope);

	/**
	 * 设置给定名称的范围属性的值，替换现有的值（如果有的话）。
	 *
	 * @param name  属性的名称
	 * @param scope 范围标识符
	 * @param value 属性的值
	 */
	void setAttribute(String name, Object value, int scope);

	/**
	 * 删除给定名称的范围属性（如果存在）。
	 * <p>请注意，实现在删除指定属性时也应删除已注册的销毁回调（如果有）。
	 * 但是，在这种情况下，不需要<i>执行</i>注册的销毁回调，
	 * 因为对象将由调用者销毁（如果适用）。
	 *
	 * @param name  属性的名称
	 * @param scope 范围标识符
	 */
	void removeAttribute(String name, int scope);

	/**
	 * 检索范围内所有属性的名称。
	 *
	 * @param scope 范围标识符
	 * @return 属性名称作为字符串数组
	 */
	String[] getAttributeNames(int scope);

	/**
	 * 注册在给定范围的指定属性销毁时执行的回调。
	 * <p>实现应尽力在适当的时间执行回调：即，在请求完成或会话终止时。
	 * 如果底层运行环境不支持此类回调，则<i>必须忽略</i>回调，并应记录相应的警告。
	 * <p>请注意，“销毁”通常对应于整个范围的销毁，而不是单个属性被应用程序显式删除。
	 * 如果通过此 facade 的 {@link #removeAttribute(String, int)} 方法删除属性，
	 * 则应禁用任何已注册的销毁回调，假设删除的对象将被重用或手动销毁。
	 * <p><b>注意：</b> 如果为会话范围注册回调对象，则这些对象通常应该是可序列化的。
	 * 否则，回调（甚至整个会话）可能无法在 web 应用重启时保持。
	 *
	 * @param name     要为其注册回调的属性名称
	 * @param callback 要执行的销毁回调
	 * @param scope    范围标识符
	 */
	void registerDestructionCallback(String name, Runnable callback, int scope);

	/**
	 * 解析给定键的上下文引用（如果有）。
	 * <p>至少：键为“request”的 HttpServletRequest 引用，和键为“session”的 HttpSession 引用。
	 *
	 * @param key 上下文键
	 * @return 对应的对象，如果未找到则返回 {@code null}
	 */
	@Nullable
	Object resolveReference(String key);

	/**
	 * 返回当前底层会话的 id。
	 *
	 * @return 会话 id 作为字符串（从不为 {@code null}）
	 */
	String getSessionId();

	/**
	 * 暴露底层会话的最佳可用互斥体：即用于底层会话同步的对象。
	 *
	 * @return 要使用的会话互斥体（从不为 {@code null}）
	 */
	Object getSessionMutex();

}
