/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.validation.beanvalidation;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import javax.validation.*;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * 这是Spring应用上下文中{@code javax.validation}（JSR-303）配置的核心类：它引导{@code javax.validation.ValidationFactory}，
 * 并通过Spring的{@link org.springframework.validation.Validator}接口、JSR-303的{@link javax.validation.Validator}
 * 接口以及{@link javax.validation.ValidatorFactory}接口本身暴露该工厂。
 *
 * <p>当通过Spring或JSR-303的Validator接口与此bean的实例交互时，您实际上是在与底层ValidatorFactory的默认Validator通信。
 * 这非常方便，因为您不必再对工厂执行额外的调用，假设您几乎总是使用默认Validator。这也可以直接注入到任何
 * 类型为{@link org.springframework.validation.Validator}的目标依赖中！
 *
 * <p><b>从Spring 5.0开始，此类需要Bean Validation 1.1+，并对Hibernate Validator 5.x提供特殊支持
 * </b>（参见{@link #setValidationMessageSource}）。此类也与Bean Validation 2.0和Hibernate Validator 6.0运行时兼容，
 * 但有一个特别注意：如果您想调用BV 2.0的{@code getClockProvider()}方法，请通过
 * {@code #unwrap(ValidatorFactory.class)}获取原生{@code ValidatorFactory}，然后在返回的原生引用上调用
 * {@code getClockProvider()}方法。
 *
 * <p>此类也被Spring的MVC配置命名空间使用，以防{@code javax.validation} API存在但未配置显式Validator的情况。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see javax.validation.ValidatorFactory
 * @see javax.validation.Validator
 * @see javax.validation.Validation#buildDefaultValidatorFactory()
 * @see javax.validation.ValidatorFactory#getValidator()
 */
public class LocalValidatorFactoryBean extends SpringValidatorAdapter
		implements ValidatorFactory, ApplicationContextAware, InitializingBean, DisposableBean {

	@SuppressWarnings("rawtypes")
	@Nullable
	private Class providerClass;

	@Nullable
	private ValidationProviderResolver validationProviderResolver;

	@Nullable
	private MessageInterpolator messageInterpolator;

	@Nullable
	private TraversableResolver traversableResolver;

	@Nullable
	private ConstraintValidatorFactory constraintValidatorFactory;

	@Nullable
	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	@Nullable
	private Resource[] mappingLocations;

	private final Map<String, String> validationPropertyMap = new HashMap<>();

	@Nullable
	private Consumer<Configuration<?>> configurationInitializer;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private ValidatorFactory validatorFactory;


	/**
	 * 指定所需的提供程序类（如果有的话）。
	 * <p>如果未指定，将使用JSR-303的默认搜索机制。
	 * @see javax.validation.Validation#byProvider(Class)
	 * @see javax.validation.Validation#byDefaultProvider()
	 */
	@SuppressWarnings("rawtypes")
	public void setProviderClass(Class providerClass) {
		this.providerClass = providerClass;
	}

	/**
	 * 指定用于引导所选提供程序的JSR-303 {@link ValidationProviderResolver}，
	 * 作为{@code META-INF}驱动解析的替代方案。
	 * @since 4.3
	 */
	public void setValidationProviderResolver(ValidationProviderResolver validationProviderResolver) {
		this.validationProviderResolver = validationProviderResolver;
	}

	/**
	 * 指定用于此ValidatorFactory及其暴露的默认Validator的自定义MessageInterpolator。
	 */
	public void setMessageInterpolator(MessageInterpolator messageInterpolator) {
		this.messageInterpolator = messageInterpolator;
	}

	/**
	 * 指定一个自定义的 Spring MessageSource 用于解析验证消息，而不是依赖于类路径中 JSR-303 默认的 "ValidationMessages.properties" bundle。
	 * 这可以是指向 Spring 上下文的共享 "messageSource" bean，也可以是专门为验证目的设置的特殊 MessageSource。
	 * <p><b>注意：</b>此功能需要类路径上存在 Hibernate Validator 4.3 或更高版本。您仍然可以使用不同的验证提供程序，
	 * 但在配置期间必须可以访问 Hibernate Validator 的 {@link ResourceBundleMessageInterpolator} 类。
	 * <p>只能指定此属性或 {@link #setMessageInterpolator "messageInterpolator"} 中的一个，不能同时指定两者。
	 * 如果您想构建自定义的 MessageInterpolator，请考虑从 Hibernate Validator 的 {@link ResourceBundleMessageInterpolator} 派生，
	 * 并在构造插值器时传递一个基于 Spring 的 {@code ResourceBundleLocator}。
	 * <p>为了仍然解析 Hibernate 的默认验证消息，您的 {@link MessageSource} 必须配置为可选解析（通常是默认的）。
	 * 特别地，这里指定的 {@code MessageSource} 实例不应用 {@link org.springframework.context.support.AbstractMessageSource#setUseCodeAsDefaultMessage
	 * "useCodeAsDefaultMessage"} 行为。请仔细检查您的设置。
	 * @see ResourceBundleMessageInterpolator
	 */
	public void setValidationMessageSource(MessageSource messageSource) {
		this.messageInterpolator = HibernateValidatorDelegate.buildMessageInterpolator(messageSource);
	}

	/**
	 * 指定用于此ValidatorFactory及其暴露的默认Validator的自定义TraversableResolver。
	 */
	public void setTraversableResolver(TraversableResolver traversableResolver) {
		this.traversableResolver = traversableResolver;
	}

	/**
	 * 指定用于此ValidatorFactory的自定义ConstraintValidatorFactory。
	 * <p>默认是{@link SpringConstraintValidatorFactory}，它委托给包含的ApplicationContext来创建
	 * 自动装配的ConstraintValidator实例。
	 */
	public void setConstraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
		this.constraintValidatorFactory = constraintValidatorFactory;
	}

	/**
	 * 设置用于解析方法和构造函数参数名称的ParameterNameDiscoverer（如果消息插值需要）。
	 * <p>默认是{@link org.springframework.core.DefaultParameterNameDiscoverer}。
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 指定要从何处加载XML约束映射文件的资源位置（如果有的话）。
	 */
	public void setMappingLocations(Resource... mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	/**
	 * 指定要传递给验证提供程序的bean验证属性。
	 * <p>可以使用String "value"（通过PropertiesEditor解析）或XML bean定义中的"props"元素进行填充。
	 * @see javax.validation.Configuration#addProperty(String, String)
	 */
	public void setValidationProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.validationPropertyMap);
	}

	/**
	 * 以Map形式指定要传递给验证提供程序的bean验证属性。
	 * <p>可以使用XML bean定义中的"map"或"props"元素进行填充。
	 * @see javax.validation.Configuration#addProperty(String, String)
	 */
	public void setValidationPropertyMap(@Nullable Map<String, String> validationProperties) {
		if (validationProperties != null) {
			this.validationPropertyMap.putAll(validationProperties);
		}
	}

	/**
	 * 允许通过Map访问要传递给验证提供程序的bean验证属性，
	 * 可选择添加或覆盖特定条目。
	 * <p>对于直接指定条目很有用，例如通过"validationPropertyMap[myKey]"。
	 */
	public Map<String, String> getValidationPropertyMap() {
		return this.validationPropertyMap;
	}

	/**
	 * 指定用于自定义Bean Validation {@code Configuration}实例的回调，
	 * 作为在自定义{@code LocalValidatorFactoryBean}子类中覆盖
	 * {@link #postProcessConfiguration(Configuration)}方法的替代方案。
	 * <p>这为应用程序目的提供了方便的自定义。基础结构扩展可以继续覆盖
	 * {@link #postProcessConfiguration}模板方法。
	 * @since 5.3.19
	 */
	public void setConfigurationInitializer(Consumer<Configuration<?>> configurationInitializer) {
		this.configurationInitializer = configurationInitializer;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void afterPropertiesSet() {
		Configuration<?> configuration;
		if (this.providerClass != null) {
			ProviderSpecificBootstrap bootstrap = Validation.byProvider(this.providerClass);
			if (this.validationProviderResolver != null) {
				bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
			}
			configuration = bootstrap.configure();
		}
		else {
			GenericBootstrap bootstrap = Validation.byDefaultProvider();
			if (this.validationProviderResolver != null) {
				bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
			}
			configuration = bootstrap.configure();
		}

		// 尝试使用Hibernate Validator 5.2的externalClassLoader(ClassLoader)方法
		if (this.applicationContext != null) {
			try {
				Method eclMethod = configuration.getClass().getMethod("externalClassLoader", ClassLoader.class);
				ReflectionUtils.invokeMethod(eclMethod, configuration, this.applicationContext.getClassLoader());
			}
			catch (NoSuchMethodException ex) {
				// 忽略 - 没有Hibernate Validator 5.2+或类似提供程序
			}
		}

		MessageInterpolator targetInterpolator = this.messageInterpolator;
		if (targetInterpolator == null) {
			targetInterpolator = configuration.getDefaultMessageInterpolator();
		}
		configuration.messageInterpolator(new LocaleContextMessageInterpolator(targetInterpolator));

		if (this.traversableResolver != null) {
			configuration.traversableResolver(this.traversableResolver);
		}

		ConstraintValidatorFactory targetConstraintValidatorFactory = this.constraintValidatorFactory;
		if (targetConstraintValidatorFactory == null && this.applicationContext != null) {
			targetConstraintValidatorFactory =
					new SpringConstraintValidatorFactory(this.applicationContext.getAutowireCapableBeanFactory());
		}
		if (targetConstraintValidatorFactory != null) {
			configuration.constraintValidatorFactory(targetConstraintValidatorFactory);
		}

		if (this.parameterNameDiscoverer != null) {
			configureParameterNameProvider(this.parameterNameDiscoverer, configuration);
		}

		List<InputStream> mappingStreams = null;
		if (this.mappingLocations != null) {
			mappingStreams = new ArrayList<>(this.mappingLocations.length);
			for (Resource location : this.mappingLocations) {
				try {
					InputStream stream = location.getInputStream();
					mappingStreams.add(stream);
					configuration.addMapping(stream);
				}
				catch (IOException ex) {
					closeMappingStreams(mappingStreams);
					throw new IllegalStateException("Cannot read mapping resource: " + location);
				}
			}
		}

		this.validationPropertyMap.forEach(configuration::addProperty);

		// 允许在实际构建ValidatorFactory之前进行自定义后处理。
		if (this.configurationInitializer != null) {
			this.configurationInitializer.accept(configuration);
		}
		postProcessConfiguration(configuration);

		try {
			this.validatorFactory = configuration.buildValidatorFactory();
			setTargetValidator(this.validatorFactory.getValidator());
		}
		finally {
			closeMappingStreams(mappingStreams);
		}
	}

	private void configureParameterNameProvider(ParameterNameDiscoverer discoverer, Configuration<?> configuration) {
		final ParameterNameProvider defaultProvider = configuration.getDefaultParameterNameProvider();
		configuration.parameterNameProvider(new ParameterNameProvider() {
			@Override
			public List<String> getParameterNames(Constructor<?> constructor) {
				String[] paramNames = discoverer.getParameterNames(constructor);
				return (paramNames != null ? Arrays.asList(paramNames) :
						defaultProvider.getParameterNames(constructor));
			}
			@Override
			public List<String> getParameterNames(Method method) {
				String[] paramNames = discoverer.getParameterNames(method);
				return (paramNames != null ? Arrays.asList(paramNames) :
						defaultProvider.getParameterNames(method));
			}
		});
	}

	private void closeMappingStreams(@Nullable List<InputStream> mappingStreams){
		if (!CollectionUtils.isEmpty(mappingStreams)) {
			for (InputStream stream : mappingStreams) {
				try {
					stream.close();
				}
				catch (IOException ignored) {
				}
			}
		}
	}

	/**
	 * 对给定的Bean Validation配置进行后处理，
	 * 添加或覆盖其任何设置。
	 * <p>在构建{@link ValidatorFactory}之前调用。
	 * @param configuration 预填充了由LocalValidatorFactoryBean属性驱动的设置的Configuration对象
	 */
	protected void postProcessConfiguration(Configuration<?> configuration) {
	}


	@Override
	public Validator getValidator() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getValidator();
	}

	@Override
	public ValidatorContext usingContext() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.usingContext();
	}

	@Override
	public MessageInterpolator getMessageInterpolator() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getMessageInterpolator();
	}

	@Override
	public TraversableResolver getTraversableResolver() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getTraversableResolver();
	}

	@Override
	public ConstraintValidatorFactory getConstraintValidatorFactory() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getConstraintValidatorFactory();
	}

	@Override
	public ParameterNameProvider getParameterNameProvider() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getParameterNameProvider();
	}

	// Bean Validation 2.0：目前未在此处实现，因为这将意味着
	// 对新的javax.validation.ClockProvider接口的硬依赖。
	// 一旦Spring Framework要求Bean Validation 2.0+，将解决此问题。
	// 通过unwrap(ValidatorFactory.class)获取原生ValidatorFactory
	// 它将完全支持getClockProvider()调用。
	/*
	@Override
	public javax.validation.ClockProvider getClockProvider() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getClockProvider();
	}
	*/

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(@Nullable Class<T> type) {
		if (type == null || !ValidatorFactory.class.isAssignableFrom(type)) {
			try {
				return super.unwrap(type);
			}
			catch (ValidationException ex) {
				// 忽略 - 我们接下来将尝试ValidatorFactory解包
			}
		}
		if (this.validatorFactory != null) {
			try {
				return this.validatorFactory.unwrap(type);
			}
			catch (ValidationException ex) {
				// 如果只是请求ValidatorFactory则忽略
				if (ValidatorFactory.class == type) {
					return (T) this.validatorFactory;
				}
				throw ex;
			}
		}
		throw new ValidationException("Cannot unwrap to " + type);
	}

	@Override
	public void close() {
		if (this.validatorFactory != null) {
			this.validatorFactory.close();
		}
	}

	@Override
	public void destroy() {
		close();
	}


	/**
	 * 内部类，以避免对Hibernate Validator的硬编码依赖。
	 */
	private static class HibernateValidatorDelegate {

		public static MessageInterpolator buildMessageInterpolator(MessageSource messageSource) {
			return new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(messageSource));
		}
	}

}
