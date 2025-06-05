/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context;

import java.time.Clock;
import java.util.EventObject;

/**
 * 所有应用事件的基类。此类为抽象类，
 * 因为直接发布通用事件没有实际意义。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.event.EventListener
 */
public abstract class ApplicationEvent extends EventObject {

	/** 使用 Spring 1.2 版本的 serialVersionUID 保持兼容性。 */
	private static final long serialVersionUID = 7099057708183571937L;

	/** 事件发生的系统时间戳（毫秒）。 */
	private final long timestamp;


	/**
	 * 创建一个新的 {@code ApplicationEvent}，其 {@link #getTimestamp() timestamp}
	 * 设置为 {@link System#currentTimeMillis()} 返回的当前时间。
	 * @param source 事件最初发生的对象，或与事件关联的对象（不能为空）
	 * @see #ApplicationEvent(Object, Clock)
	 */
	public ApplicationEvent(Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * 创建一个新的 {@code ApplicationEvent}，其 {@link #getTimestamp() timestamp}
	 * 设置为传入 {@link Clock} 的 {@link Clock#millis()} 返回值。
	 * <p>此构造方法通常用于测试场景。
	 * @param source 事件最初发生的对象，或与事件关联的对象（不能为空）
	 * @param clock 提供时间戳的时钟实例
	 * @since 5.3.8
	 * @see #ApplicationEvent(Object)
	 */
	public ApplicationEvent(Object source, Clock clock) {
		super(source);
		this.timestamp = clock.millis();
	}


	/**
	 * 返回事件发生的时间戳（毫秒）。
	 * @see #ApplicationEvent(Object)
	 * @see #ApplicationEvent(Object, Clock)
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}

}
