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

package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * YAML 工厂的基类。
 *
 * <p>从 Spring Framework 5.0.6 开始，要求使用 SnakeYAML 1.18 或更高版本。
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.1
 */
public abstract class YamlProcessor {

	private final Log logger = LogFactory.getLog(getClass());

	private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;

	private Resource[] resources = new Resource[0];

	private List<DocumentMatcher> documentMatchers = Collections.emptyList();

	private boolean matchDefault = true;

	private Set<String> supportedTypes = Collections.emptySet();


	/**
	 * 一个文档匹配器映射，允许调用方在 YAML 资源中选择性地只使用部分文档。
	 * 在 YAML 中，文档通过 {@code ---} 分隔，每个文档在进行匹配前会被转换为属性集合（properties）。
	 * 例如：
	 * <pre class="code">
	 * environment: dev
	 * url: https://dev.bar.com
	 * name: Developer Setup
	 * ---
	 * environment: prod
	 * url:https://foo.bar.com
	 * name: My Cool App
	 * </pre>
	 * 配合以下设置：
	 * <pre class="code">
	 * setDocumentMatchers(properties -&gt;
	 *     ("prod".equals(properties.getProperty("environment")) ? MatchStatus.FOUND : MatchStatus.NOT_FOUND));
	 * </pre>
	 * 最终结果将是：
	 * <pre class="code">
	 * environment=prod
	 * url=https://foo.bar.com
	 * name=My Cool App
	 * </pre>
	 */
	public void setDocumentMatchers(DocumentMatcher... matchers) {
		this.documentMatchers = Arrays.asList(matchers);
	}

	/**
	 * 一个标志，指示当所有 {@link #setDocumentMatchers(DocumentMatcher...) 文档匹配器}
	 * 都弃权（即不匹配）时，该文档是否仍然应被视为匹配。默认值为 {@code true}。
	 */
	public void setMatchDefault(boolean matchDefault) {
		this.matchDefault = matchDefault;
	}

	/**
	 * 设置用于解析资源的方法。每个资源将被转换为一个 Map，
	 * 因此此属性用于决定从该工厂的最终输出中保留哪些 Map 条目。
	 * 默认值为 {@link ResolutionMethod#OVERRIDE}。
	 */
	public void setResolutionMethod(ResolutionMethod resolutionMethod) {
		Assert.notNull(resolutionMethod, "ResolutionMethod must not be null");
		this.resolutionMethod = resolutionMethod;
	}

	/**
	 * 设置要加载的 YAML {@link Resource 资源} 的位置。
	 * @see ResolutionMethod
	 */
	public void setResources(Resource... resources) {
		this.resources = resources;
	}

	/**
	 * 设置可以从 YAML 文档中加载的支持类型。
	 * <p>如果未配置支持类型，则仅支持 YAML 文档中出现的 Java 标准类
	 * （定义于 {@link org.yaml.snakeyaml.constructor.SafeConstructor} 中）。
	 * 如果遇到不支持的类型，在处理对应的 YAML 节点时将抛出 {@link IllegalStateException}。
	 * @param supportedTypes 支持的类型，若传入空数组则表示清除所有支持类型
	 * @since 5.1.16
	 * @see #createYaml()
	 */
	public void setSupportedTypes(Class<?>... supportedTypes) {
		if (ObjectUtils.isEmpty(supportedTypes)) {
			this.supportedTypes = Collections.emptySet();
		}
		else {
			Assert.noNullElements(supportedTypes, "'supportedTypes' must not contain null elements");
			this.supportedTypes = Arrays.stream(supportedTypes).map(Class::getName)
					.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
		}
	}

