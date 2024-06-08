/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerContainer;

/**
 * 用于配置 {@link javax.websocket.server.ServerContainer} 的 {@link FactoryBean}。
 * 由于通常只有一个 {@code ServerContainer} 实例可在知名的 {@code javax.servlet.ServletContext} 属性下访问，
 * 因此简单地声明此 FactoryBean 并使用其 setter 方法允许通过 Spring 配置来配置 {@code ServerContainer}。
 *
 * <p>即使 {@code ServerContainer} 没有注入到 Spring 应用程序上下文中的任何其他 bean 中，这也是有用的。
 * 例如，一个应用程序可以配置一个 {@link org.springframework.web.socket.server.support.DefaultHandshakeHandler}、
 * 一个 {@link org.springframework.web.socket.sockjs.SockJsService} 或 {@link ServerEndpointExporter}，
 * 并分别声明此 FactoryBean 以便自定义（唯一的）{@code ServerContainer} 实例的属性。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class ServletServerContainerFactoryBean
		implements FactoryBean<WebSocketContainer>, ServletContextAware, InitializingBean {

	/**
	 * 异步发送超时时间（毫秒）。
	 */
	@Nullable
	private Long asyncSendTimeout;

	/**
	 * 最大会话空闲超时时间（毫秒）。
	 */
	@Nullable
	private Long maxSessionIdleTimeout;

	/**
	 * 最大文本消息缓冲区大小。
	 */
	@Nullable
	private Integer maxTextMessageBufferSize;

	/**
	 * 最大二进制消息缓冲区大小。
	 */
	@Nullable
	private Integer maxBinaryMessageBufferSize;

	/**
	 * Servlet 上下文。
	 */
	@Nullable
	private ServletContext servletContext;

	/**
	 * 服务器容器。
	 */
	@Nullable
	private ServerContainer serverContainer;


	public void setAsyncSendTimeout(Long timeoutInMillis) {
		this.asyncSendTimeout = timeoutInMillis;
	}

	@Nullable
	public Long getAsyncSendTimeout() {
		return this.asyncSendTimeout;
	}

	public void setMaxSessionIdleTimeout(Long timeoutInMillis) {
		this.maxSessionIdleTimeout = timeoutInMillis;
	}

	@Nullable
	public Long getMaxSessionIdleTimeout() {
		return this.maxSessionIdleTimeout;
	}

	public void setMaxTextMessageBufferSize(Integer bufferSize) {
		this.maxTextMessageBufferSize = bufferSize;
	}

	@Nullable
	public Integer getMaxTextMessageBufferSize() {
		return this.maxTextMessageBufferSize;
	}

	public void setMaxBinaryMessageBufferSize(Integer bufferSize) {
		this.maxBinaryMessageBufferSize = bufferSize;
	}

	@Nullable
	public Integer getMaxBinaryMessageBufferSize() {
		return this.maxBinaryMessageBufferSize;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.state(this.servletContext != null,
				"A ServletContext is required to access the javax.websocket.server.ServerContainer instance");
		// 从 Servlet上下文 获取 javax.websocket.server.ServerContainer 实例
		this.serverContainer = (ServerContainer) this.servletContext.getAttribute(
				"javax.websocket.server.ServerContainer");
		Assert.state(this.serverContainer != null,
				"Attribute 'javax.websocket.server.ServerContainer' not found in ServletContext");

		// 如果设置了 异步发送超时时间，则设置 serverContainer 的 异步发送超时时间 属性
		if (this.asyncSendTimeout != null) {
			this.serverContainer.setAsyncSendTimeout(this.asyncSendTimeout);
		}
		// 如果设置了 最大会话空闲超时时间，则设置 serverContainer 的 默认最大会话空闲超时时间 属性
		if (this.maxSessionIdleTimeout != null) {
			this.serverContainer.setDefaultMaxSessionIdleTimeout(this.maxSessionIdleTimeout);
		}
		// 如果设置了 最大文本消息缓冲区大小，则设置 serverContainer 的 默认最大文本消息缓冲区大小 属性
		if (this.maxTextMessageBufferSize != null) {
			this.serverContainer.setDefaultMaxTextMessageBufferSize(this.maxTextMessageBufferSize);
		}
		// 如果设置了 最大二进制消息缓冲区大小，则设置 serverContainer 的 默认最大二进制消息缓冲区大小 属性
		if (this.maxBinaryMessageBufferSize != null) {
			this.serverContainer.setDefaultMaxBinaryMessageBufferSize(this.maxBinaryMessageBufferSize);
		}
	}


	@Override
	@Nullable
	public ServerContainer getObject() {
		return this.serverContainer;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.serverContainer != null ? this.serverContainer.getClass() : ServerContainer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
