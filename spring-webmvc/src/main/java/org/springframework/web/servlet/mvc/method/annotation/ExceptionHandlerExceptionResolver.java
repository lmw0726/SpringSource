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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.SpringProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.method.annotation.MapMethodProcessor;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.handler.AbstractHandlerMethodExceptionResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过 {@code @ExceptionHandler} 方法解析异常的 {@link AbstractHandlerMethodExceptionResolver}。
 *
 * <p>可以通过 {@link #setCustomArgumentResolvers} 和 {@link #setCustomReturnValueHandlers} 添加对自定义参数和返回值类型的支持。
 * 或者，可以使用 {@link #setArgumentResolvers} 和 {@link #setReturnValueHandlers(List)} 重新配置所有参数和返回值类型。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1
 */
public class ExceptionHandlerExceptionResolver extends AbstractHandlerMethodExceptionResolver
		implements ApplicationContextAware, InitializingBean {

	/**
	 * 布尔标志，由 {@code spring.xml.ignore} 系统属性控制，指示 Spring 是否忽略 XML，即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	/**
	 * 自定义参数解析器
	 */
	@Nullable
	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	/**
	 * 支持的参数解析器列表
	 */
	@Nullable
	private HandlerMethodArgumentResolverComposite argumentResolvers;

	/**
	 * 自定义返回值处理器列表
	 */
	@Nullable
	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers;

	/**
	 * 返回值处理器
	 */
	@Nullable
	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

	/**
	 * HTTP消息转换器列表
	 */
	private List<HttpMessageConverter<?>> messageConverters;

	/**
	 * 内容协商管理器
	 */
	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	/**
	 * 响应体建言列表
	 */
	private final List<Object> responseBodyAdvice = new ArrayList<>();

	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;

	/**
	 * 处理器类型与异常处理程序方法解析器缓存
	 */
	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<>(64);

	/**
	 * ControllerAdvice Bean与异常处理程序方法解析器缓存
	 */
	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>();


	public ExceptionHandlerExceptionResolver() {
		// 创建一个消息转换器列表
		this.messageConverters = new ArrayList<>();
		// 向消息转换器列表中添加 ByteArrayHttpMessageConverter
		this.messageConverters.add(new ByteArrayHttpMessageConverter());
		// 向消息转换器列表中添加 StringHttpMessageConverter
		this.messageConverters.add(new StringHttpMessageConverter());
		if (!shouldIgnoreXml) {
			try {
				// 如果不应忽略 XML，则尝试将 SourceHttpMessageConverter 添加到消息转换器列表中
				this.messageConverters.add(new SourceHttpMessageConverter<>());
			} catch (Error err) {
				// 当有 TransformerFactory 实现不可用时忽略
			}
		}
		// 向消息转换器列表中添加 AllEncompassingFormHttpMessageConverter
		this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
	}


	/**
	 * 提供自定义参数类型的解析器。自定义解析器在内置解析器之后排序。
	 * 要覆盖默认的参数解析支持，请改用 {@link #setArgumentResolvers}。
	 */
	public void setCustomArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}

	/**
	 * 返回自定义参数解析器，或 {@code null}。
	 */
	@Nullable
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * 配置完整的支持参数类型列表，从而覆盖默认情况下将配置的解析器。
	 */
	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			// 如果参数解析器为空，则将 参数解析器列表 设为 null
			this.argumentResolvers = null;
		} else {
			// 否则，创建一个 HandlerMethodArgumentResolverComposite 实例
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
			// 并将参数解析器添加到其中
			this.argumentResolvers.addResolvers(argumentResolvers);
		}

	}

	/**
	 * 返回已配置的参数解析器，如果尚未通过 {@link #afterPropertiesSet()} 进行初始化，则可能为 {@code null}。
	 */
	@Nullable
	public HandlerMethodArgumentResolverComposite getArgumentResolvers() {
		return this.argumentResolvers;
	}

	/**
	 * 为自定义返回值类型提供处理程序。
	 * 自定义处理程序位于内置处理程序之后。
	 * 要覆盖对返回值处理的内置支持，请使用{@link #setReturnValueHandlers}。
	 */
	public void setCustomReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.customReturnValueHandlers = returnValueHandlers;
	}

	/**
	 * 返回自定义的返回值处理器，如果没有则返回null。
	 */
	@Nullable
	public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
		return this.customReturnValueHandlers;
	}

	/**
	 * 配置支持的返回值类型的完整列表，从而覆盖默认情况下配置的处理程序。
	 */
	public void setReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers == null) {
			// 如果返回值处理器为空，则将 当前类的返回值处理器 设为 null
			this.returnValueHandlers = null;
		} else {
			// 否则，创建一个 HandlerMethodReturnValueHandlerComposite 实例
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
			// 并将返回值处理器添加到其中
			this.returnValueHandlers.addHandlers(returnValueHandlers);
		}
	}

	/**
	 * 返回配置的处理器，如果尚未通过{@link #afterPropertiesSet()}初始化，则可能为{@code null}。
	 */
	@Nullable
	public HandlerMethodReturnValueHandlerComposite getReturnValueHandlers() {
		return this.returnValueHandlers;
	}

	/**
	 * 设置要使用的消息体转换器。
	 * <p>这些转换器用于将HTTP请求和响应进行转换。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * 返回配置的消息体转换器。
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * 设置要使用的{@link ContentNegotiationManager}以确定请求的媒体类型。
	 * 如果未设置，则使用默认构造函数。
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回配置的ContentNegotiationManager。
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * 添加一个或多个组件，在执行带有@ResponseBody注解的控制器方法或返回ResponseEntity后调用，
	 * 但在使用选定的HttpMessageConverter将响应体写入响应之前调用。
	 */
	public void setResponseBodyAdvice(@Nullable List<ResponseBodyAdvice<?>> responseBodyAdvice) {
		if (responseBodyAdvice != null) {
			// 如果响应体建言存在，设置响应体建言
			this.responseBodyAdvice.addAll(responseBodyAdvice);
		}
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Override
	public void afterPropertiesSet() {
		// 首先执行这个操作，它可能会添加ResponseBodyAdvice beans
		initExceptionHandlerAdviceCache();

		if (this.argumentResolvers == null) {
			// 如果参数解析器为空
			// 获取默认的参数解析器列表
			List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
			// 创建一个新的HandlerMethodArgumentResolverComposite对象，并添加解析器
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
		}
		if (this.returnValueHandlers == null) {
			// 如果返回值处理器为空
			// 获取默认的返回值处理器列表
			List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
			// 创建一个新的HandlerMethodReturnValueHandlerComposite对象，并添加处理器
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
		}
	}

	private void initExceptionHandlerAdviceCache() {
		// 检查应用程序上下文是否为空，如果为空则直接返回
		if (getApplicationContext() == null) {
			return;
		}

		// 查找带有@ControllerAdvice注解的Bean
		List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());

		// 遍历带有@ControllerAdvice注解的Bean列表
		for (ControllerAdviceBean adviceBean : adviceBeans) {
			// 获取Bean的类型
			Class<?> beanType = adviceBean.getBeanType();

			// 如果Bean类型为空，则抛出异常
			if (beanType == null) {
				throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
			}

			// 创建一个ExceptionHandlerMethodResolver对象，用于解析异常处理方法
			ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);

			if (resolver.hasExceptionMappings()) {
				// 如果存在异常映射，则将 ControllerAdviceBean和异常方法解析器 缓存起来。
				this.exceptionHandlerAdviceCache.put(adviceBean, resolver);
			}

			if (ResponseBodyAdvice.class.isAssignableFrom(beanType)) {
				// 如果Bean类型是ResponseBodyAdvice的子类或实现类，则将其添加到 响应体建言列表 中
				this.responseBodyAdvice.add(adviceBean);
			}
		}

		// 如果日志级别为DEBUG，则输出ControllerAdvice beans的信息
		if (logger.isDebugEnabled()) {
			int handlerSize = this.exceptionHandlerAdviceCache.size();
			int adviceSize = this.responseBodyAdvice.size();

			// 如果没有找到任何ControllerAdvice beans，则输出"none"
			if (handlerSize == 0 && adviceSize == 0) {
				logger.debug("ControllerAdvice beans: none");
			} else {
				// 输出找到的ControllerAdvice beans的数量和类型信息
				logger.debug("ControllerAdvice beans: " +
						handlerSize + " @ExceptionHandler, " + adviceSize + " ResponseBodyAdvice");
			}
		}
	}

	/**
	 * 返回一个不可修改的Map，其中包含在ApplicationContext中发现的{@link ControllerAdvice @ControllerAdvice}
	 * beans。如果在此方法被调用之前bean尚未通过{@link #afterPropertiesSet()}初始化，则返回的map将为空。
	 */
	public Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> getExceptionHandlerAdviceCache() {
		return Collections.unmodifiableMap(this.exceptionHandlerAdviceCache);
	}

	/**
	 * 返回要使用的参数解析器列表，包括内置解析器和通过{@link #setCustomArgumentResolvers}提供的自定义解析器。
	 */
	protected List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		// 基于注解的参数解析
		// 添加会话属性方法参数解析器
		resolvers.add(new SessionAttributeMethodArgumentResolver());
		// 添加请求属性方法参数解析器
		resolvers.add(new RequestAttributeMethodArgumentResolver());

		// 基于类型的参数解析
		// 添加 ServletRequest方法参数解析器
		resolvers.add(new ServletRequestMethodArgumentResolver());
		// 添加 ServletResponse方法参数解析器
		resolvers.add(new ServletResponseMethodArgumentResolver());
		// 添加 重定向属性方法参数解析器
		resolvers.add(new RedirectAttributesMethodArgumentResolver());
		// 添加 模型方法处理器
		resolvers.add(new ModelMethodProcessor());

		if (getCustomArgumentResolvers() != null) {
			// 自定义参数存在，则添加到解析器列表中
			resolvers.addAll(getCustomArgumentResolvers());
		}

		// 捕获所有情况
		// 添加 Principal方法参数解析器
		resolvers.add(new PrincipalMethodArgumentResolver());

		return resolvers;
	}

	/**
	 * 返回要使用的返回值处理器列表，包括内置的和通过{@link #setReturnValueHandlers}提供的自定义处理器。
	 */
	protected List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();

		// 单一目的的返回值类型
		// 添加ModelAndView方法返回值处理器
		handlers.add(new ModelAndViewMethodReturnValueHandler());
		// 添加Model方法处理器
		handlers.add(new ModelMethodProcessor());
		// 添加视图方法返回值处理器
		handlers.add(new ViewMethodReturnValueHandler());
		// 添加HttpEntity方法处理器
		handlers.add(new HttpEntityMethodProcessor(
				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice));

		// 基于注解的返回值类型
		// 添加ServletModelAttribute方法处理器
		handlers.add(new ServletModelAttributeMethodProcessor(false));
		// 添加RequestResponseBody方法处理器
		handlers.add(new RequestResponseBodyMethodProcessor(
				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice));

		// 多用途的返回值类型
		// 添加视图名称方法返回值处理器
		handlers.add(new ViewNameMethodReturnValueHandler());
		// 添加Map方法处理器
		handlers.add(new MapMethodProcessor());

		// 自定义的返回值类型
		if (getCustomReturnValueHandlers() != null) {
			// 添加自定义返回值处理器
			handlers.addAll(getCustomReturnValueHandlers());
		}

		// 捕获所有情况
		// 添加ServletModelAttribute方法处理器
		handlers.add(new ServletModelAttributeMethodProcessor(true));

		// 返回处理器列表
		return handlers;
	}

	@Override
	protected boolean hasGlobalExceptionHandlers() {
		return !this.exceptionHandlerAdviceCache.isEmpty();
	}

	/**
	 * 查找一个带有 {@code @ExceptionHandler} 注解的方法，并调用它来处理抛出的异常。
	 */
	@Override
	@Nullable
	protected ModelAndView doResolveHandlerMethodException(HttpServletRequest request,
														   HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception exception) {

		// 获取异常处理方法
		ServletInvocableHandlerMethod exceptionHandlerMethod = getExceptionHandlerMethod(handlerMethod, exception);
		if (exceptionHandlerMethod == null) {
			// 如果异常处理方法不存在，则返回 null
			return null;
		}

		if (this.argumentResolvers != null) {
			// 设置参数解析器
			exceptionHandlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		}
		if (this.returnValueHandlers != null) {
			// 设置返回值处理器
			exceptionHandlerMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
		}

		// 创建 Web 请求和模型视图容器
		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();

		// 收集异常链
		ArrayList<Throwable> exceptions = new ArrayList<>();
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Using @ExceptionHandler " + exceptionHandlerMethod);
			}
			// 将异常链作为参数暴露给方法
			Throwable exToExpose = exception;
			while (exToExpose != null) {
				exceptions.add(exToExpose);
				Throwable cause = exToExpose.getCause();
				exToExpose = (cause != exToExpose ? cause : null);
			}
			// 异常参数
			Object[] arguments = new Object[exceptions.size() + 1];
			// 将异常列表的数据复制到异常参数数组中。
			exceptions.toArray(arguments);
			// 最后一个参数为处理方法
			arguments[arguments.length - 1] = handlerMethod;
			// 调用处理方法
			exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, arguments);
		} catch (Throwable invocationEx) {
			// 除了原始异常或其原因之外的任何其他异常都是意外的，可能是由于失败的断言或其他原因。
			if (!exceptions.contains(invocationEx) && logger.isWarnEnabled()) {
				logger.warn("Failure in @ExceptionHandler " + exceptionHandlerMethod, invocationEx);
			}
			// 继续处理原始异常的默认处理...
			return null;
		}

		if (mavContainer.isRequestHandled()) {
			// 如果请求已经被处理，则返回 空的ModelAndView
			return new ModelAndView();
		} else {
			// 获取ModelMap对象，用于存储模型数据
			ModelMap model = mavContainer.getModel();

			// 获取HTTP状态码
			HttpStatus status = mavContainer.getStatus();

			// 使用 ModelAndView容器中的视图名称、模型数据和HTTP状态码 创建一个新的ModelAndView对象
			ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model, status);

			// 设置ModelAndView对象的视图名称为ModelAndView容器中的视图名称
			mav.setViewName(mavContainer.getViewName());

			if (!mavContainer.isViewReference()) {
				// 如果ModelAndView容器的视图不是引用视图，则设置为 ModelAndView容器中的视图
				mav.setView((View) mavContainer.getView());
			}

			if (model instanceof RedirectAttributes) {
				// 如果模型数据是RedirectAttributes类型，则将其中的闪存属性添加到请求的输出闪存映射中
				Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
				RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
			}

			// 返回ModelAndView对象
			return mav;
		}
	}

	/**
	 * 查找给定异常的{@code @ExceptionHandler}方法。默认实现首先在控制器类的类层次结构中搜索方法，
	 * 如果未找到，则继续搜索额外的{@code @ExceptionHandler}方法，假设检测到一些{@linkplain ControllerAdvice @ControllerAdvice}
	 * Spring管理的bean。
	 *
	 * @param handlerMethod 引发异常的方法（可能为{@code null}）
	 * @param exception     引发的异常
	 * @return 处理异常的方法，如果找不到则为{@code null}
	 */
	@Nullable
	protected ServletInvocableHandlerMethod getExceptionHandlerMethod(
			@Nullable HandlerMethod handlerMethod, Exception exception) {

		Class<?> handlerType = null;

		if (handlerMethod != null) {
			// 在控制器类本身上的本地异常处理方法。
			// 通过代理调用，即使在基于接口的代理的情况下也是如此。
			// 获取处理器类型
			handlerType = handlerMethod.getBeanType();
			// 从缓存中获取异常方法解析器
			ExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(handlerType);
			if (resolver == null) {
				// 如果 异常方法解析器 不存在，则使用处理类型创建 异常方法解析器
				resolver = new ExceptionHandlerMethodResolver(handlerType);
				// 将处理类型和异常方法解析器映射关系缓存起来。
				this.exceptionHandlerCache.put(handlerType, resolver);
			}
			// 解析方法
			Method method = resolver.resolveMethod(exception);
			if (method != null) {
				// 如果解析的方法存在，则 可调用的Servlet处理方法 对象
				return new ServletInvocableHandlerMethod(handlerMethod.getBean(), method, this.applicationContext);
			}
			// 对于下面的建议适用性检查（涉及基本包、可分配类型和注解存在），请使用目标类而不是基于接口的代理。
			if (Proxy.isProxyClass(handlerType)) {
				// 如果处理器类型是代理类，则获取目标类
				handlerType = AopUtils.getTargetClass(handlerMethod.getBean());
			}
		}

		// 遍历异常处理器缓存中的所有条目
		for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
			// 获取当前条目的控制器建议对象
			ControllerAdviceBean advice = entry.getKey();
			// 判断当前控制器建议对象是否适用于处理目标类型
			if (advice.isApplicableToBeanType(handlerType)) {
				// 获取当前条目的异常处理方法解析器
				ExceptionHandlerMethodResolver resolver = entry.getValue();
				// 根据异常类型解析对应的处理方法
				Method method = resolver.resolveMethod(exception);
				if (method != null) {
					// 如果找到了处理方法，则创建一个新的 可调用的Servlet处理方法 对象并返回
					return new ServletInvocableHandlerMethod(advice.resolveBean(), method, this.applicationContext);
				}
			}
		}

		return null;
	}

}
