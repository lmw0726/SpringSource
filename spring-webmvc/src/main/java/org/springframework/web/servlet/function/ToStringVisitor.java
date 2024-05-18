/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.function;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * 实现{@link RouterFunctions.Visitor}，用于创建路由函数的格式化字符串表示。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class ToStringVisitor implements RouterFunctions.Visitor, RequestPredicates.Visitor {
	/**
	 * 字符串构建器
	 */
	private final StringBuilder builder = new StringBuilder();
	/**
	 * 缩进数量
	 */
	private int indent = 0;


	// RouterFunctions.Visitor

	@Override
	public void startNested(RequestPredicate predicate) {
		// 缩进代码块
		indent();
		// 调用谓词对象的accept方法，将当前对象传递给它
		predicate.accept(this);
		// 在生成器中添加字符串 " => {\n"
		this.builder.append(" => {\n");
		// 增加缩进层级
		this.indent++;
	}

	@Override
	public void endNested(RequestPredicate predicate) {
		// 减少缩进层级
		this.indent--;
		// 缩进代码块
		indent();
		// 在生成器中添加字符串 "}\n"
		this.builder.append("}\n");
	}

	@Override
	public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
		// 缩进代码块
		indent();
		// 访问谓词
		predicate.accept(this);
		// 在生成器中添加字符串 " -> "
		this.builder.append(" -> ");
		// 将处理函数添加到生成器中，并在结尾添加换行符
		this.builder.append(handlerFunction).append('\n');
	}

	@Override
	public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
		// 缩进代码块
		indent();
		// 在生成器中添加查找函数，并在结尾添加换行符
		this.builder.append(lookupFunction).append('\n');
	}

	@Override
	public void attributes(Map<String, Object> attributes) {
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {
		// 缩进代码块
		indent();
		// 在生成器中添加路由函数
		this.builder.append(routerFunction);
	}

	private void indent() {
		for (int i = 0; i < this.indent; i++) {
			this.builder.append(' ');
		}
	}


	// RequestPredicates.Visitor

	@Override
	public void method(Set<HttpMethod> methods) {
		// 如果方法集合中只有一个方法，则直接添加该方法
		if (methods.size() == 1) {
			this.builder.append(methods.iterator().next());
		} else {
			// 否则将方法集合作为字符串添加到生成器中
			this.builder.append(methods);
		}
	}

	@Override
	public void path(String pattern) {
		this.builder.append(pattern);
	}

	@Override
	public void pathExtension(String extension) {
		this.builder.append(String.format("*.%s", extension));
	}

	@Override
	public void header(String name, String value) {
		this.builder.append(String.format("%s: %s", name, value));
	}

	@Override
	public void param(String name, String value) {
		this.builder.append(String.format("?%s == %s", name, value));
	}

	@Override
	public void startAnd() {
		this.builder.append('(');
	}

	@Override
	public void and() {
		this.builder.append(" && ");
	}

	@Override
	public void endAnd() {
		this.builder.append(')');
	}

	@Override
	public void startOr() {
		this.builder.append('(');
	}

	@Override
	public void or() {
		this.builder.append(" || ");
	}

	@Override
	public void endOr() {
		this.builder.append(')');
	}

	@Override
	public void startNegate() {
		this.builder.append("!(");
	}

	@Override
	public void endNegate() {
		this.builder.append(')');
	}

	@Override
	public void unknown(RequestPredicate predicate) {
		this.builder.append(predicate);
	}

	@Override
	public String toString() {
		// 将生成器内容转换为字符串
		String result = this.builder.toString();
		// 如果字符串末尾有换行符，则去除末尾的换行符
		if (result.endsWith("\n")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

}
