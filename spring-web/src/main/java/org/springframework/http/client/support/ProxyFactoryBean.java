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

package org.springframework.http.client.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

/**
 * {@link FactoryBean}，用于创建 {@link Proxy java.net.Proxy}。
 *
 * @author Arjen Poutsma
 * @see FactoryBean
 * @see Proxy
 * @since 3.0.4
 */
public class ProxyFactoryBean implements FactoryBean<Proxy>, InitializingBean {
	/**
	 * 代理类型
	 */
	private Proxy.Type type = Proxy.Type.HTTP;

	/**
	 * 主机名称
	 */
	@Nullable
	private String hostname;

	/**
	 * 端口号
	 */
	private int port = -1;

	/**
	 * 代理
	 */
	@Nullable
	private Proxy proxy;


	/**
	 * 设置代理类型。
	 * <p>默认为 {@link java.net.Proxy.Type#HTTP}。
	 *
	 * @param type 代理类型
	 */
	public void setType(Proxy.Type type) {
		this.type = type;
	}

	/**
	 * 设置代理主机名。
	 *
	 * @param hostname 代理主机名
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * 设置代理端口。
	 *
	 * @param port 代理端口
	 */
	public void setPort(int port) {
		this.port = port;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.type, "Property 'type' is required");
		Assert.notNull(this.hostname, "Property 'hostname' is required");
		// 如果端口不在有效范围内
		if (this.port < 0 || this.port > 65535) {
			// 抛出参数异常
			throw new IllegalArgumentException("Property 'port' value out of range: " + this.port);
		}

		// 创建 SocketAddress 对象，指定主机名和端口号
		SocketAddress socketAddress = new InetSocketAddress(this.hostname, this.port);
		// 创建 Proxy 对象，指定代理类型和 SocketAddress
		this.proxy = new Proxy(this.type, socketAddress);
	}


	@Override
	@Nullable
	public Proxy getObject() {
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return Proxy.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
