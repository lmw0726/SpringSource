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

package org.springframework.scripting.bsh;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.XThis;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 处理 BeanShell 脚本对象的工具方法。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class BshScriptUtils {

	/**
	 * 从给定的脚本源码创建一个新的 BeanShell 脚本对象。
	 * <p>在此 {@code createBshObject} 变体中，脚本需要声明一个完整的类，
	 * 或返回脚本对象的实际实例。
	 * @param scriptSource 脚本源码文本
	 * @return 脚本化的 Java 对象
	 * @throws EvalError BeanShell 解析失败时抛出
	 */
	public static Object createBshObject(String scriptSource) throws EvalError {
		return createBshObject(scriptSource, null, null);
	}

	/**
	 * 从给定的脚本源码创建一个新的 BeanShell 脚本对象，
	 * 使用默认的 ClassLoader。
	 * <p>脚本可以是一个需要生成相应代理（实现指定接口）的简单脚本，
	 * 也可以声明一个完整的类或返回脚本对象的实际实例
	 * （在这种情况下，指定的接口（如果有）需要由该类/实例实现）。
	 * @param scriptSource 脚本源码文本
	 * @param scriptInterfaces 脚本化 Java 对象应实现的接口
	 * （如果脚本本身声明了完整的类或返回了脚本对象的实际实例，则可能为 {@code null} 或为空）
	 * @return 脚本化的 Java 对象
	 * @throws EvalError BeanShell 解析失败时抛出
	 * @see #createBshObject(String, Class[], ClassLoader)
	 */
	public static Object createBshObject(String scriptSource, @Nullable Class<?>... scriptInterfaces) throws EvalError {
		return createBshObject(scriptSource, scriptInterfaces, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 从给定的脚本源码创建一个新的 BeanShell 脚本对象。
	 * <p>脚本可以是一个需要生成相应代理（实现指定接口）的简单脚本，
	 * 也可以声明一个完整的类或返回脚本对象的实际实例
	 * （在这种情况下，指定的接口（如果有）需要由该类/实例实现）。
	 * @param scriptSource 脚本源码文本
	 * @param scriptInterfaces 脚本化 Java 对象应实现的接口
	 * （如果脚本本身声明了完整的类或返回了脚本对象的实际实例，则可能为 {@code null} 或为空）
	 * @param classLoader 用于求值脚本的 ClassLoader
	 * @return 脚本化的 Java 对象
	 * @throws EvalError BeanShell 解析失败时抛出
	 */
	public static Object createBshObject(String scriptSource, @Nullable Class<?>[] scriptInterfaces, @Nullable ClassLoader classLoader)
			throws EvalError {

		Object result = evaluateBshScript(scriptSource, scriptInterfaces, classLoader);
		if (result instanceof Class) {
			Class<?> clazz = (Class<?>) result;
			try {
				return ReflectionUtils.accessibleConstructor(clazz).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not instantiate script class: " + clazz.getName(), ex);
			}
		}
		else {
			return result;
		}
	}

	/**
	 * 根据给定的脚本源码求值指定的 BeanShell 脚本，
	 * 返回脚本定义的 Class。
	 * <p>脚本可以声明一个完整的类或返回脚本对象的实际实例
	 * （在这种情况下将返回该对象的 Class）。
	 * 在其他情况下，返回的 Class 将为 {@code null}。
	 * @param scriptSource 脚本源码文本
	 * @param classLoader 用于求值脚本的 ClassLoader
	 * @return 脚本化的 Java 类，如果无法确定则返回 {@code null}
	 * @throws EvalError BeanShell 解析失败时抛出
	 */
	@Nullable
	static Class<?> determineBshObjectType(String scriptSource, @Nullable ClassLoader classLoader) throws EvalError {
		Assert.hasText(scriptSource, "Script source must not be empty");
		Interpreter interpreter = new Interpreter();
		if (classLoader != null) {
			interpreter.setClassLoader(classLoader);
		}
		Object result = interpreter.eval(scriptSource);
		if (result instanceof Class) {
			return (Class<?>) result;
		}
		else if (result != null) {
			return result.getClass();
		}
		else {
			return null;
		}
	}

	/**
	 * 根据给定的脚本源码求值指定的 BeanShell 脚本，
	 * 保持返回的脚本 Class 或脚本对象不变。
	 * <p>脚本可以是一个需要生成相应代理（实现指定接口）的简单脚本，
	 * 也可以声明一个完整的类或返回脚本对象的实际实例
	 * （在这种情况下，指定的接口（如果有）需要由该类/实例实现）。
	 * @param scriptSource 脚本源码文本
	 * @param scriptInterfaces 脚本化 Java 对象应实现的接口
	 * （如果脚本本身声明了完整的类或返回了脚本对象的实际实例，则可能为 {@code null} 或为空）
	 * @param classLoader 用于求值脚本的 ClassLoader
	 * @return 脚本化的 Java 类或 Java 对象
	 * @throws EvalError BeanShell 解析失败时抛出
	 */
	static Object evaluateBshScript(
			String scriptSource, @Nullable Class<?>[] scriptInterfaces, @Nullable ClassLoader classLoader)
			throws EvalError {

		Assert.hasText(scriptSource, "Script source must not be empty");
		Interpreter interpreter = new Interpreter();
		interpreter.setClassLoader(classLoader);
		Object result = interpreter.eval(scriptSource);
		if (result != null) {
			return result;
		}
		else {
			// 简单的 BeanShell 脚本：让我们为它创建一个代理，实现给定的接口。
			if (ObjectUtils.isEmpty(scriptInterfaces)) {
				throw new IllegalArgumentException("Given script requires a script proxy: " +
						"At least one script interface is required.\nScript: " + scriptSource);
			}
			XThis xt = (XThis) interpreter.eval("return this");
			return Proxy.newProxyInstance(classLoader, scriptInterfaces, new BshObjectInvocationHandler(xt));
		}
	}


	/**
	 * 调用 BeanShell 脚本方法的 InvocationHandler。
	 */
	private static class BshObjectInvocationHandler implements InvocationHandler {

		private final XThis xt;

		public BshObjectInvocationHandler(XThis xt) {
			this.xt = xt;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				return (isProxyForSameBshObject(args[0]));
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				return this.xt.hashCode();
			}
			else if (ReflectionUtils.isToStringMethod(method)) {
				return "BeanShell object [" + this.xt + "]";
			}
			try {
				Object result = this.xt.invokeMethod(method.getName(), args);
				if (result == Primitive.NULL || result == Primitive.VOID) {
					return null;
				}
				if (result instanceof Primitive) {
					return ((Primitive) result).getValue();
				}
				return result;
			}
			catch (EvalError ex) {
				throw new BshExecutionException(ex);
			}
		}

		private boolean isProxyForSameBshObject(Object other) {
			if (!Proxy.isProxyClass(other.getClass())) {
				return false;
			}
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			return (ih instanceof BshObjectInvocationHandler &&
					this.xt.equals(((BshObjectInvocationHandler) ih).xt));
		}
	}


	/**
	 * 脚本执行失败时抛出的异常。
	 */
	@SuppressWarnings("serial")
	public static final class BshExecutionException extends NestedRuntimeException {

		private BshExecutionException(EvalError ex) {
			super("BeanShell script execution failed", ex);
		}
	}

}
