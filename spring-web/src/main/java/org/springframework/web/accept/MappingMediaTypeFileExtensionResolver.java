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

package org.springframework.web.accept;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@code MediaTypeFileExtensionResolver} 的实现，用于在文件扩展名和媒体类型之间维护双向查找。
 *
 * <p>最初创建时包含一个文件扩展名和媒体类型的映射。随后，子类可以使用 {@link #addMapping} 方法添加更多映射。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class MappingMediaTypeFileExtensionResolver implements MediaTypeFileExtensionResolver {
	/**
	 * 扩展名 —— 媒体类型的映射
	 */
	private final ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<>(64);
	/**
	 * 媒体类型 —— 扩展名列表映射
	 */
	private final ConcurrentMap<MediaType, List<String>> fileExtensions = new ConcurrentHashMap<>(64);

	/**
	 * 所有的文件扩展名
	 */
	private final List<String> allFileExtensions = new CopyOnWriteArrayList<>();


	/**
	 * 使用给定的文件扩展名和媒体类型映射创建一个实例。
	 */
	public MappingMediaTypeFileExtensionResolver(@Nullable Map<String, MediaType> mediaTypes) {
		// 如果媒体类型映射不为空
		if (mediaTypes != null) {
			// 创建一个新的集合，用于存储所有的文件扩展名
			Set<String> allFileExtensions = new HashSet<>(mediaTypes.size());

			// 遍历媒体类型映射中的每个键值对
			mediaTypes.forEach((extension, mediaType) -> {
				// 将文件扩展名转换为小写形式
				String lowerCaseExtension = extension.toLowerCase(Locale.ENGLISH);

				// 将扩展名和媒体类型添加到内部的媒体类型映射中
				this.mediaTypes.put(lowerCaseExtension, mediaType);

				// 将扩展名添加到与媒体类型关联的文件扩展名集合中
				addFileExtension(mediaType, lowerCaseExtension);

				// 将扩展名添加到所有文件扩展名的集合中
				allFileExtensions.add(lowerCaseExtension);
			});

			// 将所有文件扩展名集合添加到内部的所有文件扩展名集合中
			this.allFileExtensions.addAll(allFileExtensions);
		}
	}


	public Map<String, MediaType> getMediaTypes() {
		return this.mediaTypes;
	}

	protected List<MediaType> getAllMediaTypes() {
		return new ArrayList<>(this.mediaTypes.values());
	}

	/**
	 * 将扩展名映射到 MediaType。如果扩展名已映射，则忽略。
	 */
	protected void addMapping(String extension, MediaType mediaType) {
		// 将媒体类型添加到媒体类型映射中，并返回先前关联的媒体类型（如果存在）
		MediaType previous = this.mediaTypes.putIfAbsent(extension, mediaType);

		// 如果先前未关联媒体类型（即扩展名之前未在映射中出现）
		if (previous == null) {
			// 向与媒体类型关联的文件扩展名集合中添加扩展名
			addFileExtension(mediaType, extension);

			// 将扩展名添加到所有文件扩展名的集合中
			this.allFileExtensions.add(extension);
		}
	}

	private void addFileExtension(MediaType mediaType, String extension) {
		this.fileExtensions.computeIfAbsent(mediaType, key -> new CopyOnWriteArrayList<>())
				.add(extension);
	}


	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		// 根据媒体类型获取与之关联的文件扩展名列表
		List<String> fileExtensions = this.fileExtensions.get(mediaType);

		// 如果文件扩展名列表不为空，则返回该列表；否则返回空列表
		return (fileExtensions != null ? fileExtensions : Collections.emptyList());
	}

	@Override
	public List<String> getAllFileExtensions() {
		return Collections.unmodifiableList(this.allFileExtensions);
	}

	/**
	 * 使用此方法进行反向查找，从扩展名到 MediaType。
	 *
	 * @return 扩展名对应的 MediaType，如果找不到则返回 {@code null}
	 */
	@Nullable
	protected MediaType lookupMediaType(String extension) {
		return this.mediaTypes.get(extension.toLowerCase(Locale.ENGLISH));
	}

}
