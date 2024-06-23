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

package org.springframework.http.codec.multipart;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code HttpMessageReader}用于将 {@code "multipart/form-data"} 请求解析为 {@code MultiValueMap<String, Part>}。
 *
 * <p>请注意，此读取器依赖于对 {@code HttpMessageReader<Part>} 的访问，用于实际解析多部分内容。
 * 此读取器的目的是将部分内容收集到映射中。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MultipartHttpMessageReader extends LoggingCodecSupport
		implements HttpMessageReader<MultiValueMap<String, Part>> {
	/**
	 * 多部分值类型
	 */
	private static final ResolvableType MULTIPART_VALUE_TYPE = ResolvableType.forClassWithGenerics(
			MultiValueMap.class, String.class, Part.class);

	/**
	 * Mime类型
	 */
	static final List<MediaType> MIME_TYPES = Collections.unmodifiableList(Arrays.asList(
			MediaType.MULTIPART_FORM_DATA, MediaType.MULTIPART_MIXED, MediaType.MULTIPART_RELATED));

	/**
	 * 部分读取器
	 */
	private final HttpMessageReader<Part> partReader;


	public MultipartHttpMessageReader(HttpMessageReader<Part> partReader) {
		Assert.notNull(partReader, "'partReader' is required");
		this.partReader = partReader;
	}


	/**
	 * 返回配置的 parts 读取器。
	 *
	 * @since 5.1.11
	 */
	public HttpMessageReader<Part> getPartReader() {
		return this.partReader;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return MIME_TYPES;
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		// 检查 元素类型 是否是 多部分值类型 的子类或接口
		if (MULTIPART_VALUE_TYPE.isAssignableFrom(elementType)) {
			// 如果 元素类型 为 null，则返回 true
			if (mediaType == null) {
				return true;
			}
			// 遍历支持的 MIME 类型列表
			for (MediaType supportedMediaType : MIME_TYPES) {
				// 如果 元素类型 兼容于当前遍历的 支持的媒体类型，则返回 true
				if (supportedMediaType.isCompatibleWith(mediaType)) {
					return true;
				}
			}
		}
		// 如果不满足条件，则返回 false
		return false;
	}


	@Override
	public Flux<MultiValueMap<String, Part>> read(ResolvableType elementType,
												  ReactiveHttpInputMessage message, Map<String, Object> hints) {

		return Flux.from(readMono(elementType, message, hints));
	}


	@Override
	public Mono<MultiValueMap<String, Part>> readMono(ResolvableType elementType,
													  ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {


		// 合并提示
		Map<String, Object> allHints = Hints.merge(hints, Hints.SUPPRESS_LOGGING_HINT, true);

		// 读取元素
		return this.partReader.read(elementType, inputMessage, allHints)
				// 将读取的 Part 元素收集为一个 Multimap，以 Part 的 名称 作为键
				.collectMultimap(Part::name)
				// 对每个收集到的 Multimap 进行操作
				.doOnNext(map ->
						// 记录调试日志
						LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Parsed " +
								(isEnableLoggingRequestDetails() ?
										// 如果启用请求详情日志记录，则格式化输出 映射
										LogFormatUtils.formatValue(map, !traceOn) :
										// 否则输出部件的键集合和内容屏蔽提示
										"parts " + map.keySet() + " (content masked)"))
				)
				// 将收集到的 Multimap 转换为 MultiValueMap
				.map(this::toMultiValueMap);
	}

	private LinkedMultiValueMap<String, Part> toMultiValueMap(Map<String, Collection<Part>> map) {
		return new LinkedMultiValueMap<>(map.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> toList(e.getValue()))));
	}

	private List<Part> toList(Collection<Part> collection) {
		return collection instanceof List ? (List<Part>) collection : new ArrayList<>(collection);
	}

}
