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

package org.springframework.web.reactive.socket.server.upgrade;

import org.springframework.util.Assert;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link javax.websocket.server.ServerEndpointConfig} 的默认实现，用于在 {@code RequestUpgradeStrategy} 实现中使用。
 * <p>
 * 该实现用于配置 WebSocket 服务器端点的一些参数。
 * 包括路径、端点实例、编码器、解码器等信息。
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultServerEndpointConfig extends ServerEndpointConfig.Configurator
		implements ServerEndpointConfig {

	/**
	 * 端点路径
	 */
	private final String path;

	/**
	 * 端点实例
	 */
	private final Endpoint endpoint;

	/**
	 * 支持的子协议列表
	 */
	private List<String> protocols = new ArrayList<>();


	/**
	 * 构造函数，传入路径和 {@code javax.websocket.Endpoint} 实例。
	 *
	 * @param path     端点路径
	 * @param endpoint 端点实例
	 */
	public DefaultServerEndpointConfig(String path, Endpoint endpoint) {
		Assert.hasText(path, "path must not be empty");
		Assert.notNull(endpoint, "endpoint must not be null");
		this.path = path;
		this.endpoint = endpoint;
	}

	/**
	 * 获取编码器列表。
	 *
	 * @return 返回空的编码器列表
	 */
	@Override
	public List<Class<? extends Encoder>> getEncoders() {
		return new ArrayList<>();
	}

	/**
	 * 获取解码器列表。
	 *
	 * @return 返回空的解码器列表
	 */
	@Override
	public List<Class<? extends Decoder>> getDecoders() {
		return new ArrayList<>();
	}

	/**
	 * 获取用户属性映射。
	 *
	 * @return 返回空的用户属性映射
	 */
	@Override
	public Map<String, Object> getUserProperties() {
		return new HashMap<>();
	}

	/**
	 * 获取端点类。
	 *
	 * @return 返回当前端点实例的类
	 */
	@Override
	public Class<?> getEndpointClass() {
		return this.endpoint.getClass();
	}

	/**
	 * 获取路径。
	 *
	 * @return 返回端点路径
	 */
	@Override
	public String getPath() {
		return this.path;
	}

	/**
	 * 设置子协议列表。
	 *
	 * @param protocols 子协议列表
	 */
	public void setSubprotocols(List<String> protocols) {
		this.protocols = protocols;
	}

	/**
	 * 获取子协议列表。
	 *
	 * @return 返回子协议列表
	 */
	@Override
	public List<String> getSubprotocols() {
		return this.protocols;
	}

	/**
	 * 获取扩展列表。
	 *
	 * @return 返回空的扩展列表
	 */
	@Override
	public List<Extension> getExtensions() {
		return new ArrayList<>();
	}

	/**
	 * 获取配置器。
	 *
	 * @return 返回当前实例作为配置器
	 */
	@Override
	public Configurator getConfigurator() {
		return this;
	}

	/**
	 * 获取端点实例。
	 *
	 * @param endpointClass 端点类的类型
	 * @param <T>           端点类的泛型
	 * @return 返回当前端点实例
	 * @throws InstantiationException 如果无法实例化端点类，则抛出此异常
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
		return (T) this.endpoint;
	}

	/**
	 * 返回描述此配置的字符串。
	 *
	 * @return 返回描述信息，包含路径和端点类信息
	 */
	@Override
	public String toString() {
		return "DefaultServerEndpointConfig for path '" + getPath() + "': " + getEndpointClass();
	}

}
