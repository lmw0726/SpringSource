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

package org.springframework.core.type.classreading;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ASM 访问者，用于查找类或方法上定义的注解，
 * 包括元注解。
 *
 * <p>该访问者是完全递归的，会处理任何嵌套注解或嵌套注解数组。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.0
 * @deprecated 自 Spring Framework 5.2 起，此类及本包中相关类
 * 已被 {@link SimpleAnnotationMetadataReadingVisitor} 及其相关类替代，
 * 仅供框架内部使用。
 */
@Deprecated
final class AnnotationAttributesReadingVisitor extends RecursiveAnnotationAttributesVisitor {

	private final MultiValueMap<String, AnnotationAttributes> attributesMap;

	private final Map<String, Set<String>> metaAnnotationMap;


	public AnnotationAttributesReadingVisitor(String annotationType,
			MultiValueMap<String, AnnotationAttributes> attributesMap, Map<String, Set<String>> metaAnnotationMap,
			@Nullable ClassLoader classLoader) {

		super(annotationType, new AnnotationAttributes(annotationType, classLoader), classLoader);
		this.attributesMap = attributesMap;
		this.metaAnnotationMap = metaAnnotationMap;
	}


	@Override
	public void visitEnd() {
		super.visitEnd();

		Class<? extends Annotation> annotationClass = this.attributes.annotationType();
		if (annotationClass != null) {
			List<AnnotationAttributes> attributeList = this.attributesMap.get(this.annotationType);
			if (attributeList == null) {
				this.attributesMap.add(this.annotationType, this.attributes);
			}
			else {
				attributeList.add(0, this.attributes);
			}
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationClass.getName())) {
				try {
					Annotation[] metaAnnotations = annotationClass.getAnnotations();
					if (!ObjectUtils.isEmpty(metaAnnotations)) {
						Set<Annotation> visited = new LinkedHashSet<>();
						for (Annotation metaAnnotation : metaAnnotations) {
							recursivelyCollectMetaAnnotations(visited, metaAnnotation);
						}
						if (!visited.isEmpty()) {
							Set<String> metaAnnotationTypeNames = new LinkedHashSet<>(visited.size());
							for (Annotation ann : visited) {
								metaAnnotationTypeNames.add(ann.annotationType().getName());
							}
							this.metaAnnotationMap.put(annotationClass.getName(), metaAnnotationTypeNames);
						}
					}
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to introspect meta-annotations on " + annotationClass + ": " + ex);
					}
				}
			}
		}
	}

	private void recursivelyCollectMetaAnnotations(Set<Annotation> visited, Annotation annotation) {
		Class<? extends Annotation> annotationType = annotation.annotationType();
		String annotationName = annotationType.getName();
		if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationName) && visited.add(annotation)) {
			try {
				// 仅对公共注解进行属性扫描；
				// 否则可能遇到 IllegalAccessException，
				// 并且我们不想在 SecurityManager 环境中调整访问权限。
				if (Modifier.isPublic(annotationType.getModifiers())) {
					this.attributesMap.add(annotationName,
							AnnotationUtils.getAnnotationAttributes(annotation, false, true));
				}
				for (Annotation metaMetaAnnotation : annotationType.getAnnotations()) {
					recursivelyCollectMetaAnnotations(visited, metaMetaAnnotation);
				}
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to introspect meta-annotations on " + annotation + ": " + ex);
				}
			}
		}
	}

}
