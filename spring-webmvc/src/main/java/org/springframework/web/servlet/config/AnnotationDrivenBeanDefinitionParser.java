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

package org.springframework.web.servlet.config;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.*;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.*;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Properties;

/**
 * 一个 {@link BeanDefinitionParser}，为 {@code <annotation-driven/>} MVC 命名空间元素提供配置。
 *
 * <p>该类注册了以下 {@link HandlerMapping HandlerMappings}：</p>
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}，按顺序排列为 0，用于映射到注解控制器方法的请求。
 * <li>{@link BeanNameUrlHandlerMapping}，按顺序排列为 2，将 URL 路径映射到控制器 bean 名称。
 * </ul>
 *
 * <p><strong>注意：</strong> 使用 {@code <view-controller>} 或 {@code <resources>} MVC 命名空间元素可能会注册其他 HandlerMappings。</p>
 *
 * <p>该类注册了以下 {@link HandlerAdapter HandlerAdapters}：</p>
 * <ul>
 * <li>{@link RequestMappingHandlerAdapter}，用于处理带有注解控制器方法的请求。
 * <li>{@link HttpRequestHandlerAdapter}，用于处理带有 {@link HttpRequestHandler HttpRequestHandlers} 的请求。
 * <li>{@link SimpleControllerHandlerAdapter}，用于处理基于接口的 {@link Controller Controllers} 的请求。
 * </ul>
 *
 * <p>该类注册了以下 {@link HandlerExceptionResolver HandlerExceptionResolvers}：</p>
 * <ul>
 * <li>{@link ExceptionHandlerExceptionResolver}，用于通过 {@link org.springframework.web.bind.annotation.ExceptionHandler} 方法处理异常。
 * <li>{@link ResponseStatusExceptionResolver}，用于处理带有 {@link org.springframework.web.bind.annotation.ResponseStatus} 注解的异常。
 * <li>{@link DefaultHandlerExceptionResolver}，用于解析已知的 Spring 异常类型。
 * </ul>
 *
 * <p>该类注册了一个 {@link org.springframework.util.AntPathMatcher} 和一个 {@link org.springframework.web.util.UrlPathHelper}，
 * 供以下对象使用：</p>
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}，
 * <li>ViewControllers 的 {@link HandlerMapping}，
 * <li>服务资源的 {@link HandlerMapping}。
 * </ul>
 * 请注意，这些 bean 可以通过使用 {@code path-matching} MVC 命名空间元素进行配置。
 *
 * <p>{@link RequestMappingHandlerAdapter} 和 {@link ExceptionHandlerExceptionResolver} 都默认配置了以下实例：</p>
 * <ul>
 * <li>一个 {@link ContentNegotiationManager}，
 * <li>一个 {@link DefaultFormattingConversionService}，
 * <li>如果类路径上有 JSR-303 实现，则是一个 {@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}，
 * <li>根据类路径上的第三方库的可用性，一系列 {@link HttpMessageConverter HttpMessageConverters}。
 * </ul>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Agim Emruli
 * @since 3.0
 */
