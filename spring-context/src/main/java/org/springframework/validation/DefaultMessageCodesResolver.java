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

package org.springframework.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link MessageCodesResolver} 接口的默认实现。
 *
 * <p>对于对象级别的错误，将按以下顺序创建两个消息代码（当使用
 * {@link Format#PREFIX_ERROR_CODE 前缀式}
 * {@link #setMessageCodeFormatter(MessageCodeFormatter) 格式化器}时）：
 * <ul>
 * <li>1.: code + "." + 对象名称
 * <li>2.: code
 * </ul>
 *
 * <p>对于字段级别的错误，将按以下顺序创建四个消息代码：
 * <ul>
 * <li>1.: code + "." + 对象名称 + "." + 字段名
 * <li>2.: code + "." + 字段名
 * <li>3.: code + "." + 字段类型
 * <li>4.: code
 * </ul>
 *
 * <p>例如，当 code 为 "typeMismatch"、对象名称为 "user"、字段为 "age" 时：
 * <ul>
 * <li>1. 尝试 "typeMismatch.user.age"
 * <li>2. 尝试 "typeMismatch.age"
 * <li>3. 尝试 "typeMismatch.int"
 * <li>4. 尝试 "typeMismatch"
 * </ul>
 *
 * <p>通过此解析算法，可以针对绑定错误（如 "required" 和 "typeMismatch"）显示
 * 特定的消息：
 * <ul>
 * <li>在对象+字段级别（"age" 字段，但仅限于 "user" 对象）；
 * <li>在字段级别（所有 "age" 字段，无论对象名称是什么）；
 * <li>或在通用级别（所有字段，适用于任何对象）。
 * </ul>
 *
 * <p>对于数组、{@link List} 或 {@link java.util.Map} 类型的属性，
 * 会同时为特定元素和整个集合生成代码。假设对象 "user" 中有一个数组 "groups"，
 * 其字段为 "name"：
 * <ul>
 * <li>1. 尝试 "typeMismatch.user.groups[0].name"
 * <li>2. 尝试 "typeMismatch.user.groups.name"
 * <li>3. 尝试 "typeMismatch.groups[0].name"
 * <li>4. 尝试 "typeMismatch.groups.name"
 * <li>5. 尝试 "typeMismatch.name"
 * <li>6. 尝试 "typeMismatch.java.lang.String"
 * <li>7. 尝试 "typeMismatch"
 * </ul>
 *
 * <p>默认情况下，{@code errorCode} 会被放置在构造的消息字符串的开头。
 * 可以使用 {@link #setMessageCodeFormatter(MessageCodeFormatter)
 * messageCodeFormatter} 属性来指定替代的拼接
 * {@link MessageCodeFormatter 格式}。
 *
 * <p>为了将所有代码归入资源包中的特定分类（例如使用 "validation.typeMismatch.name"
 * 而不是默认的 "typeMismatch.name"），可以考虑指定 {@link #setPrefix 前缀}。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Chris Beams
 * @since 1.0.1
 */
@SuppressWarnings("serial")
public class DefaultMessageCodesResolver implements MessageCodesResolver, Serializable {

	/**
	 * 此实现在解析消息代码时使用的分隔符。
	 */
	public static final String CODE_SEPARATOR = ".";

	private static final MessageCodeFormatter DEFAULT_FORMATTER = Format.PREFIX_ERROR_CODE;


	private String prefix = "";

	private MessageCodeFormatter formatter = DEFAULT_FORMATTER;


	/**
	 * 指定应用于此解析器构建的任何代码的前缀。
	 * <p>默认为空。例如，指定 "validation." 可以得到
	 * 类似 "validation.typeMismatch.name" 的错误代码。
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回应用于此解析器构建的任何代码的前缀。
	 * <p>如果没有前缀，则返回空字符串。
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 指定此解析器构建的消息代码的格式。
	 * <p>默认为 {@link Format#PREFIX_ERROR_CODE}。
	 * @since 3.2
	 * @see Format
	 */
	public void setMessageCodeFormatter(@Nullable MessageCodeFormatter formatter) {
		this.formatter = (formatter != null ? formatter : DEFAULT_FORMATTER);
	}


	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName) {
		return resolveMessageCodes(errorCode, objectName, "", null);
	}

	/**
	 * 为给定的代码和字段构建代码列表：一个
	 * 对象/字段特定的代码、一个字段特定的代码、一个通用错误代码。
	 * <p>对于数组、List 和 Map，会同时为特定元素和
	 * 整个集合生成代码。
	 * <p>生成的代码详情请参见 {@link DefaultMessageCodesResolver 类级别的 Javadoc}。
	 * @return 代码列表
	 */
	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName, String field, @Nullable Class<?> fieldType) {
		Set<String> codeList = new LinkedHashSet<>();
		List<String> fieldList = new ArrayList<>();
		buildFieldList(field, fieldList);
		addCodes(codeList, errorCode, objectName, fieldList);
		int dotIndex = field.lastIndexOf('.');
		if (dotIndex != -1) {
			buildFieldList(field.substring(dotIndex + 1), fieldList);
		}
		addCodes(codeList, errorCode, null, fieldList);
		if (fieldType != null) {
			addCode(codeList, errorCode, null, fieldType.getName());
		}
		addCode(codeList, errorCode, null, null);
		return StringUtils.toStringArray(codeList);
	}

	private void addCodes(Collection<String> codeList, String errorCode, @Nullable String objectName, Iterable<String> fields) {
		for (String field : fields) {
			addCode(codeList, errorCode, objectName, field);
		}
	}

	private void addCode(Collection<String> codeList, String errorCode, @Nullable String objectName, @Nullable String field) {
		codeList.add(postProcessMessageCode(this.formatter.format(errorCode, objectName, field)));
	}

	/**
	 * 将带键和不带键的条目添加到给定的 {@code field}
	 * 字段列表中。
	 */
	protected void buildFieldList(String field, List<String> fieldList) {
		fieldList.add(field);
		String plainField = field;
		int keyIndex = plainField.lastIndexOf('[');
		while (keyIndex != -1) {
			int endKeyIndex = plainField.indexOf(']', keyIndex);
			if (endKeyIndex != -1) {
				plainField = plainField.substring(0, keyIndex) + plainField.substring(endKeyIndex + 1);
				fieldList.add(plainField);
				keyIndex = plainField.lastIndexOf('[');
			}
			else {
				keyIndex = -1;
			}
		}
	}

	/**
	 * 对此解析器构建的给定消息代码进行后处理。
	 * <p>默认实现会应用指定的前缀（如果有的话）。
	 * @param code 此解析器构建的消息代码
	 * @return 最终返回的消息代码
	 * @see #setPrefix
	 */
	protected String postProcessMessageCode(String code) {
		return getPrefix() + code;
	}


	/**
	 * 常用的消息代码格式。
	 * @see MessageCodeFormatter
	 * @see DefaultMessageCodesResolver#setMessageCodeFormatter(MessageCodeFormatter)
	 */
	public enum Format implements MessageCodeFormatter {

		/**
		 * 在生成的消息代码开头添加错误代码前缀。例如：
		 * {@code errorCode + "." + 对象名称 + "." + 字段名}
		 */
		PREFIX_ERROR_CODE {
			@Override
			public String format(String errorCode, @Nullable String objectName, @Nullable String field) {
				return toDelimitedString(errorCode, objectName, field);
			}
		},

		/**
		 * 在生成的消息代码末尾添加错误代码后缀。例如：
		 * {@code 对象名称 + "." + 字段名 + "." + errorCode}
		 */
		POSTFIX_ERROR_CODE {
			@Override
			public String format(String errorCode, @Nullable String objectName, @Nullable String field) {
				return toDelimitedString(objectName, field, errorCode);
			}
		};

		/**
		 * 将给定的元素用 {@link DefaultMessageCodesResolver#CODE_SEPARATOR} 连接，
		 * 跳过长度为零或 null 的元素。
		 */
		public static String toDelimitedString(String... elements) {
			StringJoiner rtn = new StringJoiner(CODE_SEPARATOR);
			for (String element : elements) {
				if (StringUtils.hasLength(element)) {
					rtn.add(element);
				}
			}
			return rtn.toString();
		}
	}

}
