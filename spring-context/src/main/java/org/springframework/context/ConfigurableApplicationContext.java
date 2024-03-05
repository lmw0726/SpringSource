/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.Closeable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;

/**
 * SPI 接口，由大多数（如果不是所有）应用程序上下文实现。
 * 提供了配置应用程序上下文的功能，除了 {@link org.springframework.context.ApplicationContext} 接口中的应用程序上下文客户端方法之外。
 *
 * <p>配置和生命周期方法被封装在这里，以避免让它们对 ApplicationContext 客户端代码显而易见。
 * 这些方法只应由启动和关闭代码使用。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 03.11.2003
 */
public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, Closeable {

	/**
	 * 任何数量的这些字符被视为单个 String 值中的多个上下文配置路径之间的分隔符。
	 * @see org.springframework.context.support.AbstractXmlApplicationContext#setConfigLocation
	 * @see org.springframework.web.context.ContextLoader#CONFIG_LOCATION_PARAM
	 * @see org.springframework.web.servlet.FrameworkServlet#setContextConfigLocation
	 */
	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

	/**
	 * 工厂中 ConversionService bean 的名称。
	 * 如果没有提供，则适用默认转换规则。
	 * @since 3.0
	 * @see org.springframework.core.convert.ConversionService
	 */
	String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	/**
	 * 工厂中 LoadTimeWeaver bean 的名称。如果提供了这样的 bean，
	 * 则上下文将使用临时 ClassLoader 进行类型匹配，以便让 LoadTimeWeaver 处理所有实际的 bean 类。
	 * @since 2.5
	 * @see org.springframework.instrument.classloading.LoadTimeWeaver
	 */
	String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

	/**
	 * 工厂中的 Environment bean 的名称。
	 * @since 3.1
	 */
	String ENVIRONMENT_BEAN_NAME = "environment";

	/**
	 * 工厂中的 System 属性 bean 的名称。
	 * @see java.lang.System#getProperties()
	 */
	String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

	/**
	 * 工厂中的 System 环境 bean 的名称。
	 * @see java.lang.System#getenv()
	 */
	String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";

	/**
	 * 工厂中的 ApplicationStartup bean 的名称。
	 * @since 5.3
	 */
	String APPLICATION_STARTUP_BEAN_NAME = "applicationStartup";

	/**
	 * {@linkplain #registerShutdownHook() 注册关闭钩子} 线程的 {@linkplain Thread#getName() 名称}：{@value}。
	 * @since 5.2
	 * @see #registerShutdownHook()
	 */
	String SHUTDOWN_HOOK_THREAD_NAME = "SpringContextShutdownHook";


	/**
	 * 设置此应用程序上下文的唯一 id。
	 * @since 3.0
	 */
	void setId(String id);

	/**
	 * 设置此应用程序上下文的父级。
	 * <p>请注意，父级不应更改：只有在此类的对象创建时没有可用时才能在构造函数外部设置它，
	 * 例如在 WebApplicationContext 设置中。
	 * @param parent 父上下文
	 * @see org.springframework.web.context.ConfigurableWebApplicationContext
	 */
	void setParent(@Nullable ApplicationContext parent);

	/**
	 * 为此应用程序上下文设置环境。
	 * @param environment 新环境
	 * @since 3.1
	 */
	void setEnvironment(ConfigurableEnvironment environment);

	/**
	 * 以可配置的形式返回此应用程序上下文的环境。
	 * @since 3.1
	 */
	@Override
	ConfigurableEnvironment getEnvironment();

	/**
	 * 为此应用程序上下文设置 {@link ApplicationStartup}。
	 * <p>这允许应用程序上下文在启动期间记录指标。
	 * @param applicationStartup 新上下文事件工厂
	 * @since 5.3
	 */
	void setApplicationStartup(ApplicationStartup applicationStartup);

	/**
	 * 返回此应用程序上下文的 {@link ApplicationStartup}。
	 * @since 5.3
	 */
	ApplicationStartup getApplicationStartup();

	/**
	 * 添加一个新的 BeanFactoryPostProcessor，它将在刷新时应用于此应用程序上下文的内部 bean 工厂，
	 * 在评估任何 bean 定义之前。在上下文配置期间调用。
	 * @param postProcessor 要注册的工厂处理器
	 */
	void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

	/**
	 * 添加一个新的 ApplicationListener，它将在上下文事件（如上下文刷新和上下文关闭）发生时得到通知。
	 * <p>请注意，此处注册的任何 ApplicationListener 将在刷新时应用，如果上下文尚未处于活动状态，
	 * 或者如果上下文已经处于活动状态，则在当前事件多路广播器上动态应用。
	 * @param listener 要注册的 ApplicationListener
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.springframework.context.event.ContextClosedEvent
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * 指定用于加载类路径资源和 bean 类的 ClassLoader。
	 * <p>此上下文类加载器将传递给内部 bean 工厂。
	 * @since 5.2.7
	 * @see org.springframework.core.io.DefaultResourceLoader#DefaultResourceLoader(ClassLoader)
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setBeanClassLoader
	 */
	void setClassLoader(ClassLoader classLoader);

	/**
	 * 使用此应用程序上下文注册给定的协议解析器，以便处理附加的资源协议。
	 * <p>任何此类解析器都将在此上下文的标准解析规则之前调用。因此，它可能还会覆盖任何默认规则。
	 * @since 4.3
	 */
	void addProtocolResolver(ProtocolResolver resolver);

	/**
	 * 加载或刷新配置的持久表示形式，该形式可能来自基于 Java 的配置、XML 文件、属性文件、关系数据库模式或其他格式。
	 * <p>由于这是一个启动方法，如果失败，它应该销毁已创建的所有单例，以避免悬挂资源。
	 * 换句话说，在调用此方法之后，将实例化所有单例或不实例化所有单例。
	 * @throws BeansException 如果无法初始化 bean 工厂
	 * @throws IllegalStateException 如果已经初始化且不支持多次刷新尝试
	 */
	void refresh() throws BeansException, IllegalStateException;

	/**
	 * 使用 JVM 运行时注册关闭钩子，除非在此时已关闭此上下文，在 JVM 关闭时关闭此上下文。
	 * <p>此方法可以多次调用。每个上下文实例最多只会注册一个关闭钩子。
	 * <p>从 Spring Framework 5.2 开始，关闭钩子线程的 {@linkplain Thread#getName() 名称} 应为 {@link #SHUTDOWN_HOOK_THREAD_NAME}。
	 * @see java.lang.Runtime#addShutdownHook
	 * @see #close()
	 */
	void registerShutdownHook();

	/**
	 * 关闭此应用程序上下文，释放实现可能持有的所有资源和锁。
	 * 这包括销毁所有缓存的单例 bean。
	 * <p>注意：不要调用此方法来关闭父上下文；父上下文具有自己独立的生命周期。
	 * <p>此方法可多次调用而不产生副作用：在已关闭的上下文上多次调用 {@code close} 将被忽略。
	 */
	@Override
	void close();

	/**
	 * 确定此应用程序上下文是否处于活动状态，即至少已刷新一次且尚未关闭。
	 * @return 上下文是否仍处于活动状态
	 * @see #refresh()
	 * @see #close()
	 * @see #getBeanFactory()
	 */
	boolean isActive();

	/**
	 * 返回此应用程序上下文的内部 bean 工厂。
	 * 可用于访问底层工厂的特定功能。
	 * <p>注意：不要使用此方法来后处理 bean 工厂；单例 bean 在被触摸之前已经被实例化了。
	 * 使用 BeanFactoryPostProcessor 来拦截 BeanFactory 设置过程中的 BeanFactory。
	 * <p>通常情况下，此内部工厂只有在上下文处于活动状态时才可访问，即在 {@link #refresh()} 和 {@link #close()} 之间。
	 * 可以使用 {@link #isActive()} 标志来检查上下文是否处于适当的状态。
	 * @return 底层的 bean 工厂
	 * @throws IllegalStateException 如果上下文不持有内部 bean 工厂（通常是如果尚未调用 {@link #refresh()} 或已调用 {@link #close()}）
	 * @see #isActive()
	 * @see #refresh()
	 * @see #close()
	 * @see #addBeanFactoryPostProcessor
	 */
	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}
