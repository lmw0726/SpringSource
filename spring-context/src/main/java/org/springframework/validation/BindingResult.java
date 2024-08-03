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

package org.springframework.validation;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.lang.Nullable;

import java.beans.PropertyEditor;
import java.util.Map;

/**
 * 表示绑定结果的通用接口。扩展了 {@link Errors} 接口，提供错误注册功能，
 * 允许应用 {@link Validator}，并增加了绑定特定的分析和模型构建。
 *
 * <p>作为 {@link DataBinder} 的结果持有器，通过 {@link DataBinder#getBindingResult()} 方法获取。
 * BindingResult 实现也可以直接使用，例如在单元测试中调用 {@link Validator}。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DataBinder
 * @see Errors
 * @see Validator
 * @see BeanPropertyBindingResult
 * @see DirectFieldBindingResult
 * @see MapBindingResult
 */
public interface BindingResult extends Errors {

	/**
	 * 模型中 BindingResult 实例名称的前缀，
	 * 后跟对象名称。
	 */
	String MODEL_KEY_PREFIX = BindingResult.class.getName() + ".";


	/**
	 * 返回包装的目标对象，该对象可以是一个 bean、一个具有公共字段的对象或一个 Map，
	 * 具体取决于绑定策略。
	 */
	@Nullable
	Object getTarget();

	/**
	 * 返回模型 Map，暴露一个 BindingResult 实例作为 '{@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName'
	 * 和对象本身作为 'objectName'。
	 * <p>请注意，每次调用此方法时都会构造 Map。
	 * 向 Map 中添加内容后再重新调用此方法将不起作用。
	 * <p>通过此方法返回的模型 Map 中的属性通常包含在使用 Spring 的 {@code bind} 标签的 JSP 表单视图的
	 * {@link org.springframework.web.servlet.ModelAndView} 中，提供对 BindingResult 实例的访问。
	 * Spring 的预构建表单控制器在渲染表单视图时会为您执行此操作。
	 * 在自己构建 ModelAndView 实例时，需要包括从此方法返回的模型 Map 中的属性。
	 * @see #getObjectName()
	 * @see #MODEL_KEY_PREFIX
	 * @see org.springframework.web.servlet.ModelAndView
	 * @see org.springframework.web.servlet.tags.BindTag
	 */
	Map<String, Object> getModel();

	/**
	 * 提取给定字段的原始字段值。
	 * 通常用于比较目的。
	 * @param field 要检查的字段
	 * @return 字段的当前值（以原始形式），如果未知则为 {@code null}
	 */
	@Nullable
	Object getRawFieldValue(String field);

	/**
	 * 查找给定类型和属性的自定义属性编辑器。
	 * @param field 属性的路径（名称或嵌套路径），如果查找所有属性的编辑器则为 {@code null}
	 * @param valueType 属性的类型（如果给定属性，可能为 {@code null}，但为一致性检查应指定）
	 * @return 注册的编辑器，如果没有则为 {@code null}
	 */
	@Nullable
	PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType);

	/**
	 * 返回底层的 PropertyEditorRegistry。
	 * @return PropertyEditorRegistry，如果没有则为 {@code null}
	 */
	@Nullable
	PropertyEditorRegistry getPropertyEditorRegistry();

	/**
	 * 将给定错误代码解析为消息代码。
	 * <p>调用配置的 {@link MessageCodesResolver} 以适当的参数。
	 * @param errorCode 要解析为消息代码的错误代码
	 * @return 解析的消息代码
	 */
	String[] resolveMessageCodes(String errorCode);

	/**
	 * 将给定错误代码解析为给定字段的消息代码。
	 * <p>调用配置的 {@link MessageCodesResolver} 以适当的参数。
	 * @param errorCode 要解析为消息代码的错误代码
	 * @param field 要解析消息代码的字段
	 * @return 解析的消息代码
	 */
	String[] resolveMessageCodes(String errorCode, String field);

	/**
	 * 将自定义的 {@link ObjectError} 或 {@link FieldError} 添加到错误列表中。
	 * <p>打算由协作策略如 {@link BindingErrorProcessor} 使用。
	 * @see ObjectError
	 * @see FieldError
	 * @see BindingErrorProcessor
	 */
	void addError(ObjectError error);

	/**
	 * 记录指定字段的值。
	 * <p>当目标对象无法构造时使用，使得原始字段值可以通过 {@link #getFieldValue} 获取。
	 * 在注册错误的情况下，将为每个受影响的字段暴露被拒绝的值。
	 * @param field 要记录值的字段
	 * @param type 字段的类型
	 * @param value 原始值
	 * @since 5.0.4
	 */
	default void recordFieldValue(String field, Class<?> type, @Nullable Object value) {
	}

	/**
	 * 标记指定的被禁止字段为已抑制。
	 * <p>数据绑定器为每个检测到的目标被禁止字段的字段值调用此方法。
	 * @see DataBinder#setAllowedFields
	 */
	default void recordSuppressedField(String field) {
	}

	/**
	 * 返回在绑定过程中被抑制的字段列表。
	 * <p>可用于确定是否有字段值针对被禁止字段。
	 * @see DataBinder#setAllowedFields
	 */
	default String[] getSuppressedFields() {
		return new String[0];
	}

}
