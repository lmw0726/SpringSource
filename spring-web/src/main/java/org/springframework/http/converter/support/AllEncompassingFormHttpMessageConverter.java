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

package org.springframework.http.converter.support;

import org.springframework.core.SpringProperties;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.ClassUtils;

/**
 * 扩展自 {@link org.springframework.http.converter.FormHttpMessageConverter}，
 * 添加对基于 XML 和 JSON 的部分支持。
 *
 * <p>通过控制 {@code spring.xml.ignore} 系统属性的布尔标志来控制，指示 Spring 是否忽略 XML，
 * 即不初始化与 XML 相关的基础设施。
 * <p>默认值为 "false"。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.2
 */
public class AllEncompassingFormHttpMessageConverter extends FormHttpMessageConverter {

	/**
	 * 布尔标志，由 {@code spring.xml.ignore} 系统属性控制，指示 Spring 是否忽略 XML，
	 * 即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	/**
	 * 标志变量，指示是否存在 JAXB 2.x。
	 */
	private static final boolean jaxb2Present;

	/**
	 * 标志变量，指示是否存在 Jackson 2.x。
	 */
	private static final boolean jackson2Present;

	/**
	 * 标志变量，指示是否存在 Jackson 2.x 的 XML 模块。
	 */
	private static final boolean jackson2XmlPresent;

	/**
	 * 标志变量，指示是否存在 Jackson 2.x 的 Smile 模块。
	 */
	private static final boolean jackson2SmilePresent;

	/**
	 * 标志变量，指示是否存在 Gson 库。
	 */
	private static final boolean gsonPresent;

	/**
	 * 标志变量，指示是否存在 JSON-B 库。
	 */
	private static final boolean jsonbPresent;

	/**
	 * 标志变量，指示是否存在 Kotlin Serialization 的 JSON 支持。
	 */
	private static final boolean kotlinSerializationJsonPresent;

	static {
		// 获取 AllEncompassingFormHttpMessageConverter 类的类加载器
		ClassLoader classLoader = AllEncompassingFormHttpMessageConverter.class.getClassLoader();
		// 检查 javax.xml.bind.Binder 类是否存在于类加载器中，从而判断 JAXB2 是否存在
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
		// 检查 com.fasterxml.jackson.databind.ObjectMapper 和 com.fasterxml.jackson.core.JsonGenerator 类
		// 是否都存在于类加载器中，从而判断 Jackson2 是否存在
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		// 检查 com.fasterxml.jackson.dataformat.xml.XmlMapper 类是否存在于类加载器中，从而判断 Jackson2Xml 是否存在
		jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
		// 检查 com.fasterxml.jackson.dataformat.smile.SmileFactory 类是否存在于类加载器中，从而判断 Jackson2Smile 是否存在
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		// 检查 com.google.gson.Gson 类是否存在于类加载器中，从而判断 Gson 是否存在
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		// 检查 javax.json.bind.Jsonb 类是否存在于类加载器中，从而判断 Jsonb 是否存在
		jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
		// 检查 kotlinx.serialization.json.Json 类是否存在于类加载器中，从而判断 KotlinSerializationJson 是否存在
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
	}

	/**
	 * 默认构造函数。根据是否忽略 XML 来注册相应的消息转换器。
	 */
	public AllEncompassingFormHttpMessageConverter() {
		// 如果不应忽略 XML 处理
		if (!shouldIgnoreXml) {
			try {
				// 尝试添加 SourceHttpMessageConverter
				addPartConverter(new SourceHttpMessageConverter<>());
			} catch (Error err) {
				// 当没有可用的 TransformerFactory 实现时忽略异常
			}

			// 如果 JAXB2 存在并且 Jackson2Xml 不存在，则添加 Jaxb2RootElementHttpMessageConverter
			if (jaxb2Present && !jackson2XmlPresent) {
				addPartConverter(new Jaxb2RootElementHttpMessageConverter());
			}
		}

		// 如果 Jackson2 存在，则添加 MappingJackson2HttpMessageConverter
		if (jackson2Present) {
			addPartConverter(new MappingJackson2HttpMessageConverter());
		} else if (gsonPresent) {
			// 如果存在 Gson ，则添加 GsonHttpMessageConverter
			addPartConverter(new GsonHttpMessageConverter());
		} else if (jsonbPresent) {
			// 如果存在 Jsonb ，则添加 JsonbHttpMessageConverter
			addPartConverter(new JsonbHttpMessageConverter());
		} else if (kotlinSerializationJsonPresent) {
			// 如果存在KotlinSerializationJson ，则添加 KotlinSerializationJsonHttpMessageConverter
			addPartConverter(new KotlinSerializationJsonHttpMessageConverter());
		}

		// 如果 Jackson2Xml 存在并且不应忽略 XML 处理，则添加 MappingJackson2XmlHttpMessageConverter
		if (jackson2XmlPresent && !shouldIgnoreXml) {
			addPartConverter(new MappingJackson2XmlHttpMessageConverter());
		}

		// 如果 Jackson2Smile 存在，则添加 MappingJackson2SmileHttpMessageConverter
		if (jackson2SmilePresent) {
			addPartConverter(new MappingJackson2SmileHttpMessageConverter());
		}
	}

}
