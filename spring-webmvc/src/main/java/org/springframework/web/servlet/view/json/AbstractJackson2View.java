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
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * 抽象基类，用于基于Jackson的视图实现，与内容类型无关。
 *
 * <p>兼容Jackson 2.9到2.12，截至Spring 5.3。
 *
 * @author Jeremy Grelle
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.1
 */
public abstract class AbstractJackson2View extends AbstractView {
	/**
	 * Jackson对象映射
	 */
	private ObjectMapper objectMapper;

	/**
	 * 编码
	 */
	private JsonEncoding encoding = JsonEncoding.UTF8;

	/**
	 * 是否美化打印
	 */
	@Nullable
	private Boolean prettyPrint;

	/**
	 * 是否禁用缓存，默认为禁用
	 */
	private boolean disableCaching = true;

	/**
	 * 是否更新内容长度，默认为不更新
	 */
	protected boolean updateContentLength = false;


	protected AbstractJackson2View(ObjectMapper objectMapper, String contentType) {
		// 设置 对象映射
		this.objectMapper = objectMapper;
		// 配置是否进行格式化输出
		configurePrettyPrint();
		// 设置内容类型
		setContentType(contentType);
		// 设置是否暴露路径变量
		setExposePathVariables(false);
	}

	/**
	 * 设置此视图的{@code ObjectMapper}。
	 * 如果未设置，则将使用默认的{@link ObjectMapper＃ObjectMapper() ObjectMapper}。
	 * <p>设置自定义配置的{@code ObjectMapper}是控制JSON序列化过程的另一种方法。
	 * 其他选项是在要序列化的类型上使用Jackson提供的注解，在这种情况下，不需要自定义配置的ObjectMapper。
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		// 配置是否进行格式化输出
		configurePrettyPrint();
	}

	/**
	 * 返回此视图的{@code ObjectMapper}。
	 */
	public final ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	/**
	 * 设置此视图的{@code JsonEncoding}。
	 * 默认情况下，使用{@linkplain JsonEncoding＃UTF8 UTF-8}。
	 */
	public void setEncoding(JsonEncoding encoding) {
		Assert.notNull(encoding, "'encoding' must not be null");
		this.encoding = encoding;
	}

	/**
	 * 返回此视图的{@code JsonEncoding}。
	 */
	public final JsonEncoding getEncoding() {
		return this.encoding;
	}

	/**
	 * 是否在写入输出时使用默认的漂亮打印。
	 * 这是一个快捷方式，用于设置如下的{@code ObjectMapper}：
	 * <pre class="code">
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	 * </pre>
	 * <p>默认值为{@code false}。
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		configurePrettyPrint();
	}

	private void configurePrettyPrint() {
		if (this.prettyPrint != null) {
			// 格式化输出
			this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
		}
	}

	/**
	 * 禁用生成的JSON的缓存。
	 * <p>默认为{@code true}，这将防止客户端缓存生成的JSON。
	 */
	public void setDisableCaching(boolean disableCaching) {
		this.disableCaching = disableCaching;
	}

	/**
	 * 是否更新响应的'Content-Length'头。
	 * 当设置为{@code true}时，响应将被缓冲以确定内容的长度，并设置响应的'Content-Length'头。
	 * <p>默认设置为{@code false}。
	 */
	public void setUpdateContentLength(boolean updateContentLength) {
		this.updateContentLength = updateContentLength;
	}

