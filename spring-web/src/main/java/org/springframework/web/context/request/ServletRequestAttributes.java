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

package org.springframework.web.context.request;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link RequestAttributes}接口的基于Servlet的实现。
 *
 * <p>从servlet请求和HTTP会话作用域中访问对象，无区别地区分“会话”和“全局会话”。
 *
 * @author Juergen Hoeller
 * @see javax.servlet.ServletRequest#getAttribute
 * @see javax.servlet.http.HttpSession#getAttribute
 * @since 2.0
 */
public class ServletRequestAttributes extends AbstractRequestAttributes {

	/**
	 * 用于标识当销毁回调存储在{@link HttpSession}中时其名称前缀的{@link String}的常量。
	 */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX =
			ServletRequestAttributes.class.getName() + ".DESTRUCTION_CALLBACK.";
	/**
	 * 不可变值类型
	 */
	protected static final Set<Class<?>> immutableValueTypes = new HashSet<>(16);

	static {
		// 将标准数值类型添加到不可变值类型集合中
		immutableValueTypes.addAll(NumberUtils.STANDARD_NUMBER_TYPES);
		// 添加 Boolean 类型到不可变值类型集合中
		immutableValueTypes.add(Boolean.class);
		// 添加 Character 类型到不可变值类型集合中
		immutableValueTypes.add(Character.class);
		// 添加 String 类型到不可变值类型集合中
		immutableValueTypes.add(String.class);
	}


	/**
	 * HttpServlet请求。
	 */
	private final HttpServletRequest request;

	/**
	 * HttpServlet响应
	 */
	@Nullable
	private HttpServletResponse response;

	/**
	 * Http会话
	 */
	@Nullable
	private volatile HttpSession session;

	/**
	 * 用于更新的会话属性的映射。
	 */
	private final Map<String, Object> sessionAttributesToUpdate = new ConcurrentHashMap<>(1);


	/**
	 * 为给定的请求创建一个新的ServletRequestAttributes实例。
	 *
	 * @param request 当前的HTTP请求
	 */
	public ServletRequestAttributes(HttpServletRequest request) {
		Assert.notNull(request, "Request must not be null");
		this.request = request;
	}

	/**
	 * 为给定的请求创建一个新的ServletRequestAttributes实例。
	 *
	 * @param request  当前的HTTP请求
	 * @param response 当前的HTTP响应（可选暴露）
	 */
	public ServletRequestAttributes(HttpServletRequest request, @Nullable HttpServletResponse response) {
		this(request);
		this.response = response;
	}


	/**
	 * 暴露我们正在包装的原生{@link HttpServletRequest}。
	 */
	public final HttpServletRequest getRequest() {
		return this.request;
	}

	/**
	 * 暴露我们正在包装的原生{@link HttpServletResponse}（如果有的话）。
	 */
	@Nullable
	public final HttpServletResponse getResponse() {
		return this.response;
	}

	/**
	 * 暴露我们正在包装的{@link HttpSession}。
	 *
	 * @param allowCreate 是否允许在尚不存在的情况下创建新会话
	 */
	@Nullable
	protected final HttpSession getSession(boolean allowCreate) {
		// 如果请求仍然处于活跃状态
		if (isRequestActive()) {
			// 获取请求的会话对象
			HttpSession session = this.request.getSession(allowCreate);
			// 将会话对象保存在成员变量中
			this.session = session;
			// 返回会话对象
			return session;
		} else {
			// 如果请求已经完成，则通过存储的会话引用进行访问
			HttpSession session = this.session;
			// 如果会话为 null
			if (session == null) {
				// 如果允许创建新会话
				if (allowCreate) {
					// 抛出 IllegalStateException 异常
					throw new IllegalStateException(
							"No session found and request already completed - cannot create new session!");
				} else {
					// 否则，尝试获取请求的会话对象（如果允许创建新会话）
					session = this.request.getSession(false);
					// 将获取的会话对象保存在成员变量中
					this.session = session;
				}
			}
			// 返回会话对象
			return session;
		}
	}

	private HttpSession obtainSession() {
		// 获取当前会话，若不存在则创建一个新的会话
		HttpSession session = getSession(true);
		Assert.state(session != null, "No HttpSession");
		// 返回会话对象
		return session;
	}


