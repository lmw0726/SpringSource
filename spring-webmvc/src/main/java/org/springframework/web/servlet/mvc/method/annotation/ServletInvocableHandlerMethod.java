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

import org.springframework.context.MessageSource;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.util.NestedServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

/**
 * 扩展 {@link InvocableHandlerMethod}，具有通过注册的 {@link HandlerMethodReturnValueHandler} 处理返回值的能力，
 * 还支持根据方法级别的 {@code @ResponseStatus} 注解设置响应状态。
 *
 * <p>{@code null} 返回值（包括 void）可能被解释为请求处理的结束，与 {@code @ResponseStatus} 注解结合使用，
 * 不修改的检查条件（参见 {@link ServletWebRequest#checkNotModified(long)}），
 * 或提供对响应流访问的方法参数。
 *
 * @since 3.1
 */
public class ServletInvocableHandlerMethod extends InvocableHandlerMethod {
	/**
	 * Callable方法
	 */
	private static final Method CALLABLE_METHOD = ClassUtils.getMethod(Callable.class, "call");
	/**
	 * 返回值处理器
	 */
	@Nullable
	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;


	/**
	 * 使用给定的处理程序和方法创建一个实例。
	 */
	public ServletInvocableHandlerMethod(Object handler, Method method) {
		super(handler, method);
	}

	/**
	 * {@link #ServletInvocableHandlerMethod(Object, Method)} 的变体，还接受一个 {@link MessageSource}，
	 * 例如用于解析 {@code @ResponseStatus} 消息。
	 */
	public ServletInvocableHandlerMethod(Object handler, Method method, @Nullable MessageSource messageSource) {
		super(handler, method, messageSource);
	}

