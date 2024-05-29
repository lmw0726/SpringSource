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

package org.springframework.web.multipart;

import org.springframework.lang.Nullable;

/**
 * 在上传超过允许的最大上传大小时抛出的 MultipartException 子类。
 *
 * @author Juergen Hoeller
 * @since 1.0.1
 */
@SuppressWarnings("serial")
public class MaxUploadSizeExceededException extends MultipartException {
	/**
	 * 最大上传大小
	 */
	private final long maxUploadSize;


	/**
	 * MaxUploadSizeExceededException 的构造方法。
	 *
	 * @param maxUploadSize 允许的最大上传大小，如果大小限制未知，则为 -1
	 */
	public MaxUploadSizeExceededException(long maxUploadSize) {
		this(maxUploadSize, null);
	}

	/**
	 * MaxUploadSizeExceededException 的构造方法。
	 *
	 * @param maxUploadSize 允许的最大上传大小，如果大小限制未知，则为 -1
	 * @param ex            使用中的多部分解析 API 的根原因
	 */
	public MaxUploadSizeExceededException(long maxUploadSize, @Nullable Throwable ex) {
		super("Maximum upload size " + (maxUploadSize >= 0 ? "of " + maxUploadSize + " bytes " : "") + "exceeded", ex);
		this.maxUploadSize = maxUploadSize;
	}


	/**
	 * 返回允许的最大上传大小，如果大小限制未知，则返回 -1。
	 */
	public long getMaxUploadSize() {
		return this.maxUploadSize;
	}

}
