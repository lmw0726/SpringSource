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

package org.springframework.web.servlet.view.tiles3;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.TilesContainer;
import org.apache.tiles.TilesException;
import org.apache.tiles.definition.DefinitionsFactory;
import org.apache.tiles.definition.DefinitionsReader;
import org.apache.tiles.definition.dao.BaseLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.dao.CachingLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.digester.DigesterDefinitionsReader;
import org.apache.tiles.el.ELAttributeEvaluator;
import org.apache.tiles.el.ScopeELResolver;
import org.apache.tiles.el.TilesContextBeanELResolver;
import org.apache.tiles.el.TilesContextELResolver;
import org.apache.tiles.evaluator.AttributeEvaluator;
import org.apache.tiles.evaluator.AttributeEvaluatorFactory;
import org.apache.tiles.evaluator.BasicAttributeEvaluatorFactory;
import org.apache.tiles.evaluator.impl.DirectAttributeEvaluator;
import org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory;
import org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer;
import org.apache.tiles.factory.AbstractTilesContainerFactory;
import org.apache.tiles.factory.BasicTilesContainerFactory;
import org.apache.tiles.impl.mgmt.CachingTilesContainer;
import org.apache.tiles.locale.LocaleResolver;
import org.apache.tiles.preparer.factory.PreparerFactory;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.ApplicationContextAware;
import org.apache.tiles.request.ApplicationResource;
import org.apache.tiles.startup.DefaultTilesInitializer;
import org.apache.tiles.startup.TilesInitializer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ServletContextAware;

import javax.el.*;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 辅助类，用于为Spring Framework配置Tiles 3.x。有关Tiles的更多信息，请参见
 * <a href="https://tiles.apache.org">https://tiles.apache.org</a>
 * ，Tiles基本上是一个用于Web应用程序的模板机制，使用JSP和其他模板引擎。
 *
 * <p>TilesConfigurer简单地配置一个TilesContainer，使用一组包含定义的文件，由{@link TilesView}实例访问。
 * 这是Tiles提供的{@code ServletContextListener}（例如用于{@code web.xml}中的使用的
 * {@link org.apache.tiles.extras.complete.CompleteAutoloadTilesListener}）的Spring基于的替代方法。
 *
 * <p>TilesViews可以由任何{@link org.springframework.web.servlet.ViewResolver}管理。
 * 对于简单的基于约定的视图解析，请考虑使用{@link TilesViewResolver}。
 *
 * <p>典型的TilesConfigurer bean定义如下所示：
 *
 * <pre class="code">
 * &lt;bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles3.TilesConfigurer"&gt;
 *   &lt;property name="definitions"&gt;
 *     &lt;list&gt;
 *       &lt;value&gt;/WEB-INF/defs/general.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/widgets.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/administrator.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/customer.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/templates.xml&lt;/value&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * 列表中的值是包含定义的实际Tiles XML文件。如果未指定列表，则默认为{@code "/WEB-INF/tiles.xml"}。
 *
 * <p>请注意，在Tiles 3中，包含Tiles定义的文件名称中的下划线用于指示语言环境信息，例如：
 *
 * <pre class="code">
 * &lt;bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles3.TilesConfigurer"&gt;
 *   &lt;property name="definitions"&gt;
 *     &lt;list&gt;
 *       &lt;value&gt;/WEB-INF/defs/tiles.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/tiles_fr_FR.xml&lt;/value&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author mick semb wever
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see TilesView
 * @see TilesViewResolver
 * @since 3.2
 */
public class TilesConfigurer implements ServletContextAware, InitializingBean, DisposableBean {

	/**
	 * 是否存在Tiles EL表达式
	 */
	private static final boolean tilesElPresent =
			ClassUtils.isPresent("org.apache.tiles.el.ELAttributeEvaluator", TilesConfigurer.class.getClassLoader());


	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Tiles初始化器
	 */
	@Nullable
	private TilesInitializer tilesInitializer;

	/**
	 * 包含定义的文件列表
	 */
	@Nullable
	private String[] definitions;

	/**
	 * 是否在运行时检查Tiles定义文件以进行刷新
	 */
	private boolean checkRefresh = false;

	/**
	 * 是否验证Tiles XML定义
	 */
	private boolean validateDefinitions = true;

	/**
	 * Bean定义工厂类型
	 */
	@Nullable
	private Class<? extends DefinitionsFactory> definitionsFactoryClass;

