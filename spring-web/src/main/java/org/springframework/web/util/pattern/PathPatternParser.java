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

package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer;

/**
 * 解析 URI 路径模式的解析器，生成 {@link PathPattern} 实例，然后可以将其与请求进行匹配。
 *
 * <p>{@link PathPatternParser} 和 {@link PathPattern} 是专门为 Web 应用程序中的 HTTP URL 路径设计的，
 * 在这些应用程序中，大量的 URI 路径模式连续匹配传入请求，这促使了对匹配的高效性的需求。
 *
 * <p>有关路径模式语法的详细信息，请参见 {@link PathPattern}。
 *
 * @author Andy Clement
 * @since 5.0
 */
public class PathPatternParser {

	/**
	 * 是否匹配可选的尾随斜杠，默认为 {@code true}。
	 */
	private boolean matchOptionalTrailingSeparator = true;

	/**
	 * 是否启用大小写敏感的模式匹配，默认为 {@code true}。
	 */
	private boolean caseSensitive = true;

	/**
	 * 解析模式的选项，默认为 {@link org.springframework.http.server.PathContainer.Options#HTTP_PATH}。
	 */
	private PathContainer.Options pathOptions = PathContainer.Options.HTTP_PATH;


	/**
	 * 设置此解析器生成的 {@link PathPattern} 是否应自动匹配带有尾随斜杠的请求路径。
	 * <p>如果设置为 {@code true}，则没有尾随斜杠的 {@code PathPattern} 也将匹配带有尾随斜杠的请求路径。
	 * 如果设置为 {@code false}，则 {@code PathPattern} 仅匹配带有尾随斜杠的请求路径。
	 * <p>默认值为 {@code true}。
	 *
	 * @param matchOptionalTrailingSeparator 是否匹配可选的尾随斜杠
	 */
	public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
		this.matchOptionalTrailingSeparator = matchOptionalTrailingSeparator;
	}

	/**
	 * 返回是否启用了可选尾部斜杠匹配。
	 */
	public boolean isMatchOptionalTrailingSeparator() {
		return this.matchOptionalTrailingSeparator;
	}

	/**
	 * 设置路径模式匹配是否区分大小写。
	 * <p>默认为 {@code true}。
	 *
	 * @param caseSensitive 是否区分大小写的模式匹配
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * 返回是否启用了区分大小写的模式匹配。
	 */
	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	/**
	 * 设置用于解析模式的选项。这些选项应与用于解析输入路径的选项相同。
	 * <p>默认使用 {@link org.springframework.http.server.PathContainer.Options#HTTP_PATH}。
	 *
	 * @since 5.2
	 */
	public void setPathOptions(PathContainer.Options pathOptions) {
		this.pathOptions = pathOptions;
	}

	/**
	 * 返回已配置的模式解析选项。
	 *
	 * @since 5.2
	 */
	public PathContainer.Options getPathOptions() {
		return this.pathOptions;
	}


	/**
	 * 处理路径模式内容，逐个字符地处理，将其分解为围绕分隔符边界的路径元素，并在每个阶段验证结构。
	 * 生成一个 {@link PathPattern} 对象，可用于快速匹配路径。
	 * 每次调用此方法都会委托给 {@link InternalPathPatternParser} 的新实例，
	 * 因为该类不是线程安全的。
	 *
	 * @param pathPattern 输入路径模式，例如 /project/{name}
	 * @return 用于快速匹配路径的 PathPattern
	 * @throws PatternParseException 如果解析出错
	 */
	public PathPattern parse(String pathPattern) throws PatternParseException {
		return new InternalPathPatternParser(this).parse(pathPattern);
	}


	/**
	 * 共享的只读 {@code PathPatternParser} 实例。使用默认设置：
	 * <ul>
	 * <li>{@code matchOptionalTrailingSeparator=true}
	 * <li>{@code caseSensitive=true}
	 * <li>{@code pathOptions=PathContainer.Options.HTTP_PATH}
	 * </ul>
	 */
	public final static PathPatternParser defaultInstance = new PathPatternParser() {

		@Override
		public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
			raiseError();
		}

		@Override
		public void setCaseSensitive(boolean caseSensitive) {
			raiseError();
		}

		@Override
		public void setPathOptions(PathContainer.Options pathOptions) {
			raiseError();
		}

		private void raiseError() {
			throw new UnsupportedOperationException(
					"This is a read-only, shared instance that cannot be modified");
		}
	};
}
