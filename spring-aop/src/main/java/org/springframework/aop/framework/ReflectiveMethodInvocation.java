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

package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.lang.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring 对 AOP Alliance {@link org.aopalliance.intercept.MethodInvocation}
 * 接口的实现，同时实现了扩展接口
 * {@link org.springframework.aop.ProxyMethodInvocation}。
 *
 * <p>使用反射调用目标对象。子类可以重写 {@link #invokeJoinpoint()} 方法
 * 以改变此行为，因此这也是更专门的 MethodInvocation 实现的有用基类。
 *
 * <p>可以使用 {@link #invocableClone()} 方法克隆一次调用，
 * 从而重复调用 {@link #proceed()}（每个克隆一次）。
 * 也可以使用 {@link #setUserAttribute} / {@link #getUserAttribute} 方法
 * 将自定义属性附加到调用上。
 *
 * <p><b>注意：</b>此类被视为内部类，不应直接访问。
 * 将其设为 public 的唯一原因是为了兼容现有框架集成（例如 Pitchfork）。
 * 对于任何其他用途，请改用 {@link ProxyMethodInvocation} 接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @see #invokeJoinpoint
 * @see #proceed
 * @see #invocableClone
 * @see #setUserAttribute
 * @see #getUserAttribute
 */
public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {

	protected final Object proxy;

	@Nullable
	protected final Object target;

	protected final Method method;

	protected Object[] arguments;

	@Nullable
	private final Class<?> targetClass;

	/**
	 * 此调用中用户特定属性的延迟初始化映射。
	 */
	@Nullable
	private Map<String, Object> userAttributes;

	/**
	 * MethodInterceptor 和需要动态检查的 InterceptorAndDynamicMethodMatcher 的列表。
	 */
	protected final List<?> interceptorsAndDynamicMethodMatchers;

	/**
	 * 当前正在调用的拦截器的索引，从 0 开始。
	 * 在调用前为 -1；调用后则表示当前拦截器。
	 */
	private int currentInterceptorIndex = -1;


	/**
	 * 使用给定参数构造一个新的 ReflectiveMethodInvocation。
	 * @param proxy 发起调用所基于的代理对象
	 * @param target 要调用的目标对象
	 * @param method 要调用的方法
	 * @param arguments 调用方法时使用的参数
	 * @param targetClass 目标类，用于 MethodMatcher 调用
	 * @param interceptorsAndDynamicMethodMatchers 应用的拦截器，
	 * 以及任何需要在运行时求值的 InterceptorAndDynamicMethodMatcher。
	 * 此结构中包含的 MethodMatcher 必须已经在尽可能静态的范围内被判定为匹配。
	 * 传入数组可能会快约 10%，但会使代码复杂化，并且只适用于静态切点。
	 */
	protected ReflectiveMethodInvocation(
			Object proxy, @Nullable Object target, Method method, @Nullable Object[] arguments,
			@Nullable Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {

		this.proxy = proxy;
		this.target = target;
		this.targetClass = targetClass;
		this.method = BridgeMethodResolver.findBridgedMethod(method);
		this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
		this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
	}


	@Override
	public final Object getProxy() {
		return this.proxy;
	}

	@Override
	@Nullable
	public final Object getThis() {
		return this.target;
	}

	@Override
	public final AccessibleObject getStaticPart() {
		return this.method;
	}

	/**
	 * 返回在被代理接口上调用的方法。
	 * 该方法可能与该接口底层实现上调用的方法对应，也可能不对应。
	 */
	@Override
	public final Method getMethod() {
		return this.method;
	}

	@Override
	public final Object[] getArguments() {
		return this.arguments;
	}

	@Override
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}


	@Override
	@Nullable
	public Object proceed() throws Throwable {
		// 我们从 -1 的索引开始，并提前递增。
		// 判断当前拦截器索引是否已经到达最后一个拦截器
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			// 如果已经执行完所有拦截器，则调用目标方法，即真正的业务方法执行
			return invokeJoinpoint();
		}
		// 将拦截器索引 +1，获取当前要执行的拦截器
		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		// 如果当前对象是“动态方法匹配拦截器”
		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
			// 在此处求值动态方法匹配器：静态部分已经被求值并判定为匹配。
			InterceptorAndDynamicMethodMatcher dm =
					(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
			// 获取目标类
			Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
			// 执行“动态方法匹配”
			if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
				// 如果匹配成功，则执行当前拦截器
				return dm.interceptor.invoke(this);
			}
			else {
				// 动态匹配失败。
				// 跳过此拦截器并调用链中的下一个拦截器。
				return proceed();
			}
		}
		else {
			// 这是一个拦截器，因此直接调用它：切点已经在此对象构造前被静态求值。
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
		}
	}

	/**
	 * 使用反射调用连接点。
	 * 子类可以重写此方法以使用自定义调用。
	 * @return 连接点的返回值
	 * @throws Throwable 如果调用连接点导致异常
	 */
	@Nullable
	protected Object invokeJoinpoint() throws Throwable {
		return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
	}


	/**
	 * 此实现返回该调用对象的浅拷贝，
	 * 包括原始参数数组的独立副本。
	 * <p>在此场景中我们需要浅拷贝：希望使用相同的拦截器链和其他对象引用，
	 * 但希望当前拦截器索引具有独立的值。
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MethodInvocation invocableClone() {
		Object[] cloneArguments = this.arguments;
		if (this.arguments.length > 0) {
			// 构建参数数组的独立副本。
			cloneArguments = this.arguments.clone();
		}
		return invocableClone(cloneArguments);
	}

	/**
	 * 此实现返回该调用对象的浅拷贝，
	 * 并将给定参数数组用于该克隆。
	 * <p>在此场景中我们需要浅拷贝：希望使用相同的拦截器链和其他对象引用，
	 * 但希望当前拦截器索引具有独立的值。
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MethodInvocation invocableClone(Object... arguments) {
		// 强制初始化用户属性 Map，
		// 以便在克隆中共享 Map 引用。
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}

		// 创建 MethodInvocation 克隆。
		try {
			ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
			clone.arguments = arguments;
			return clone;
		}
		catch (CloneNotSupportedException ex) {
			throw new IllegalStateException(
					"Should be able to clone object of type [" + getClass() + "]: " + ex);
		}
	}


	@Override
	public void setUserAttribute(String key, @Nullable Object value) {
		if (value != null) {
			if (this.userAttributes == null) {
				this.userAttributes = new HashMap<>();
			}
			this.userAttributes.put(key, value);
		}
		else {
			if (this.userAttributes != null) {
				this.userAttributes.remove(key);
			}
		}
	}

	@Override
	@Nullable
	public Object getUserAttribute(String key) {
		return (this.userAttributes != null ? this.userAttributes.get(key) : null);
	}

	/**
	 * 返回与此调用关联的用户属性。
	 * 此方法提供了一种绑定到调用的 ThreadLocal 替代方案。
	 * <p>此映射会延迟初始化，并且不会在 AOP 框架自身中使用。
	 * @return 与此调用关联的所有用户属性
	 * （绝不为 {@code null}）
	 */
	public Map<String, Object> getUserAttributes() {
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}
		return this.userAttributes;
	}


	@Override
	public String toString() {
		// 不要对目标对象调用 toString，它可能已被代理。
		StringBuilder sb = new StringBuilder("ReflectiveMethodInvocation: ");
		sb.append(this.method).append("; ");
		if (this.target == null) {
			sb.append("target is null");
		}
		else {
			sb.append("target is of class [").append(this.target.getClass().getName()).append(']');
		}
		return sb.toString();
	}

}
