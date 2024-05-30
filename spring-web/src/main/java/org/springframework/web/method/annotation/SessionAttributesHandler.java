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

package org.springframework.web.method.annotation;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.WebRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理通过{@link SessionAttributes @SessionAttributes}声明的特定于控制器的会话属性。
 * 实际存储被委托给{@link SessionAttributeStore}实例。
 *
 * <p>当标记有{@code @SessionAttributes}注解的控制器将属性添加到其模型时，这些属性将与通过
 * {@code @SessionAttributes}指定的名称和类型进行匹配。匹配的模型属性将保存在HTTP会话中，
 * 并在控制器调用{@link SessionStatus#setComplete()}时保留在那里。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class SessionAttributesHandler {
	/**
	 * 存放 @SessionAttribute 的属性名称集合
	 */
	private final Set<String> attributeNames = new HashSet<>();

	/**
	 * 存放 @SessionAttribute 的属性类型集合
	 */
	private final Set<Class<?>> attributeTypes = new HashSet<>();

	/**
	 * 已知的属性名称集合
	 */
	private final Set<String> knownAttributeNames = Collections.newSetFromMap(new ConcurrentHashMap<>(4));

	/**
	 * 会话属性值存储
	 */
	private final SessionAttributeStore sessionAttributeStore;


	/**
	 * 创建一个新的会话属性处理程序。会话属性名称和类型从给定类型的{@code @SessionAttributes}注解中提取。
	 *
	 * @param handlerType           控制器类型
	 * @param sessionAttributeStore 用于会话访问的SessionAttributeStore
	 */
	public SessionAttributesHandler(Class<?> handlerType, SessionAttributeStore sessionAttributeStore) {
		// 确保 会话属性值存储 不为空，如果为空则抛出异常
		Assert.notNull(sessionAttributeStore, "SessionAttributeStore may not be null");
		// 将传入的 会话属性值存储 赋值给当前对象的 会话属性值存储 属性
		this.sessionAttributeStore = sessionAttributeStore;

		// 查找 处理类 上是否存在 SessionAttributes 注解，并获取该注解
		SessionAttributes ann = AnnotatedElementUtils.findMergedAnnotation(handlerType, SessionAttributes.class);
		// 如果存在@SessionAttributes注解
		if (ann != null) {
			// 将注解中的属性名添加到 属性名称 集合中
			Collections.addAll(this.attributeNames, ann.names());
			// 将注解中的属性类型添加到 属性类型 集合中
			Collections.addAll(this.attributeTypes, ann.types());
		}
		// 将 属性名称 集合中的所有元素添加到 已知的属性名称集合 集合中
		this.knownAttributeNames.addAll(this.attributeNames);
	}


	/**
	 * 此实例表示的控制器是否通过{@link SessionAttributes @SessionAttributes}注解声明了任何会话属性。
	 */
	public boolean hasSessionAttributes() {
		return (!this.attributeNames.isEmpty() || !this.attributeTypes.isEmpty());
	}

	/**
	 * 属性名称或类型是否与基础控制器上通过{@code @SessionAttributes}指定的名称和类型匹配。
	 * <p>通过此方法成功解析的属性会被“记住”，并在{@link #retrieveAttributes(WebRequest)}和
	 * {@link #cleanupAttributes(WebRequest)}中后续使用。
	 *
	 * @param attributeName 属性名称
	 * @param attributeType 属性类型
	 */
	public boolean isHandlerSessionAttribute(String attributeName, Class<?> attributeType) {
		// 确保 属性名称 不为空，如果为空则抛出异常
		Assert.notNull(attributeName, "Attribute name must not be null");
		// 判断 属性名称集合 中是否包含 属性名称 或 属性类型集合 中是否包含 属性类型
		if (this.attributeNames.contains(attributeName) || this.attributeTypes.contains(attributeType)) {
			// 如果包含，将 属性名称 添加到 已知属性名称集合 中
			this.knownAttributeNames.add(attributeName);
			// 返回 true，表示该属性名或属性类型是已知的
			return true;
		} else {
			// 返回 false，表示该属性名或属性类型不是已知的
			return false;
		}

	}

	/**
	 * 将给定属性的子集存储在会话中。未通过{@code @SessionAttributes}声明为会话属性的属性将被忽略。
	 *
	 * @param request    当前请求
	 * @param attributes 候选的会话存储属性
	 */
	public void storeAttributes(WebRequest request, Map<String, ?> attributes) {
		// 遍历 attributes 集合中的每个键值对
		attributes.forEach((name, value) -> {
			// 如果值不为空且该属性名和属性类型是处理器的会话属性
			if (value != null && isHandlerSessionAttribute(name, value.getClass())) {
				// 将该属性存储到会话中
				this.sessionAttributeStore.storeAttribute(request, name, value);
			}
		});
	}

	/**
	 * 从会话中检索“已知”属性，即在{@code @SessionAttributes}中列出的名称或以前存储在模型中的匹配类型的属性。
	 *
	 * @param request 当前请求
	 * @return 带有处理程序会话属性的映射，可能为空
	 */
	public Map<String, Object> retrieveAttributes(WebRequest request) {
		// 创建一个新的 HashMap 来存储属性
		Map<String, Object> attributes = new HashMap<>();
		// 遍历已知的属性名称集合
		for (String name : this.knownAttributeNames) {
			// 从会话中检索该属性名对应的值
			Object value = this.sessionAttributeStore.retrieveAttribute(request, name);
			// 如果值不为空，则将其添加到属性映射中
			if (value != null) {
				attributes.put(name, value);
			}
		}
		// 返回包含属性的映射
		return attributes;
	}

	/**
	 * 从会话中删除“已知”属性，即在{@code @SessionAttributes}中列出的名称或以前存储在模型中的匹配类型的属性。
	 *
	 * @param request 当前请求
	 */
	public void cleanupAttributes(WebRequest request) {
		// 遍历已知的属性名称集合
		for (String attributeName : this.knownAttributeNames) {
			// 清理会话中的该属性名对应的值
			this.sessionAttributeStore.cleanupAttribute(request, attributeName);
		}
	}

	/**
	 * 对底层的{@link SessionAttributeStore}进行的一个透传调用。
	 *
	 * @param request       当前请求
	 * @param attributeName 属性的名称
	 * @return 属性值，如果没有则为{@code null}
	 */
	@Nullable
	Object retrieveAttribute(WebRequest request, String attributeName) {
		return this.sessionAttributeStore.retrieveAttribute(request, attributeName);
	}

}