	/**
	 * 准备的工厂类型
	 */
	@Nullable
	private Class<? extends PreparerFactory> preparerFactoryClass;

	/**
	 * 设置是否使用MutableTilesContainer（通常是CachingTilesContainer实现）
	 */
	private boolean useMutableTilesContainer = false;

	/**
	 * Servlet上下文
	 */
	@Nullable
	private ServletContext servletContext;


	/**
	 * 使用自定义的TilesInitializer配置Tiles，通常指定为内部bean。
	 * <p>默认值是{@link org.apache.tiles.startup.DefaultTilesInitializer}的变体，
	 * respoecting在此配置器上的"definitions"、"preparerFactoryClass"等属性。
	 * <p><b>注意：指定自定义的TilesInitializer实际上会禁用此配置器上的所有其他bean属性。</b>整个初始化过程
	 * 都由TilesInitializer指定。
	 */
	public void setTilesInitializer(TilesInitializer tilesInitializer) {
		this.tilesInitializer = tilesInitializer;
	}

	/**
	 * 指定是否应用Tiles 3.0的"complete-autoload"配置。
	 * <p>有关complete-autoload模式的详细信息，请参见{@link org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory}。
	 * <p><b>注意：指定complete-autoload模式实际上会禁用此配置器上的所有其他bean属性。</b>整个初始化过程都由
	 * {@link org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer}指定。
	 *
	 * @see org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory
	 * @see org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer
	 */
	public void setCompleteAutoload(boolean completeAutoload) {
		if (completeAutoload) {
			try {
				this.tilesInitializer = new SpringCompleteAutoloadTilesInitializer();
			} catch (Throwable ex) {
				throw new IllegalStateException("Tiles-Extras 3.0 not available", ex);
			}
		} else {
			this.tilesInitializer = null;
		}
	}

	/**
	 * 设置Tiles定义，即包含定义的文件列表。
	 * 默认值是"/WEB-INF/tiles.xml"。
	 */
	public void setDefinitions(String... definitions) {
		this.definitions = definitions;
	}

	/**
	 * 设置是否在运行时检查Tiles定义文件以进行刷新。
	 * 默认值是"false"。
	 */
	public void setCheckRefresh(boolean checkRefresh) {
		this.checkRefresh = checkRefresh;
	}

	/**
	 * 设置是否验证Tiles XML定义。默认值是"true"。
	 */
	public void setValidateDefinitions(boolean validateDefinitions) {
		this.validateDefinitions = validateDefinitions;
	}

	/**
	 * 设置要使用的{@link org.apache.tiles.definition.DefinitionsFactory}实现。
	 * 默认值是{@link org.apache.tiles.definition.UnresolvingLocaleDefinitionsFactory}，
	 * 在定义资源URL上操作。
	 * <p>指定自定义的DefinitionsFactory，例如UrlDefinitionsFactory子类，
	 * 以自定义Tiles Definition对象的创建。请注意，这样的DefinitionsFactory必须能够处理
	 * {@link java.net.URL}源对象，除非您配置了不同的TilesContainerFactory。
	 */
	public void setDefinitionsFactoryClass(Class<? extends DefinitionsFactory> definitionsFactoryClass) {
		this.definitionsFactoryClass = definitionsFactoryClass;
	}

	/**
	 * 设置要使用的{@link org.apache.tiles.preparer.factory.PreparerFactory}实现。
	 * 默认值是{@link org.apache.tiles.preparer.factory.BasicPreparerFactory}，为指定的准备器类创建共享实例。
	 * <p>指定{@link SimpleSpringPreparerFactory}以根据指定的准备器类自动装配
	 * {@link org.apache.tiles.preparer.ViewPreparer}实例，应用Spring的容器回调以及应用
	 * 配置的Spring BeanPostProcessors。如果启用了Spring的上下文范围的注解配置，将自动检测和应用ViewPreparer类中的注解。
	 * <p>指定{@link SpringBeanPreparerFactory}以操作指定的准备器
	 * <i>名称</i>而不是类，从DispatcherServlet的应用程序上下文中获取相应的Spring bean。
	 * 在这种情况下，完整的bean创建过程将由Spring应用程序上下文控制，允许使用作用域bean等。
	 * 请注意，您需要针对每个准备器名称（在您的Tiles定义中使用的名称）定义一个Spring bean定义。
	 *
	 * @see SimpleSpringPreparerFactory
	 * @see SpringBeanPreparerFactory
	 */
	public void setPreparerFactoryClass(Class<? extends PreparerFactory> preparerFactoryClass) {
		this.preparerFactoryClass = preparerFactoryClass;
	}

