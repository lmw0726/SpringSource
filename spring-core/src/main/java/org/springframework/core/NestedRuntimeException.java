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

package org.springframework.core;

import org.springframework.lang.Nullable;

/**
 * 便捷的类，用于将运行时 {@code Exceptions} 包装为根本原因。
 *
 * <p>这个类是 {@code abstract} 的，强制程序员扩展该类。
 * {@code getMessage} 将包含嵌套异常信息；
 * {@code printStackTrace} 和其他类似的方法将委托给被包装的异常（如果有）。
 *
 * <p>这个类与 {@link NestedCheckedException} 类的相似之处是无法避免的，
 * 因为 Java 强制这两个类有不同的超类（啊，具体继承的不灵活性！）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getMessage
 * @see #printStackTrace
 * @see NestedCheckedException
 */
public abstract class NestedRuntimeException extends RuntimeException {

	/**
	 * 使用 Spring 1.2 的 serialVersionUID 以实现互操作性。
	 */
	private static final long serialVersionUID = 5439915454935047936L;

	static {
		// 热切加载 NestedExceptionUtils 类以避免在 OSGi 上调用 getMessage() 时出现类加载器死锁问题。由 Don Brown 报告；SPR-5607。
		NestedExceptionUtils.class.getName();
	}


	/**
	 * 使用指定的详细消息构造一个 {@code NestedRuntimeException}。
	 *
	 * @param msg 详细消息
	 */
	public NestedRuntimeException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和嵌套异常构造一个 {@code NestedRuntimeException}。
	 *
	 * @param msg   详细消息
	 * @param cause 嵌套异常
	 */
	public NestedRuntimeException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回详细消息，包括嵌套异常的消息（如果有）。
	 */
	@Override
	@Nullable
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}


	/**
	 * 检索此异常的最内层原因（如果有）。
	 *
	 * @return 最内层异常，如果没有则为 {@code null}
	 * @since 2.0
	 */
	@Nullable
	public Throwable getRootCause() {
		return NestedExceptionUtils.getRootCause(this);
	}

	/**
	 * 检索此异常的最具体原因，即最内层原因（根本原因）或此异常本身。
	 * <p>与 {@link #getRootCause()} 不同之处在于，如果没有根本原因，则回退到当前异常。
	 *
	 * @return 最具体的原因（永远不会为 {@code null}）
	 * @since 2.0.3
	 */
	public Throwable getMostSpecificCause() {
		// 获取根本原因
		Throwable rootCause = getRootCause();
		// 如果没有根本原因，则返回当前异常
		return (rootCause != null ? rootCause : this);
	}

	/**
	 * 检查此异常是否包含指定类型的异常：
	 * 它要么是给定类的实例，要么包含给定类型的嵌套原因。
	 *
	 * @param exType 要查找的异常类型
	 * @return 是否存在指定类型的嵌套异常
	 */
	public boolean contains(@Nullable Class<?> exType) {
		// 如果异常类型为空，则返回 false
		if (exType == null) {
			return false;
		}
		// 如果异常类型是当前实例的类型，则返回 true
		if (exType.isInstance(this)) {
			return true;
		}
		// 获取当前异常的原因
		Throwable cause = getCause();
		// 如果原因是当前异常本身，则返回 false
		if (cause == this) {
			return false;
		}
		// 如果原因是 NestedRuntimeException 类型的异常
		if (cause instanceof NestedRuntimeException) {
			// 递归调用 contains 方法检查原因是否包含指定的异常类型
			return ((NestedRuntimeException) cause).contains(exType);
		} else {
			// 遍历异常的原因链，直到找到指定类型的异常或者到达原因链的末尾
			while (cause != null) {
				// 如果找到指定类型的异常，则返回 true
				if (exType.isInstance(cause)) {
					return true;
				}
				// 如果原因的原因是原因本身，则跳出循环
				if (cause.getCause() == cause) {
					break;
				}
				// 获取下一个原因
				cause = cause.getCause();
			}
			// 如果未找到指定类型的异常，则返回 false
			return false;
		}
	}

}
