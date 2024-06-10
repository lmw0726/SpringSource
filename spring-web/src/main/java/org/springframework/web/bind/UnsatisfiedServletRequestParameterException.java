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

package org.springframework.web.bind;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * {@link ServletRequestBindingException}的子类，表示不满足的参数条件，
 * 通常通过在{@code @Controller}类型级别上使用{@code @RequestMapping}注释表示。
 * <p>
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.bind.annotation.RequestMapping#params()
 * @since 3.0
 */
@SuppressWarnings("serial")
public class UnsatisfiedServletRequestParameterException extends ServletRequestBindingException {
	/**
	 * 参数条件列表
	 */
	private final List<String[]> paramConditions;

	/**
	 * 实际参数映射
	 */
	private final Map<String, String[]> actualParams;


	/**
	 * 创建一个新的UnsatisfiedServletRequestParameterException。
	 *
	 * @param paramConditions 被违反的参数条件
	 * @param actualParams    与ServletRequest关联的实际参数Map
	 */
	public UnsatisfiedServletRequestParameterException(String[] paramConditions, Map<String, String[]> actualParams) {
		super("");
		this.paramConditions = Arrays.<String[]>asList(paramConditions);
		this.actualParams = actualParams;
	}

	/**
	 * 创建一个新的UnsatisfiedServletRequestParameterException。
	 *
	 * @param paramConditions 被违反的所有参数条件组
	 * @param actualParams    与ServletRequest关联的实际参数Map
	 * @since 4.2
	 */
	public UnsatisfiedServletRequestParameterException(List<String[]> paramConditions,
													   Map<String, String[]> actualParams) {

		super("");
		Assert.notEmpty(paramConditions, "Parameter conditions must not be empty");
		this.paramConditions = paramConditions;
		this.actualParams = actualParams;
	}


	@Override
	public String getMessage() {
		// 创建一个 字符串构建器 对象，初始内容为 "Parameter conditions "
		StringBuilder sb = new StringBuilder("Parameter conditions ");

		// 初始化计数器 i 为 0
		int i = 0;

		// 遍历参数条件列表
		for (String[] conditions : this.paramConditions) {
			// 如果不是第一个条件，则在之前添加 " OR "
			if (i > 0) {
				sb.append(" OR ");
			}
			// 添加一个双引号
			sb.append('"');
			// 将当前条件数组转换为逗号分隔的字符串，并添加到 字符串构建器 中
			sb.append(StringUtils.arrayToDelimitedString(conditions, ", "));
			// 添加一个双引号
			sb.append('"');
			// 计数器加1
			i++;
		}

		// 添加描述实际请求参数的字符串
		sb.append(" not met for actual request parameters: ");
		// 将实际请求参数转换为字符串并添加到 StringBuilder 中
		sb.append(requestParameterMapToString(this.actualParams));

		// 返回构建的完整字符串
		return sb.toString();
	}

	/**
	 * 返回被违反的参数条件或多个组中的第一个组。
	 *
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 */
	public final String[] getParamConditions() {
		return this.paramConditions.get(0);
	}

	/**
	 * 返回被违反的所有参数条件组。
	 *
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 * @since 4.2
	 */
	public final List<String[]> getParamConditionGroups() {
		return this.paramConditions;
	}

	/**
	 * 返回与ServletRequest关联的实际参数Map。
	 *
	 * @see javax.servlet.ServletRequest#getParameterMap()
	 */
	public final Map<String, String[]> getActualParams() {
		return this.actualParams;
	}


	private static String requestParameterMapToString(Map<String, String[]> actualParams) {
		// 创建一个 字符串构建器 对象
		StringBuilder result = new StringBuilder();

		// 获取实际参数 Map 的迭代器，并遍历每个键值对
		for (Iterator<Map.Entry<String, String[]>> it = actualParams.entrySet().iterator(); it.hasNext(); ) {
			// 获取当前的键值对
			Map.Entry<String, String[]> entry = it.next();
			// 将键和值（使用 ObjectUtils.nullSafeToString 方法将数组转换为字符串）添加到 字符串构建器 中
			result.append(entry.getKey()).append('=').append(ObjectUtils.nullSafeToString(entry.getValue()));
			// 如果不是最后一个键值对，则添加逗号和空格作为分隔符
			if (it.hasNext()) {
				result.append(", ");
			}
		}

		// 返回构建的完整字符串
		return result.toString();
	}

}