	@Override
	public Object getAttribute(String name, int scope) {
		// 如果作用域是请求范围
		if (scope == SCOPE_REQUEST) {
			// 如果请求不再活跃
			if (!isRequestActive()) {
				// 抛出 IllegalStateException 异常
				throw new IllegalStateException(
						"Cannot ask for request attribute - request is not active anymore!");
			}
			// 返回请求属性值
			return this.request.getAttribute(name);
		} else {
			// 否则，作用域为会话范围
			// 获取会话对象
			HttpSession session = getSession(false);
			// 如果会话不为 null
			if (session != null) {
				try {
					// 获取会话属性值
					Object value = session.getAttribute(name);
					// 如果属性值不为 null
					if (value != null) {
						// 将属性名称和值添加到需要更新的会话属性映射中
						this.sessionAttributesToUpdate.put(name, value);
					}
					// 返回属性值
					return value;
				} catch (IllegalStateException ex) {
					// 会话无效 - 通常不应发生。
				}
			}
			// 返回 null
			return null;
		}
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		// 如果作用域是请求作用域
		if (scope == SCOPE_REQUEST) {
			// 如果请求不再活动状态，则抛出异常
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot set request attribute - request is not active anymore!");
			}
			// 否则设置请求属性值
			this.request.setAttribute(name, value);
		} else {
			// 否则，获取会话对象
			HttpSession session = obtainSession();
			// 从待更新的会话属性集合中移除指定名称的属性
			this.sessionAttributesToUpdate.remove(name);
			// 设置会话属性值
			session.setAttribute(name, value);
		}
	}

	@Override
	public void removeAttribute(String name, int scope) {
		// 如果作用域是请求范围
		if (scope == SCOPE_REQUEST) {
			// 如果请求仍然活跃
			if (isRequestActive()) {
				// 移除请求销毁回调
				removeRequestDestructionCallback(name);
				// 移除请求属性
				this.request.removeAttribute(name);
			}
		} else {
			// 否则，作用域是会话范围
			// 获取会话对象
			HttpSession session = getSession(false);
			// 如果会话不为 null
			if (session != null) {
				// 从需要更新的会话属性映射中移除属性名称
				this.sessionAttributesToUpdate.remove(name);
				try {
					// 移除属性销毁回调
					session.removeAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name);
					// 移除会话属性
					session.removeAttribute(name);
				} catch (IllegalStateException ex) {
					// 会话无效 - 通常不应发生。
				}
			}
		}
	}

	@Override
	public String[] getAttributeNames(int scope) {
		// 如果作用域是请求范围
		if (scope == SCOPE_REQUEST) {
			// 如果请求不再活跃
			if (!isRequestActive()) {
				// 抛出 IllegalStateException 异常
				throw new IllegalStateException(
						"Cannot ask for request attributes - request is not active anymore!");
			}
			// 返回请求属性的名称数组
			return StringUtils.toStringArray(this.request.getAttributeNames());
		} else {
			// 否则，作用域是会话范围
			// 获取会话对象
			HttpSession session = getSession(false);
			// 如果会话不为 null
			if (session != null) {
				try {
					// 返回会话属性的名称数组
					return StringUtils.toStringArray(session.getAttributeNames());
				} catch (IllegalStateException ex) {
					// 会话无效 - 通常不应发生。
				}
			}
			// 返回空数组
			return new String[0];
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback, int scope) {
		// 如果作用域是请求作用域
		if (scope == SCOPE_REQUEST) {
			// 注册请求销毁回调
			registerRequestDestructionCallback(name, callback);
		} else {
			// 否则，注册会话销毁回调
			registerSessionDestructionCallback(name, callback);
		}
	}

	@Override
	public Object resolveReference(String key) {
		// 如果键是 request
		if (REFERENCE_REQUEST.equals(key)) {
			// 返回当前请求对象
			return this.request;
		} else if (REFERENCE_SESSION.equals(key)) {
			// 如果键是 session
			// 返回会话对象
			return getSession(true);
		} else {
			// 否则，返回 null
			return null;
		}
	}

	@Override
	public String getSessionId() {
		return obtainSession().getId();
	}

	@Override
	public Object getSessionMutex() {
		return WebUtils.getSessionMutex(obtainSession());
	}


	/**
	 * 通过 {@code session.setAttribute} 调用更新所有访问过的会话属性，明确向容器指示它们可能已被修改。
	 */
	@Override
	protected void updateAccessedSessionAttributes() {
		// 如果待更新的会话属性集合不为空
		if (!this.sessionAttributesToUpdate.isEmpty()) {
			// 获取当前会话，若不存在则返回null
			// 更新所有受影响的会话属性。
			HttpSession session = getSession(false);
			// 如果会话不为空
			if (session != null) {
				try {
					// 遍历待更新的会话属性集合中的每个条目
					for (Map.Entry<String, Object> entry : this.sessionAttributesToUpdate.entrySet()) {
						// 属性名
						String name = entry.getKey();
						// 新的属性值
						Object newValue = entry.getValue();
						// 获取旧的属性值
						Object oldValue = session.getAttribute(name);
						// 如果旧值等于新值且属性不是不可变的
						if (oldValue == newValue && !isImmutableSessionAttribute(name, newValue)) {
							// 更新会话中的属性值
							session.setAttribute(name, newValue);
						}
					}
				} catch (IllegalStateException ex) {
					// 捕获会话已失效的异常，通常不会发生
				}
			}
			// 清空待更新的会话属性集合
			this.sessionAttributesToUpdate.clear();
		}
	}

	/**
	 * 确定给定值是否被视为不可变会话属性，即，不必通过 {@code session.setAttribute} 重新设置其值，因为其值在内部不能有意义地更改。
	 * <p>默认实现对 {@code String}、{@code Character}、{@code Boolean} 和标准的 {@code Number} 值返回 {@code true}。
	 *
	 * @param name  属性的名称
	 * @param value 要检查的相应值
	 * @return 如果为了会话属性管理目的要将值视为不可变，则为 {@code true}；否则为 {@code false}
	 * @see #updateAccessedSessionAttributes()
	 */
	protected boolean isImmutableSessionAttribute(String name, @Nullable Object value) {
		return (value == null || immutableValueTypes.contains(value.getClass()));
	}

	/**
	 * 将给定回调注册为在会话终止后执行。
	 * <p>注意：为了在 Web 应用程序重新启动时生存下来，回调对象应该是可序列化的。
	 *
	 * @param name     要为其注册回调的属性的名称
	 * @param callback 用于销毁的回调
	 */
	protected void registerSessionDestructionCallback(String name, Runnable callback) {
		// 获取会话对象
		HttpSession session = obtainSession();
		// 设置属性到会话中，属性名称为 ServletRequestAttributes.DESTRUCTION_CALLBACK. + name，
		// 属性值为 销毁回调绑定监听器 包装的回调对象
		session.setAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name, new DestructionCallbackBindingListener(callback));
	}


	@Override
	public String toString() {
		return this.request.toString();
	}

}
