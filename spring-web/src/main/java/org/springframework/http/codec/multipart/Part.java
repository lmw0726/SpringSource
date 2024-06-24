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

package org.springframework.http.codec.multipart;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;

/**
 * 表示"multipart/form-data"请求中的一个部分。
 *
 * <p>multipart请求的来源可以是浏览器表单，此时每个部分可以是 {@link FormFieldPart} 或 {@link FilePart}。
 *
 * <p>multipart请求也可以在浏览器以外的场景中使用，用于传输任何类型的数据（如JSON、PDF等）。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578 (multipart/form-data)</a>
 * @see <a href="https://tools.ietf.org/html/rfc2183">RFC 2183 (Content-Disposition)</a>
 * @see <a href="https://www.w3.org/TR/html5/forms.html#multipart-form-data">HTML5 (multipart forms)</a>
 * @since 5.0
 */
public interface Part {

	/**
	 * 返回multipart表单中部分的名称。
	 *
	 * @return 部分的名称，永不为 {@code null} 或空字符串
	 */
	String name();

	/**
	 * 返回与部分关联的头信息。
	 */
	HttpHeaders headers();

	/**
	 * 返回此部分的内容。
	 * <p>请注意，对于 {@link FormFieldPart}，可以通过 {@link FormFieldPart#value()} 更轻松地访问内容。
	 */
	Flux<DataBuffer> content();

	/**
	 * 返回一个单元素 {@code Mono}，当订阅时，删除此部分的底层存储。
	 *
	 * @since 5.3.13
	 */
	default Mono<Void> delete() {
		return Mono.empty();
	}

}
