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

package org.springframework.http.codec.json;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 使用Jackson 2.9将{@code Object}流编码为JSON对象的字节流。
 * 对于非流式使用情况，{@link Flux}元素在序列化之前会被收集到{@link List}中，
 * 以提升性能。
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @see Jackson2JsonDecoder
 * @since 5.0
 */
public class Jackson2JsonEncoder extends AbstractJackson2Encoder {
	/**
	 * SSE美化打印器
	 */
	@Nullable
	private final PrettyPrinter ssePrettyPrinter;


	public Jackson2JsonEncoder() {
		this(Jackson2ObjectMapperBuilder.json().build());
	}

	@SuppressWarnings("deprecation")
	public Jackson2JsonEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		// 设置流媒体类型为 application/x-ndjson以及 application/stream+json
		setStreamingMediaTypes(Arrays.asList(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_STREAM_JSON));
		this.ssePrettyPrinter = initSsePrettyPrinter();
	}

	private static PrettyPrinter initSsePrettyPrinter() {
		// 创建一个 默认美化打印器 对象
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		// 设置对象缩进格式，使用两个空格缩进，并在对象之间添加新行和"data:"字符串
		printer.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
		// 返回配置好的 打印器对象
		return printer;
	}


	@Override
	protected ObjectWriter customizeWriter(ObjectWriter writer, @Nullable MimeType mimeType,
										   ResolvableType elementType, @Nullable Map<String, Object> hints) {

		// 如果 SSE美化打印器 不为 null，并且 Mime类型 兼容于 text/event-stream，
		// 且 写入器 配置启用了 缩进输出，则设置SSE美化打印器
		return (this.ssePrettyPrinter != null &&
				MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mimeType) &&
				writer.getConfig().isEnabled(SerializationFeature.INDENT_OUTPUT) ?
				writer.with(this.ssePrettyPrinter) : writer);
	}

}
