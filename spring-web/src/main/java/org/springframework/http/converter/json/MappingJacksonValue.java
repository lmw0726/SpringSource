/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.converter.json;

import com.fasterxml.jackson.databind.ser.FilterProvider;

import org.springframework.lang.Nullable;

/**
 * 用于包装要通过 {@link MappingJackson2HttpMessageConverter} 序列化的 POJO，
 * 并传递给转换器的进一步序列化指令的简单持有者。
 *
 * <p>在服务器端，此包装器在内容协商选择使用转换器但在写入之前，使用 {@code ResponseBodyInterceptor} 添加。
 *
 * <p>在客户端，只需包装 POJO 并将其传递给 {@code RestTemplate} 即可。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class MappingJacksonValue {
	/**
	 * 要序列化的实体类
	 */
	private Object value;

	/**
	 * 序列化视图类
	 */
	@Nullable
	private Class<?> serializationView;

	/**
	 * 筛选器提供者
	 */
	@Nullable
	private FilterProvider filters;


	/**
	 * 创建一个新实例，包装要序列化的给定 POJO。
	 *
	 * @param value 要序列化的对象
	 */
	public MappingJacksonValue(Object value) {
		this.value = value;
	}


	/**
	 * 修改要序列化的 POJO。
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 返回需要序列化的 POJO。
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * 设置要用于序列化 POJO 的序列化视图。
	 *
	 * @see com.fasterxml.jackson.databind.ObjectMapper#writerWithView(Class)
	 * @see com.fasterxml.jackson.annotation.JsonView
	 */
	public void setSerializationView(@Nullable Class<?> serializationView) {
		this.serializationView = serializationView;
	}

	/**
	 * 返回要使用的序列化视图。
	 *
	 * @see com.fasterxml.jackson.databind.ObjectMapper#writerWithView(Class)
	 * @see com.fasterxml.jackson.annotation.JsonView
	 */
	@Nullable
	public Class<?> getSerializationView() {
		return this.serializationView;
	}

	/**
	 * 设置要用于序列化 POJO 的 Jackson 过滤器提供程序。
	 *
	 * @see com.fasterxml.jackson.databind.ObjectMapper#writer(FilterProvider)
	 * @see com.fasterxml.jackson.annotation.JsonFilter
	 * @see Jackson2ObjectMapperBuilder#filters(FilterProvider)
	 * @since 4.2
	 */
	public void setFilters(@Nullable FilterProvider filters) {
		this.filters = filters;
	}

	/**
	 * 返回要使用的 Jackson 过滤器提供程序。
	 *
	 * @see com.fasterxml.jackson.databind.ObjectMapper#writer(FilterProvider)
	 * @see com.fasterxml.jackson.annotation.JsonFilter
	 * @since 4.2
	 */
	@Nullable
	public FilterProvider getFilters() {
		return this.filters;
	}

}
