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

package org.springframework.web.servlet.view.xml;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.json.AbstractJackson2View;

import java.util.Map;

/**
 * 通过使用<a href="https://github.com/FasterXML/jackson">Jackson 2</a>的{@link XmlMapper}，
 * 将模型序列化为当前请求的XML内容的Spring MVC {@link View}。
 *
 * <p>要序列化的对象作为模型的参数提供。将使用第一个可序列化的条目。用户可以通过{@link #setModelKey(String) sourceKey}属性指定模型中的特定条目。
 *
 * <p>默认构造函数使用由{@link Jackson2ObjectMapperBuilder}提供的默认配置，并将内容类型设置为{@code application/xml}。
 *
 * <p>截至Spring 5.3，兼容Jackson 2.9到2.12。
 *
 * @author Sebastien Deleuze
 * @see org.springframework.web.servlet.view.json.MappingJackson2JsonView
 * @since 4.1
 */
public class MappingJackson2XmlView extends AbstractJackson2View {

	/**
	 * 视图的默认内容类型。
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";

	/**
	 * 此模型中渲染的属性
	 */
	@Nullable
	private String modelKey;


	/**
	 * 使用由{@link Jackson2ObjectMapperBuilder}提供的默认配置构造一个新的{@code MappingJackson2XmlView}，
	 * 并将内容类型设置为{@code application/xml}。
	 */
	public MappingJackson2XmlView() {
		super(Jackson2ObjectMapperBuilder.xml().build(), DEFAULT_CONTENT_TYPE);
	}

	/**
	 * 使用提供的{@link XmlMapper}构造一个新的{@code MappingJackson2XmlView}，
	 * 并将内容类型设置为{@code application/xml}。
	 *
	 * @since 4.2.1
	 */
	public MappingJackson2XmlView(XmlMapper xmlMapper) {
		super(xmlMapper, DEFAULT_CONTENT_TYPE);
	}


	@Override
	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	@Override
	protected Object filterModel(Map<String, Object> model) {
		Object value = null;
		if (this.modelKey != null) {
			// 根据指定的键获取模型中的对象
			value = model.get(this.modelKey);
			if (value == null) {
				throw new IllegalStateException(
						"Model contains no object with key [" + this.modelKey + "]");
			}
		} else {
			// 如果没有指定键，则遍历模型，找到第一个可渲染的对象
			for (Map.Entry<String, Object> entry : model.entrySet()) {
				if (!(entry.getValue() instanceof BindingResult) && !entry.getKey().equals(JsonView.class.getName())) {
					if (value != null) {
						// 如果找到了多个可渲染的对象，则抛出异常
						throw new IllegalStateException("Model contains more than one object to render, only one is supported");
					}
					value = entry.getValue();
				}
			}
		}
		Assert.state(value != null, "Model contains no object to render");
		return value;
	}

}
