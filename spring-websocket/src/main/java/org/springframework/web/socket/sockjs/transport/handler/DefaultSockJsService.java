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

package org.springframework.web.socket.sockjs.transport.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;

import javax.servlet.ServletContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 一个 {@link org.springframework.web.socket.sockjs.SockJsService} 的默认实现，预注册了所有默认的 {@link TransportHandler} 实现。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultSockJsService extends TransportHandlingSockJsService implements ServletContextAware {

	/**
	 * 使用默认的 {@link TransportHandler handler} 类型创建一个 DefaultSockJsService。
	 *
	 * @param scheduler 一个用于心跳消息和移除超时会话的任务调度器;
	 *                  提供的 TaskScheduler 应作为 Spring bean 声明，以确保它在启动时初始化并在应用程序停止时关闭
	 */
	public DefaultSockJsService(TaskScheduler scheduler) {
		this(scheduler, getDefaultTransportHandlers(null));
	}

	/**
	 * 使用重写的 {@link TransportHandler handler} 类型创建一个 DefaultSockJsService，
	 * 替换相应的默认处理程序实现。
	 *
	 * @param scheduler        一个用于心跳消息和移除超时会话的任务调度器;
	 *                         提供的 TaskScheduler 应作为 Spring bean 声明，以确保它在启动时初始化并在应用程序停止时关闭
	 * @param handlerOverrides 零个或多个要覆盖的默认传输处理程序类型
	 */
	public DefaultSockJsService(TaskScheduler scheduler, TransportHandler... handlerOverrides) {
		this(scheduler, Arrays.asList(handlerOverrides));
	}

	/**
	 * 使用重写的 {@link TransportHandler handler} 类型创建一个 DefaultSockJsService，
	 * 替换相应的默认处理程序实现。
	 *
	 * @param scheduler        一个用于心跳消息和移除超时会话的任务调度器;
	 *                         提供的 TaskScheduler 应作为 Spring bean 声明，以确保它在启动时初始化并在应用程序停止时关闭
	 * @param handlerOverrides 零个或多个要覆盖的默认传输处理程序类型
	 */
	public DefaultSockJsService(TaskScheduler scheduler, Collection<TransportHandler> handlerOverrides) {
		super(scheduler, getDefaultTransportHandlers(handlerOverrides));
	}


	private static Set<TransportHandler> getDefaultTransportHandlers(@Nullable Collection<TransportHandler> overrides) {
		// 创建一个包含默认传输处理程序的集合
		Set<TransportHandler> result = new LinkedHashSet<>(8);
		result.add(new XhrPollingTransportHandler());
		result.add(new XhrReceivingTransportHandler());
		result.add(new XhrStreamingTransportHandler());
		result.add(new EventSourceTransportHandler());
		result.add(new HtmlFileTransportHandler());
		try {
			// 尝试创建一个WebSocket传输处理程序，并添加到集合中
			result.add(new WebSocketTransportHandler(new DefaultHandshakeHandler()));
		} catch (Exception ex) {
			// 如果创建WebSocket传输处理程序时发生异常，则记录警告信息
			Log logger = LogFactory.getLog(DefaultSockJsService.class);
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to create a default WebSocketTransportHandler", ex);
			}
		}
		// 如果有自定义的传输处理程序，则将其添加到集合中
		if (overrides != null) {
			result.addAll(overrides);
		}
		return result;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		// 遍历传输处理程序集合中的每个处理程序
		for (TransportHandler handler : getTransportHandlers().values()) {
			// 如果处理程序实现了ServletContextAware接口
			if (handler instanceof ServletContextAware) {
				// 将Servlet上下文设置给处理程序
				((ServletContextAware) handler).setServletContext(servletContext);
			}
		}
	}
}