	@Override
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		//设置响应的内容类型
		setResponseContentType(request, response);
		// 设置字符编码，默认为 UTF-8
		response.setCharacterEncoding(this.encoding.getJavaName());
		if (this.disableCaching) {
			// 如果禁用缓存，则设置请求头 Cache-Control = no-store
			response.addHeader("Cache-Control", "no-store");
		}
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
										   HttpServletResponse response) throws Exception {

		// 创建一个临时的 ByteArrayOutputStream 对象
		ByteArrayOutputStream temporaryStream = null;
		// 初始化输出流
		OutputStream stream;

		// 如果需要更新内容长度
		if (this.updateContentLength) {
			// 创建临时的输出流
			temporaryStream = createTemporaryOutputStream();
			// 使用临时的输出流
			stream = temporaryStream;
		} else {
			// 使用响应对象的输出流
			stream = response.getOutputStream();
		}

		// 对模型进行过滤和包装
		Object value = filterAndWrapModel(model, request);
		// 将内容写入流中
		writeContent(stream, value);

		// 如果使用了临时输出流
		if (temporaryStream != null) {
			// 将临时流的内容写入响应
			writeToResponse(response, temporaryStream);
		}
	}

	/**
	 * 过滤并选择性地在{@link MappingJacksonValue}容器中包装模型。
	 *
	 * @param model   要过滤的模型
	 * @param request 当前HTTP请求
	 * @return 要渲染的包装或未包装的值
	 */
	protected Object filterAndWrapModel(Map<String, Object> model, HttpServletRequest request) {
		// 过滤模型数据
		Object value = filterModel(model);
		// 获取序列化视图
		Class<?> serializationView = (Class<?>) model.get(JsonView.class.getName());
		// 获取过滤器提供程序
		FilterProvider filters = (FilterProvider) model.get(FilterProvider.class.getName());

		// 如果存在序列化视图或过滤器
		if (serializationView != null || filters != null) {
			// 创建 MappingJacksonValue 容器
			MappingJacksonValue container = new MappingJacksonValue(value);
			// 设置序列化视图
			if (serializationView != null) {
				container.setSerializationView(serializationView);
			}
			// 设置过滤器
			if (filters != null) {
				container.setFilters(filters);
			}
			// 更新值为容器对象
			value = container;
		}

		// 返回处理后的值
		return value;
	}

	/**
	 * 写入实际的JSON内容到流中。
	 *
	 * @param stream 要使用的输出流
	 * @param object 要渲染的值
	 * @throws IOException 如果写入失败
	 */
	protected void writeContent(OutputStream stream, Object object) throws IOException {
		try (JsonGenerator generator = this.objectMapper.getFactory().createGenerator(stream, this.encoding)) {
			// 写入前缀
			writePrefix(generator, object);

			Object value = object;
			Class<?> serializationView = null;
			FilterProvider filters = null;

			// 如果值是 MappingJacksonValue 类型，提取其中的值、序列化视图和过滤器
			if (value instanceof MappingJacksonValue) {
				MappingJacksonValue container = (MappingJacksonValue) value;
				// 获取容器中的值
				value = container.getValue();
				// 获取序列化视图
				serializationView = container.getSerializationView();
				// 获取容器内的过滤器
				filters = container.getFilters();
			}

			// 根据序列化视图获取对应的 ObjectWriter
			ObjectWriter objectWriter = (serializationView != null ?
					this.objectMapper.writerWithView(serializationView) : this.objectMapper.writer());
			// 如果存在过滤器，设置到 ObjectWriter 中
			if (filters != null) {
				objectWriter = objectWriter.with(filters);
			}
			// 使用 ObjectWriter 将值写入生成器
			objectWriter.writeValue(generator, value);

			// 写入后缀
			writeSuffix(generator, object);
			// 刷新生成器
			generator.flush();
		}
	}


	/**
	 * 设置在此视图中应渲染的模型中的属性。
	 * 当设置时，将忽略所有其他模型属性。
	 */
	public abstract void setModelKey(String modelKey);

	/**
	 * 从给定的模型中过滤出不需要的属性。
	 * 返回值可以是另一个{@link Map}或单个值对象。
	 *
	 * @param model 要过滤的模型
	 * @return 要渲染的值
	 */
	protected abstract Object filterModel(Map<String, Object> model);

	/**
	 * 写入主内容之前的前缀。
	 *
	 * @param generator 用于写入内容的生成器。
	 * @param object    要写入输出消息的对象。
	 */
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
	}

	/**
	 * 写入主内容之后的后缀。
	 *
	 * @param generator 用于写入内容的生成器。
	 * @param object    要写入输出消息的对象。
	 */
	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
	}

}
