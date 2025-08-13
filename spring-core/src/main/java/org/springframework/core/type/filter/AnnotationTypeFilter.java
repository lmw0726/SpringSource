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

package org.springframework.core.type.filter;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;

/**
 * 一个简单的 {@link TypeFilter}，用于匹配带有指定注解的类，
 * 也会检查继承的注解。
 *
 * <p>默认情况下，匹配逻辑与
 * {@link AnnotationUtils#getAnnotation(java.lang.reflect.AnnotatedElement, Class)}
 * 一致，支持单层元注解的 <em>存在</em> 或 <em>元存在</em> 注解。
 * 可以禁用对元注解的搜索。同样，也可以选择是否启用对接口注解的搜索。
 * 详细信息请查看本类的各种构造函数。
 *
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
public class AnnotationTypeFilter extends AbstractTypeHierarchyTraversingFilter {

	private final Class<? extends Annotation> annotationType;

	private final boolean considerMetaAnnotations;


	/**
	 * 创建一个新的 {@code AnnotationTypeFilter} 用于指定的注解类型。
	 * <p>该过滤器也会匹配元注解。若要禁用元注解匹配，
	 * 请使用带有 {@code considerMetaAnnotations} 参数的构造函数。
	 * <p>该过滤器不会匹配接口。
	 * @param annotationType 要匹配的注解类型
	 */
	public AnnotationTypeFilter(Class<? extends Annotation> annotationType) {
		this(annotationType, true, false);
	}

	/**
	 * 创建一个新的 {@code AnnotationTypeFilter} 用于指定的注解类型。
	 * <p>该过滤器不会匹配接口。
	 * @param annotationType 要匹配的注解类型
	 * @param considerMetaAnnotations 是否也匹配元注解
	 */
	public AnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations) {
		this(annotationType, considerMetaAnnotations, false);
	}

	/**
	 * 创建一个新的 {@code AnnotationTypeFilter} 用于指定的注解类型。
	 * @param annotationType 要匹配的注解类型
	 * @param considerMetaAnnotations 是否也匹配元注解
	 * @param considerInterfaces 是否也匹配接口
	 */
	public AnnotationTypeFilter(
			Class<? extends Annotation> annotationType, boolean considerMetaAnnotations, boolean considerInterfaces) {

		super(annotationType.isAnnotationPresent(Inherited.class), considerInterfaces);
		this.annotationType = annotationType;
		this.considerMetaAnnotations = considerMetaAnnotations;
	}

	/**
	 * 返回此实例用于过滤候选类的 {@link Annotation} 类型。
	 * @since 5.0
	 */
	public final Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	@Override
	protected boolean matchSelf(MetadataReader metadataReader) {
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		return metadata.hasAnnotation(this.annotationType.getName()) ||
				(this.considerMetaAnnotations && metadata.hasMetaAnnotation(this.annotationType.getName()));
	}

	@Override
	@Nullable
	protected Boolean matchSuperClass(String superClassName) {
		return hasAnnotation(superClassName);
	}

	@Override
	@Nullable
	protected Boolean matchInterface(String interfaceName) {
		return hasAnnotation(interfaceName);
	}

	@Nullable
	protected Boolean hasAnnotation(String typeName) {
		if (Object.class.getName().equals(typeName)) {
			return false;
		}
		else if (typeName.startsWith("java")) {
			if (!this.annotationType.getName().startsWith("java")) {
				// 标准 Java 类型上不会有非标准注解，尤其是 Java 语言接口，
				// 因此跳过尝试加载的操作。
				return false;
			}
			try {
				Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
				return ((this.considerMetaAnnotations ? AnnotationUtils.getAnnotation(clazz, this.annotationType) :
						clazz.getAnnotation(this.annotationType)) != null);
			}
			catch (Throwable ex) {
				// 类无法正常加载，无法通过此方式判断匹配。
			}
		}
		return null;
	}

}
