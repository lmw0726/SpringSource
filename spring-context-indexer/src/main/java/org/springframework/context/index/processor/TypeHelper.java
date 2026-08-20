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

package org.springframework.context.index.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 类型工具类。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
class TypeHelper {

	private final ProcessingEnvironment env;

	private final Types types;


	public TypeHelper(ProcessingEnvironment env) {
		this.env = env;
		this.types = env.getTypeUtils();
	}


	public String getType(Element element) {
		return getType(element != null ? element.asType() : null);
	}

	public String getType(AnnotationMirror annotation) {
		return getType(annotation != null ? annotation.getAnnotationType() : null);
	}

	public String getType(TypeMirror type) {
		if (type == null) {
			return null;
		}
		if (type instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) type;
			Element enclosingElement = declaredType.asElement().getEnclosingElement();
			if (enclosingElement instanceof TypeElement) {
				return getQualifiedName(enclosingElement) + "$" + declaredType.asElement().getSimpleName().toString();
			}
			else {
				return getQualifiedName(declaredType.asElement());
			}
		}
		return type.toString();
	}

	private String getQualifiedName(Element element) {
		if (element instanceof QualifiedNameable) {
			return ((QualifiedNameable) element).getQualifiedName().toString();
		}
		return element.toString();
	}

	/**
	 * 返回指定{@link Element} 的父类，如果该
	 * {@code element} 表示 {@link Object}，则返回 null。
	 */
	public Element getSuperClass(Element element) {
		List<? extends TypeMirror> superTypes = this.types.directSupertypes(element.asType());
		if (superTypes.isEmpty()) {
			return null;  // 已到达 java.lang.Object
		}
		return this.types.asElement(superTypes.get(0));
	}

	/**
	 * 返回指定 {@link Element} <strong>直接</strong>实现的接口列表，
	 * 如果该 {@code element} 未实现任何接口，则返回空列表。
	 */
	public List<Element> getDirectInterfaces(Element element) {
		List<? extends TypeMirror> superTypes = this.types.directSupertypes(element.asType());
		List<Element> directInterfaces = new ArrayList<>();
		if (superTypes.size() > 1) { // 索引 0 是父类
			for (int i = 1; i < superTypes.size(); i++) {
				Element e = this.types.asElement(superTypes.get(i));
				if (e != null) {
					directInterfaces.add(e);
				}
			}
		}
		return directInterfaces;
	}

	public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
		try {
			return this.env.getElementUtils().getAllAnnotationMirrors(e);
		}
		catch (Exception ex) {
			// 如果某个注解不可用，此处可能会失败。
			return Collections.emptyList();
		}
	}

}
