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

package org.springframework.http;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 从 {@link Resource} 句柄或文件名解析 {@link MediaType} 对象的工厂委托。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 5.0
 */
public final class MediaTypeFactory {
	/**
	 * MIME类型文件名称
	 */
	private static final String MIME_TYPES_FILE_NAME = "/org/springframework/http/mime.types";

	/**
	 * 文件扩展名——媒体类型映射
	 */
	private static final MultiValueMap<String, MediaType> fileExtensionToMediaTypes = parseMimeTypes();


	private MediaTypeFactory() {
	}


	/**
	 * 解析资源中的 {@code mime.types} 文件。其格式如下：
	 * <code>
	 * # 以 '#' 开头的行为注释<br>
	 * # 格式为 &lt;mime type> &lt;以空格分隔的文件扩展名><br>
	 * # 例如：<br>
	 * text/plain    txt text<br>
	 * # 这将把 file.txt 和 file.text 映射到<br>
	 * # mime 类型 "text/plain"<br>
	 * </code>
	 *
	 * @return 一个多值映射，将媒体类型映射到文件扩展名
	 */
	private static MultiValueMap<String, MediaType> parseMimeTypes() {
		// 获取MIME_TYPES_FILE_NAME文件的输入流
		InputStream is = MediaTypeFactory.class.getResourceAsStream(MIME_TYPES_FILE_NAME);

		Assert.state(is != null, MIME_TYPES_FILE_NAME + " not found in classpath");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII))) {
			// 创建一个多值映射，用于存储文件扩展名与MediaType的映射关系
			MultiValueMap<String, MediaType> result = new LinkedMultiValueMap<>();
			String line;
			// 逐行读取文件内容
			while ((line = reader.readLine()) != null) {
				// 如果是空行或者以#开头的注释行，则跳过
				if (line.isEmpty() || line.charAt(0) == '#') {
					continue;
				}
				// 使用空格、制表符、换行符等分隔符将每行内容分割成数组
				String[] tokens = StringUtils.tokenizeToStringArray(line, " \t\n\r\f");
				// 第一个token是MediaType字符串表示形式
				MediaType mediaType = MediaType.parseMediaType(tokens[0]);
				// 后续token是文件扩展名，将每个扩展名与对应的MediaType关联存储起来
				for (int i = 1; i < tokens.length; i++) {
					String fileExtension = tokens[i].toLowerCase(Locale.ENGLISH);
					result.add(fileExtension, mediaType);
				}
			}
			// 返回构建好的文件扩展名与MediaType映射关系的多值映射
			return result;
		} catch (IOException ex) {
			// 如果发生IO异常，抛出非法状态异常，并附带异常信息
			throw new IllegalStateException("Could not read " + MIME_TYPES_FILE_NAME, ex);
		}
	}

	/**
	 * 根据资源确定其媒体类型，如果可能的话。
	 *
	 * @param resource 要检查的资源
	 * @return 对应的媒体类型，如果找不到则返回 {@code null}
	 */
	public static Optional<MediaType> getMediaType(@Nullable Resource resource) {
		// 返回一个Optional对象，包含resource的文件名
		// 然后调用getMediaType方法获取其MediaType
		return Optional.ofNullable(resource)
				.map(Resource::getFilename)
				.flatMap(MediaTypeFactory::getMediaType);
	}

	/**
	 * 根据文件名确定其媒体类型，如果可能的话。
	 *
	 * @param filename 带扩展名的文件名
	 * @return 对应的媒体类型，如果找不到则返回 {@code null}
	 */
	public static Optional<MediaType> getMediaType(@Nullable String filename) {
		return getMediaTypes(filename).stream().findFirst();
	}

	/**
	 * 根据文件名确定其媒体类型列表，如果可能的话。
	 *
	 * @param filename 带扩展名的文件名
	 * @return 对应的媒体类型列表，如果找不到则返回空列表
	 */
	public static List<MediaType> getMediaTypes(@Nullable String filename) {
		List<MediaType> mediaTypes = null;

		// 获取文件名的扩展名
		String ext = StringUtils.getFilenameExtension(filename);

		// 如果扩展名不为null
		if (ext != null) {
			// 将扩展名转换为小写，并从映射中获取对应的MediaType列表
			mediaTypes = fileExtensionToMediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
		}

		// 如果获取到的MediaType列表不为null，则返回该列表；
		// 否则返回一个空的集合
		return (mediaTypes != null ? mediaTypes : Collections.emptyList());
	}

}
