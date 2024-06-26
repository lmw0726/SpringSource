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

package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * Context that gets passed along a bean definition reading process,
 * encapsulating all relevant configuration as well as state.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ReaderContext {
	/**
	 * 资源
	 */
	private final Resource resource;

	private final ProblemReporter problemReporter;

	private final ReaderEventListener eventListener;
	/**
	 * 源提取器
	 */
	private final SourceExtractor sourceExtractor;


	/**
	 * 构建一个新的 {@code ReaderContext}.
	 *
	 * @param resource        XML bean定义资源
	 * @param problemReporter 使用中的问题报告者
	 * @param eventListener   正在使用的事件侦听器
	 * @param sourceExtractor 正在使用的源提取器
	 */
	public ReaderContext(Resource resource, ProblemReporter problemReporter,
						 ReaderEventListener eventListener, SourceExtractor sourceExtractor) {

		this.resource = resource;
		this.problemReporter = problemReporter;
		this.eventListener = eventListener;
		this.sourceExtractor = sourceExtractor;
	}

	public final Resource getResource() {
		return this.resource;
	}


	// Errors and warnings

	/**
	 * Raise a fatal error.
	 */
	public void fatal(String message, @Nullable Object source) {
		fatal(message, source, null, null);
	}

	/**
	 * Raise a fatal error.
	 */
	public void fatal(String message, @Nullable Object source, @Nullable Throwable cause) {
		fatal(message, source, null, cause);
	}

	/**
	 * Raise a fatal error.
	 */
	public void fatal(String message, @Nullable Object source, @Nullable ParseState parseState) {
		fatal(message, source, parseState, null);
	}

	/**
	 * Raise a fatal error.
	 */
	public void fatal(String message, @Nullable Object source, @Nullable ParseState parseState, @Nullable Throwable cause) {
		Location location = new Location(getResource(), source);
		this.problemReporter.fatal(new Problem(message, location, parseState, cause));
	}

	/**
	 * 标注常规错误
	 */
	public void error(String message, @Nullable Object source) {
		error(message, source, null, null);
	}

	/**
	 * Raise a regular error.
	 */
	public void error(String message, @Nullable Object source, @Nullable Throwable cause) {
		error(message, source, null, cause);
	}

	/**
	 * Raise a regular error.
	 */
	public void error(String message, @Nullable Object source, @Nullable ParseState parseState) {
		error(message, source, parseState, null);
	}

	/**
	 * 标注常规错误
	 */
	public void error(String message, @Nullable Object source, @Nullable ParseState parseState, @Nullable Throwable cause) {
		//获取资源的位置
		Location location = new Location(getResource(), source);
		//问题报告者标注错误
		this.problemReporter.error(new Problem(message, location, parseState, cause));
	}

	/**
	 * Raise a non-critical warning.
	 */
	public void warning(String message, @Nullable Object source) {
		warning(message, source, null, null);
	}

	/**
	 * Raise a non-critical warning.
	 */
	public void warning(String message, @Nullable Object source, @Nullable Throwable cause) {
		warning(message, source, null, cause);
	}

	/**
	 * Raise a non-critical warning.
	 */
	public void warning(String message, @Nullable Object source, @Nullable ParseState parseState) {
		warning(message, source, parseState, null);
	}

	/**
	 * Raise a non-critical warning.
	 */
	public void warning(String message, @Nullable Object source, @Nullable ParseState parseState, @Nullable Throwable cause) {
		Location location = new Location(getResource(), source);
		this.problemReporter.warning(new Problem(message, location, parseState, cause));
	}


	// Explicit parse events

	/**
	 * 触发默认注册的事件。
	 */
	public void fireDefaultsRegistered(DefaultsDefinition defaultsDefinition) {
		this.eventListener.defaultsRegistered(defaultsDefinition);
	}

	/**
	 * 触发组件注册的事件。
	 */
	public void fireComponentRegistered(ComponentDefinition componentDefinition) {
		this.eventListener.componentRegistered(componentDefinition);
	}

	/**
	 * 触发别名注册的事件。
	 */
	public void fireAliasRegistered(String beanName, String alias, @Nullable Object source) {
		this.eventListener.aliasRegistered(new AliasDefinition(beanName, alias, source));
	}

	/**
	 * Fire an import-processed event.
	 */
	public void fireImportProcessed(String importedResource, @Nullable Object source) {
		this.eventListener.importProcessed(new ImportDefinition(importedResource, source));
	}

	/**
	 * 触发导入处理的事件。
	 */
	public void fireImportProcessed(String importedResource, Resource[] actualResources, @Nullable Object source) {
		this.eventListener.importProcessed(new ImportDefinition(importedResource, actualResources, source));
	}


	// Source extraction

	/**
	 * Return the source extractor in use.
	 */
	public SourceExtractor getSourceExtractor() {
		return this.sourceExtractor;
	}

	/**
	 * 调用给定源对象的源提取器。
	 *
	 * @param sourceCandidate 原始源对象
	 * @return 要存储的源对象，可能为 {@code null} 或者无。
	 * @see #getSourceExtractor()
	 * @see SourceExtractor#extractSource
	 */
	@Nullable
	public Object extractSource(Object sourceCandidate) {
		return this.sourceExtractor.extractSource(sourceCandidate, this.resource);
	}

}
