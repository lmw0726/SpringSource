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

package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 表示由HTML 4.0标准定义的一组字符实体引用。
 *
 * <p>可以在 https://www.w3.org/TR/html4/charset.html 上找到HTML 4.0字符集的完整描述。
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @author Craig Andrews
 * @since 1.2.1
 */
class HtmlCharacterEntityReferences {

	/**
	 * 表示HTML字符实体引用的属性文件。
	 */
	private static final String PROPERTIES_FILE = "HtmlCharacterEntityReferences.properties";

	/**
	 * 实体引用开始字符。
	 */
	static final char REFERENCE_START = '&';

	/**
	 * 十进制实体引用开始字符串。
	 */
	static final String DECIMAL_REFERENCE_START = "&#";

	/**
	 * 十六进制实体引用开始字符串。
	 */
	static final String HEX_REFERENCE_START = "&#x";

	/**
	 * 实体引用结束字符。
	 */
	static final char REFERENCE_END = ';';

	/**
	 * 表示空字符的常量。
	 */
	static final char CHAR_NULL = (char) -1;

	/**
	 * 字符到实体引用的映射数组。
	 */
	private final String[] characterToEntityReferenceMap = new String[3000];

	/**
	 * 实体引用到字符的映射。
	 */
	private final Map<String, Character> entityReferenceToCharacterMap = new HashMap<>(512);


	/**
	 * 返回反映HTML 4.0字符集的一组新的字符实体引用。
	 */
	public HtmlCharacterEntityReferences() {
		// 创建一个 Properties 对象来存储字符实体引用
		Properties entityReferences = new Properties();

		// 加载引用定义文件，位于org/springframework/web/util/HtmlCharacterEntityReferences.properties
		InputStream is = HtmlCharacterEntityReferences.class.getResourceAsStream(PROPERTIES_FILE);
		if (is == null) {
			// 如果输入流为空，则抛出异常
			throw new IllegalStateException(
					"Cannot find reference definition file [HtmlCharacterEntityReferences.properties] as class path resource");
		}
		try {
			try {
				// 加载引用定义文件到 Properties 对象
				entityReferences.load(is);
			} finally {
				// 关闭输入流
				is.close();
			}
		} catch (IOException ex) {
			// 抛出异常，指示无法解析引用定义文件
			throw new IllegalStateException(
					"Failed to parse reference definition file [HtmlCharacterEntityReferences.properties]: " +  ex.getMessage());
		}

		// 解析引用定义属性
		Enumeration<?> keys = entityReferences.propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			// 解析引用的字符
			int referredChar = Integer.parseInt(key);
			Assert.isTrue((referredChar < 1000 || (referredChar >= 8000 && referredChar < 10000)),
					() -> "Invalid reference to special HTML entity: " + referredChar);
			// 获取索引位置
			int index = (referredChar < 1000 ? referredChar : referredChar - 7000);
			String reference = entityReferences.getProperty(key);
			// 将字符实体引用映射到字符
			this.characterToEntityReferenceMap[index] = REFERENCE_START + reference + REFERENCE_END;
			// 将 引用属性值 和 引用字符 添加到 实体引用到字符的映射 中。
			this.entityReferenceToCharacterMap.put(reference, (char) referredChar);
		}
	}


	/**
	 * 返回受支持的实体引用数量。
	 */
	public int getSupportedReferenceCount() {
		return this.entityReferenceToCharacterMap.size();
	}

	/**
	 * 如果给定的字符映射到受支持的实体引用，则返回true。
	 */
	public boolean isMappedToReference(char character) {
		return isMappedToReference(character, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 如果给定的字符映射到受支持的实体引用，则返回true。
	 */
	public boolean isMappedToReference(char character, String encoding) {
		return (convertToReference(character, encoding) != null);
	}

	/**
	 * 返回映射到给定字符的引用，如果没有找到则返回 {@code null}。
	 */
	@Nullable
	public String convertToReference(char character) {
		return convertToReference(character, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 返回映射到给定字符的引用，如果没有找到则返回 {@code null}。
	 *
	 * @since 4.1.2
	 */
	@Nullable
	public String convertToReference(char character, String encoding) {
		// 如果编码以 "UTF-" 开头
		if (encoding.startsWith("UTF-")) {
			// 根据字符返回相应的 HTML 实体引用
			switch (character) {
				case '<':
					return "&lt;";
				case '>':
					return "&gt;";
				case '"':
					return "&quot;";
				case '&':
					return "&amp;";
				case '\'':
					return "&#39;";
			}
		} else if (character < 1000 || (character >= 8000 && character < 10000)) {
			// 如果字符小于 1000 或者在 8000 到 10000 之间
			int index = (character < 1000 ? character : character - 7000);
			// 获取字符对应的实体引用
			String entityReference = this.characterToEntityReferenceMap[index];
			if (entityReference != null) {
				// 如果找到实体引用，则返回
				return entityReference;
			}
		}
		// 如果没有相应的实体引用，则返回 null
		return null;
	}

	/**
	 * 返回映射到给定实体引用的字符，如果没有找到则返回 -1。
	 */
	public char convertToCharacter(String entityReference) {
		// 获取字符实体引用对应的字符
		Character referredCharacter = this.entityReferenceToCharacterMap.get(entityReference);
		// 如果找到对应的字符，则返回该字符
		if (referredCharacter != null) {
			return referredCharacter;
		}
		// 否则返回 NULL 字符
		return CHAR_NULL;
	}

}
