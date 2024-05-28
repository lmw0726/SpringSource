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

package org.springframework.web.util.pattern;

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * 表示路径模式的Ast节点所创建的通用超类型。
 *
 * @author Andy Clement
 * @since 5.0
 */
abstract class PathElement {

	/**
	 * 分数相关
	 * 通配符权重值
	 */
	protected static final int WILDCARD_WEIGHT = 100;

	/**
	 * 捕获可变权重值
	 */
	protected static final int CAPTURE_VARIABLE_WEIGHT = 1;

	/**
	 * 无参数
	 */
	protected static final MultiValueMap<String, String> NO_PARAMETERS = new LinkedMultiValueMap<>();

	/**
	 * 此路径元素在模式中开始的位置
	 */
	protected final int pos;

	/**
	 * 路径模式中使用的分隔符
	 */
	protected final char separator;

	/**
	 * 链中的下一个路径元素
	 */
	@Nullable
	protected PathElement next;

	/**
	 * 链中的上一个路径元素
	 */
	@Nullable
	protected PathElement prev;


	/**
	 * 创建一个新的路径元素。
	 *
	 * @param pos       此路径元素在模式数据中开始的位置
	 * @param separator 路径模式中使用的分隔符
	 */
	PathElement(int pos, char separator) {
		this.pos = pos;
		this.separator = separator;
	}


	/**
	 * 尝试匹配此路径元素。
	 *
	 * @param candidatePos    候选路径中的当前位置
	 * @param matchingContext 封装了匹配的上下文，包括候选路径
	 * @return 如果匹配成功，则返回 {@code true}，否则返回 {@code false}
	 */
	public abstract boolean matches(int candidatePos, MatchingContext matchingContext);

	/**
	 * 返回路径元素的长度，其中捕获被视为一个字符长。
	 *
	 * @return 规范化长度
	 */
	public abstract int getNormalizedLength();

	public abstract char[] getChars();

	/**
	 * 返回路径元素捕获的变量数。
	 */
	public int getCaptureCount() {
		return 0;
	}

	/**
	 * 返回路径元素中通配符元素（*、?）的数量。
	 */
	public int getWildcardCount() {
		return 0;
	}

	/**
	 * 返回此PathElement的分数，组合分数用于比较解析的模式。
	 */
	public int getScore() {
		return 0;
	}

	/**
	 * 返回模式中是否没有更多的PathElements。
	 *
	 * @return 如果没有更多元素，则返回 {@code true}
	 */
	protected final boolean isNoMorePattern() {
		return this.next == null;
	}

}
