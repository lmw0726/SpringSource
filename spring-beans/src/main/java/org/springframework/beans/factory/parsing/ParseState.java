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

import org.springframework.lang.Nullable;

import java.util.ArrayDeque;

/**
 * 基于{@link ArrayDeque}的简单结构，用于在解析过程中跟踪逻辑位置。
 * {@link Entry 条目}在解析阶段的每个点以特定于读取器的方式添加到ArrayDeque中。
 *
 * <p>调用{@link #toString()}将呈现解析阶段当前逻辑位置的树状视图。
 * 此表示形式旨在用于错误消息中。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public final class ParseState {

	/**
	 * 内部 {@link ArrayDeque} 存储。
	 */
	private final ArrayDeque<Entry> state;


	/**
	 * 用空的 {@link ArrayDeque} 创建一个新的 {@code ParseState}。
	 */
	public ParseState() {
		this.state = new ArrayDeque<>();
	}

	/**
	 * 创建一个新的 {@code ParseState}，其 {@link ArrayDeque} 是传入 {@code ParseState} 中状态的克隆。
	 */
	private ParseState(ParseState other) {
		this.state = other.state.clone();
	}


	/**
	 * 为 {@link ArrayDeque} 添加一个新的 {@link Entry}。
	 */
	public void push(Entry entry) {
		this.state.push(entry);
	}

	/**
	 * 从 {@link ArrayDeque} 中删除 {@link Entry}。
	 */
	public void pop() {
		this.state.pop();
	}

	/**
	 * 如果 {@link ArrayDeque} 为空，则返回当前位于 {@link ArrayDeque} 顶部的 {@link Entry} 或 {@code null}。
	 */
	@Nullable
	public Entry peek() {
		return this.state.peek();
	}

	/**
	 * 创建一个 {@link ParseState} 的新实例，该实例是该实例的独立快照。
	 */
	public ParseState snapshot() {
		return new ParseState(this);
	}


	/**
	 * 返回当前 {@code ParseState} 的树状表示形式。
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		int i = 0;
		for (ParseState.Entry entry : this.state) {
			if (i > 0) {
				sb.append('\n');
				for (int j = 0; j < i; j++) {
					sb.append('\t');
				}
				sb.append("-> ");
			}
			sb.append(entry);
			i++;
		}
		return sb.toString();
	}


	/**
	 * {@link ParseState} 条目的标记接口。
	 */
	public interface Entry {
	}

}