	/**
	 * 提供给子类处理从指定资源中解析出的 Yaml 数据的机会。
	 * 每个资源依次被解析，并通过 {@link #setDocumentMatchers(DocumentMatcher...) 匹配器}
	 * 来检查其中的文档是否匹配。如果文档匹配，则会将其传递给回调方法，
	 * 同时传入该文档转换成的 Properties 表示。
	 * 根据 {@link #setResolutionMethod(ResolutionMethod)} 的设置，并非所有文档都会被解析。
	 * @param callback 找到匹配文档后执行的回调方法
	 * @see #createYaml()
	 */
	protected void process(MatchCallback callback) {
		Yaml yaml = createYaml();
		for (Resource resource : this.resources) {
			boolean found = process(callback, yaml, resource);
			if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
				return;
			}
		}
	}

	/**
	 * 创建要使用的 {@link Yaml} 实例。
	 * <p>默认实现会将 "allowDuplicateKeys" 标志设置为 {@code false}，
	 * 启用 SnakeYAML 1.18+ 的内建重复键处理功能。
	 * <p>从 Spring Framework 5.1.16 开始，如果配置了自定义的 {@linkplain #setSupportedTypes 支持类型}，
	 * 则默认实现会创建一个在 YAML 文档中过滤不支持类型的 {@code Yaml} 实例。
	 * 如果遇到不支持的类型，在处理该节点时将抛出 {@link IllegalStateException} 异常。
	 * @see LoaderOptions#setAllowDuplicateKeys(boolean)
	 */
	protected Yaml createYaml() {
		LoaderOptions loaderOptions = new LoaderOptions();
		loaderOptions.setAllowDuplicateKeys(false);
		return new Yaml(new FilteringConstructor(loaderOptions), new Representer(),
				new DumperOptions(), loaderOptions);
	}

	private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
		int count = 0;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading from YAML: " + resource);
			}
			try (Reader reader = new UnicodeReader(resource.getInputStream())) {
				for (Object object : yaml.loadAll(reader)) {
					if (object != null && process(asMap(object), callback)) {
						count++;
						if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
							break;
						}
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Loaded " + count + " document" + (count > 1 ? "s" : "") +
							" from YAML resource: " + resource);
				}
			}
		}
		catch (IOException ex) {
			handleProcessError(resource, ex);
		}
		return (count > 0);
	}

	private void handleProcessError(Resource resource, IOException ex) {
		if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND &&
				this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
			throw new IllegalStateException(ex);
		}
		if (logger.isWarnEnabled()) {
			logger.warn("Could not load map from " + resource + ": " + ex.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object object) {
		// YAML可以将数字作为键
		Map<String, Object> result = new LinkedHashMap<>();
		if (!(object instanceof Map)) {
			// 文档可以是文本文字
			result.put("document", object);
			return result;
		}

		Map<Object, Object> map = (Map<Object, Object>) object;
		map.forEach((key, value) -> {
			if (value instanceof Map) {
				value = asMap(value);
			}
			if (key instanceof CharSequence) {
				result.put(key.toString(), value);
			}
			else {
				// 在这种情况下，它必须是一个映射键
				result.put("[" + key.toString() + "]", value);
			}
		});
		return result;
	}

	private boolean process(Map<String, Object> map, MatchCallback callback) {
		Properties properties = CollectionFactory.createStringAdaptingProperties();
		properties.putAll(getFlattenedMap(map));

		if (this.documentMatchers.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Merging document (no matchers set): " + map);
			}
			callback.process(properties, map);
			return true;
		}

		MatchStatus result = MatchStatus.ABSTAIN;
		for (DocumentMatcher matcher : this.documentMatchers) {
			MatchStatus match = matcher.matches(properties);
			result = MatchStatus.getMostSpecific(match, result);
			if (match == MatchStatus.FOUND) {
				if (logger.isDebugEnabled()) {
					logger.debug("Matched document with document matcher: " + properties);
				}
				callback.process(properties, map);
				return true;
			}
		}

		if (result == MatchStatus.ABSTAIN && this.matchDefault) {
			if (logger.isDebugEnabled()) {
				logger.debug("Matched document with default matcher: " + map);
			}
			callback.process(properties, map);
			return true;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Unmatched document: " + map);
		}
		return false;
	}

	/**
	 * 返回给定 Map 的扁平化版本，会递归地展开所有嵌套的 Map 或 Collection 值。
	 * 结果 Map 中的条目顺序与原始 Map 保持一致。
	 * 如果该方法用于 {@link MatchCallback} 中提供的 Map，
	 * 则其结果将包含与 {@link MatchCallback} Properties 相同的键值。
	 * @param source 源 Map
	 * @return 扁平化后的 Map
	 * @since 4.1.3
	 */
	protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
		Map<String, Object> result = new LinkedHashMap<>();
		buildFlattenedMap(result, source, null);
		return result;
	}

	private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, @Nullable String path) {
		source.forEach((key, value) -> {
			if (StringUtils.hasText(path)) {
				if (key.startsWith("[")) {
					key = path + key;
				}
				else {
					key = path + '.' + key;
				}
			}
			if (value instanceof String) {
				result.put(key, value);
			}
			else if (value instanceof Map) {
				// 需要一个复合键
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) value;
				buildFlattenedMap(result, map, key);
			}
			else if (value instanceof Collection) {
				// 需要一个复合键
				@SuppressWarnings("unchecked")
				Collection<Object> collection = (Collection<Object>) value;
				if (collection.isEmpty()) {
					result.put(key, "");
				}
				else {
					int count = 0;
					for (Object object : collection) {
						buildFlattenedMap(result, Collections.singletonMap(
								"[" + (count++) + "]", object), key);
					}
				}
			}
			else {
				result.put(key, (value != null ? value : ""));
			}
		});
	}


	/**
	 * 用于处理 YAML 解析结果的回调接口。
	 */
	@FunctionalInterface
	public interface MatchCallback {

		/**
		 * 处理解析结果的表示形式。
		 * @param properties 要处理的属性（在集合或映射的情况下为带索引键的扁平化表示）
		 * @param map 结果 Map（保留 YAML 文档中的原始值结构）
		 */
		void process(Properties properties, Map<String, Object> map);
	}


	/**
	 * 用于测试属性是否匹配的策略接口。
	 */
	@FunctionalInterface
	public interface DocumentMatcher {

		/**
		 * 测试给定属性是否匹配。
		 * @param properties 要测试的属性
		 * @return 匹配状态
		 */
		MatchStatus matches(Properties properties);
	}


	/**
	 * 从 {@link DocumentMatcher#matches(java.util.Properties)} 返回的匹配状态。
	 */
	public enum MatchStatus {

		/**
		 * 找到了匹配项。
		 */
		FOUND,

		/**
		 * 未找到匹配项。
		 */
		NOT_FOUND,

		/**
		 * 匹配器应被忽略。
		 */
		ABSTAIN;

		/**
		 * 比较两个 {@link MatchStatus}，返回更具体的状态。
		 */
		public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
			return (a.ordinal() < b.ordinal() ? a : b);
		}
	}


	/**
	 * 用于解析资源的方法。
	 */
	public enum ResolutionMethod {

		/**
		 * 替换列表中较早的值。
		 */
		OVERRIDE,

		/**
		 * 替换列表中较早的值，忽略任何失败。
		 */
		OVERRIDE_AND_IGNORE,

		/**
		 * 使用列表中第一个存在的资源，仅使用该资源。
		 */
		FIRST_FOUND
	}


	/**
	 * 支持过滤不受支持类型的 {@link Constructor}。
	 * <p>如果在 YAML 文档中遇到不受支持的类型，将从 {@link #getClassForName} 抛出
	 * {@link IllegalStateException}。
	 */
	private class FilteringConstructor extends Constructor {

		FilteringConstructor(LoaderOptions loaderOptions) {
			super(loaderOptions);
		}

		@Override
		protected Class<?> getClassForName(String name) throws ClassNotFoundException {
			Assert.state(YamlProcessor.this.supportedTypes.contains(name),
					() -> "Unsupported type encountered in YAML document: " + name);
			return super.getClassForName(name);
		}
	}

}
