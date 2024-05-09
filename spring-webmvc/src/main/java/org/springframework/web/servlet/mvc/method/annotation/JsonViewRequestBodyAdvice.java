/*
 * Copyright 2002-2017 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonInputMessage;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * {@link RequestBodyAdvice} 的实现，为 Spring MVC {@code @HttpEntity} 或 {@code @RequestBody}
 * 方法参数添加对 Jackson 的 {@code @JsonView} 注解的支持。
 *
 * <p>注解中指定的反序列化视图将传递给 {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter}，
 * 然后用它来反序列化请求体。
 *
 * <p>请注意，尽管 {@code @JsonView} 允许指定多个类，但请求体建议仅支持一个类参数。考虑使用复合接口。
 *
 * @author Sebastien Deleuze
 * @see com.fasterxml.jackson.annotation.JsonView
 * @see com.fasterxml.jackson.databind.ObjectMapper#readerWithView(Class)
 * @since 4.2
 */
public class JsonViewRequestBodyAdvice extends RequestBodyAdviceAdapter {

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
							Class<? extends HttpMessageConverter<?>> converterType) {

		// 检查converterType是否是AbstractJackson2HttpMessageConverter的子类或实现类，
		// 并且methodParameter参数上是否存在@JsonView注解
		return (AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType) &&
				methodParameter.getParameterAnnotation(JsonView.class) != null);
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter methodParameter,
										   Type targetType, Class<? extends HttpMessageConverter<?>> selectedConverterType) throws IOException {
		// 获取方法参数上的@JsonView注解
		JsonView ann = methodParameter.getParameterAnnotation(JsonView.class);
		// 检查是否存在@JsonView注解
		Assert.state(ann != null, "No JsonView annotation");

		Class<?>[] classes = ann.value();
		if (classes.length != 1) {
			// 如果@JsonView注解的值不止一个类，则抛出异常
			throw new IllegalArgumentException(
					"@JsonView only supported for request body advice with exactly 1 class argument: " + methodParameter);
		}

		// 返回新的MappingJacksonInputMessage对象
		return new MappingJacksonInputMessage(inputMessage.getBody(), inputMessage.getHeaders(), classes[0]);
	}

}
