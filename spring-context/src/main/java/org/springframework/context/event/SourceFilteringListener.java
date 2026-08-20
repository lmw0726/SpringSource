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

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.context.ApplicationListener} 装饰器，用于过滤
 * 来自指定事件源的事件，仅对匹配的 {@link org.springframework.context.ApplicationEvent}
 * 对象调用其代理监听器。
 *
 * <p>也可作为基类使用，此时应覆盖 {@link #onApplicationEventInternal}
 * 方法，而非指定代理监听器。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0.5
 */
public class SourceFilteringListener implements GenericApplicationListener {
	/**
	 * 原始类
	 */
	private final Object source;

	/**
	 * 代理的监听器
	 */
	@Nullable
	private GenericApplicationListener delegate;


	/**
	 * 为指定的事件源创建一个 SourceFilteringListener。
	 *
	 * @param source   此监听器所过滤的事件源，
	 *                 仅处理来自该事件源的事件
	 * @param delegate 当事件来自指定的事件源时，
	 *                 要调用的代理监听器
	 */
	public SourceFilteringListener(Object source, ApplicationListener<?> delegate) {
		this.source = source;
		this.delegate = (delegate instanceof GenericApplicationListener ?
				(GenericApplicationListener) delegate : new GenericApplicationListenerAdapter(delegate));
	}

	/**
	 * 为指定的事件源创建一个 SourceFilteringListener，
	 * 期望子类覆盖 {@link #onApplicationEventInternal}
	 * 方法（而非指定代理监听器）。
	 *
	 * @param source 此监听器所过滤的事件源，
	 *               仅处理来自该事件源的事件
	 */
	protected SourceFilteringListener(Object source) {
		this.source = source;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event.getSource() == this.source) {
			onApplicationEventInternal(event);
		}
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		return (this.delegate == null || this.delegate.supportsEventType(eventType));
	}

	@Override
	public boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return (sourceType != null && sourceType.isInstance(this.source));
	}

	@Override
	public int getOrder() {
		return (this.delegate != null ? this.delegate.getOrder() : Ordered.LOWEST_PRECEDENCE);
	}

	@Override
	public String getListenerId() {
		return (this.delegate != null ? this.delegate.getListenerId() : "");
	}


	/**
	 * 根据所需的事件源过滤后，实际处理事件。
	 * <p>默认实现会调用指定的代理监听器（如果有的话）。
	 *
	 * @param event 要处理的事件（与指定的事件源匹配）
	 */
	protected void onApplicationEventInternal(ApplicationEvent event) {
		if (this.delegate == null) {
			throw new IllegalStateException(
					"Must specify a delegate object or override the onApplicationEventInternal method");
		}
		this.delegate.onApplicationEvent(event);
	}

}
