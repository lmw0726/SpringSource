/*
 * Copyright 2002-2021 the original author or authors.
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

import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;

/**
 * {@link Part} 的特化接口，表示在多部分请求中接收到的上传文件。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public interface FilePart extends Part {

	/**
	 * 返回客户端文件系统中的原始文件名。
	 * <p><strong>注意：</strong>请注意，此文件名由客户端提供，不应盲目使用。除了不使用目录部分外，
	 * 文件名还可能包含诸如 ".." 和其他可能被恶意使用的字符。建议不直接使用此文件名。最好生成一个唯一的文件名，
	 * 并在需要时保存原始文件名作为参考。
	 *
	 * @return 原始文件名；如果未在多部分表单中选择文件，则返回空字符串；如果未定义或不可用，则返回 {@code null}
	 * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578，第4.2节</a>
	 * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">不受限制的文件上传</a>
	 */
	String filename();

	/**
	 * 将此部分中文件的内容复制到给定的目标文件中的便捷方法。如果目标文件已经存在，则首先将其截断。
	 * <p>默认实现委托给 {@link #transferTo(Path)}。
	 *
	 * @param dest 目标文件
	 * @return 完成的 {@code Mono}，包含文件传输的结果，如果部分不是文件可能会抛出 {@link IllegalStateException}
	 * @see #transferTo(Path)
	 */
	default Mono<Void> transferTo(File dest) {
		return transferTo(dest.toPath());
	}

	/**
	 * 将此部分中文件的内容复制到给定的目标文件中的便捷方法。如果目标文件已经存在，则首先将其截断。
	 *
	 * @param dest 目标文件
	 * @return 完成的 {@code Mono}，包含文件传输的结果，如果部分不是文件可能会抛出 {@link IllegalStateException}
	 * @see #transferTo(File)
	 * @since 5.1
	 */
	Mono<Void> transferTo(Path dest);

}
