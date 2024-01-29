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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.*;
import org.springframework.context.event.*;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link org.springframework.context.ApplicationContext} 接口的抽象实现。不强制要求用于配置的存储类型；
 * 只是实现了常见的上下文功能。使用模板方法设计模式，要求具体的子类实现抽象方法。
 *
 * <p>与普通的 BeanFactory 不同，ApplicationContext 应该检测其内部 Bean 工厂中定义的特殊 bean：
 * 因此，这个类自动注册在上下文中作为 bean 定义的
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors}、
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors} 和
 * {@link org.springframework.context.ApplicationListener ApplicationListeners}。
 *
 * <p>一个 {@link org.springframework.context.MessageSource} 也可以作为上下文中的 bean 提供，
 * 名称为 "messageSource"；否则，消息解析将委托给父上下文。此外，一个应用事件的多路广播器
 * 可以作为上下文中的类型为 {@link org.springframework.context.event.ApplicationEventMulticaster}
 * 的 "applicationEventMulticaster" bean 提供；否则，将使用类型为
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} 的默认多路广播器。
 *
 * <p>通过扩展 {@link org.springframework.core.io.DefaultResourceLoader} 来实现资源加载。
 * 因此，将非 URL 资源路径视为类路径资源（支持包含包路径的完整类路径资源名称，例如 "mypackage/myresource.dat"），
 * 除非在子类中重写了 {@link #getResourceByPath} 方法。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 * @since January 21, 2001
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext {

	/**
	 * 在工厂中的 MessageSource bean 的名称。
	 * 如果没有提供，则消息解析将委托给父级。
	 *
	 * @see MessageSource
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * 工厂中的 LifecycleProcessor bean 的名称。
	 * 如果没有提供，则使用 DefaultLifecycleProcessor。
	 *
	 * @see org.springframework.context.LifecycleProcessor
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	/**
	 * 工厂中的 ApplicationEventMulticaster bean 的名称。
	 * 如果没有提供，则使用默认的 SimpleApplicationEventMulticaster。
	 *
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	/**
	 * 由 spring.spel.ignore 系统属性控制的布尔标志，指示 Spring 忽略 SpEL，即不初始化 SpEL 基础结构。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreSpel = SpringProperties.getFlag("spring.spel.ignore");


	static {
		// 更早地加载 ContextClosedEvent 类以避免在 WebLogic 8.1 中关闭应用程序时出现奇怪的类加载器问题。
		ContextClosedEvent.class.getName();
	}

	/**
	 * 该类使用的日志记录器，对子类可见。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 如果存在，用于此上下文的唯一标识符。
	 */
	private String id = ObjectUtils.identityToString(this);

	/**
	 * 显示名称
	 */
	private String displayName = ObjectUtils.identityToString(this);

	/**
	 * 父上下文
	 */
	@Nullable
	private ApplicationContext parent;

	/**
	 * 此上下文使用的环境。
	 */
	@Nullable
	private ConfigurableEnvironment environment;

	/**
	 * 要在刷新时应用的BeanFactoryPostProcessor列表。
	 */
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

	/**
	 * 此上下文启动时的系统时间 (以毫秒为单位)。
	 */
	private long startupDate;

	/**
	 * 表示此上下文当前是否处于活动状态的标志。
	 */
	private final AtomicBoolean active = new AtomicBoolean();

	/**
	 * 表示此上下文是否已经关闭的标志。
	 */
	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * 表示“刷新”和“销毁”操作的同步监视器。
	 */
	private final Object startupShutdownMonitor = new Object();

	/**
	 * JVM关闭挂钩的引用，如果已注册。
	 */
	@Nullable
	private Thread shutdownHook;

	/**
	 * 此上下文使用的ResourcePatternResolver。
	 */
	private ResourcePatternResolver resourcePatternResolver;

	/**
	 * 用于管理此上下文中bean的生命周期的LifecycleProcessor。
	 */
	@Nullable
	private LifecycleProcessor lifecycleProcessor;

	/**
	 * 我们将此接口的实现委托给的MessageSource。
	 */
	@Nullable
	private MessageSource messageSource;

	/**
	 * 事件发布中使用的辅助类。
	 */
	@Nullable
	private ApplicationEventMulticaster applicationEventMulticaster;

	/**
	 * 应用程序启动时的度量。
	 */
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	/**
	 * 静态指定的监听器。
	 */
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

	/**
	 * 在刷新之前注册的本地监听器。
	 */
	@Nullable
	private Set<ApplicationListener<?>> earlyApplicationListeners;

	/**
	 * 在多播器设置之前发布的 ApplicationEvents。
	 */
	@Nullable
	private Set<ApplicationEvent> earlyApplicationEvents;

	/**
	 * 创建一个没有父级的新的 AbstractApplicationContext。
	 */
	public AbstractApplicationContext() {
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	/**
	 * 创建一个具有给定父级上下文的新 AbstractApplicationContext。
	 *
	 * @param parent 父级上下文
	 */
	public AbstractApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * 设置此应用程序上下文的唯一标识。
	 * <p>默认为上下文实例的对象标识，或者如果上下文本身被定义为bean，则为上下文bean的名称。
	 *
	 * @param id 上下文的唯一标识
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	/**
	 * 为此上下文设置友好名称。
	 * 通常在具体上下文实现初始化期间完成。
	 * <p>默认为上下文实例的对象标识。
	 *
	 * @param displayName 显示名称，不能为空
	 */
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	/**
	 * 返回此上下文的友好名称。
	 *
	 * @return 此上下文的显示名称（永不为 {@code null}）
	 */
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * 返回父上下文，如果没有父上下文则返回 {@code null}
	 * （即，此上下文是上下文层次结构的根）。
	 */
	@Override
	@Nullable
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * 设置此应用程序上下文的 {@code Environment}。
	 * <p>默认值由 {@link #createEnvironment()} 确定。用此方法替换默认值是一种选择，
	 * 但也应考虑通过 {@link #getEnvironment()} 进行配置。在任何情况下，这样的修改应在
	 * {@link #refresh()} <em>之前</em> 执行。
	 *
	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * 返回此应用程序上下文的 {@code Environment}，以可配置的形式，以便进行进一步的定制。
	 * <p>如果没有指定，将通过 {@link #createEnvironment()} 初始化默认环境。
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回一个新的 {@link StandardEnvironment}。
	 * <p>子类可以重写此方法以提供自定义的 {@link ConfigurableEnvironment} 实现。
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * 如果已经可用，则返回此上下文的内部 Bean 工厂作为 AutowireCapableBeanFactory。
	 *
	 * @see #getBeanFactory()
	 */
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * 返回此上下文首次加载时的时间戳（毫秒）。
	 */
	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	/**
	 * 将给定的事件发布给所有监听器。
	 * <p>注意：监听器在MessageSource之后初始化，以便能够在监听器实现中访问它。
	 * 因此，MessageSource实现不能发布事件。
	 *
	 * @param event 要发布的事件（可能是特定于应用程序的事件或标准框架事件）
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}

	/**
	 * 将给定的事件发布给所有监听器。
	 * <p>注意：监听器在MessageSource之后初始化，以便能够在监听器实现中访问它。
	 * 因此，MessageSource实现不能发布事件。
	 *
	 * @param event 要发布的事件（可以是ApplicationEvent或要转换为PayloadApplicationEvent的有效载荷对象）
	 */
	@Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

	/**
	 * 将给定的事件发布给所有监听器。
	 *
	 * @param event     要发布的事件（可以是ApplicationEvent或要转换为PayloadApplicationEvent的有效载荷对象）
	 * @param eventType 已解析的事件类型（如果已知）
	 * @since 4.2
	 */
	protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
		Assert.notNull(event, "Event must not be null");

		// 如果必要，将事件装饰为 ApplicationEvent
		ApplicationEvent applicationEvent;
		if (event instanceof ApplicationEvent) {
			applicationEvent = (ApplicationEvent) event;
		} else {
			applicationEvent = new PayloadApplicationEvent<>(this, event);
			if (eventType == null) {
				eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
			}
		}

		// 如果可能，立即进行多播 - 或者在多播器初始化后慢慢地进行
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		} else {
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}

		// 通过父上下文也发布事件...
		if (this.parent != null) {
			if (this.parent instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
			} else {
				this.parent.publishEvent(event);
			}
		}
	}

	/**
	 * 返回上下文使用的内部ApplicationEventMulticaster。
	 *
	 * @return 内部ApplicationEventMulticaster（永远不为null）
	 * @throws IllegalStateException 如果上下文尚未初始化
	 */
	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "applicationStartup should not be null");
		this.applicationStartup = applicationStartup;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * 返回上下文使用的内部LifecycleProcessor。
	 *
	 * @return 内部LifecycleProcessor（永远不为null）
	 * @throws IllegalStateException 如果上下文尚未初始化
	 */
	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " +
					"call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	/**
	 * 返回用于将位置模式解析为Resource实例的ResourcePatternResolver。
	 * 默认值是一个PathMatchingResourcePatternResolver，支持Ant样式的位置模式。
	 * <p>可以在子类中覆盖，以扩展解析策略，例如在Web环境中。
	 * <p><b>在需要解析位置模式时不要调用此方法。</b>而是调用上下文的getResources方法，该方法将委托给ResourcePatternResolver。
	 *
	 * @return 用于此上下文的ResourcePatternResolver
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * 设置此应用程序上下文的父级。
	 * 如果父级不为null，并且其环境是ConfigurableEnvironment的实例，则将父级的环境与此（子级）应用程序上下文的环境{@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment)合并}。
	 *
	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		this.parent = parent;

		// 如果存在父上下文
		if (parent != null) {
			// 获取父上下文的环境
			Environment parentEnvironment = parent.getEnvironment();
			// 如果父上下文的环境是可配置的环境
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				// 合并当前上下文的环境与父上下文的环境
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}

	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}

	/**
	 * 返回将应用于内部BeanFactory的BeanFactoryPostProcessor列表。
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}

	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		this.applicationListeners.add(listener);
	}

	/**
	 * 返回静态指定的ApplicationListeners列表。
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");

			//准备对此上下文进行刷新
			prepareRefresh();

			//告诉子类刷新内部Bean工厂
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			//准备在此上下文中使用Bean工厂
			prepareBeanFactory(beanFactory);

			try {
				//允许在上下文子类中对 bean 工厂进行后处理。
				postProcessBeanFactory(beanFactory);

				StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");
				//调用在上下文中注册为 bean 的工厂处理器。
				invokeBeanFactoryPostProcessors(beanFactory);

				//注册拦截 bean 创建的 bean 处理器。
				registerBeanPostProcessors(beanFactory);
				beanPostProcess.end();

				//为此上下文初始化消息源。
				initMessageSource();

				//为此上下文初始化事件多播器。
				initApplicationEventMulticaster();

				//初始化特定上下文子类中的其他特殊 bean。
				onRefresh();

				//检查侦听器 bean 并注册它们。
				registerListeners();

				//实例化所有剩余的（非懒加载）单例。
				finishBeanFactoryInitialization(beanFactory);

				//最后一步：发布相应的事件。
				finishRefresh();
			} catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				//销毁已经创建的单例以避免悬空资源。
				destroyBeans();

				//重置“活动”标志。
				cancelRefresh(ex);

				//将异常传播给调用者。
				throw ex;
			} finally {
				//重置 Spring 核心中的常见自省缓存，因为我们可能不再需要单例 bean 的元数据......
				resetCommonCaches();
				contextRefresh.end();
			}
		}
	}

	/**
	 * 准备刷新此上下文，设置其启动日期和活动标志，以及执行属性源的任何初始化。
	 */
	protected void prepareRefresh() {
		// 切换为活动状态。
		this.startupDate = System.currentTimeMillis();
		this.closed.set(false);
		this.active.set(true);

		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			} else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		// 初始化上下文环境中的任何占位符属性源。
		initPropertySources();

		// 验证所有标记为必需的属性都是可解析的：
		// 请参见 ConfigurablePropertyResolver#setRequiredProperties
		getEnvironment().validateRequiredProperties();

		// 存储预刷新 ApplicationListeners...
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		} else {
			// 重置本地应用程序监听器为预刷新状态。
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// 允许收集早期的 ApplicationEvents，
		// 一旦多播器可用，就发布它们。
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}

	/**
	 * <p>用实际实例替换任何存根属性源。
	 *
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
	 */
	protected void initPropertySources() {
		// 对于子类：默认情况下不执行任何操作。
	}

	/**
	 * 告诉子类刷新内部的 Bean 工厂。
	 *
	 * @return 新的 BeanFactory 实例
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 */
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		//刷新Bean工厂
		refreshBeanFactory();
		//获取Bean工厂
		return getBeanFactory();
	}

	/**
	 * 配置工厂的标准上下文特性，例如上下文的类加载器和后处理器。
	 *
	 * @param beanFactory 要配置的 BeanFactory
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		//配置工厂的标准上下文特征，例如上下文的 ClassLoader 和后处理器。
		beanFactory.setBeanClassLoader(getClassLoader());
		if (!shouldIgnoreSpel) {
			//设置Bean表达式解析器，这里是初始化Spring EL表达式
			beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		}
		//设置属性编辑注册器
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		//使用上下文回调配置Bean工厂
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		//忽略依赖接口
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationStartupAware.class);

		//BeanFactory 接口未在普通工厂中注册为可解析类型。
		//MessageSource 作为 bean 注册（并为自动装配找到）。
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		//将用于检测内部 bean 的早期后处理器注册为 ApplicationListener。
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		//检测 LoadTimeWeaver 并准备编织（如果找到）。
		if (!NativeDetector.inNativeImage() && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// 为类型匹配设置一个临时类加载器。
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// Register default environment beans.
		//注册默认环境Bean
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		//注册系统属性Bean
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		//注册系统环境Bean
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
		//注册应用程序启动Bean
		if (!beanFactory.containsLocalBean(APPLICATION_STARTUP_BEAN_NAME)) {
			beanFactory.registerSingleton(APPLICATION_STARTUP_BEAN_NAME, getApplicationStartup());
		}
	}

	/**
	 * 在标准初始化后修改应用程序上下文的内部 Bean 工厂。所有 Bean 定义都将被加载，但尚未实例化任何 Bean。
	 * 这允许在某些 ApplicationContext 实现中注册特殊的 BeanPostProcessors 等。
	 *
	 * @param beanFactory 应用程序上下文使用的 Bean 工厂
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	/**
	 * 实例化和调用所有已注册的 BeanFactoryPostProcessor bean，如果给定的话，遵循显式顺序。
	 * <p>必须在单例实例化之前调用。
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {

		//调用 Bean Factory 后处理器
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		//检测 LoadTimeWeaver 并准备编织（如果同时发现）
		//（例如，通过 ConfigurationClassPostProcessor 注册的 @Bean 方法）
		if (!NativeDetector.inNativeImage() && beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			// 如果不是运行在Native Image环境下，临时类加载器为空，且没有bean名称为loadTimeWeaver的bean
			// 添加LoadTimeWeaverAwareProcessor作为Bean后置处理器
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));

			// 设置临时类加载器为ContextTypeMatchClassLoader，该类加载器匹配BeanFactory的类加载器
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

	/**
	 * 实例化并注册所有BeanPostProcessor bean，如果给定则遵循显式顺序。
	 * <p>必须在实例化应用程序bean之前调用。
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	}

	/**
	 * 初始化 MessageSource。
	 * 如果在此上下文中未定义，则使用父级的 MessageSource。
	 */
	protected void initMessageSource() {
		// 获取可配置的可列举的Bean工厂
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();

		// 如果Bean工厂包含名为MESSAGE_SOURCE_BEAN_NAME的本地Bean
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			// 获取MessageSource实例
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);

			// 使MessageSource意识到父级MessageSource
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// 只有在尚未注册父级MessageSource的情况下，才将父级上下文设置为父级MessageSource。
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}

			// 输出日志，显示使用的MessageSource
			if (logger.isTraceEnabled()) {
				logger.trace("Using MessageSource [" + this.messageSource + "]");
			}
		} else {
			// 如果Bean工厂不包含名为MESSAGE_SOURCE_BEAN_NAME的本地Bean

			// 使用空的MessageSource以便能够接受getMessage调用。
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;

			// 在Bean工厂中注册单例的MESSAGE_SOURCE_BEAN_NAME，并将其设置为刚刚创建的MessageSource实例
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);

			// 输出日志，显示未找到MESSAGE_SOURCE_BEAN_NAME的Bean，使用新创建的MessageSource
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * 初始化 ApplicationEventMulticaster。
	 * 如果在上下文中未定义，则使用 SimpleApplicationEventMulticaster。
	 *
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	protected void initApplicationEventMulticaster() {
		// 获取可配置的可列举的Bean工厂
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();

		// 如果Bean工厂包含名为APPLICATION_EVENT_MULTICASTER_BEAN_NAME的本地Bean
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			// 获取ApplicationEventMulticaster实例
			this.applicationEventMulticaster =
					beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);

			// 输出日志，显示使用的ApplicationEventMulticaster
			if (logger.isTraceEnabled()) {
				logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		} else {
			// 如果Bean工厂不包含名为APPLICATION_EVENT_MULTICASTER_BEAN_NAME的本地Bean

			// 创建一个新的SimpleApplicationEventMulticaster，并将其设置为应用程序事件的多路广播器
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);

			// 在Bean工厂中注册单例的APPLICATION_EVENT_MULTICASTER_BEAN_NAME，并将其设置为刚刚创建的ApplicationEventMulticaster实例
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);

			// 输出日志，显示未找到APPLICATION_EVENT_MULTICASTER_BEAN_NAME的Bean，使用新创建的ApplicationEventMulticaster
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
						"[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 初始化 LifecycleProcessor。
	 * 如果在上下文中未定义，则使用 DefaultLifecycleProcessor。
	 *
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	protected void initLifecycleProcessor() {
		// 获取可配置的可列举的Bean工厂
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();

		// 如果Bean工厂包含名为 lifecycleProcessor 的本地Bean
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			// 获取LifecycleProcessor实例
			this.lifecycleProcessor =
					beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);

			// 输出日志，显示使用的LifecycleProcessor
			if (logger.isTraceEnabled()) {
				logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		} else {
			// 如果Bean工厂不包含名为 lifecycleProcessor 的本地Bean

			// 创建一个新的DefaultLifecycleProcessor，并将其设置为Bean工厂
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			defaultProcessor.setBeanFactory(beanFactory);

			// 将刚刚创建的DefaultLifecycleProcessor设置为应用程序上下文的LifecycleProcessor
			this.lifecycleProcessor = defaultProcessor;

			// 在Bean工厂中注册单例的 lifecycleProcessor ，并将其设置为刚刚创建的LifecycleProcessor实例
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);

			// 输出日志，显示未找到lifecycleProcessor的Bean，使用新创建的LifecycleProcessor
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
						"[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 模板方法，可重写以添加特定上下文刷新工作。
	 * 在特殊 bean 初始化之前、单例实例化之前调用。
	 * <p>此实现为空。
	 *
	 * @throws BeansException 如果发生错误
	 * @see #refresh()
	 */
	protected void onRefresh() throws BeansException {
		// 对于子类：默认情况下什么都不做。
	}

	/**
	 * 将实现 ApplicationListener 的 bean 添加为监听器。
	 * 不影响其他可以添加为非 bean 的监听器。
	 */
	protected void registerListeners() {
		// 首先注册静态指定的监听器
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			// 将监听器添加到应用程序事件多播器
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// 不要在这里初始化FactoryBeans：我们需要保持所有常规bean未初始化，以便后处理器可以应用于它们！
		// 获取所有ApplicationListener类型的Bean的名称，包括非懒加载的和懒加载的
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			// 将监听器的Bean名称添加到应用程序事件多播器
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// 现在我们最终有了一个多播器，发布早期的应用程序事件
		// 获取待处理的早期应用程序事件集合，并将早期应用程序事件集合置为空
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null;

		// 如果早期应用程序事件集合不为空，则逐个处理每个早期应用程序事件
		if (!CollectionUtils.isEmpty(earlyEventsToProcess)) {
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				// 使用应用程序事件多播器广播早期应用程序事件
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

	/**
	 * 完成此上下文的 bean 工厂的初始化，初始化所有剩余的单例 bean。
	 */
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// 为此上下文初始化转换服务。
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			// 如果BeanFactory包含指定名称的Bean，并且该Bean的类型与ConversionService.class匹配，
			// 则将ConversionService设置为BeanFactory中的指定Bean
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// 如果BeanFactory没有嵌入式值解析器，注册默认的嵌入式值解析器：
		// 在这一点上，主要用于解析注解属性值。
		if (!beanFactory.hasEmbeddedValueResolver()) {
			// 如果BeanFactory中没有嵌入式值解析器，添加默认的嵌入式值解析器
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// 提前初始化LoadTimeWeaverAware类型的Bean，以便尽早注册其转换器。
		// 获取所有LoadTimeWeaverAware类型的Bean的名称，包括非懒加载的和懒加载的
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			// 获取并实例化LoadTimeWeaverAware类型的Bean
			getBean(weaverAwareName);
		}

		// 停止使用临时的ClassLoader进行类型匹配。
		beanFactory.setTempClassLoader(null);

		// 允许缓存所有bean定义元数据，不希望进一步更改。
		beanFactory.freezeConfiguration();

		// 实例化所有剩余的（非懒加载）单例bean。
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * 完成此上下文的刷新，调用 LifecycleProcessor 的 onRefresh() 方法，并发布 org.springframework.context.event.ContextRefreshedEvent 事件。
	 */
	@SuppressWarnings("deprecation")
	protected void finishRefresh() {
		// 清除上下文级别的资源缓存（例如从扫描中获取的 ASM 元数据）。
		clearResourceCaches();

		// 为此上下文初始化生命周期处理器。
		initLifecycleProcessor();

		// 首先将刷新传播到生命周期处理器。
		getLifecycleProcessor().onRefresh();

		// 发布最终事件。
		publishEvent(new ContextRefreshedEvent(this));

		// 如果活动，则参与 LiveBeansView MBean。
		if (!NativeDetector.inNativeImage()) {
			LiveBeansView.registerApplicationContext(this);
		}
	}

	/**
	 * Cancel this context's refresh attempt, resetting the {@code active} flag
	 * after an exception got thrown.
	 *
	 * @param ex the exception that led to the cancellation
	 */
	protected void cancelRefresh(BeansException ex) {
		this.active.set(false);
	}

	/**
	 * Reset Spring's common reflection metadata caches, in particular the
	 * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
	 * and {@link CachedIntrospectionResults} caches.
	 *
	 * @see ReflectionUtils#clearCache()
	 * @see AnnotationUtils#clearCache()
	 * @see ResolvableType#clearCache()
	 * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
	 * @since 4.2
	 */
	protected void resetCommonCaches() {
		ReflectionUtils.clearCache();
		AnnotationUtils.clearCache();
		ResolvableType.clearCache();
		CachedIntrospectionResults.clearClassLoader(getClassLoader());
	}


	/**
	 * Register a shutdown hook {@linkplain Thread#getName() named}
	 * {@code SpringContextShutdownHook} with the JVM runtime, closing this
	 * context on JVM shutdown unless it has already been closed at that time.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 *
	 * @see Runtime#addShutdownHook
	 * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
	 * @see #close()
	 * @see #doClose()
	 */
	@Override
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// No shutdown hook registered yet.
			this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
				@Override
				public void run() {
					synchronized (startupShutdownMonitor) {
						doClose();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * Callback for destruction of this instance, originally attached
	 * to a {@code DisposableBean} implementation (not anymore in 5.0).
	 * <p>The {@link #close()} method is the native way to shut down
	 * an ApplicationContext, which this method simply delegates to.
	 *
	 * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
	 */
	@Deprecated
	public void destroy() {
		close();
	}

	/**
	 * Close this application context, destroying all beans in its bean factory.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
	 *
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	@Override
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// If we registered a JVM shutdown hook, we don't need it anymore now:
			// We've already explicitly closed the context.
			if (this.shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				} catch (IllegalStateException ex) {
					// ignore - VM is already shutting down
				}
			}
		}
	}

	/**
	 * Actually performs context closing: publishes a ContextClosedEvent and
	 * destroys the singletons in the bean factory of this application context.
	 * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
	 *
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see #destroyBeans()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	@SuppressWarnings("deprecation")
	protected void doClose() {
		// Check whether an actual close attempt is necessary...
		if (this.active.get() && this.closed.compareAndSet(false, true)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this);
			}

			if (!NativeDetector.inNativeImage()) {
				LiveBeansView.unregisterApplicationContext(this);
			}

			try {
				// Publish shutdown event.
				publishEvent(new ContextClosedEvent(this));
			} catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// Stop all Lifecycle beans, to avoid delays during individual destruction.
			if (this.lifecycleProcessor != null) {
				try {
					this.lifecycleProcessor.onClose();
				} catch (Throwable ex) {
					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
				}
			}

			// Destroy all cached singletons in the context's BeanFactory.
			destroyBeans();

			// Close the state of this context itself.
			closeBeanFactory();

			// Let subclasses do some final clean-up if they wish...
			onClose();

			// Reset local application listeners to pre-refresh state.
			if (this.earlyApplicationListeners != null) {
				this.applicationListeners.clear();
				this.applicationListeners.addAll(this.earlyApplicationListeners);
			}

			// Switch to inactive.
			this.active.set(false);
		}
	}

	/**
	 * Template method for destroying all beans that this context manages.
	 * The default implementation destroy all cached singletons in this context,
	 * invoking {@code DisposableBean.destroy()} and/or the specified
	 * "destroy-method".
	 * <p>Can be overridden to add context-specific bean destruction steps
	 * right before or right after standard singleton destruction,
	 * while the context's BeanFactory is still active.
	 *
	 * @see #getBeanFactory()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
	 */
	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}

	/**
	 * Template method which can be overridden to add context-specific shutdown work.
	 * The default implementation is empty.
	 * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
	 * this context's BeanFactory has been closed. If custom shutdown logic
	 * needs to execute while the BeanFactory is still active, override
	 * the {@link #destroyBeans()} method instead.
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	@Override
	public boolean isActive() {
		return this.active.get();
	}

	/**
	 * Assert that this context's BeanFactory is currently active,
	 * throwing an {@link IllegalStateException} if it isn't.
	 * <p>Invoked by all {@link BeanFactory} delegation methods that depend
	 * on an active context, i.e. in particular all bean accessor methods.
	 * <p>The default implementation checks the {@link #isActive() 'active'} status
	 * of this context overall. May be overridden for more specific checks, or for a
	 * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
	 */
	protected void assertBeanFactoryActive() {
		if (!this.active.get()) {
			if (this.closed.get()) {
				throw new IllegalStateException(getDisplayName() + " has been closed already");
			} else {
				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, requiredType);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType, args);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name, allowFactoryBeanInit);
	}

	@Override
	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType, allowFactoryBeanInit);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	@Override
	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * Return the internal bean factory of the parent context if it implements
	 * ConfigurableApplicationContext; else, return the parent context itself.
	 *
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	@Nullable
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	@Override
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * Return the internal MessageSource used by the context.
	 *
	 * @return the internal MessageSource (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	@Nullable
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext ?
				((AbstractApplicationContext) getParent()).messageSource : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() {
		getLifecycleProcessor().start();
		publishEvent(new ContextStartedEvent(this));
	}

	@Override
	public void stop() {
		getLifecycleProcessor().stop();
		publishEvent(new ContextStoppedEvent(this));
	}

	@Override
	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Subclasses must implement this method to perform the actual configuration load.
	 * The method is invoked by {@link #refresh()} before any other initialization work.
	 * <p>A subclass will either create a new bean factory and hold a reference to it,
	 * or return a single BeanFactory instance that it holds. In the latter case, it will
	 * usually throw an IllegalStateException if refreshing the context more than once.
	 *
	 * @throws BeansException        if initialization of the bean factory failed
	 * @throws IllegalStateException if already initialized and multiple refresh
	 *                               attempts are not supported
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * Subclasses must implement this method to release their internal bean factory.
	 * This method gets invoked by {@link #close()} after all other shutdown work.
	 * <p>Should never throw an exception but rather log shutdown failures.
	 */
	protected abstract void closeBeanFactory();

	/**
	 * Subclasses must return their internal bean factory here. They should implement the
	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
	 * <p>Note: Subclasses should check whether the context is still active before
	 * returning the internal bean factory. The internal factory should generally be
	 * considered unavailable once the context has been closed.
	 *
	 * @return this application context's internal bean factory (never {@code null})
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 *                               (usually if {@link #refresh()} has never been called) or if the context has been
	 *                               closed already
	 * @see #refreshBeanFactory()
	 * @see #closeBeanFactory()
	 */
	@Override
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * Return information about this context.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(", started on ").append(new Date(getStartupDate()));
		ApplicationContext parent = getParent();
		if (parent != null) {
			sb.append(", parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}

}
