/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 将 {@link WebArgumentResolver} 适配到 {@link HandlerMethodArgumentResolver} 合同的抽象基类。
 *
 * <p><strong>注意：</strong>此类提供了向后兼容性。
 * 但是建议将 {@code WebArgumentResolver} 重写为 {@code HandlerMethodArgumentResolver}。
 * 由于 {@link #supportsParameter} 只能通过实际解析值然后检查结果不是 {@code WebArgumentResolver#UNRESOLVED} 来实现，
 * 所以引发的任何异常都必须被吸收和忽略，因为不清楚适配器是否不支持参数还是由于内部原因而失败。
 * {@code HandlerMethodArgumentResolver} 合同还提供了访问模型属性和 {@code WebDataBinderFactory}（用于类型转换）。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractWebArgumentResolverAdapter implements HandlerMethodArgumentResolver {

	/**
	 * 日志记录器
	 */
	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * Web参数解析器
	 */
	private final WebArgumentResolver adaptee;


	/**
	 * 创建一个新实例。
	 */
	public AbstractWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		Assert.notNull(adaptee, "'adaptee' must not be null");
		this.adaptee = adaptee;
	}


	/**
	 * 实际解析值并检查解析的值不是 {@link WebArgumentResolver#UNRESOLVED}，吸收 _任何_ 异常。
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		try {
			// 获取原生的 Web请求
			NativeWebRequest webRequest = getWebRequest();
			// 解析参数值
			Object result = this.adaptee.resolveArgument(parameter, webRequest);
			// 如果解析结果为未解析，则返回 false
			if (result == WebArgumentResolver.UNRESOLVED) {
				return false;
			} else {
				// 检查解析结果是否可以分配给参数类型
				return ClassUtils.isAssignableValue(parameter.getParameterType(), result);
			}
		} catch (Exception ex) {
			// 忽略异常，并返回 false
			if (logger.isDebugEnabled()) {
				logger.debug("Error in checking support for parameter [" + parameter + "]: " + ex.getMessage());
			}
			return false;
		}
	}

	/**
	 * 委托给 {@link WebArgumentResolver} 实例。
	 *
	 * @throws IllegalStateException 如果解析的值不能分配给方法参数。
	 */
	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Class<?> paramType = parameter.getParameterType();
		// 解析参数值
		Object result = this.adaptee.resolveArgument(parameter, webRequest);
		if (result == WebArgumentResolver.UNRESOLVED || !ClassUtils.isAssignableValue(paramType, result)) {
			// 如果解析结果为未解析或解析结果无法分配给参数类型，则抛出异常
			throw new IllegalStateException(
					"Standard argument type [" + paramType.getName() + "] in method " + parameter.getMethod() +
							"resolved to incompatible value of type [" + (result != null ? result.getClass() : null) +
							"]. Consider declaring the argument type in a less specific fashion.");
		}
		return result;
	}


	/**
	 * 需要访问 {@link #supportsParameter} 中的 NativeWebRequest。
	 */
	protected abstract NativeWebRequest getWebRequest();

}
