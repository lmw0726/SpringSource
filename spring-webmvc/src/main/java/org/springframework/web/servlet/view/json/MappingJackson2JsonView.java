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

package org.springframework.web.servlet.view.json;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 渲染当前请求的模型的JSON内容的Spring MVC {@link View}，
 * 使用<a href="https://github.com/FasterXML/jackson">Jackson 2</a>的{@link ObjectMapper}。
 *
 * <p>默认情况下，模型映射的整个内容（除了框架特定类）将被编码为JSON。
 * 如果模型只包含一个键，则可以将其提取为JSON并单独编码，通过{@link #setExtractValueFromSingleKeyModel}。
 *
 * <p>默认构造函数使用由{@link Jackson2ObjectMapperBuilder}提供的默认配置，并将内容类型设置为{@code application/json}。
 *
 * <p>截至Spring 5.3，兼容Jackson 2.9到2.12。
 *
 * @author Jeremy Grelle
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1.2
 */
public class MappingJackson2JsonView extends AbstractJackson2View {

	/**
	 * 默认内容类型："application/json"。
	 * 可通过{@link #setContentType}覆盖。
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/json";

	/**
	 * JSON前缀
	 */
	@Nullable
	private String jsonPrefix;

	/**
	 * 此模型中渲染的属性
	 */
	@Nullable
	private Set<String> modelKeys;

	/**
	 * 是否从单键模型中提取值
	 */
	private boolean extractValueFromSingleKeyModel = false;


	/**
	 * 使用由{@link Jackson2ObjectMapperBuilder}提供的默认配置构造一个新的{@code MappingJackson2JsonView}，
	 * 并将内容类型设置为{@code application/json}。
	 */
	public MappingJackson2JsonView() {
		super(Jackson2ObjectMapperBuilder.json().build(), DEFAULT_CONTENT_TYPE);
	}

	/**
	 * 使用提供的{@link ObjectMapper}构造一个新的{@code MappingJackson2JsonView}，并将内容类型设置为{@code application/json}。
	 *
	 * @since 4.2.1
	 */
	public MappingJackson2JsonView(ObjectMapper objectMapper) {
		super(objectMapper, DEFAULT_CONTENT_TYPE);
	}


	/**
	 * 指定用于此视图的JSON输出的自定义前缀。默认为无。
	 *
	 * @see #setPrefixJson
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * 指示此视图输出的JSON是否应以<tt>")]}', "</tt>为前缀。
	 * 默认为{@code false}。
	 * <p>通过在此方式前缀JSON字符串来帮助防止JSON劫持。
	 * 前缀将字符串呈现为语法无效的脚本，因此无法劫持。
	 * 在解析字符串为JSON之前，应删除此前缀。
	 *
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setModelKey(String modelKey) {
		this.modelKeys = Collections.singleton(modelKey);
	}

	/**
	 * 设置应由此视图呈现的模型中的属性。设置后，将忽略所有其他模型属性。
	 */
	public void setModelKeys(@Nullable Set<String> modelKeys) {
		this.modelKeys = modelKeys;
	}

	/**
	 * 返回应由此视图呈现的模型中的属性。
	 */
	@Nullable
	public final Set<String> getModelKeys() {
		return this.modelKeys;
	}

	/**
	 * 设置是否将包含单个属性的模型序列化为映射，还是从模型中提取单个值并直接序列化它。
	 * <p>设置此标志的效果类似于使用{@code MappingJackson2HttpMessageConverter}与{@code @ResponseBody}请求处理方法。
	 * <p>默认为{@code false}。
	 */
	public void setExtractValueFromSingleKeyModel(boolean extractValueFromSingleKeyModel) {
		this.extractValueFromSingleKeyModel = extractValueFromSingleKeyModel;
	}

	/**
	 * 从给定的模型中过滤出不需要的属性。
	 * 返回值可以是另一个{@link Map}或单个值对象。
	 * <p>默认实现移除{@link BindingResult}实例和不包含在{@link #setModelKeys modelKeys}属性中的条目。
	 *
	 * @param model 要过滤的模型，作为{@link #renderMergedOutputModel}的参数传递
	 * @return 要呈现的值
	 */
	@Override
	protected Object filterModel(Map<String, Object> model) {
		Map<String, Object> result = CollectionUtils.newHashMap(model.size());
		Set<String> modelKeys = (!CollectionUtils.isEmpty(this.modelKeys) ? this.modelKeys : model.keySet());
		model.forEach((clazz, value) -> {
			// 排除 BindingResult 类型和特定的键
			if (!(value instanceof BindingResult) && modelKeys.contains(clazz) &&
					!clazz.equals(JsonView.class.getName()) &&
					!clazz.equals(FilterProvider.class.getName())) {
				result.put(clazz, value);
			}
		});
		// 如果开启了从单个键的模型中提取值，并且结果只包含一个键，则返回该值，否则返回整个结果集合
		return (this.extractValueFromSingleKeyModel && result.size() == 1 ? result.values().iterator().next() : result);
	}

	@Override
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
		if (this.jsonPrefix != null) {
			// 如果JSON前缀不为空，Json生成器则写入前缀
			generator.writeRaw(this.jsonPrefix);
		}
	}

}