	/**
	 * 从 {@code HandlerMethod} 创建一个实例。
	 */
	public ServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}


	/**
	 * 注册要用于处理返回值的 {@link HandlerMethodReturnValueHandler} 实例。
	 */
	public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite returnValueHandlers) {
		this.returnValueHandlers = returnValueHandlers;
	}


	/**
	 * 调用方法并通过配置的一个 {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers} 处理返回值。
	 *
	 * @param webRequest   当前请求
	 * @param mavContainer 此请求的 ModelAndViewContainer
	 * @param providedArgs 按类型匹配的“给定”参数（未解析）
	 */
	public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
								Object... providedArgs) throws Exception {
		// 从请求中获取返回值
		Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
		// 设置响应状态
		setResponseStatus(webRequest);

		if (returnValue == null) {
			// 如果返回值为null，则根据条件处理请求
			if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
				// 如果请求未修改，或者已设置了响应状态，或者请求已处理
				// 禁用内容缓存
				disableContentCachingIfNecessary(webRequest);
				// 标记请求已处理
				mavContainer.setRequestHandled(true);
				return;
			}
		} else if (StringUtils.hasText(getResponseStatusReason())) {
			// 如果响应状态的原因不为空，则标记请求已处理
			mavContainer.setRequestHandled(true);
			return;
		}

		// 标记请求未处理
		mavContainer.setRequestHandled(false);
		Assert.state(this.returnValueHandlers != null, "No return value handlers");
		try {
			// 尝试处理返回值
			this.returnValueHandlers.handleReturnValue(
					returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
		} catch (Exception ex) {
			// 处理过程中出现异常则记录日志并重新抛出异常
			if (logger.isTraceEnabled()) {
				logger.trace(formatErrorForReturnValue(returnValue), ex);
			}
			throw ex;
		}
	}

	/**
	 * 根据 {@link ResponseStatus} 注解设置响应状态。
	 */
	private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
		// 获取响应状态
		HttpStatus status = getResponseStatus();
		if (status == null) {
			// 如果响应状态为null，则直接返回
			return;
		}

		// 获取HttpServletResponse
		HttpServletResponse response = webRequest.getResponse();
		if (response != null) {
			// 如果响应不为空，获取状态码原因
			String reason = getResponseStatusReason();
			if (StringUtils.hasText(reason)) {
				// 如果原因为空，则设置错误
				response.sendError(status.value(), reason);
			} else {
				// 否则设置状态码
				response.setStatus(status.value());
			}
		}

		// 由 RedirectView 使用
		webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, status);
	}

	/**
	 * 给定请求是否符合“未修改”条件？
	 */
	private boolean isRequestNotModified(ServletWebRequest webRequest) {
		return webRequest.isNotModified();
	}

	private void disableContentCachingIfNecessary(ServletWebRequest webRequest) {
		// 如果请求未修改
		if (isRequestNotModified(webRequest)) {
			// 获取 HttpServletResponse
			HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
			Assert.notNull(response, "Expected HttpServletResponse");
			// 如果响应头中包含ETag
			if (StringUtils.hasText(response.getHeader(HttpHeaders.ETAG))) {
				// 获取 HttpServletRequest
				HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
				Assert.notNull(request, "Expected HttpServletRequest");
			}
		}
	}

	private String formatErrorForReturnValue(@Nullable Object returnValue) {
		return "Error handling return value=[" + returnValue + "]" +
				(returnValue != null ? ", type=" + returnValue.getClass().getName() : "") +
				" in " + toString();
	}

	/**
	 * 创建一个嵌套的 ServletInvocableHandlerMethod 子类，它返回给定值（如果该值是一个），
	 * 而不是实际调用控制器方法。在处理异步返回值（例如 Callable、DeferredResult、ListenableFuture）时很有用。
	 */
	ServletInvocableHandlerMethod wrapConcurrentResult(Object result) {
		return new ConcurrentResultHandlerMethod(result, new ConcurrentResultMethodParameter(result));
	}


	/**
	 * 使用简单的 {@link Callable} 而不是原始控制器作为处理程序，以返回给定的（并发）结果值。
	 * 有效地“恢复”处理异步生成的返回值。
	 */
	private class ConcurrentResultHandlerMethod extends ServletInvocableHandlerMethod {

		/**
		 * 返回类型的方法参数
		 */
		private final MethodParameter returnType;

		public ConcurrentResultHandlerMethod(final Object result, ConcurrentResultMethodParameter returnType) {
			// 调用父类的构造函数
			super((Callable<Object>) () -> {
				// 如果结果是异常，抛出异常
				if (result instanceof Exception) {
					throw (Exception) result;
				} else if (result instanceof Throwable) {
					// 如果结果是 Throwable，创建 NestedServletException 异常
					throw new NestedServletException("Async processing failed", (Throwable) result);
				}
				// 返回结果
				return result;
			}, CALLABLE_METHOD);

			// 设置处理方法返回值处理器
			if (ServletInvocableHandlerMethod.this.returnValueHandlers != null) {
				setHandlerMethodReturnValueHandlers(ServletInvocableHandlerMethod.this.returnValueHandlers);
			}
			// 设置返回类型
			this.returnType = returnType;
		}

		/**
		 * 桥接到实际控制器类型级别的注解。
		 */
		@Override
		public Class<?> getBeanType() {
			return ServletInvocableHandlerMethod.this.getBeanType();
		}

		/**
		 * 桥接到实际返回值或声明的异步返回类型内的泛型类型，例如 Foo 而不是 {@code DeferredResult<Foo>}。
		 */
		@Override
		public MethodParameter getReturnValueType(@Nullable Object returnValue) {
			return this.returnType;
		}

		/**
		 * 桥接到控制器方法级别的注解。
		 */
		@Override
		public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
			return ServletInvocableHandlerMethod.this.getMethodAnnotation(annotationType);
		}

		/**
		 * 桥接到控制器方法级别的注解。
		 */
		@Override
		public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
			return ServletInvocableHandlerMethod.this.hasMethodAnnotation(annotationType);
		}
	}


	/**
	 * 基于实际返回值类型的 MethodParameter 子类，如果返回值为 null，则退回到声明的异步返回类型内的泛型类型，
	 * 例如 Foo 而不是 {@code DeferredResult<Foo>}。
	 */
	private class ConcurrentResultMethodParameter extends HandlerMethodParameter {
		/**
		 * 返回值
		 */
		@Nullable
		private final Object returnValue;

		/**
		 * 可解析的返回值类型
		 */
		private final ResolvableType returnType;

		public ConcurrentResultMethodParameter(Object returnValue) {
			// 调用父类的构造函数，设置默认的参数索引为 -1
			super(-1);
			// 设置返回值和返回类型
			this.returnValue = returnValue;
			this.returnType = (returnValue instanceof ReactiveTypeHandler.CollectedValuesList ?
					// 如果返回值是 ReactiveTypeHandler.CollectedValuesList 类型，则获取其返回类型
					((ReactiveTypeHandler.CollectedValuesList) returnValue).getReturnType() :
					// 否则根据方法类型来确定返回类型
					KotlinDetector.isSuspendingFunction(super.getMethod()) ?
							// 获取返回值类型
							ResolvableType.forMethodParameter(getReturnType()) :
							// 获取泛型类型
							ResolvableType.forType(super.getGenericParameterType()).getGeneric());
		}

		public ConcurrentResultMethodParameter(ConcurrentResultMethodParameter original) {
			super(original);
			this.returnValue = original.returnValue;
			this.returnType = original.returnType;
		}

		@Override
		public Class<?> getParameterType() {
			if (this.returnValue != null) {
				// 如果存在返回值，则返回其类型
				return this.returnValue.getClass();
			}
			if (!ResolvableType.NONE.equals(this.returnType)) {
				// 如果 返回值类型 不为空，则返回其类型
				return this.returnType.toClass();
			}
			// 否则返回父类的参数类型
			return super.getParameterType();
		}

		@Override
		public Type getGenericParameterType() {
			return this.returnType.getType();
		}

		@Override
		public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
			// 即使实际返回类型是 ResponseEntity<Flux<T>>，也要确保从反应类型收集的值具有 @ResponseBody 类型的处理方式
			return (super.hasMethodAnnotation(annotationType) ||
					(annotationType == ResponseBody.class &&
							this.returnValue instanceof ReactiveTypeHandler.CollectedValuesList));
		}

		@Override
		public ConcurrentResultMethodParameter clone() {
			return new ConcurrentResultMethodParameter(this);
		}
	}

}
