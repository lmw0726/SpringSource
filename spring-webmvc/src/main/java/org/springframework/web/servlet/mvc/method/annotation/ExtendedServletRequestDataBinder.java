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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletRequest;
import java.util.Map;

/**
 * {@link ServletRequestDataBinder} 的子类，将 URI 模板变量添加到用于数据绑定的值中。
 *
 * <p><strong>警告</strong>：数据绑定可能会通过公开不应由外部客户端访问或修改的对象图的部分而导致安全问题。因此，应该仔细考虑数据绑定的设计和使用，特别是在考虑安全性方面。有关详细信息，请参阅参考手册中关于
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * 的数据绑定专用部分。
 *
 * @author Rossen Stoyanchev
 * @see ServletRequestDataBinder
 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
 * @since 3.1
 */
public class ExtendedServletRequestDataBinder extends ServletRequestDataBinder {

	/**
	 * 创建一个新实例，使用默认对象名称。
	 *
	 * @param target 要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ExtendedServletRequestDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新实例。
	 *
	 * @param target     要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
	 * @param objectName 目标对象的名称
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ExtendedServletRequestDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将 URI 变量合并到用于数据绑定的属性值中。
	 */
	@Override
	protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
		// 获取 URI 模板变量的属性名称
		String attr = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		// 从请求中获取 URI 模板变量的映射
		@SuppressWarnings("unchecked")
		Map<String, String> uriVars = (Map<String, String>) request.getAttribute(attr);
		// 如果 URI 模板变量映射不为空
		if (uriVars != null) {
			// 遍历 URI 模板变量映射
			uriVars.forEach((name, value) -> {
				// 如果数据绑定参数列表中包含当前 URI 模板变量的名称
				if (mpvs.contains(name)) {
					// 如果日志记录级别为调试，则记录 URI 变量已被请求绑定值覆盖的调试消息
					if (logger.isDebugEnabled()) {
						logger.debug("URI variable '" + name + "' overridden by request bind value.");
					}
				} else {
					// 否则，将 URI 模板变量的名称和值添加到数据绑定参数列表中
					mpvs.addPropertyValue(name, value);
				}
			});
		}
	}

}