class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * HandlerMapping Bean 的默认名称，对应 {@link RequestMappingHandlerMapping} 类的名称。
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = RequestMappingHandlerMapping.class.getName();

	/**
	 * HandlerAdapter Bean 的默认名称，对应 {@link RequestMappingHandlerAdapter} 类的名称。
	 */
	public static final String HANDLER_ADAPTER_BEAN_NAME = RequestMappingHandlerAdapter.class.getName();

	/**
	 * ContentNegotiationManager Bean 的默认名称。
	 */
	public static final String CONTENT_NEGOTIATION_MANAGER_BEAN_NAME = "mvcContentNegotiationManager";

	/**
	 * 是否存在 javax.validation 包的标志。
	 */
	private static final boolean javaxValidationPresent;

	/**
	 * 是否存在 rome 包的标志。
	 */
	private static final boolean romePresent;

	/**
	 * 是否存在 jaxb2 包的标志。
	 */
	private static final boolean jaxb2Present;

	/**
	 * 是否存在 jackson2 包的标志。
	 */
	private static final boolean jackson2Present;

	/**
	 * 是否存在 jackson2Xml 包的标志。
	 */
	private static final boolean jackson2XmlPresent;

	/**
	 * 是否存在 jackson2Smile 包的标志。
	 */
	private static final boolean jackson2SmilePresent;

	/**
	 * 是否存在 jackson2Cbor 包的标志。
	 */
	private static final boolean jackson2CborPresent;

	/**
	 * 是否存在 gson 包的标志。
	 */
	private static final boolean gsonPresent;

	static {
		// 获取当前类的类加载器
		ClassLoader classLoader = AnnotationDrivenBeanDefinitionParser.class.getClassLoader();

		// 检查 javax.validation.Validator 是否存在于类路径中
		javaxValidationPresent = ClassUtils.isPresent("javax.validation.Validator", classLoader);

		// 检查 com.rometools.rome.feed.WireFeed 是否存在于类路径中
		romePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);

		// 检查 javax.xml.bind.Binder 是否存在于类路径中
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);

		// 检查 com.fasterxml.jackson.databind.ObjectMapper 和 com.fasterxml.jackson.core.JsonGenerator 是否同时存在于类路径中
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);

		// 检查 com.fasterxml.jackson.dataformat.xml.XmlMapper 是否存在于类路径中
		jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);

		// 检查 com.fasterxml.jackson.dataformat.smile.SmileFactory 是否存在于类路径中
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);

		// 检查 com.fasterxml.jackson.dataformat.cbor.CBORFactory 是否存在于类路径中
		jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);

		// 检查 com.google.gson.Gson 是否存在于类路径中
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
	}


	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext context) {
		// 从元素中提取源对象
		Object source = context.extractSource(element);

		// 获取 XML 读取上下文
		XmlReaderContext readerContext = context.getReaderContext();

		// 创建组合组件定义
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		// 推入包含组件
		context.pushContainingComponent(compDefinition);

		// 获取内容协商管理器的运行时 Bean 引用
		RuntimeBeanReference contentNegotiationManager = getContentNegotiationManager(element, source, context);

		// 创建 请求映射处理程序映射 的根 Bean 定义
		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(RequestMappingHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 将 排序值 设置为0
		handlerMappingDef.getPropertyValues().add("order", 0);
		// 设置内容协商管理器
		handlerMappingDef.getPropertyValues().add("contentNegotiationManager", contentNegotiationManager);

		if (element.hasAttribute("enable-matrix-variables")) {
			// 如果启用矩阵变量，则配置移除分号内容
			boolean enableMatrixVariables = Boolean.parseBoolean(element.getAttribute("enable-matrix-variables"));
			// 设置是否移除分号内容
			handlerMappingDef.getPropertyValues().add("removeSemicolonContent", !enableMatrixVariables);
		}

		// 配置路径匹配属性
		configurePathMatchingProperties(handlerMappingDef, element, context);

		// 注册请求映射处理程序映射的 Bean 定义
		readerContext.getRegistry().registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME, handlerMappingDef);

		// 注册跨域配置
		RuntimeBeanReference corsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
		// 为 请求映射处理程序映射 的根 Bean 定义 设置跨域配置
		handlerMappingDef.getPropertyValues().add("corsConfigurations", corsRef);

		// 获取转换服务的运行时 Bean 引用
		RuntimeBeanReference conversionService = getConversionService(element, source, context);

		// 获取验证器的运行时 Bean 引用
		RuntimeBeanReference validator = getValidator(element, source, context);

		// 获取消息代码解析器的运行时 Bean 引用
		RuntimeBeanReference messageCodesResolver = getMessageCodesResolver(element);

		// 创建可配置的 Web 绑定初始化器的根 Bean 定义
		RootBeanDefinition bindingDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
		bindingDef.setSource(source);
		bindingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 为 可配置的 Web 绑定初始化器的根 Bean 定义 设置转换服务
		bindingDef.getPropertyValues().add("conversionService", conversionService);
		// 为 可配置的 Web 绑定初始化器的根 Bean 定义 设置验证器
		bindingDef.getPropertyValues().add("validator", validator);
		// 为 可配置的 Web 绑定初始化器的根 Bean 定义 设置消息代码解析器
		bindingDef.getPropertyValues().add("messageCodesResolver", messageCodesResolver);

		// 获取消息转换器
		ManagedList<?> messageConverters = getMessageConverters(element, source, context);
		// 获取参数解析器
		ManagedList<?> argumentResolvers = getArgumentResolvers(element, context);
		// 获取返回值处理程序
		ManagedList<?> returnValueHandlers = getReturnValueHandlers(element, context);
		// 获取异步超时时间
		String asyncTimeout = getAsyncTimeout(element);
		// 获取异步执行器
		RuntimeBeanReference asyncExecutor = getAsyncExecutor(element);
		// 获取回调拦截器
		ManagedList<?> callableInterceptors = getInterceptors(element, source, context, "callable-interceptors");
		// 获取延时结果拦截器
		ManagedList<?> deferredResultInterceptors = getInterceptors(element, source, context, "deferred-result-interceptors");

		// 创建 请求映射处理程序适配器 的根 Bean 定义
		RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
		handlerAdapterDef.setSource(source);
		handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 为 请求映射处理程序适配器的根Bean定义 设置内容协商管理器
		handlerAdapterDef.getPropertyValues().add("contentNegotiationManager", contentNegotiationManager);
		// 为 请求映射处理程序适配器的根Bean定义 设置可配置的 Web 绑定初始化器
		handlerAdapterDef.getPropertyValues().add("webBindingInitializer", bindingDef);
		// 为 请求映射处理程序适配器的根Bean定义 设置消息转换器
		handlerAdapterDef.getPropertyValues().add("messageConverters", messageConverters);
		// 将 请求映射处理程序适配器 添加请求体断言和响应体断言中
		addRequestBodyAdvice(handlerAdapterDef);
		addResponseBodyAdvice(handlerAdapterDef);

		if (element.hasAttribute("ignore-default-model-on-redirect")) {
			// 如果有 "ignore-default-model-on-redirect" 属性，则获取是否忽略默认模型属性值
			Boolean ignoreDefaultModel = Boolean.valueOf(element.getAttribute("ignore-default-model-on-redirect"));
			// 并将其设置到 请求映射处理程序适配器的根Bean定义 属性中
			handlerAdapterDef.getPropertyValues().add("ignoreDefaultModelOnRedirect", ignoreDefaultModel);
		}
		if (argumentResolvers != null) {
			// 如果 参数解析器 不为空，并将其设置到 请求映射处理程序适配器的根Bean定义 属性中
			handlerAdapterDef.getPropertyValues().add("customArgumentResolvers", argumentResolvers);
		}
		if (returnValueHandlers != null) {
			// 如果 返回值处理器 不为空，并将其设置到 请求映射处理程序适配器的根Bean定义 属性中
			handlerAdapterDef.getPropertyValues().add("customReturnValueHandlers", returnValueHandlers);
		}
		if (asyncTimeout != null) {
			// 如果 异步超时时间 不为空，并将其设置到 请求映射处理程序适配器的根Bean定义 属性中
			handlerAdapterDef.getPropertyValues().add("asyncRequestTimeout", asyncTimeout);
		}
		if (asyncExecutor != null) {
			// 如果 异步执行器 不为空，并将其设置到 请求映射处理程序适配器的根Bean定义 属性中
			handlerAdapterDef.getPropertyValues().add("taskExecutor", asyncExecutor);
		}
		// 为 请求映射处理程序适配器的根Bean定义 设置回调拦截器
		handlerAdapterDef.getPropertyValues().add("callableInterceptors", callableInterceptors);
		// 为 请求映射处理程序适配器的根Bean定义 设置延时结果拦截器
		handlerAdapterDef.getPropertyValues().add("deferredResultInterceptors", deferredResultInterceptors);

		// 注册请求映射处理程序适配器的 Bean 定义
		readerContext.getRegistry().registerBeanDefinition(HANDLER_ADAPTER_BEAN_NAME, handlerAdapterDef);

		// 创建 URI 组件的根 Bean 定义
		RootBeanDefinition uriContributorDef =
				new RootBeanDefinition(CompositeUriComponentsContributorFactoryBean.class);
		uriContributorDef.setSource(source);
		// 设置处理器适配器
		uriContributorDef.getPropertyValues().addPropertyValue("handlerAdapter", handlerAdapterDef);
		// 设置转换服务
		uriContributorDef.getPropertyValues().addPropertyValue("conversionService", conversionService);
		// 获取 CompositeUriComponentsContributor 的Bean名称
		String uriContributorName = MvcUriComponentsBuilder.MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME;
		// 注册 URI 组件的 Bean 定义
		readerContext.getRegistry().registerBeanDefinition(uriContributorName, uriContributorDef);

		// 创建转换服务暴露拦截器的根 Bean 定义
		RootBeanDefinition csInterceptorDef = new RootBeanDefinition(ConversionServiceExposingInterceptor.class);
		csInterceptorDef.setSource(source);
		// 将第一个构造函数值设置为转换服务
		csInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, conversionService);

		// 创建映射拦截器的根 Bean 定义
		RootBeanDefinition mappedInterceptorDef = new RootBeanDefinition(MappedInterceptor.class);
		mappedInterceptorDef.setSource(source);
		mappedInterceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 将第一个构造函数值设置为 null
		mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, (Object) null);
		// 第二个构造函数值设置为 转换服务暴露拦截器的根 Bean 定义
		mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(1, csInterceptorDef);

		// 注册拦截器的 Bean 定义
		String mappedInterceptorName = readerContext.registerWithGeneratedName(mappedInterceptorDef);

		// 创建方法异常解析器的根 Bean 定义
		RootBeanDefinition methodExceptionResolver = new RootBeanDefinition(ExceptionHandlerExceptionResolver.class);
		methodExceptionResolver.setSource(source);
		methodExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 设置内容协商管理器
		methodExceptionResolver.getPropertyValues().add("contentNegotiationManager", contentNegotiationManager);
		// 设置消息转换器
		methodExceptionResolver.getPropertyValues().add("messageConverters", messageConverters);
		// 设置排序值
		methodExceptionResolver.getPropertyValues().add("order", 0);
		// 将 方法异常解析器的根 Bean 定义 添加进 响应体断言 中
		addResponseBodyAdvice(methodExceptionResolver);
		if (argumentResolvers != null) {
			// 如果 参数解析器 存在，将 参数解析器 设置到 方法异常解析器的根 Bean 定义 的属性中
			methodExceptionResolver.getPropertyValues().add("customArgumentResolvers", argumentResolvers);
		}
		if (returnValueHandlers != null) {
			// 如果 返回值处理器 存在，将 返回值处理器 设置到 方法异常解析器的根 Bean 定义 的属性中
			methodExceptionResolver.getPropertyValues().add("customReturnValueHandlers", returnValueHandlers);
		}
		// 注册 方法异常解析器的Bean 定义
		String methodExResolverName = readerContext.registerWithGeneratedName(methodExceptionResolver);

		// 创建状态异常解析器的根 Bean 定义
		RootBeanDefinition statusExceptionResolver = new RootBeanDefinition(ResponseStatusExceptionResolver.class);
		statusExceptionResolver.setSource(source);
		statusExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 将排序值设置为 1
		statusExceptionResolver.getPropertyValues().add("order", 1);
		// 注册状态异常解析器的Bean 定义
		String statusExResolverName = readerContext.registerWithGeneratedName(statusExceptionResolver);

		// 创建默认异常解析器的根 Bean 定义
		RootBeanDefinition defaultExceptionResolver = new RootBeanDefinition(DefaultHandlerExceptionResolver.class);
		defaultExceptionResolver.setSource(source);
		defaultExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 将排序值设置为 2
		defaultExceptionResolver.getPropertyValues().add("order", 2);
		// 注册默认异常解析器的Bean 定义
		String defaultExResolverName = readerContext.registerWithGeneratedName(defaultExceptionResolver);

		// 注册 请求映射处理程序映射 的组件定义
		context.registerComponent(new BeanComponentDefinition(handlerMappingDef, HANDLER_MAPPING_BEAN_NAME));
		// 注册 请求映射处理程序适配器 的组件定义
		context.registerComponent(new BeanComponentDefinition(handlerAdapterDef, HANDLER_ADAPTER_BEAN_NAME));
		// 注册 URI 组件 的组件定义
		context.registerComponent(new BeanComponentDefinition(uriContributorDef, uriContributorName));
		// 注册 映射拦截器 的组件定义
		context.registerComponent(new BeanComponentDefinition(mappedInterceptorDef, mappedInterceptorName));
		// 注册 方法异常解析器 的组件定义
		context.registerComponent(new BeanComponentDefinition(methodExceptionResolver, methodExResolverName));
		// 注册 状态异常解析器 的组件定义
		context.registerComponent(new BeanComponentDefinition(statusExceptionResolver, statusExResolverName));
		// 注册 默认异常解析器 的组件定义
		context.registerComponent(new BeanComponentDefinition(defaultExceptionResolver, defaultExResolverName));

		// 注册默认组件
		MvcNamespaceUtils.registerDefaultComponents(context, source);

		// 弹出并注册包含组件
		context.popAndRegisterContainingComponent();

		return null;
	}

	protected void addRequestBodyAdvice(RootBeanDefinition beanDef) {
		if (jackson2Present) {
			// 如果 Jackson2 存在，则添加请求体建议的 Bean 定义
			beanDef.getPropertyValues().add("requestBodyAdvice",
					new RootBeanDefinition(JsonViewRequestBodyAdvice.class));
		}
	}

	protected void addResponseBodyAdvice(RootBeanDefinition beanDef) {
		if (jackson2Present) {
			// 如果 Jackson2 存在，则添加响应体建议的 Bean 定义
			beanDef.getPropertyValues().add("responseBodyAdvice",
					new RootBeanDefinition(JsonViewResponseBodyAdvice.class));
		}
	}

	private RuntimeBeanReference getConversionService(Element element, @Nullable Object source, ParserContext context) {
		RuntimeBeanReference conversionServiceRef;
		if (element.hasAttribute("conversion-service")) {
			// 如果存在 conversion-service 属性，则创建对应的 Bean 引用
			conversionServiceRef = new RuntimeBeanReference(element.getAttribute("conversion-service"));
		} else {
			// 否则，创建 FormattingConversionServiceFactoryBean 类型的 Bean 定义
			RootBeanDefinition conversionDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
			conversionDef.setSource(source);
			conversionDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 注册 Bean 定义并获取生成的 Bean 名称
			String conversionName = context.getReaderContext().registerWithGeneratedName(conversionDef);
			// 注册 Bean 组件
			context.registerComponent(new BeanComponentDefinition(conversionDef, conversionName));
			// 创建对生成的 Bean 的引用
			conversionServiceRef = new RuntimeBeanReference(conversionName);
		}
		return conversionServiceRef;
	}

	@Nullable
	private RuntimeBeanReference getValidator(Element element, @Nullable Object source, ParserContext context) {
		if (element.hasAttribute("validator")) {
			// 如果存在 validator 属性，则创建对应的 Bean 引用
			return new RuntimeBeanReference(element.getAttribute("validator"));
		} else if (javaxValidationPresent) {
			// 如果 javax.validation 包存在，则创建 OptionalValidatorFactoryBean 类型的 Bean 定义
			RootBeanDefinition validatorDef = new RootBeanDefinition(
					"org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean");
			validatorDef.setSource(source);
			validatorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 注册 Bean 定义并获取生成的 Bean 名称
			String validatorName = context.getReaderContext().registerWithGeneratedName(validatorDef);
			// 注册 Bean 组件
			context.registerComponent(new BeanComponentDefinition(validatorDef, validatorName));
			// 创建对生成的 Bean 的引用
			return new RuntimeBeanReference(validatorName);
		} else {
			// 如果 javax.validation 包不存在，则返回 null
			return null;
		}
	}

	private RuntimeBeanReference getContentNegotiationManager(
			Element element, @Nullable Object source, ParserContext context) {

		RuntimeBeanReference beanRef;
		if (element.hasAttribute("content-negotiation-manager")) {
			// 如果存在 content-negotiation-manager 属性，则创建对应的 Bean 引用
			String name = element.getAttribute("content-negotiation-manager");
			beanRef = new RuntimeBeanReference(name);
		} else {
			// 如果不存在 content-negotiation-manager 属性，则创建 ContentNegotiationManagerFactoryBean 类型的 Bean 定义
			RootBeanDefinition factoryBeanDef = new RootBeanDefinition(ContentNegotiationManagerFactoryBean.class);
			factoryBeanDef.setSource(source);
			factoryBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			factoryBeanDef.getPropertyValues().add("mediaTypes", getDefaultMediaTypes());
			// 使用预定义的名称进行注册
			String name = CONTENT_NEGOTIATION_MANAGER_BEAN_NAME;
			context.getReaderContext().getRegistry().registerBeanDefinition(name, factoryBeanDef);
			// 注册 Bean 组件
			context.registerComponent(new BeanComponentDefinition(factoryBeanDef, name));
			// 创建对生成的 Bean 的引用
			beanRef = new RuntimeBeanReference(name);
		}
		return beanRef;
	}

	private void configurePathMatchingProperties(
			RootBeanDefinition handlerMappingDef, Element element, ParserContext context) {

		Element pathMatchingElement = DomUtils.getChildElementByTagName(element, "path-matching");
		if (pathMatchingElement != null) {
			// 提取元素的来源
			Object source = context.extractSource(element);

			// 处理路径匹配元素的属性
			if (pathMatchingElement.hasAttribute("suffix-pattern")) {
				// 如果存在 suffix-pattern 属性，获取其布尔值
				Boolean useSuffixPatternMatch = Boolean.valueOf(pathMatchingElement.getAttribute("suffix-pattern"));
				// 添加到 处理器映射Bean定义 的属性中
				handlerMappingDef.getPropertyValues().add("useSuffixPatternMatch", useSuffixPatternMatch);
			}
			if (pathMatchingElement.hasAttribute("trailing-slash")) {
				// 如果存在 trailing-slash 属性，获取其布尔值
				Boolean useTrailingSlashMatch = Boolean.valueOf(pathMatchingElement.getAttribute("trailing-slash"));
				// 添加到 处理器映射Bean定义 的属性中
				handlerMappingDef.getPropertyValues().add("useTrailingSlashMatch", useTrailingSlashMatch);
			}
			if (pathMatchingElement.hasAttribute("registered-suffixes-only")) {
				// 如果存在 registered-suffixes-only 属性，获取其布尔值
				Boolean useRegisteredSuffixPatternMatch = Boolean.valueOf(pathMatchingElement.getAttribute("registered-suffixes-only"));
				// 添加到 处理器映射Bean定义 的属性中
				handlerMappingDef.getPropertyValues().add("useRegisteredSuffixPatternMatch", useRegisteredSuffixPatternMatch);
			}

			RuntimeBeanReference pathHelperRef = null;
			if (pathMatchingElement.hasAttribute("path-helper")) {
				// 如果存在 path-helper 属性，获取其引用
				pathHelperRef = new RuntimeBeanReference(pathMatchingElement.getAttribute("path-helper"));
			}
			// 注册路径助手并获取对其的引用
			pathHelperRef = MvcNamespaceUtils.registerUrlPathHelper(pathHelperRef, context, source);
			// 将 路径助手引用 添加到 处理器映射Bean定义 的属性中
			handlerMappingDef.getPropertyValues().add("urlPathHelper", pathHelperRef);

			RuntimeBeanReference pathMatcherRef = null;
			if (pathMatchingElement.hasAttribute("path-matcher")) {
				// 如果存在 path-matcher 属性，获取其引用
				pathMatcherRef = new RuntimeBeanReference(pathMatchingElement.getAttribute("path-matcher"));
			}
			// 注册路径匹配器并获取对其的引用
			pathMatcherRef = MvcNamespaceUtils.registerPathMatcher(pathMatcherRef, context, source);
			// 将 路径匹配器引用 添加到 处理器映射Bean定义 的属性中
			handlerMappingDef.getPropertyValues().add("pathMatcher", pathMatcherRef);
		}
	}

	private Properties getDefaultMediaTypes() {
		Properties defaultMediaTypes = new Properties();
		if (romePresent) {
			// 如果  rome包 存在，添加适用于Atom和RSS的默认媒体类型
			defaultMediaTypes.put("atom", MediaType.APPLICATION_ATOM_XML_VALUE);
			defaultMediaTypes.put("rss", MediaType.APPLICATION_RSS_XML_VALUE);
		}
		if (jaxb2Present || jackson2XmlPresent) {
			// 如果存在JAXB2或Jackson2 XML支持，添加XML的默认媒体类型
			defaultMediaTypes.put("xml", MediaType.APPLICATION_XML_VALUE);
		}
		if (jackson2Present || gsonPresent) {
			// 如果存在Jackson2或Gson支持，添加JSON的默认媒体类型
			defaultMediaTypes.put("json", MediaType.APPLICATION_JSON_VALUE);
		}
		if (jackson2SmilePresent) {
			// 如果存在Jackson2 Smile支持，添加Smile的默认媒体类型
			defaultMediaTypes.put("smile", "application/x-jackson-smile");
		}
		if (jackson2CborPresent) {
			// 如果存在Jackson2 CBOR支持，添加CBOR的默认媒体类型
			defaultMediaTypes.put("cbor", MediaType.APPLICATION_CBOR_VALUE);
		}
		return defaultMediaTypes;
	}

	@Nullable
	private RuntimeBeanReference getMessageCodesResolver(Element element) {
		if (element.hasAttribute("message-codes-resolver")) {
			// 如果存在 message-codes-resolver 属性，获取其引用
			return new RuntimeBeanReference(element.getAttribute("message-codes-resolver"));
		} else {
			return null;
		}
	}

	@Nullable
	private String getAsyncTimeout(Element element) {
		// 获取 async-support 标签
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		// async-support 标签不为空，则获取 "default-timeout" 属性值；否则返回 null。
		return (asyncElement != null ? asyncElement.getAttribute("default-timeout") : null);
	}

	@Nullable
	private RuntimeBeanReference getAsyncExecutor(Element element) {
		// 获取 async-support 标签
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		if (asyncElement != null && asyncElement.hasAttribute("task-executor")) {
			// 如果存在 async-support 标签。并且有 "task-executor" 属性值，获取其引用
			return new RuntimeBeanReference(asyncElement.getAttribute("task-executor"));
		}
		return null;
	}

	private ManagedList<?> getInterceptors(
			Element element, @Nullable Object source, ParserContext context, String interceptorElementName) {

		ManagedList<Object> interceptors = new ManagedList<>();
		// 获取 async-support 标签
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		if (asyncElement != null) {
			// 如果存在 async-support 标签，获取 指定过滤器名称 的标签
			Element interceptorsElement = DomUtils.getChildElementByTagName(asyncElement, interceptorElementName);
			if (interceptorsElement != null) {
				// 如果过滤器标签存在
				interceptors.setSource(source);
				// 遍历所有的bean元素，解析并添加拦截器到interceptors列表中
				for (Element converter : DomUtils.getChildElementsByTagName(interceptorsElement, "bean")) {
					// 解析bean定义
					BeanDefinitionHolder beanDef = context.getDelegate().parseBeanDefinitionElement(converter);
					if (beanDef != null) {
						// 如果bean定义不为空，装饰bean定义并添加到interceptors列表中
						beanDef = context.getDelegate().decorateBeanDefinitionIfRequired(converter, beanDef);
						interceptors.add(beanDef);
					}
				}
			}
		}
		return interceptors;
	}

	@Nullable
	private ManagedList<?> getArgumentResolvers(Element element, ParserContext context) {
		// 获取argument-resolvers子元素
		Element resolversElement = DomUtils.getChildElementByTagName(element, "argument-resolvers");
		if (resolversElement != null) {
			// 提取argument-resolvers子元素，
			ManagedList<Object> resolvers = extractBeanSubElements(resolversElement, context);
			// 返回包装后的解析器列表
			return wrapLegacyResolvers(resolvers, context);
		}
		return null;
	}

	private ManagedList<Object> wrapLegacyResolvers(List<Object> list, ParserContext context) {
		ManagedList<Object> result = new ManagedList<>();
		for (Object object : list) {
			if (object instanceof BeanDefinitionHolder) {
				// 如果对象是 Bean定义持有者
				BeanDefinitionHolder beanDef = (BeanDefinitionHolder) object;
				// 获取解析器类名
				String className = beanDef.getBeanDefinition().getBeanClassName();
				// 确保类名不为空
				Assert.notNull(className, "No resolver class");
				// 解析类名为Class对象
				Class<?> clazz = ClassUtils.resolveClassName(className, context.getReaderContext().getBeanClassLoader());
				// 检查解析器是否是WebArgumentResolver的实现类
				if (WebArgumentResolver.class.isAssignableFrom(clazz)) {
					// 如果是，则创建ServletWebArgumentResolverAdapter的BeanDefinition，将解析器包装成适配器
					RootBeanDefinition adapter = new RootBeanDefinition(ServletWebArgumentResolverAdapter.class);
					// 将它的第一个构造函数参数设置为解析器
					adapter.getConstructorArgumentValues().addIndexedArgumentValue(0, beanDef);
					// 将它装饰成BeanDefinitionHolder，并添加进结果列表中
					result.add(new BeanDefinitionHolder(adapter, beanDef.getBeanName() + "Adapter"));
					continue;
				}
			}
			// 如果不是WebArgumentResolver的实现类，则直接添加到结果列表中
			result.add(object);
		}
		return result;
	}

	@Nullable
	private ManagedList<?> getReturnValueHandlers(Element element, ParserContext context) {
		// 获取return-value-handlers子元素
		Element handlers = DomUtils.getChildElementByTagName(element, "return-value-handlers");
		// 如果存在return-value-handlers子元素，提取其子元素。否则返回null、
		return (handlers != null ? extractBeanSubElements(handlers, context) : null);
	}

	private ManagedList<?> getMessageConverters(Element element, @Nullable Object source, ParserContext context) {
		// 获取 message-converters 子元素
		Element convertersElement = DomUtils.getChildElementByTagName(element, "message-converters");
		// 创建消息转换器列表
		ManagedList<Object> messageConverters = new ManagedList<>();
		// 如果存在 message-converters 子元素
		if (convertersElement != null) {
			// 设置来源
			messageConverters.setSource(source);
			for (Element beanElement : DomUtils.getChildElementsByTagName(convertersElement, "bean", "ref")) {
				// 遍历子元素，解析消息转换器并添加到列表中
				Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
				messageConverters.add(object);
			}
		}

		// 如果 message-converters子元素 不存在，或者需要注册默认转换器
		if (convertersElement == null || Boolean.parseBoolean(convertersElement.getAttribute("register-defaults"))) {
			// 设置来源
			messageConverters.setSource(source);
			// 添加默认消息转换器到列表中
			messageConverters.add(createConverterDefinition(ByteArrayHttpMessageConverter.class, source));
			// 创建 StringHttpMessageConverter 根Bean定义
			RootBeanDefinition stringConverterDef = createConverterDefinition(StringHttpMessageConverter.class, source);
			// 设置writeAcceptCharset属性为false
			stringConverterDef.getPropertyValues().add("writeAcceptCharset", false);
			// 将 StringHttpMessageConverter根Bean定义 添加到消息转换器列表中
			messageConverters.add(stringConverterDef);
			// 添加资源消息转换器
			messageConverters.add(createConverterDefinition(ResourceHttpMessageConverter.class, source));
			// 添加资源区域消息转换器
			messageConverters.add(createConverterDefinition(ResourceRegionHttpMessageConverter.class, source));
			// 添加源消息转换器
			messageConverters.add(createConverterDefinition(SourceHttpMessageConverter.class, source));
			// 添加表单消息转换器
			messageConverters.add(createConverterDefinition(AllEncompassingFormHttpMessageConverter.class, source));

			if (romePresent) {
				// 如果  rome包 存在， 添加适用于Atom和RSS的消息转换器
				messageConverters.add(createConverterDefinition(AtomFeedHttpMessageConverter.class, source));
				messageConverters.add(createConverterDefinition(RssChannelHttpMessageConverter.class, source));
			}

			if (jackson2XmlPresent) {
				// 如果  jackson2-xml包 存在，创建 MappingJackson2XmlHttpMessageConverter 根Bean定义
				Class<?> type = MappingJackson2XmlHttpMessageConverter.class;
				RootBeanDefinition jacksonConverterDef = createConverterDefinition(type, source);
				// 创建Jackson XML工厂Bean定义
				GenericBeanDefinition jacksonFactoryDef = createObjectMapperFactoryDefinition(source);
				// 将 创建XmlMapper属性设置为true
				jacksonFactoryDef.getPropertyValues().add("createXmlMapper", true);
				// 将 Jackson工厂Bean定义 添加 MappingJackson2XmlHttpMessageConverter 根Bean定义的第一个构造函数值中
				jacksonConverterDef.getConstructorArgumentValues().addIndexedArgumentValue(0, jacksonFactoryDef);
				messageConverters.add(jacksonConverterDef);
			} else if (jaxb2Present) {
				// 如果  jaxb2包 存在，创建并添加 Jaxb2RootElementHttpMessageConverter 根Bean定义
				messageConverters.add(createConverterDefinition(Jaxb2RootElementHttpMessageConverter.class, source));
			}

			if (jackson2Present) {
				// 如果  jackson2包 存在，创建 MappingJackson2HttpMessageConverter 根Bean定义
				Class<?> type = MappingJackson2HttpMessageConverter.class;
				// 创建Jackson消息转换器根Bean定义
				RootBeanDefinition jacksonConverterDef = createConverterDefinition(type, source);
				// 创建Jackson工厂Bean定义
				GenericBeanDefinition jacksonFactoryDef = createObjectMapperFactoryDefinition(source);
				// 将 Jackson工厂Bean定义 添加 MappingJackson2HttpMessageConverter 根Bean定义的第一个构造函数值中
				jacksonConverterDef.getConstructorArgumentValues().addIndexedArgumentValue(0, jacksonFactoryDef);
				// 添加进消息转换器列表中
				messageConverters.add(jacksonConverterDef);
			} else if (gsonPresent) {
				// 如果  gson包 存在，创建并添加 GsonHttpMessageConverter 根Bean定义
				messageConverters.add(createConverterDefinition(GsonHttpMessageConverter.class, source));
			}

			if (jackson2SmilePresent) {
				// 如果  jackson2-smile包 存在，创建 MappingJackson2SmileHttpMessageConverter 根Bean定义
				Class<?> type = MappingJackson2SmileHttpMessageConverter.class;
				RootBeanDefinition jacksonConverterDef = createConverterDefinition(type, source);
				// 创建Jackson SMILE工厂Bean定义
				GenericBeanDefinition jacksonFactoryDef = createObjectMapperFactoryDefinition(source);
				// 设置工厂对象
				jacksonFactoryDef.getPropertyValues().add("factory", new SmileFactory());
				// 将 Jackson工厂Bean定义 添加 MappingJackson2SmileHttpMessageConverter 根Bean定义的第一个构造函数值中
				jacksonConverterDef.getConstructorArgumentValues().addIndexedArgumentValue(0, jacksonFactoryDef);
				// 添加进消息转换器列表中
				messageConverters.add(jacksonConverterDef);
			}

			if (jackson2CborPresent) {
				// 如果  jackson2-cbor包 存在，创建 MappingJackson2CborHttpMessageConverter 根Bean定义
				Class<?> type = MappingJackson2CborHttpMessageConverter.class;
				RootBeanDefinition jacksonConverterDef = createConverterDefinition(type, source);
				// 创建Jackson CBOR工厂Bean定义
				GenericBeanDefinition jacksonFactoryDef = createObjectMapperFactoryDefinition(source);
				// 设置工厂对象
				jacksonFactoryDef.getPropertyValues().add("factory", new CBORFactory());
				// 将 Jackson工厂Bean定义 添加 MappingJackson2CborHttpMessageConverter 根Bean定义的第一个构造函数值中
				jacksonConverterDef.getConstructorArgumentValues().addIndexedArgumentValue(0, jacksonFactoryDef);
				// 添加进消息转换器列表中
				messageConverters.add(jacksonConverterDef);
			}
		}
		// 返回消息转换器列表
		return messageConverters;
	}

	private GenericBeanDefinition createObjectMapperFactoryDefinition(@Nullable Object source) {
		// 创建一个新的GenericBeanDefinition实例
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		// 设置Bean类为Jackson2ObjectMapperFactoryBean
		beanDefinition.setBeanClass(Jackson2ObjectMapperFactoryBean.class);
		// 设置来源
		beanDefinition.setSource(source);
		// 设置角色为基础设施角色
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 返回Bean定义
		return beanDefinition;
	}

	private RootBeanDefinition createConverterDefinition(Class<?> converterClass, @Nullable Object source) {
		// 创建 转换器类型 的根Bean定义
		RootBeanDefinition beanDefinition = new RootBeanDefinition(converterClass);
		// 设置来源
		beanDefinition.setSource(source);
		// 设置角色为基础设施角色
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 返回Bean定义
		return beanDefinition;
	}

	private ManagedList<Object> extractBeanSubElements(Element parentElement, ParserContext context) {
		// 创建一个新的ManagedList实例，用于存储解析后的bean元素
		ManagedList<Object> list = new ManagedList<>();
		// 设置来源，提取父元素的源信息
		list.setSource(context.extractSource(parentElement));
		// 遍历父元素下名为"bean"和"ref"的子元素
		for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, "bean", "ref")) {
			// 解析子元素并返回相应的对象
			Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
			// 将解析出的对象添加到列表中
			list.add(object);
		}
		// 返回解析后的列表
		return list;
	}


	/**
	 * 一个 FactoryBean，用于创建 CompositeUriComponentsContributor，它在 RequestMappingHandlerAdapter
	 * 完全初始化后获取其中配置的 HandlerMethodArgumentResolver。
	 */
	static class CompositeUriComponentsContributorFactoryBean
			implements FactoryBean<CompositeUriComponentsContributor>, InitializingBean {
		/**
		 * 请求映射处理器适配器
		 */
		@Nullable
		private RequestMappingHandlerAdapter handlerAdapter;

		/**
		 * 转换服务
		 */
		@Nullable
		private ConversionService conversionService;

		/**
		 * URL 组件贡献者
		 */
		@Nullable
		private CompositeUriComponentsContributor uriComponentsContributor;

		public void setHandlerAdapter(RequestMappingHandlerAdapter handlerAdapter) {
			this.handlerAdapter = handlerAdapter;
		}

		public void setConversionService(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		@Override
		public void afterPropertiesSet() {
			Assert.state(this.handlerAdapter != null, "No RequestMappingHandlerAdapter set");
			this.uriComponentsContributor = new CompositeUriComponentsContributor(
					this.handlerAdapter.getArgumentResolvers(), this.conversionService);
		}

		@Override
		@Nullable
		public CompositeUriComponentsContributor getObject() {
			return this.uriComponentsContributor;
		}

		@Override
		public Class<?> getObjectType() {
			return CompositeUriComponentsContributor.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

}
