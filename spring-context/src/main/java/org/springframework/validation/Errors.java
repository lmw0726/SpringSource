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

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * 存储并暴露特定对象的数据绑定和验证错误的信息。
 *
 * <p>字段名可以是目标对象的属性（例如，当绑定到客户对象时的“name”），
 * 也可以是在子对象中的嵌套字段（例如“address.street”）。支持通过
 * {@link #setNestedPath(String)}进行子树导航：例如，一个
 * {@code AddressValidator} 验证“address”，而不知道这是客户对象的子对象。
 *
 * <p>注意：{@code Errors} 对象是单线程的。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setNestedPath
 * @see BindException
 * @see DataBinder
 * @see ValidationUtils
 */
public interface Errors {

	/**
	 * 嵌套路径中路径元素之间的分隔符，
	 * 例如在“customer.name”或“customer.address.street”中。
	 * <p>“.” = 同 beans 包中的
	 * {@link org.springframework.beans.PropertyAccessor#NESTED_PROPERTY_SEPARATOR 嵌套属性分隔符}。
	 */
	String NESTED_PATH_SEPARATOR = PropertyAccessor.NESTED_PROPERTY_SEPARATOR;


	/**
	 * 返回绑定根对象的名称。
	 */
	String getObjectName();

	/**
	 * 允许更改上下文，以便标准验证器可以验证子树。拒绝调用会将给定路径添加到字段名称之前。
	 * <p>例如，一个地址验证器可以验证客户对象的子对象“address”。
	 *
	 * @param nestedPath 嵌套路径中的路径，
	 *                   例如“address”（默认为""，{@code null} 也可以接受）。
	 *                   可以以点结尾：“address”和“address.”都是有效的。
	 */
	void setNestedPath(String nestedPath);

	/**
	 * 返回此 {@link Errors} 对象的当前嵌套路径。
	 * <p>返回带点的嵌套路径，即“address.”，以便于构建连接路径。默认是一个空字符串。
	 */
	String getNestedPath();

	/**
	 * 将给定的子路径推送到嵌套路径堆栈中。
	 * <p>调用 {@link #popNestedPath()} 将重置相应
	 * {@code pushNestedPath(String)} 调用之前的原始嵌套路径。
	 * <p>使用嵌套路径堆栈可以设置子对象的临时嵌套路径，而不必担心临时路径持有者。
	 * <p>例如：当前路径“spouse.”，pushNestedPath("child") &rarr;
	 * 结果路径“spouse.child.”；popNestedPath() &rarr; 再次变为“spouse.”。
	 *
	 * @param subPath 要推送到嵌套路径堆栈中的子路径
	 * @see #popNestedPath
	 */
	void pushNestedPath(String subPath);

	/**
	 * 从嵌套路径堆栈中弹出前一个嵌套路径。
	 *
	 * @throws IllegalStateException 如果堆栈中没有前一个嵌套路径
	 * @see #pushNestedPath
	 */
	void popNestedPath() throws IllegalStateException;

	/**
	 * 为整个目标对象注册全局错误，
	 * 使用给定的错误描述。
	 *
	 * @param errorCode 错误代码，可解释为消息键
	 */
	void reject(String errorCode);

	/**
	 * 为整个目标对象注册全局错误，
	 * 使用给定的错误描述。
	 *
	 * @param errorCode      错误代码，可解释为消息键
	 * @param defaultMessage 后备默认消息
	 */
	void reject(String errorCode, String defaultMessage);

	/**
	 * 为整个目标对象注册全局错误，
	 * 使用给定的错误描述。
	 *
	 * @param errorCode      错误代码，可解释为消息键
	 * @param errorArgs      错误参数，用于通过 MessageFormat 绑定参数
	 *                       （可以是 {@code null}）
	 * @param defaultMessage 后备默认消息
	 */
	void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage);

	/**
	 * 为当前对象的指定字段（考虑到当前嵌套路径，如果有的话）注册字段错误，
	 * 使用给定的错误描述。
	 * <p>字段名称可以是 {@code null} 或空字符串，以表示当前对象本身而不是其字段。
	 * 这可能会在嵌套对象图中导致相应的字段错误，或者如果当前对象是顶层对象，则会导致全局错误。
	 *
	 * @param field     字段名称（可以是 {@code null} 或空字符串）
	 * @param errorCode 错误代码，可解释为消息键
	 * @see #getNestedPath()
	 */
	void rejectValue(@Nullable String field, String errorCode);

	/**
	 * 为当前对象的指定字段（考虑到当前嵌套路径，如果有的话）注册字段错误，
	 * 使用给定的错误描述。
	 * <p>字段名称可以是 {@code null} 或空字符串，以表示当前对象本身而不是其字段。
	 * 这可能会在嵌套对象图中导致相应的字段错误，或者如果当前对象是顶层对象，则会导致全局错误。
	 *
	 * @param field          字段名称（可以是 {@code null} 或空字符串）
	 * @param errorCode      错误代码，可解释为消息键
	 * @param defaultMessage 后备默认消息
	 * @see #getNestedPath()
	 */
	void rejectValue(@Nullable String field, String errorCode, String defaultMessage);

	/**
	 * 为当前对象的指定字段（考虑到当前嵌套路径，如果有的话）注册字段错误，
	 * 使用给定的错误描述。
	 * <p>字段名称可以是 {@code null} 或空字符串，以表示当前对象本身而不是其字段。
	 * 这可能会在嵌套对象图中导致相应的字段错误，或者如果当前对象是顶层对象，则会导致全局错误。
	 *
	 * @param field          字段名称（可以是 {@code null} 或空字符串）
	 * @param errorCode      错误代码，可解释为消息键
	 * @param errorArgs      错误参数，用于通过 MessageFormat 绑定参数
	 *                       （可以是 {@code null}）
	 * @param defaultMessage 后备默认消息
	 * @see #getNestedPath()
	 */
	void rejectValue(@Nullable String field, String errorCode,
					 @Nullable Object[] errorArgs, @Nullable String defaultMessage);

	/**
	 * 将给定 {@code Errors} 实例中的所有错误添加到此
	 * {@code Errors} 实例中。
	 * <p>这是一个便利方法，可以避免重复调用 {@code reject(..)}
	 * 以便将一个 {@code Errors} 实例合并到另一个
	 * {@code Errors} 实例中。
	 * <p>注意，传入的 {@code Errors} 实例应该
	 * 指向相同的目标对象，或者至少包含适用于此 {@code Errors} 实例的目标对象的兼容错误。
	 *
	 * @param errors 要合并的 {@code Errors} 实例
	 */
	void addAllErrors(Errors errors);

	/**
	 * 返回是否存在任何错误。
	 */
	boolean hasErrors();

	/**
	 * 返回错误的总数。
	 */
	int getErrorCount();

	/**
	 * 获取所有错误，包括全局错误和字段错误。
	 *
	 * @return {@link ObjectError} 实例的列表
	 */
	List<ObjectError> getAllErrors();

	/**
	 * 是否存在任何全局错误？
	 *
	 * @return 如果存在任何全局错误，则返回 {@code true}
	 * @see #hasFieldErrors()
	 */
	boolean hasGlobalErrors();

	/**
	 * 返回全局错误的数量。
	 *
	 * @return 全局错误的数量
	 * @see #getFieldErrorCount()
	 */
	int getGlobalErrorCount();

	/**
	 * 获取所有全局错误。
	 *
	 * @return {@link ObjectError} 实例的列表
	 */
	List<ObjectError> getGlobalErrors();

	/**
	 * 获取第一个全局错误（如果有）。
	 *
	 * @return 全局错误，或 {@code null}
	 */
	@Nullable
	ObjectError getGlobalError();

	/**
	 * 是否存在任何字段错误？
	 *
	 * @return 如果存在任何与字段关联的错误，则返回 {@code true}
	 * @see #hasGlobalErrors()
	 */
	boolean hasFieldErrors();

	/**
	 * 返回与字段关联的错误的数量。
	 *
	 * @return 与字段关联的错误的数量
	 * @see #getGlobalErrorCount()
	 */
	int getFieldErrorCount();

	/**
	 * 获取与字段关联的所有错误。
	 *
	 * @return {@link FieldError} 实例的列表
	 */
	List<FieldError> getFieldErrors();

	/**
	 * 获取与字段关联的第一个错误（如果有）。
	 *
	 * @return 字段特定的错误，或 {@code null}
	 */
	@Nullable
	FieldError getFieldError();

	/**
	 * 是否存在与给定字段关联的任何错误？
	 *
	 * @param field 字段名称
	 * @return 如果存在任何与给定字段关联的错误，则返回 {@code true}
	 */
	boolean hasFieldErrors(String field);

	/**
	 * 返回与给定字段关联的错误的数量。
	 *
	 * @param field 字段名称
	 * @return 与给定字段关联的错误的数量
	 */
	int getFieldErrorCount(String field);

	/**
	 * 获取与给定字段关联的所有错误。
	 * <p>实现应支持不仅是完整字段名如“name”，
	 * 还应支持模式匹配如“na*”或“address.*”。
	 *
	 * @param field 字段名称
	 * @return {@link FieldError} 实例的列表
	 */
	List<FieldError> getFieldErrors(String field);

	/**
	 * 获取与给定字段关联的第一个错误（如果有）。
	 *
	 * @param field 字段名称
	 * @return 字段特定的错误，或 {@code null}
	 */
	@Nullable
	FieldError getFieldError(String field);

	/**
	 * 返回给定字段的当前值，无论是当前的 bean 属性值还是上次绑定的拒绝更新。
	 * <p>允许方便地访问用户指定的字段值，
	 * 即使存在类型不匹配。
	 *
	 * @param field 字段名称
	 * @return 给定字段的当前值
	 */
	@Nullable
	Object getFieldValue(String field);

	/**
	 * 返回给定字段的类型。
	 * <p>即使字段值为 {@code null}，实现也应能够确定类型，
	 * 例如从某个关联描述符中。
	 *
	 * @param field 字段名称
	 * @return 字段的类型，或如果无法确定则为 {@code null}
	 */
	@Nullable
	Class<?> getFieldType(String field);

}
