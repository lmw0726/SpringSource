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

package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * {@link ImportSelector} 的一种变体，在所有 {@code @Configuration} Bean 被处理完之后运行。
 * 当所选导入类带有 {@code @Conditional} 条件注解时，这种选择器特别有用。
 *
 * <p>实现类也可以扩展 {@link org.springframework.core.Ordered} 接口，或使用
 * {@link org.springframework.core.annotation.Order} 注解，以指定相对于其他
 * {@link DeferredImportSelector 延迟导入选择器} 的优先级。
 *
 * <p>实现类还可以提供一个 {@link #getImportGroup() 导入分组}，用于在不同选择器之间进行
 * 额外的排序和过滤逻辑。
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0
 */
public interface DeferredImportSelector extends ImportSelector {

	/**
	 * 返回一个特定的导入分组类。
	 * <p>默认实现返回 {@code null}，表示不需要分组。
	 * @return 导入分组类，如果不需要则返回 {@code null}
	 * @since 5.0
	 */
	@Nullable
	default Class<? extends Group> getImportGroup() {
		return null;
	}


	/**
	 * 用于将不同导入选择器的结果进行分组的接口。
	 * @since 5.0
	 */
	interface Group {

		/**
		 * 使用指定的 {@link DeferredImportSelector} 处理导入类（带有 @{@link Configuration} 注解）的
		 * {@link AnnotationMetadata} 元数据。
		 */
		void process(AnnotationMetadata metadata, DeferredImportSelector selector);

		/**
		 * 返回应该为该分组导入的类的 {@link Entry 条目}。
		 */
		Iterable<Entry> selectImports();


		/**
		 * 一个条目，包含导入的 {@link Configuration} 类的 {@link AnnotationMetadata} 元数据，
		 * 以及要导入的类名。
		 */
		class Entry {

			private final AnnotationMetadata metadata;

			private final String importClassName;

			public Entry(AnnotationMetadata metadata, String importClassName) {
				this.metadata = metadata;
				this.importClassName = importClassName;
			}

			/**
			 * 返回导入的 {@link Configuration} 类的 {@link AnnotationMetadata} 元数据。
			 */
			public AnnotationMetadata getMetadata() {
				return this.metadata;
			}

			/**
			 * 返回要导入类的全限定类名。
			 */
			public String getImportClassName() {
				return this.importClassName;
			}

			@Override
			public boolean equals(@Nullable Object other) {
				if (this == other) {
					return true;
				}
				if (other == null || getClass() != other.getClass()) {
					return false;
				}
				Entry entry = (Entry) other;
				return (this.metadata.equals(entry.metadata) && this.importClassName.equals(entry.importClassName));
			}

			@Override
			public int hashCode() {
				return (this.metadata.hashCode() * 31 + this.importClassName.hashCode());
			}

			@Override
			public String toString() {
				return this.importClassName;
			}
		}
	}

}
