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

package org.springframework.context;

/**
 * 封装事件发布功能的接口。
 *
 * <p>用作 {@link ApplicationContext} 的超级接口。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see ApplicationContext
 * @see ApplicationEventPublisherAware
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.event.EventPublicationInterceptor
 * @since 1.1.1
 */
@FunctionalInterface
public interface ApplicationEventPublisher {

	/**
	 * 通知所有注册到此应用程序的<strong>匹配</strong>监听器应用程序事件。
	 * 事件可以是框架事件（例如 ContextRefreshedEvent）或特定于应用程序的事件。
	 * <p>这样的事件发布步骤实际上是将事件交给多播器，并不意味着同步/异步执行，
	 * 甚至根本不意味着立即执行。鼓励事件监听器尽可能高效，可以为较长时间运行和
	 * 可能阻塞的操作使用异步执行。
	 *
	 * @param event 要发布的事件
	 * @see #publishEvent(Object)
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.springframework.context.event.ContextClosedEvent
	 */
	default void publishEvent(ApplicationEvent event) {
		publishEvent((Object) event);
	}

	/**
	 * 通知所有注册到此应用程序的<strong>匹配</strong>监听器事件。
	 * <p>如果指定的 {@code event} 不是 {@link ApplicationEvent}，则将其包装在 {@link PayloadApplicationEvent} 中。
	 * <p>这样的事件发布步骤实际上是将事件交给多播器，并不意味着同步/异步执行，
	 * 甚至根本不意味着立即执行。鼓励事件监听器尽可能高效，可以为较长时间运行和
	 * 可能阻塞的操作使用异步执行。
	 *
	 * @param event 要发布的事件
	 * @see #publishEvent(ApplicationEvent)
	 * @see PayloadApplicationEvent
	 * @since 4.2
	 */
	void publishEvent(Object event);

}
