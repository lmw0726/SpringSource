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
import org.springframework.lang.Nullable;

/**
 * 标准 {@link ApplicationListener} 接口的扩展版本，
 * 提供更多元数据，如支持的事件类型和源类型。
 *
 * <p>若需要对泛型事件类型进行完整的反射检查，建议实现
 * {@link GenericApplicationListener} 接口。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see GenericApplicationListener
 * @see GenericApplicationListenerAdapter
 */
public interface SmartApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

	/**
	 * 判断此监听器是否支持给定的事件类型。
	 * @param eventType 事件类型（永不为 {@code null}）
	 */
	boolean supportsEventType(Class<? extends ApplicationEvent> eventType);

	/**
	 * 判断此监听器是否支持给定的事件源类型。
	 * <p>默认实现始终返回 {@code true}。
	 * @param sourceType 事件源类型，若无事件源则为 {@code null}
	 */
	default boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return true;
	}

	/**
	 * 确定此监听器在同一事件监听器集合中的顺序。
	 * <p>默认实现返回 {@link #LOWEST_PRECEDENCE}。
	 */
	@Override
	default int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	/**
	 * 返回监听器的可选标识符。
	 * <p>默认值为空字符串。
	 * @since 5.3.5
	 * @see EventListener#id
	 * @see ApplicationEventMulticaster#removeApplicationListeners
	 */
	default String getListenerId() {
		return "";
	}

}