	/**
	 * 设置是否为此应用程序使用MutableTilesContainer（通常是CachingTilesContainer实现）。默认值为"false"。
	 *
	 * @see org.apache.tiles.mgmt.MutableTilesContainer
	 * @see org.apache.tiles.impl.mgmt.CachingTilesContainer
	 */
	public void setUseMutableTilesContainer(boolean useMutableTilesContainer) {
		this.useMutableTilesContainer = useMutableTilesContainer;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 为此Web应用程序创建和公开TilesContainer，委托给TilesInitializer。
	 *
	 * @throws TilesException 如果设置失败
	 */
	@Override
	public void afterPropertiesSet() throws TilesException {
		Assert.state(this.servletContext != null, "No ServletContext available");
		ApplicationContext preliminaryContext = new SpringWildcardServletTilesApplicationContext(this.servletContext);
		if (this.tilesInitializer == null) {
			// 如果Tiles初始化器为空，则设置为 SpringTilesInitializer
			this.tilesInitializer = new SpringTilesInitializer();
		}
		// 初始化应用上下文
		this.tilesInitializer.initialize(preliminaryContext);
	}

	/**
	 * 从此Web应用程序中删除TilesContainer。
	 *
	 * @throws TilesException 如果清理失败
	 */
	@Override
	public void destroy() throws TilesException {
		if (this.tilesInitializer != null) {
			this.tilesInitializer.destroy();
		}
	}


	private class SpringTilesInitializer extends DefaultTilesInitializer {

		@Override
		protected AbstractTilesContainerFactory createContainerFactory(ApplicationContext context) {
			return new SpringTilesContainerFactory();
		}
	}


	private class SpringTilesContainerFactory extends BasicTilesContainerFactory {

		@Override
		protected TilesContainer createDecoratedContainer(TilesContainer originalContainer, ApplicationContext context) {
			return (useMutableTilesContainer ? new CachingTilesContainer(originalContainer) : originalContainer);
		}

		@Override
		protected List<ApplicationResource> getSources(ApplicationContext applicationContext) {
			if (definitions != null) {
				// 创建结果列表
				List<ApplicationResource> result = new ArrayList<>();

				// 遍历定义列表
				for (String definition : definitions) {
					// 获取定义对应的资源集合
					Collection<ApplicationResource> resources = applicationContext.getResources(definition);
					if (resources != null) {
						// 如果资源集合不为空，则添加到结果列表中
						result.addAll(resources);
					}
				}

				// 返回结果列表
				return result;
			} else {
				// 调用父类获取源的方法
				return super.getSources(applicationContext);
			}
		}

		@Override
		protected BaseLocaleUrlDefinitionDAO instantiateLocaleDefinitionDao(ApplicationContext applicationContext,
																			LocaleResolver resolver) {
			// 实例化基础的本地化 URL 定义 DAO
			BaseLocaleUrlDefinitionDAO dao = super.instantiateLocaleDefinitionDao(applicationContext, resolver);

			// 如果需要检查刷新并且 DAO 是 CachingLocaleUrlDefinitionDAO 的实例
			if (checkRefresh && dao instanceof CachingLocaleUrlDefinitionDAO) {
				// 设置检查刷新标志为 true
				((CachingLocaleUrlDefinitionDAO) dao).setCheckRefresh(true);
			}

			// 返回 DAO
			return dao;
		}

		@Override
		protected DefinitionsReader createDefinitionsReader(ApplicationContext context) {
			// 创建 DigesterDefinitionsReader 实例
			DigesterDefinitionsReader reader = (DigesterDefinitionsReader) super.createDefinitionsReader(context);

			// 设置是否验证定义的标志
			reader.setValidating(validateDefinitions);

			// 返回读取器
			return reader;
		}

		@Override
		protected DefinitionsFactory createDefinitionsFactory(ApplicationContext applicationContext,
															  LocaleResolver resolver) {

			// 如果定义工厂类不为空
			if (definitionsFactoryClass != null) {
				// 实例化定义工厂类
				DefinitionsFactory factory = BeanUtils.instantiateClass(definitionsFactoryClass);
				// 如果定义工厂实现了ApplicationContextAware接口
				if (factory instanceof ApplicationContextAware) {
					// 将应用程序上下文设置到定义工厂中
					((ApplicationContextAware) factory).setApplicationContext(applicationContext);
				}
				// 为定义工厂创建BeanWrapper
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(factory);
				// 如果可以设置localeResolver属性
				if (bw.isWritableProperty("localeResolver")) {
					// 设置localeResolver属性为指定的resolver
					bw.setPropertyValue("localeResolver", resolver);
				}
				// 如果可以设置definitionDAO属性
				if (bw.isWritableProperty("definitionDAO")) {
					// 设置definitionDAO属性为创建的LocaleDefinitionDao
					bw.setPropertyValue("definitionDAO", createLocaleDefinitionDao(applicationContext, resolver));
				}
				// 返回定义工厂实例
				return factory;
			} else {
				// 否则调用父类的方法创建定义工厂
				return super.createDefinitionsFactory(applicationContext, resolver);
			}
		}

		@Override
		protected PreparerFactory createPreparerFactory(ApplicationContext context) {
			// 如果准备工厂类不为空
			if (preparerFactoryClass != null) {
				// 实例化准备工厂类并返回
				return BeanUtils.instantiateClass(preparerFactoryClass);
			} else {
				// 否则调用父类的方法创建准备工厂
				return super.createPreparerFactory(context);
			}
		}

		@Override
		protected LocaleResolver createLocaleResolver(ApplicationContext context) {
			return new SpringLocaleResolver();
		}

		@Override
		protected AttributeEvaluatorFactory createAttributeEvaluatorFactory(ApplicationContext context,
																			LocaleResolver resolver) {
			AttributeEvaluator evaluator;
			// 如果存在Tiles EL表达式且JspFactory的默认工厂不为空
			if (tilesElPresent && JspFactory.getDefaultFactory() != null) {
				// 使用TilesElActivator创建一个评估器
				evaluator = new TilesElActivator().createEvaluator();
			} else {
				// 否则使用DirectAttributeEvaluator创建一个评估器
				evaluator = new DirectAttributeEvaluator();
			}
			// 返回一个BasicAttributeEvaluatorFactory，使用上面创建的评估器
			return new BasicAttributeEvaluatorFactory(evaluator);
		}
	}


	private static class SpringCompleteAutoloadTilesInitializer extends CompleteAutoloadTilesInitializer {

		@Override
		protected AbstractTilesContainerFactory createContainerFactory(ApplicationContext context) {
			return new SpringCompleteAutoloadTilesContainerFactory();
		}
	}


	private static class SpringCompleteAutoloadTilesContainerFactory extends CompleteAutoloadTilesContainerFactory {

		@Override
		protected LocaleResolver createLocaleResolver(ApplicationContext applicationContext) {
			return new SpringLocaleResolver();
		}
	}


	private class TilesElActivator {

		public AttributeEvaluator createEvaluator() {
			// 创建一个ELAttributeEvaluator实例
			ELAttributeEvaluator evaluator = new ELAttributeEvaluator();
			// 设置表达式工厂为JSP应用程序上下文的表达式工厂
			evaluator.setExpressionFactory(
					JspFactory.getDefaultFactory().getJspApplicationContext(servletContext).getExpressionFactory());
			// 设置解析器为CompositeELResolverImpl的实例
			evaluator.setResolver(new CompositeELResolverImpl());
			// 返回创建的评估器实例
			return evaluator;
		}
	}


	private static class CompositeELResolverImpl extends CompositeELResolver {

		public CompositeELResolverImpl() {
			// 添加ScopeELResolver到解析器链中
			add(new ScopeELResolver());
			// 添加TilesContextELResolver到解析器链中，并传入TilesContextBeanELResolver
			add(new TilesContextELResolver(new TilesContextBeanELResolver()));
			// 添加TilesContextBeanELResolver到解析器链中
			add(new TilesContextBeanELResolver());
			// 添加ArrayELResolver到解析器链中，不允许写入
			add(new ArrayELResolver(false));
			// 添加ListELResolver到解析器链中，不允许写入
			add(new ListELResolver(false));
			// 添加MapELResolver到解析器链中，不允许写入
			add(new MapELResolver(false));
			// 添加ResourceBundleELResolver到解析器链中
			add(new ResourceBundleELResolver());
			// 添加BeanELResolver到解析器链中，不允许写入
			add(new BeanELResolver(false));
		}
	}

}
