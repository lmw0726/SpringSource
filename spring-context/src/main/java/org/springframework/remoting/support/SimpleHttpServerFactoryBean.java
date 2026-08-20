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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 基于 Sun JRE 1.6 中内置的 HTTP 服务器创建简单 HTTP 服务器的
 * {@link org.springframework.beans.factory.FactoryBean}。
 * 在初始化时启动 HTTP 服务器，在销毁时停止它。
 * 公开生成的 {@link com.sun.net.httpserver.HttpServer} 对象。
 *
 * <p>允许为特定的 {@link #setContexts 上下文路径}注册
 * {@link com.sun.net.httpserver.HttpHandler HttpHandler}。
 * 或者，也可以在 {@link com.sun.net.httpserver.HttpServer} 本身上
 * 以编程方式注册此类特定于上下文的处理器。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 2.5.1
 * @see #setPort
 * @see #setContexts
 * @deprecated 从 Spring Framework 5.1 开始弃用，建议使用内嵌的 Tomcat/Jetty/Undertow
 */
@Deprecated
@org.springframework.lang.UsesSunHttpServer
public class SimpleHttpServerFactoryBean implements FactoryBean<HttpServer>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private int port = 8080;

	private String hostname;

	private int backlog = -1;

	private int shutdownDelay = 0;

	private Executor executor;

	private Map<String, HttpHandler> contexts;

	private List<Filter> filters;

	private Authenticator authenticator;

	private HttpServer server;


	/**
	 * 指定 HTTP 服务器的端口。默认值为 8080。
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 指定 HTTP 服务器要绑定的主机名。默认为 localhost；
	 * 可以用特定的网络地址来覆盖，以绑定到指定地址。
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * 指定 HTTP 服务器的 TCP backlog 值。默认值为 -1，
	 * 表示使用系统默认值。
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * 指定在关闭 HTTP 服务器时等待 HTTP 交换完成的秒数。默认值为 0。
	 */
	public void setShutdownDelay(int shutdownDelay) {
		this.shutdownDelay = shutdownDelay;
	}

	/**
	 * 设置用于分发传入请求的 JDK 并发执行器。
	 * @see com.sun.net.httpserver.HttpServer#setExecutor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * 为特定上下文路径注册 {@link com.sun.net.httpserver.HttpHandler HttpHandler}。
	 * @param contexts 以上下文路径为键、HttpHandler 对象为值的 Map
	 * @see org.springframework.remoting.httpinvoker.SimpleHttpInvokerServiceExporter
	 * @see org.springframework.remoting.caucho.SimpleHessianServiceExporter
	 */
	public void setContexts(Map<String, HttpHandler> contexts) {
		this.contexts = contexts;
	}

	/**
	 * 注册要应用于所有本地注册的 {@link #setContexts 上下文}的通用
	 * {@link com.sun.net.httpserver.Filter 过滤器}。
	 */
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	/**
	 * 注册要应用于所有本地注册的 {@link #setContexts 上下文}的通用
	 * {@link com.sun.net.httpserver.Authenticator 验证器}。
	 */
	public void setAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}


	@Override
	public void afterPropertiesSet() throws IOException {
		InetSocketAddress address = (this.hostname != null ?
				new InetSocketAddress(this.hostname, this.port) : new InetSocketAddress(this.port));
		this.server = HttpServer.create(address, this.backlog);
		if (this.executor != null) {
			this.server.setExecutor(this.executor);
		}
		if (this.contexts != null) {
			this.contexts.forEach((key, context) -> {
				HttpContext httpContext = this.server.createContext(key, context);
				if (this.filters != null) {
					httpContext.getFilters().addAll(this.filters);
				}
				if (this.authenticator != null) {
					httpContext.setAuthenticator(this.authenticator);
				}
			});
		}
		if (logger.isInfoEnabled()) {
			logger.info("Starting HttpServer at address " + address);
		}
		this.server.start();
	}

	@Override
	public HttpServer getObject() {
		return this.server;
	}

	@Override
	public Class<? extends HttpServer> getObjectType() {
		return (this.server != null ? this.server.getClass() : HttpServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() {
		logger.info("Stopping HttpServer");
		this.server.stop(this.shutdownDelay);
	}

}
