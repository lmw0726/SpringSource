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

package org.springframework.web.bind.support;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingErrorProcessor;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;

/**
 * 用于在Spring应用程序上下文中声明式配置的便捷 {@link WebBindingInitializer}。
 * 允许重用预配置的初始化器以供多个控制器/处理程序使用。
 *
 * @author Juergen Hoeller
 * @see #setDirectFieldAccess
 * @see #setMessageCodesResolver
 * @see #setBindingErrorProcessor
 * @see #setValidator(Validator)
 * @see #setConversionService(ConversionService)
 * @see #setPropertyEditorRegistrar
 * @since 2.5
 */
public class ConfigurableWebBindingInitializer implements WebBindingInitializer {

	/**
	 * 是否尝试“自动增长”包含 null 值的嵌套路径，默认为 true。
	 */
	private boolean autoGrowNestedPaths = true;

	/**
	 * 是否使用直接字段访问而不是 bean 属性访问，默认为 false。
	 */
	private boolean directFieldAccess = false;

	/**
	 * 用于将错误解析为消息代码的策略。
	 */
	@Nullable
	private MessageCodesResolver messageCodesResolver;

	/**
	 * 用于处理绑定错误的策略。
	 */
	@Nullable
	private BindingErrorProcessor bindingErrorProcessor;

	/**
	 * 验证器。
	 */
	@Nullable
	private Validator validator;

	/**
	 * 转换服务。
	 */
	@Nullable
	private ConversionService conversionService;

	/**
	 * 属性编辑器注册器数组。
	 */
	@Nullable
	private PropertyEditorRegistrar[] propertyEditorRegistrars;


	/**
	 * 设置是否绑定器应尝试“自动增长”包含 null 值的嵌套路径。
	 * <p>如果设置为“true”，则会将空路径位置填充为默认对象值并进行遍历，而不会导致异常。
	 * 此标志还在访问超出范围的索引时启用集合元素的自动增长。
	 * <p>在标准 DataBinder 上，默认为“true”。请注意，此功能仅支持 bean 属性访问（DataBinder 的默认模式），
	 * 而不支持字段访问。
	 *
	 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
	 * @see org.springframework.validation.DataBinder#setAutoGrowNestedPaths
	 */
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	/**
	 * 返回是否绑定器应尝试“自动增长”包含 null 值的嵌套路径。
	 */
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	/**
	 * 设置是否使用直接字段访问而不是 bean 属性访问。
	 * <p>默认为 {@code false}，即使用 bean 属性访问。
	 * 将其设置为 {@code true} 以强制使用直接字段访问。
	 *
	 * @see org.springframework.validation.DataBinder#initDirectFieldAccess()
	 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
	 */
	public final void setDirectFieldAccess(boolean directFieldAccess) {
		this.directFieldAccess = directFieldAccess;
	}

	/**
	 * 返回是否使用直接字段访问而不是 bean 属性访问。
	 */
	public boolean isDirectFieldAccess() {
		return this.directFieldAccess;
	}

	/**
	 * 设置用于将错误解析为消息代码的策略。
	 * 将给定策略应用于此控制器使用的所有数据绑定器。
	 * <p>默认为 {@code null}，即使用数据绑定器的默认策略。
	 *
	 * @see org.springframework.validation.DataBinder#setMessageCodesResolver
	 */
	public final void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * 返回用于将错误解析为消息代码的策略。
	 */
	@Nullable
	public final MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}

	/**
	 * 设置用于处理绑定错误的策略，即，必填字段错误和 {@code PropertyAccessException}。
	 * <p>默认为 {@code null}，即使用数据绑定器的默认策略。
	 *
	 * @see org.springframework.validation.DataBinder#setBindingErrorProcessor
	 */
	public final void setBindingErrorProcessor(@Nullable BindingErrorProcessor bindingErrorProcessor) {
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * 返回用于处理绑定错误的策略。
	 */
	@Nullable
	public final BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * 设置在每个绑定步骤之后应用的验证器。
	 */
	public final void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

	/**
	 * 返回在每个绑定步骤之后应用的验证器（如果有的话）。
	 */
	@Nullable
	public final Validator getValidator() {
		return this.validator;
	}

	/**
	 * 指定将应用于每个 DataBinder 的 ConversionService。
	 *
	 * @since 3.0
	 */
	public final void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回将应用于每个 DataBinder 的 ConversionService。
	 */
	@Nullable
	public final ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * 指定单个 PropertyEditorRegistrar，应用于每个 DataBinder。
	 */
	public final void setPropertyEditorRegistrar(PropertyEditorRegistrar propertyEditorRegistrar) {
		this.propertyEditorRegistrars = new PropertyEditorRegistrar[]{propertyEditorRegistrar};
	}

	/**
	 * 指定多个 PropertyEditorRegistrar，应用于每个 DataBinder。
	 */
	public final void setPropertyEditorRegistrars(@Nullable PropertyEditorRegistrar[] propertyEditorRegistrars) {
		this.propertyEditorRegistrars = propertyEditorRegistrars;
	}

	/**
	 * 返回将应用于每个 DataBinder 的 PropertyEditorRegistrar。
	 */
	@Nullable
	public final PropertyEditorRegistrar[] getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}


	@Override
	public void initBinder(WebDataBinder binder) {
		// 设置是否自动增长嵌套路径
		binder.setAutoGrowNestedPaths(this.autoGrowNestedPaths);

		// 如果启用了直接字段访问，则初始化 Binder 以使用直接字段访问
		if (this.directFieldAccess) {
			binder.initDirectFieldAccess();
		}

		// 设置消息代码解析器
		if (this.messageCodesResolver != null) {
			binder.setMessageCodesResolver(this.messageCodesResolver);
		}

		// 设置绑定错误处理器
		if (this.bindingErrorProcessor != null) {
			binder.setBindingErrorProcessor(this.bindingErrorProcessor);
		}

		// 设置验证器
		if (this.validator != null && binder.getTarget() != null &&
				this.validator.supports(binder.getTarget().getClass())) {
			binder.setValidator(this.validator);
		}

		// 设置转换服务
		if (this.conversionService != null) {
			binder.setConversionService(this.conversionService);
		}

		// 如果属性编辑器注册器不为空
		if (this.propertyEditorRegistrars != null) {
			for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
				// 注册自定义属性编辑器
				propertyEditorRegistrar.registerCustomEditors(binder);
			}
		}
	}

}
