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

package org.springframework.context.index;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 提供对在 {@code META-INF/spring.components} 中定义的候选组件的访问。
 *
 * <p>可以在索引上注册（和查询）任意数量的刻板印象：一个典型的例子是用于标记类
 * 特定用例的注解的完全限定名称。以下调用返回 {@code com.example} 包（及其子包）中
 * 所有 {@code @Component} <b>候选</b>类型：
 * <pre class="code">
 * Set&lt;String&gt; candidates = index.getCandidateTypes(
 *         "com.example", "org.springframework.stereotype.Component");
 * </pre>
 *
 * <p>{@code type} 通常是类的完全限定名称，但这不是规则。类似地，{@code stereotype}
 * 通常是目标类型的完全限定名称，但它可以是任何标记。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
public class CandidateComponentsIndex {

	private static final AntPathMatcher pathMatcher = new AntPathMatcher(".");

	private final MultiValueMap<String, Entry> index;


	CandidateComponentsIndex(List<Properties> content) {
		this.index = parseIndex(content);
	}

	private static MultiValueMap<String, Entry> parseIndex(List<Properties> content) {
		MultiValueMap<String, Entry> index = new LinkedMultiValueMap<>();
		for (Properties entry : content) {
			entry.forEach((type, values) -> {
				String[] stereotypes = ((String) values).split(",");
				for (String stereotype : stereotypes) {
					index.add(stereotype, new Entry((String) type));
				}
			});
		}
		return index;
	}


	/**
	 * 返回与指定刻板印象关联的候选类型。
	 * @param basePackage 要检查候选组件的包
	 * @param stereotype 要使用的刻板印象
	 * @return 与指定 {@code stereotype} 关联的候选类型，
	 * 如果在指定 {@code basePackage} 中未找到则返回空集
	 */
	public Set<String> getCandidateTypes(String basePackage, String stereotype) {
		List<Entry> candidates = this.index.get(stereotype);
		if (candidates != null) {
			return candidates.parallelStream()
					.filter(t -> t.match(basePackage))
					.map(t -> t.type)
					.collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}


	private static class Entry {

		private final String type;

		private final String packageName;

		Entry(String type) {
			this.type = type;
			this.packageName = ClassUtils.getPackageName(type);
		}

		public boolean match(String basePackage) {
			if (pathMatcher.isPattern(basePackage)) {
				return pathMatcher.match(basePackage, this.packageName);
			}
			else {
				return this.type.startsWith(basePackage);
			}
		}
	}

}
