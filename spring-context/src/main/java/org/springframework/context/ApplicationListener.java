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

import java.util.EventListener;
import java.util.function.Consumer;

/**
 * 应用程序事件监听器应实现的接口。
 *
 * <p>基于标准的 {@code java.util.EventListener} 接口，用于观察者设计模式。
 *
 * <p>从 Spring 3.0 开始，{@code ApplicationListener} 可以通用地声明其感兴趣的事件类型。
 * 当注册到 Spring {@code ApplicationContext} 时，事件将相应地进行过滤，只有匹配的事件对象才会调用监听器。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @param <E> 要监听的具体 {@code ApplicationEvent} 子类
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.event.SmartApplicationListener
 * @see org.springframework.context.event.GenericApplicationListener
 * @see org.springframework.context.event.EventListener
 */
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

	/**
	 * 处理应用程序事件。
	 * @param event 要响应的事件
	 */
	void onApplicationEvent(E event);


	/**
	 * 为给定的 payload 消费者创建一个新的 {@code ApplicationListener}。
	 * @param consumer 事件 payload 消费者
	 * @param <T> 事件 payload 的类型
	 * @return 对应的 {@code ApplicationListener} 实例
	 * @since 5.3
	 * @see PayloadApplicationEvent
	 */
	static <T> ApplicationListener<PayloadApplicationEvent<T>> forPayload(Consumer<T> consumer) {
		return event -> consumer.accept(event.getPayload());
	}

}
