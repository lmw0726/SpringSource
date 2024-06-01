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

package org.springframework.web.method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Predicate;

/**
 * 一个用于匹配请求处理组件类型的{@code Predicate}，如果<strong>任何</strong>以下选择器匹配：
 * <ul>
 * <li>基本包 - 通过它们的包选择处理程序。
 * <li>可分配类型 - 通过超类型选择处理程序。
 * <li>注解 - 通过特定方式注释的处理程序选择。
 * </ul>
 * <p>{@link Predicate}上的组合方法可以使用：
 * <pre class="code">
 * Predicate&lt;Class&lt;?&gt;&gt; predicate =
 *      HandlerTypePredicate.forAnnotation(RestController.class)
 *              .and(HandlerTypePredicate.forBasePackage("org.example"));
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public final class HandlerTypePredicate implements Predicate<Class<?>> {
	/**
	 * 基础包数组
	 */
	private final Set<String> basePackages;

	/**
	 * 可分配类型列表
	 */
	private final List<Class<?>> assignableTypes;

	/**
	 * 注解类型列表
	 */
	private final List<Class<? extends Annotation>> annotations;


	/**
	 * 私有构造函数。请参阅静态工厂方法。
	 */
	private HandlerTypePredicate(Set<String> basePackages, List<Class<?>> assignableTypes,
								 List<Class<? extends Annotation>> annotations) {

		this.basePackages = Collections.unmodifiableSet(basePackages);
		this.assignableTypes = Collections.unmodifiableList(assignableTypes);
		this.annotations = Collections.unmodifiableList(annotations);
	}


	@Override
	public boolean test(@Nullable Class<?> controllerType) {
		// 如果没有选择器，则返回 true
		if (!hasSelectors()) {
			return true;
		} else if (controllerType != null) {
			// 检查基础包
			for (String basePackage : this.basePackages) {
				if (controllerType.getName().startsWith(basePackage)) {
					// 如果控制器类型名称以基础包名开头，则返回true
					return true;
				}
			}
			// 检查可分配类型
			for (Class<?> clazz : this.assignableTypes) {
				if (ClassUtils.isAssignable(clazz, controllerType)) {
					// 如果控制器类型可分配给可分配类型，返回 true
					return true;
				}
			}
			// 检查注解
			for (Class<? extends Annotation> annotationClass : this.annotations) {
				if (AnnotationUtils.findAnnotation(controllerType, annotationClass) != null) {
					// 如果控制器类型包含注解，返回 true
					return true;
				}
			}
		}
		// 如果没有匹配，则返回 false
		return false;
	}

	private boolean hasSelectors() {
		return (!this.basePackages.isEmpty() || !this.assignableTypes.isEmpty() || !this.annotations.isEmpty());
	}


	// 静态工厂方法

	/**
	 * 应用于任何处理程序的{@code Predicate}。
	 */
	public static HandlerTypePredicate forAnyHandlerType() {
		return new HandlerTypePredicate(
				Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * 匹配在基本包下声明的处理程序，例如 "org.example"。
	 *
	 * @param packages 一个或多个基本包名称
	 */
	public static HandlerTypePredicate forBasePackage(String... packages) {
		return new Builder().basePackage(packages).build();
	}

	/**
	 * {@link #forBasePackage(String...)}的类型安全替代方法，通过类指定基本包。
	 *
	 * @param packageClasses 一个或多个基本包类
	 */
	public static HandlerTypePredicate forBasePackageClass(Class<?>... packageClasses) {
		return new Builder().basePackageClass(packageClasses).build();
	}

	/**
	 * 匹配可分配给给定类型的处理程序。
	 *
	 * @param types 一个或多个处理程序的超类型
	 */
	public static HandlerTypePredicate forAssignableType(Class<?>... types) {
		return new Builder().assignableType(types).build();
	}

	/**
	 * 匹配使用特定注解注释的处理程序。
	 *
	 * @param annotations 要检查的一个或多个注解
	 */
	@SafeVarargs
	public static HandlerTypePredicate forAnnotation(Class<? extends Annotation>... annotations) {
		return new Builder().annotation(annotations).build();
	}

	/**
	 * 返回一个{@code HandlerTypePredicate}的构建器。
	 */
	public static Builder builder() {
		return new Builder();
	}


	/**
	 * {@link HandlerTypePredicate}的构建器。
	 */
	public static class Builder {
		/**
		 * 基础包数组
		 */
		private final Set<String> basePackages = new LinkedHashSet<>();

		/**
		 * 可分配类型列表
		 */
		private final List<Class<?>> assignableTypes = new ArrayList<>();

		/**
		 * 注解类型列表
		 */
		private final List<Class<? extends Annotation>> annotations = new ArrayList<>();

		/**
		 * 匹配在基本包下声明的处理程序，例如 "org.example"。
		 *
		 * @param packages 一个或多个基本包类
		 */
		public Builder basePackage(String... packages) {
			Arrays.stream(packages).filter(StringUtils::hasText).forEach(this::addBasePackage);
			return this;
		}

		/**
		 * {@link #forBasePackage(String...)}的类型安全替代方法，通过类指定基本包。
		 *
		 * @param packageClasses 一个或多个基本包名称
		 */
		public Builder basePackageClass(Class<?>... packageClasses) {
			Arrays.stream(packageClasses).forEach(clazz -> addBasePackage(ClassUtils.getPackageName(clazz)));
			return this;
		}

		private void addBasePackage(String basePackage) {
			this.basePackages.add(basePackage.endsWith(".") ? basePackage : basePackage + ".");
		}

		/**
		 * 匹配可分配给给定类型的处理程序。
		 *
		 * @param types 一个或多个处理程序的超类型
		 */
		public Builder assignableType(Class<?>... types) {
			this.assignableTypes.addAll(Arrays.asList(types));
			return this;
		}

		/**
		 * 匹配带有给定注解之一的类型。
		 *
		 * @param annotations 要检查的一个或多个注解
		 */
		@SuppressWarnings("unchecked")
		public final Builder annotation(Class<? extends Annotation>... annotations) {
			this.annotations.addAll(Arrays.asList(annotations));
			return this;
		}

		public HandlerTypePredicate build() {
			return new HandlerTypePredicate(this.basePackages, this.assignableTypes, this.annotations);
		}
	}

}
