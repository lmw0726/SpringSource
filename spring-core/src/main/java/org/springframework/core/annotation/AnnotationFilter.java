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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

/**
 * 可用于过滤特定注解类型的回调接口。
 *
 * <p>请注意，{@link MergedAnnotations} 模型（此接口为此而设计）
 * 始终根据 {@link #PLAIN} 过滤器（出于效率原因）忽略语言注解。
 * 任何额外的过滤器甚至自定义过滤器实现都在此边界内应用，并且只能在此基础上进一步缩小范围。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 5.2
 * @see MergedAnnotations
 */
@FunctionalInterface
public interface AnnotationFilter {

	/**
	 * {@link AnnotationFilter} 匹配 {@code java.lang} 和 {@code org.springframework.lang} 包
	 * 及其子包中的注解。
	 * <p>这是 {@link MergedAnnotations} 模型中的默认过滤器。
	 */
	AnnotationFilter PLAIN = packages("java.lang", "org.springframework.lang");

	/**
	 * {@link AnnotationFilter} 匹配 {@code java} 和 {@code javax} 包
	 * 及其子包中的注解。
	 */
	AnnotationFilter JAVA = packages("java", "javax");

	/**
	 * {@link AnnotationFilter} 始终匹配，可在预期根本不存在任何相关注解类型时使用。
	 */
	AnnotationFilter ALL = new AnnotationFilter() {
		@Override
		public boolean matches(Annotation annotation) {
			return true;
		}
		@Override
		public boolean matches(Class<?> type) {
			return true;
		}
		@Override
		public boolean matches(String typeName) {
			return true;
		}
		@Override
		public String toString() {
			return "All annotations filtered";
		}
	};

	/**
	 * {@link AnnotationFilter} 从不匹配，可在不需要过滤时使用（允许存在任何注解类型）。
	 * @see #PLAIN
	 * @deprecated 自 5.2.6 版本起已废弃，因为 {@link MergedAnnotations} 模型
	 * 始终根据 {@link #PLAIN} 过滤器（出于效率原因）忽略语言注解。
	 */
	@Deprecated
	AnnotationFilter NONE = new AnnotationFilter() {
		@Override
		public boolean matches(Annotation annotation) {
			return false;
		}
		@Override
		public boolean matches(Class<?> type) {
			return false;
		}
		@Override
		public boolean matches(String typeName) {
			return false;
		}
		@Override
		public String toString() {
			return "No annotation filtering";
		}
	};


	/**
	 * 测试给定的注解是否匹配过滤器。
	 * @param annotation 要测试的注解
	 * @return 如果注解匹配，则为 {@code true}
	 */
	default boolean matches(Annotation annotation) {
		return matches(annotation.annotationType());
	}

	/**
	 * 测试给定的类型是否匹配过滤器。
	 * @param type 要测试的注解类型
	 * @return 如果注解匹配，则为 {@code true}
	 */
	default boolean matches(Class<?> type) {
		return matches(type.getName());
	}

	/**
	 * 测试给定的类型名是否匹配过滤器。
	 * @param typeName 要测试的注解类型的完全限定类名
	 * @return 如果注解匹配，则为 {@code true}
	 */
	boolean matches(String typeName);


	/**
	 * 创建一个新的 {@link AnnotationFilter}，匹配指定包中的注解。
	 * @param packages 应该匹配的注解包
	 * @return 新的 {@link AnnotationFilter} 实例
	 */
	static AnnotationFilter packages(String... packages) {
		return new PackagesAnnotationFilter(packages);
	}

}
