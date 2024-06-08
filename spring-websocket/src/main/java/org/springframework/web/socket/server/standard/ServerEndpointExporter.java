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

package org.springframework.web.socket.server.standard;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationObjectSupport;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.*;

/**
 * 检测 {@link javax.websocket.server.ServerEndpointConfig} 类型的 bean 并注册到标准的 Java WebSocket 运行时中。
 * 还检测带有 {@link ServerEndpoint} 注解的 bean，并注册它们。虽然不是必需的，但是带有注解的端点可能应该将其 {@code configurator} 属性设置为 {@link SpringConfigurator}。
 *
 * <p>当在 Spring 配置中声明此类时使用时，应该可以通过在 {@code web.xml} 中使用 {@code <absolute-ordering>} 元素关闭 Servlet 容器对 WebSocket 端点的扫描。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see ServerEndpointRegistration
 * @see SpringConfigurator
 * @see ServletServerContainerFactoryBean
 * @since 4.0
 */
public class ServerEndpointExporter extends WebApplicationObjectSupport
		implements InitializingBean, SmartInitializingSingleton {
	/**
	 * 带注释的端点类
	 */
	@Nullable
	private List<Class<?>> annotatedEndpointClasses;

	/**
	 * 服务器容器
	 */
	@Nullable
	private ServerContainer serverContainer;


	/**
	 * 明确列出应在启动时注册的带注解的端点类型。如果您希望关闭 Servlet 容器对端点的扫描，则可以这样做，
	 * 它会遍历所有第三方 jar 包，并依赖于 Spring 配置。
	 *
	 * @param annotatedEndpointClasses {@link ServerEndpoint} 注解类型
	 */
	public void setAnnotatedEndpointClasses(Class<?>... annotatedEndpointClasses) {
		this.annotatedEndpointClasses = Arrays.asList(annotatedEndpointClasses);
	}

	/**
	 * 设置要用于端点注册的 JSR-356 {@link ServerContainer}。
	 * 如果未设置，则容器将通过 {@code ServletContext} 进行检索。
	 */
	public void setServerContainer(@Nullable ServerContainer serverContainer) {
		this.serverContainer = serverContainer;
	}

	/**
	 * 返回用于端点注册的 JSR-356 {@link ServerContainer}。
	 */
	@Nullable
	protected ServerContainer getServerContainer() {
		return this.serverContainer;
	}

	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (this.serverContainer == null) {
			// 如果服务器容器不存在，则尝试从 ServletContext 获取
			this.serverContainer =
					(ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");
		}
	}

	@Override
	protected boolean isContextRequired() {
		return false;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(getServerContainer() != null, "javax.websocket.server.ServerContainer not available");
	}

	@Override
	public void afterSingletonsInstantiated() {
		registerEndpoints();
	}


	/**
	 * 实际注册端点。由 {@link #afterSingletonsInstantiated()} 调用。
	 */
	protected void registerEndpoints() {
		// 创建用于存储端点类的集合
		Set<Class<?>> endpointClasses = new LinkedHashSet<>();

		// 如果存在使用注解声明的端点类，则添加到集合中
		if (this.annotatedEndpointClasses != null) {
			endpointClasses.addAll(this.annotatedEndpointClasses);
		}

		// 获取应用程序上下文
		ApplicationContext context = getApplicationContext();

		// 如果应用程序上下文不为空
		if (context != null) {
			// 获取标注了 @ServerEndpoint 注解的 bean 名称数组
			String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
			// 遍历 bean 名称数组
			for (String beanName : endpointBeanNames) {
				// 将 bean 对应的类添加到集合中
				endpointClasses.add(context.getType(beanName));
			}
		}

		// 遍历端点类集合，逐个注册端点
		for (Class<?> endpointClass : endpointClasses) {
			registerEndpoint(endpointClass);
		}

		// 再次检查应用程序上下文是否为空
		if (context != null) {
			// 获取所有的 服务器端点配置 bean
			Map<String, ServerEndpointConfig> endpointConfigMap = context.getBeansOfType(ServerEndpointConfig.class);
			// 遍历注册每个端点
			for (ServerEndpointConfig endpointConfig : endpointConfigMap.values()) {
				// 注册端点配置
				registerEndpoint(endpointConfig);
			}
		}
	}

	private void registerEndpoint(Class<?> endpointClass) {
		// 获取服务器容器
		ServerContainer serverContainer = getServerContainer();
		Assert.state(serverContainer != null,
				"No ServerContainer set. Most likely the server's own WebSocket ServletContainerInitializer " +
				"has not run yet. Was the Spring ApplicationContext refreshed through a " +
				"org.springframework.web.context.ContextLoaderListener, " +
				"i.e. after the ServletContext has been fully initialized?");
		try {
			// 如果日志级别为调试，则记录正在注册的 @ServerEndpoint 类
			if (logger.isDebugEnabled()) {
				logger.debug("Registering @ServerEndpoint class: " + endpointClass);
			}
			// 向服务器容器注册端点类
			serverContainer.addEndpoint(endpointClass);
		} catch (DeploymentException ex) {
			// 捕获部署异常，并抛出 IllegalStateException 异常
			throw new IllegalStateException("Failed to register @ServerEndpoint class: " + endpointClass, ex);
		}
	}

	private void registerEndpoint(ServerEndpointConfig endpointConfig) {
		// 获取服务器容器
		ServerContainer serverContainer = getServerContainer();
		Assert.state(serverContainer != null, "No ServerContainer set");
		try {
			// 如果日志级别为调试，则记录正在注册的 服务器端点配置
			if (logger.isDebugEnabled()) {
				logger.debug("Registering ServerEndpointConfig: " + endpointConfig);
			}
			// 向服务器容器注册端点配置
			serverContainer.addEndpoint(endpointConfig);
		} catch (DeploymentException ex) {
			// 捕获部署异常，并抛出 IllegalStateException 异常
			throw new IllegalStateException("Failed to register ServerEndpointConfig: " + endpointConfig, ex);
		}
	}

}
