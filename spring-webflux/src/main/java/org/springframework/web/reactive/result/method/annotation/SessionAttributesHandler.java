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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.server.WebSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 辅助 {@link ModelInitializer} 管理 {@link WebSession} 中的模型属性的 package-private 类。
 * 通过 {@link SessionAttributes @SessionAttributes} 声明的模型属性名称和类型在此进行管理。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class SessionAttributesHandler {

	/**
	 * 会话属性名称集合
	 */
	private final Set<String> attributeNames = new HashSet<>();

	/**
	 * 会话属性类型集合
	 */
	private final Set<Class<?>> attributeTypes = new HashSet<>();

	/**
	 * 已知的会话属性名称集合
	 */
	private final Set<String> knownAttributeNames = Collections.newSetFromMap(new ConcurrentHashMap<>(4));


	/**
	 * 创建一个新的会话属性处理程序。从给定类型的 {@code @SessionAttributes} 注解中提取会话属性名称和类型。
	 *
	 * @param handlerType 控制器类型
	 */
	public SessionAttributesHandler(Class<?> handlerType) {
		// 从给定类型中提取@SessionAttributes注解
		SessionAttributes ann = AnnotatedElementUtils.findMergedAnnotation(handlerType, SessionAttributes.class);
		if (ann != null) {
			//如果@SessionAttributes存在，则将它的名称和类型添加到上述的集合中。
			Collections.addAll(this.attributeNames, ann.names());
			Collections.addAll(this.attributeTypes, ann.types());
		}
		this.knownAttributeNames.addAll(this.attributeNames);
	}


	/**
	 * 此实例所代表的控制器是否通过 {@link SessionAttributes} 注解声明了任何会话属性。
	 */
	public boolean hasSessionAttributes() {
		//会话名称集合不能为空或会话类型集合不能为空
		return (!this.attributeNames.isEmpty() || !this.attributeTypes.isEmpty());
	}

	/**
	 * 属性名称或类型是否与底层控制器上的 {@code @SessionAttributes} 中指定的名称和类型匹配。
	 * <p>通过此方法成功解析的属性将被“记住”，并随后在 {@link #retrieveAttributes(WebSession)} 和 {@link #cleanupAttributes(WebSession)} 中使用。
	 *
	 * @param attributeName 要检查的属性名称
	 * @param attributeType 属性的类型
	 */
	public boolean isHandlerSessionAttribute(String attributeName, Class<?> attributeType) {
		// 确保属性名称不为 null
		Assert.notNull(attributeName, "Attribute name must not be null");

		if (this.attributeNames.contains(attributeName) || this.attributeTypes.contains(attributeType)) {
			// 检查属性名称或类型是否与已知属性名称集合或已知属性类型集合匹配
			// 将属性名称添加到已知属性名称集合中
			this.knownAttributeNames.add(attributeName);
			// 匹配成功
			return true;
		} else {
			// 未匹配成功
			return false;
		}
	}

	/**
	 * 从会话中检索“已知”属性，即在 {@code @SessionAttributes} 中按名称列出的属性，或以前与类型匹配的模型中存储的属性。
	 *
	 * @param session 当前会话
	 * @return 具有处理程序会话属性的映射，可能为空
	 */
	public Map<String, Object> retrieveAttributes(WebSession session) {
		Map<String, Object> attributes = new HashMap<>();
		this.knownAttributeNames.forEach(name -> {
			Object value = session.getAttribute(name);
			if (value != null) {
				attributes.put(name, value);
			}
		});
		return attributes;
	}

	/**
	 * 将给定属性的子集存储在会话中。
	 * 未通过 {@code @SessionAttributes} 声明为会话属性的属性将被忽略。
	 *
	 * @param session    当前会话
	 * @param attributes 用于会话存储的候选属性
	 */
	public void storeAttributes(WebSession session, Map<String, ?> attributes) {
		attributes.forEach((name, value) -> {
			if (value != null && isHandlerSessionAttribute(name, value.getClass())) {
				session.getAttributes().put(name, value);
			}
		});
	}

	/**
	 * 从会话中删除“已知”属性，即在 {@code @SessionAttributes} 中按名称列出的属性，或以前与类型匹配的模型中存储的属性。
	 *
	 * @param session 当前会话
	 */
	public void cleanupAttributes(WebSession session) {
		this.knownAttributeNames.forEach(name -> session.getAttributes().remove(name));
	}

}
