/*
 * Copyright 2002-2018 the original author or authors.
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
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Map;

/**
 * 策略，用于从查询参数中解析请求的内容类型。
 * 默认的查询参数名称是 {@literal "format"}。
 *
 * <p>您可以通过 {@link #addMapping(String, MediaType)} 注册键（即查询参数的预期值）和 MediaType 的静态映射。
 * 从 5.0 开始，此策略还支持通过 {@link org.springframework.http.MediaTypeFactory#getMediaType} 动态查找键。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ParameterContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

	/**
	 * 参数名称
	 */
	private String parameterName = "format";


	/**
	 * 使用给定的文件扩展名和媒体类型映射的映射创建一个实例。
	 */
	public ParameterContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}


	/**
	 * 设置用于确定请求的媒体类型的参数名称。
	 * <p>默认情况下，此参数设置为 {@code "format"}。
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "'parameterName' is required");
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return this.parameterName;
	}


	@Override
	@Nullable
	protected String getMediaTypeKey(NativeWebRequest request) {
		return request.getParameter(getParameterName());
	}

}
