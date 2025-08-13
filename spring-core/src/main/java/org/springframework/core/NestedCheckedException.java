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
 * 用于包装带有根本原因的已检查异常（checked Exceptions）的实用类。
 *
 * <p>该类是抽象的，强制程序员继承此类。
 * {@code getMessage} 会包含嵌套异常的信息；
 * {@code printStackTrace} 等类似方法会委托给被包装的异常（如果存在）。
 *
 * <p>该类与 {@link NestedRuntimeException} 类的相似性是不可避免的，
 * 因为 Java 要求这两个类必须有不同的父类（继承机制的局限性）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getMessage
 * @see #printStackTrace
 * @see NestedRuntimeException
 */
public abstract class NestedCheckedException extends Exception {

	/** 使用 Spring 1.2 的 serialVersionUID 以保持兼容性。 */
	private static final long serialVersionUID = 7100714597678207546L;

	static {
		// 预加载 NestedExceptionUtils 类，避免在 OSGi 环境中调用 getMessage() 时发生类加载死锁问题。
		// 报告者 Don Brown; SPR-5607。
		NestedExceptionUtils.class.getName();
	}


	/**
	 * 使用指定详细消息构造 {@code NestedCheckedException}。
	 * @param msg 详细消息
	 */
	public NestedCheckedException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定详细消息和嵌套异常构造 {@code NestedCheckedException}。
	 * @param msg 详细消息
	 * @param cause 嵌套异常
	 */
	public NestedCheckedException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回详细消息，包括嵌套异常的消息（如果存在）。
	 */
	@Override
	@Nullable
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}


	/**
	 * 获取该异常的最深层原因（根本原因），如果有的话。
	 * @return 最深层的异常，若无则返回 {@code null}
	 */
	@Nullable
	public Throwable getRootCause() {
		return NestedExceptionUtils.getRootCause(this);
	}

	/**
	 * 获取该异常的最具体原因，即最深层原因（根本原因）或该异常本身。
	 * <p>与 {@link #getRootCause()} 不同的是，如果没有根本原因，则返回当前异常实例。
	 * @return 最具体的原因（永不为 {@code null}）
	 * @since 2.0.3
	 */
	public Throwable getMostSpecificCause() {
		Throwable rootCause = getRootCause();
		return (rootCause != null ? rootCause : this);
	}

	/**
	 * 检查该异常是否包含指定类型的异常：
	 * 要么是该类型本身，要么嵌套包含该类型的异常。
	 * @param exType 要查找的异常类型
	 * @return 是否包含指定类型的嵌套异常
	 */
	public boolean contains(@Nullable Class<?> exType) {
		if (exType == null) {
			return false;
		}
		if (exType.isInstance(this)) {
			return true;
		}
		Throwable cause = getCause();
		if (cause == this) {
			return false;
		}
		if (cause instanceof NestedCheckedException) {
			return ((NestedCheckedException) cause).contains(exType);
		}
		else {
			while (cause != null) {
				if (exType.isInstance(cause)) {
					return true;
				}
				if (cause.getCause() == cause) {
					break;
				}
				cause = cause.getCause();
			}
			return false;
		}
	}

}
