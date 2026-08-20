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

package org.springframework.remoting.support;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.springframework.lang.Nullable;

/**
 * 封装远程调用结果，持有返回值或异常。
 * 用于基于 HTTP 的序列化调用器。
 *
 * <p>这是一个 SPI 类，通常不会被应用程序直接使用。
 * 可以通过子类化来添加额外的调用参数。
 *
 * <p>{@link RemoteInvocation} 和 {@link RemoteInvocationResult} 均设计为
 * 既支持标准 Java 序列化，也支持 JavaBean 风格的序列化。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see RemoteInvocation
 */
public class RemoteInvocationResult implements Serializable {

	/** 使用 Spring 1.1 版本的 serialVersionUID 以保证兼容性。 */
	private static final long serialVersionUID = 2138555143707773549L;


	@Nullable
	private Object value;

	@Nullable
	private Throwable exception;


	/**
	 * 为给定的返回值创建一个新的 RemoteInvocationResult。
	 * @param value 目标方法成功调用时返回的结果值
	 */
	public RemoteInvocationResult(@Nullable Object value) {
		this.value = value;
	}

	/**
	 * 为给定的异常创建一个新的 RemoteInvocationResult。
	 * @param exception 目标方法调用失败时抛出的异常
	 */
	public RemoteInvocationResult(@Nullable Throwable exception) {
		this.exception = exception;
	}

	/**
	 * 创建一个新的 RemoteInvocationResult，用于 JavaBean 风格的反序列化
	 * （例如使用 Jackson）。
	 * @see #setValue
	 * @see #setException
	 */
	public RemoteInvocationResult() {
	}


	/**
	 * 设置目标方法成功调用时返回的结果值（如果有的话）。
	 * <p>此 setter 适用于 JavaBean 风格的反序列化。
	 * 其他情况下请使用 {@link #RemoteInvocationResult(Object)}。
	 * @see #RemoteInvocationResult()
	 */
	public void setValue(@Nullable Object value) {
		this.value = value;
	}

	/**
	 * 返回目标方法成功调用时返回的结果值（如果有的话）。
	 * @see #hasException
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * 设置目标方法调用失败时抛出的异常（如果有的话）。
	 * <p>此 setter 适用于 JavaBean 风格的反序列化。
	 * 其他情况下请使用 {@link #RemoteInvocationResult(Throwable)}。
	 * @see #RemoteInvocationResult()
	 */
	public void setException(@Nullable Throwable exception) {
		this.exception = exception;
	}

	/**
	 * 返回目标方法调用失败时抛出的异常（如果有的话）。
	 * @see #hasException
	 */
	@Nullable
	public Throwable getException() {
		return this.exception;
	}

	/**
	 * 返回此调用结果是否持有异常。
	 * 如果返回 {@code false}，则表示结果值有效
	 * （即使其值为 {@code null}）。
	 * @see #getValue
	 * @see #getException
	 */
	public boolean hasException() {
		return (this.exception != null);
	}

	/**
	 * 返回此调用结果是否持有 InvocationTargetException，
	 * 即由目标方法自身的调用所抛出的异常。
	 * @see #hasException()
	 */
	public boolean hasInvocationTargetException() {
		return (this.exception instanceof InvocationTargetException);
	}


	/**
	 * 重新创建调用结果：在目标方法成功调用时返回结果值，
	 * 或在目标方法抛出异常时重新抛出该异常。
	 * @return 结果值（如果有的话）
	 * @throws Throwable 异常（如果有的话）
	 */
	@Nullable
	public Object recreate() throws Throwable {
		if (this.exception != null) {
			Throwable exToThrow = this.exception;
			if (this.exception instanceof InvocationTargetException) {
				exToThrow = ((InvocationTargetException) this.exception).getTargetException();
			}
			RemoteInvocationUtils.fillInClientStackTraceIfPossible(exToThrow);
			throw exToThrow;
		}
		else {
			return this.value;
		}
	}

}
