/*
 * Copyright 2002-2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.*;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormatterPropertyEditorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 允许在目标对象上设置属性值的绑定器，包括支持验证和绑定结果分析。
 *
 * <p>绑定过程可以通过指定允许的字段模式、必需字段、自定义编辑器等进行自定义。
 *
 * <p><strong>警告</strong>：数据绑定可能会通过公开不应由外部客户端访问或修改的对象图的部分来导致安全问题。因此，应该仔细考虑数据绑定的设计和使用，特别是关于安全性的考虑。有关更多详细信息，请参阅参考手册中关于Spring Web MVC和Spring WebFlux的数据绑定的专门部分。
 *
 * <p>可以通过{@link BindingResult}接口检查绑定结果，该接口扩展了{@link Errors}接口：请参阅{@link #getBindingResult()}方法。缺少字段和属性访问异常将转换为{@link FieldError FieldErrors}，并在Errors实例中收集，使用以下错误代码：
 *
 * <ul>
 * <li>缺少字段错误："required"
 * <li>类型不匹配错误："typeMismatch"
 * <li>方法调用错误："methodInvocation"
 * </ul>
 *
 * <p>默认情况下，绑定错误通过{@link BindingErrorProcessor}策略进行解析，处理缺少字段和属性访问异常：请参阅{@link #setBindingErrorProcessor}方法。如果需要，可以覆盖默认策略，例如生成不同的错误代码。
 *
 * <p>随后可以添加自定义验证错误。通常，您会希望将这些错误代码解析为适当的用户可见错误消息；这可以通过通过{@link org.springframework.context.MessageSource}解析每个错误，该对象能够通过其{@link org.springframework.context.MessageSource#getMessage(org.springframework.context.MessageSourceResolvable, java.util.Locale)}方法解析{@link ObjectError}/{@link FieldError}来实现。消息代码列表可以通过{@link MessageCodesResolver}策略进行自定义：请参阅{@link #setMessageCodesResolver}方法。{@link DefaultMessageCodesResolver}的javadoc说明了默认解析规则的详细信息。
 *
 * <p>这个通用数据绑定器可以在任何环境中使用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Sam Brannen
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #registerCustomEditor
 * @see #setMessageCodesResolver
 * @see #setBindingErrorProcessor
 * @see #bind
 * @see #getBindingResult
 * @see DefaultMessageCodesResolver
 * @see DefaultBindingErrorProcessor
 * @see org.springframework.context.MessageSource
 */
public class DataBinder implements PropertyEditorRegistry, TypeConverter {

	/**
	 * 用于绑定的默认对象名称："target"。
	 */
	public static final String DEFAULT_OBJECT_NAME = "target";

	/**
	 * 数组和集合增长的默认限制：256。
	 */
	public static final int DEFAULT_AUTO_GROW_COLLECTION_LIMIT = 256;


	/**
	 * 我们将创建许多DataBinder实例：让我们使用一个静态记录器。
	 */
	protected static final Log logger = LogFactory.getLog(DataBinder.class);

	/**
	 * 目标对象，可为空。
	 */
	@Nullable
	private final Object target;

	/**
	 * 对象名称。
	 */
	private final String objectName;

	/**
	 * 绑定结果。
	 */
	@Nullable
	private AbstractPropertyBindingResult bindingResult;

	/**
	 * 是否直接访问字段。
	 */
	private boolean directFieldAccess = false;

	/**
	 * 类型转换器。
	 */
	@Nullable
	private SimpleTypeConverter typeConverter;

	/**
	 * 是否忽略未知字段。
	 */
	private boolean ignoreUnknownFields = true;

	/**
	 * 是否忽略无效字段。
	 */
	private boolean ignoreInvalidFields = false;

	/**
	 * 是否自动增长嵌套路径。
	 */
	private boolean autoGrowNestedPaths = true;

	/**
	 * 数组和集合增长的限制。
	 */
	private int autoGrowCollectionLimit = DEFAULT_AUTO_GROW_COLLECTION_LIMIT;

	/**
	 * 允许的字段。
	 */
	@Nullable
	private String[] allowedFields;

	/**
	 * 不允许的字段。
	 */
	@Nullable
	private String[] disallowedFields;

	/**
	 * 必需字段数组。
	 */
	@Nullable
	private String[] requiredFields;

	/**
	 * 转换服务。
	 */
	@Nullable
	private ConversionService conversionService;

	/**
	 * 消息代码解析器。
	 */
	@Nullable
	private MessageCodesResolver messageCodesResolver;

	/**
	 * 绑定错误处理器，默认为 DefaultBindingErrorProcessor。
	 */
	private BindingErrorProcessor bindingErrorProcessor = new DefaultBindingErrorProcessor();

	/**
	 * 验证器列表。
	 */
	private final List<Validator> validators = new ArrayList<>();


	/**
	 * 创建一个新的 DataBinder 实例，使用默认的对象名称。
	 *
	 * @param target 目标对象进行绑定（或者为 {@code null} 如果绑定器仅用于转换普通参数值）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public DataBinder(@Nullable Object target) {
		this(target, DEFAULT_OBJECT_NAME);
	}

	/**
	 * 创建一个新的 DataBinder 实例。
	 *
	 * @param target     目标对象进行绑定（或者为 {@code null} 如果绑定器仅用于转换普通参数值）
	 * @param objectName 目标对象的名称
	 */
	public DataBinder(@Nullable Object target, String objectName) {
		this.target = ObjectUtils.unwrapOptional(target);
		this.objectName = objectName;
	}


	/**
	 * 返回包装的目标对象。
	 */
	@Nullable
	public Object getTarget() {
		return this.target;
	}

	/**
	 * 返回绑定对象的名称。
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * 设置此绑定器是否应尝试“自动增长”包含 null 值的嵌套路径。
	 * <p>如果为“true”，则将使用默认对象值填充空路径位置，并遍历该路径，而不是导致异常。
	 * 当访问超出范围的索引时，此标志还启用集合元素的自动增长。
	 * <p>默认情况下，在标准 DataBinder 上为“true”。请注意，自从 Spring 4.1，此功能支持 bean 属性访问（DataBinder 的默认模式）和字段访问。
	 *
	 * @see #initBeanPropertyAccess()
	 * @see org.springframework.beans.BeanWrapper#setAutoGrowNestedPaths
	 */
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowNestedPaths before other configuration methods");
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	/**
	 * 返回是否已激活“自动增长”嵌套路径。
	 */
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	/**
	 * 指定数组和集合自动增长的限制。
	 * <p>默认值为 256，防止在大型索引的情况下导致 OutOfMemoryErrors。
	 * 如果您的自动增长需求异常高，请提高此限制。
	 *
	 * @see #initBeanPropertyAccess()
	 * @see org.springframework.beans.BeanWrapper#setAutoGrowCollectionLimit
	 */
	public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowCollectionLimit before other configuration methods");
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}

	/**
	 * 返回当前数组和集合自动增长的限制。
	 */
	public int getAutoGrowCollectionLimit() {
		return this.autoGrowCollectionLimit;
	}

	/**
	 * 为此 DataBinder 初始化标准 JavaBean 属性访问。
	 * <p>这是默认值；显式调用只会导致急切初始化。
	 *
	 * @see #initDirectFieldAccess()
	 * @see #createBeanPropertyBindingResult()
	 */
	public void initBeanPropertyAccess() {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call initBeanPropertyAccess before other configuration methods");
		this.directFieldAccess = false;
	}

	/**
	 * 使用标准 JavaBean 属性访问创建 {@link AbstractPropertyBindingResult} 实例。
	 *
	 * @since 4.2.1
	 */
	protected AbstractPropertyBindingResult createBeanPropertyBindingResult() {
		// 创建一个 BeanPropertyBindingResult 对象
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());

		// 如果存在转换服务，初始化转换服务
		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}

		// 如果存在消息代码解析器，设置消息代码解析器
		if (this.messageCodesResolver != null) {
			result.setMessageCodesResolver(this.messageCodesResolver);
		}

		return result;
	}

	/**
	 * 为此 DataBinder 初始化直接字段访问，
	 * 作为默认 bean 属性访问的替代方案。
	 *
	 * @see #initBeanPropertyAccess()
	 * @see #createDirectFieldBindingResult()
	 */
	public void initDirectFieldAccess() {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call initDirectFieldAccess before other configuration methods");
		this.directFieldAccess = true;
	}

	/**
	 * 使用直接字段访问创建 {@link AbstractPropertyBindingResult} 实例。
	 *
	 * @since 4.2.1
	 */
	protected AbstractPropertyBindingResult createDirectFieldBindingResult() {
		// 创建一个 DirectFieldBindingResult 对象
		DirectFieldBindingResult result = new DirectFieldBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths());

		// 如果存在转换服务，初始化转换服务
		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}

		// 如果存在消息代码解析器，设置消息代码解析器
		if (this.messageCodesResolver != null) {
			result.setMessageCodesResolver(this.messageCodesResolver);
		}

		return result;
	}

	/**
	 * 返回此 DataBinder 持有的内部 BindingResult，
	 * 作为 AbstractPropertyBindingResult。
	 */
	protected AbstractPropertyBindingResult getInternalBindingResult() {
		if (this.bindingResult == null) {
			// 如果绑定结果对象为 null，则根据 directFieldAccess 属性选择创建 DirectFieldBindingResult
			// 或者 BeanPropertyBindingResult 对象
			this.bindingResult = (this.directFieldAccess ?
					createDirectFieldBindingResult() : createBeanPropertyBindingResult());
		}
		return this.bindingResult;
	}

	/**
	 * 返回此绑定器 BindingResult 的底层 PropertyAccessor。
	 */
	protected ConfigurablePropertyAccessor getPropertyAccessor() {
		return getInternalBindingResult().getPropertyAccessor();
	}

	/**
	 * 返回此绑定器的底层 SimpleTypeConverter。
	 */
	protected SimpleTypeConverter getSimpleTypeConverter() {
		if (this.typeConverter == null) {
			// 如果类型转换器对象为 null，则创建一个 SimpleTypeConverter 对象
			this.typeConverter = new SimpleTypeConverter();
			// 如果存在转换服务，则设置 conversionService（
			if (this.conversionService != null) {
				this.typeConverter.setConversionService(this.conversionService);
			}
		}
		return this.typeConverter;
	}

	/**
	 * 返回此绑定器 BindingResult 的底层 TypeConverter。
	 */
	protected PropertyEditorRegistry getPropertyEditorRegistry() {
		// 如果目标对象不为 null，则返回内部绑定结果的属性访问器；
		if (getTarget() != null) {
			return getInternalBindingResult().getPropertyAccessor();
		} else {
			// 否则返回简单类型转换器
			return getSimpleTypeConverter();
		}
	}

	/**
	 * 返回此绑定器 BindingResult 的底层 TypeConverter。
	 */
	protected TypeConverter getTypeConverter() {
		// 如果目标对象不为 null，则返回内部绑定结果的属性访问器；
		if (getTarget() != null) {
			return getInternalBindingResult().getPropertyAccessor();
		} else {
			// 否则返回简单类型转换器
			return getSimpleTypeConverter();
		}
	}

	/**
	 * 获取此 DataBinder 创建的 BindingResult 实例。
	 * 这允许在绑定操作后方便地访问绑定结果。
	 *
	 * @return BindingResult 实例，可视为 BindingResult 或 Errors 实例（Errors 是 BindingResult 的超级接口）
	 * @see Errors
	 * @see #bind
	 */
	public BindingResult getBindingResult() {
		return getInternalBindingResult();
	}

	/**
	 * 设置是否忽略未知字段，即是否忽略在目标对象中没有相应字段的绑定参数。
	 * 默认为 "true"。关闭此设置以强制所有绑定参数必须在目标对象中具有匹配字段。
	 * 注意，此设置仅适用于此 DataBinder 上的绑定操作，而不适用于通过其 BindingResult 检索值。
	 *
	 * @see #bind
	 */
	public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
		this.ignoreUnknownFields = ignoreUnknownFields;
	}

	/**
	 * 返回是否在绑定时忽略未知字段。
	 */
	public boolean isIgnoreUnknownFields() {
		return this.ignoreUnknownFields;
	}

	/**
	 * 设置是否忽略无效字段，即是否忽略目标对象中具有对应字段但不可访问的绑定参数（例如由于嵌套路径中的空值）。
	 * 默认为 "false"。打开此设置以忽略目标对象图中不存在的部分的嵌套对象的绑定参数。
	 * 注意，此设置仅适用于此 DataBinder 上的绑定操作，而不适用于通过其 BindingResult 检索值。
	 *
	 * @see #bind
	 */
	public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
		this.ignoreInvalidFields = ignoreInvalidFields;
	}

	/**
	 * 返回是否在绑定时忽略无效字段。
	 */
	public boolean isIgnoreInvalidFields() {
		return this.ignoreInvalidFields;
	}

	/**
	 * 注册应允许绑定的字段模式。
	 * 默认为所有字段。
	 * 例如，限制此以避免在绑定 HTTP 请求参数时受恶意用户的不希望修改。
	 * 支持 "xxx*"、"*xxx"、"*xxx*" 和 "xxx*yyy" 匹配（具有任意数量的模式部分），
	 * 以及直接相等。
	 * 此方法的默认实现将允许字段模式存储为规范形式。
	 * 覆盖此方法的子类因此必须考虑到这一点。
	 * 可通过覆盖 isAllowed 方法实现更复杂的匹配。
	 * 或者，指定一组不允许的字段模式。
	 *
	 * @param allowedFields 允许的字段模式数组
	 * @see #setDisallowedFields
	 * @see #isAllowed(String)
	 */
	public void setAllowedFields(@Nullable String... allowedFields) {
		this.allowedFields = PropertyAccessorUtils.canonicalPropertyNames(allowedFields);
	}

	/**
	 * 返回应允许绑定的字段模式。
	 *
	 * @return 允许绑定的字段模式数组
	 * @see #setAllowedFields(String...)
	 */
	@Nullable
	public String[] getAllowedFields() {
		return this.allowedFields;
	}

	/**
	 * 注册不应允许绑定的字段模式。
	 * 默认为无。
	 * 将字段标记为不允许，例如避免在绑定 HTTP 请求参数时受恶意用户的不希望修改。
	 * 支持 "xxx*"、"*xxx"、"*xxx*" 和 "xxx*yyy" 匹配（具有任意数量的模式部分），
	 * 以及直接相等。
	 * 此方法的默认实现将不允许的字段模式存储为规范形式。
	 * 从 Spring Framework 5.2.21 开始，此方法的默认实现还将不允许的字段模式转换为小写，以支持 isAllowed 中的不区分大小写的模式匹配。
	 * 因此，覆盖此方法的子类必须考虑这两个转换。
	 * 可通过覆盖 isAllowed 方法实现更复杂的匹配。
	 * 或者，指定一组允许的字段模式。
	 *
	 * @param disallowedFields 不允许绑定的字段模式数组
	 * @see #setAllowedFields
	 * @see #isAllowed(String)
	 */
	public void setDisallowedFields(@Nullable String... disallowedFields) {
		// 如果禁止字段数组为 null，则将 disallowedFields 设置为 null
		if (disallowedFields == null) {
			this.disallowedFields = null;
		} else {
			// 否则，创建一个与禁止字段数组长度相同的字符串数组
			String[] fieldPatterns = new String[disallowedFields.length];
			// 遍历禁止字段数组，将每个字段转换为规范的属性名称，并转换为小写后存入新数组
			for (int i = 0; i < fieldPatterns.length; i++) {
				fieldPatterns[i] = PropertyAccessorUtils.canonicalPropertyName(disallowedFields[i]).toLowerCase();
			}
			// 将新数组设置为 disallowedFields
			this.disallowedFields = fieldPatterns;
		}
	}

	/**
	 * 返回不应允许绑定的字段模式。
	 *
	 * @return 不允许绑定的字段模式数组
	 * @see #setDisallowedFields(String...)
	 */
	@Nullable
	public String[] getDisallowedFields() {
		return this.disallowedFields;
	}

	/**
	 * 注册每个绑定过程所需的字段。
	 * 如果指定的字段之一不包含在传入属性值的列表中，则将创建相应的"缺少字段"错误，错误代码为"required"（由默认的绑定错误处理器处理）。
	 *
	 * @param requiredFields 字段名称数组
	 * @see #setBindingErrorProcessor
	 * @see DefaultBindingErrorProcessor#MISSING_FIELD_ERROR_CODE
	 */
	public void setRequiredFields(@Nullable String... requiredFields) {
		this.requiredFields = PropertyAccessorUtils.canonicalPropertyNames(requiredFields);
		if (logger.isDebugEnabled()) {
			logger.debug("DataBinder requires binding of required fields [" +
					StringUtils.arrayToCommaDelimitedString(requiredFields) + "]");
		}
	}

	/**
	 * 返回每个绑定过程所需的字段。
	 *
	 * @return 字段名称数组
	 */
	@Nullable
	public String[] getRequiredFields() {
		return this.requiredFields;
	}

	/**
	 * 设置用于将错误解析为消息代码的策略。
	 * 将给定的策略应用于底层的错误持有者。
	 * 默认为 DefaultMessageCodesResolver。
	 *
	 * @see BeanPropertyBindingResult#setMessageCodesResolver
	 * @see DefaultMessageCodesResolver
	 */
	public void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
		Assert.state(this.messageCodesResolver == null, "DataBinder is already initialized with MessageCodesResolver");
		// 设置消息代码解析器为给定的消息代码解析器
		this.messageCodesResolver = messageCodesResolver;
		if (this.bindingResult != null && messageCodesResolver != null) {
			// 如果绑定结果不为 null，并且消息代码解析器不为 null，
			// 则将绑定结果的消息代码解析器设置为给定的消息代码解析器
			this.bindingResult.setMessageCodesResolver(messageCodesResolver);
		}
	}

	/**
	 * 设置用于处理绑定错误（即必需字段错误和 PropertyAccessExceptions）的策略。
	 * 默认为 DefaultBindingErrorProcessor。
	 *
	 * @see DefaultBindingErrorProcessor
	 */
	public void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
		Assert.notNull(bindingErrorProcessor, "BindingErrorProcessor must not be null");
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * 返回处理绑定错误的策略。
	 */
	public BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * 设置在每个绑定步骤之后应用的验证器。
	 *
	 * @see #addValidators(Validator...)
	 * @see #replaceValidators(Validator...)
	 */
	public void setValidator(@Nullable Validator validator) {
		// 断言验证器不为 null
		assertValidators(validator);
		// 清除现有验证器列表
		this.validators.clear();
		if (validator != null) {
			// 如果给定的验证器不为 null，则将其添加到验证器列表中
			this.validators.add(validator);
		}

	}

	private void assertValidators(Validator... validators) {
		// 获取目标对象
		Object target = getTarget();
		// 遍历验证器列表
		for (Validator validator : validators) {
			// 如果验证器不为 null 且目标对象不为 null 且验证器不支持目标对象的类，则抛出异常
			if (validator != null && (target != null && !validator.supports(target.getClass()))) {
				throw new IllegalStateException("Invalid target for Validator [" + validator + "]: " + target);
			}
		}
	}

	/**
	 * 添加验证器，在每个绑定步骤之后应用。
	 *
	 * @see #setValidator(Validator)
	 * @see #replaceValidators(Validator...)
	 */
	public void addValidators(Validator... validators) {
		assertValidators(validators);
		this.validators.addAll(Arrays.asList(validators));
	}

	/**
	 * 替换在每个绑定步骤之后应用的验证器。
	 *
	 * @see #setValidator(Validator)
	 * @see #addValidators(Validator...)
	 */
	public void replaceValidators(Validator... validators) {
		assertValidators(validators);
		this.validators.clear();
		this.validators.addAll(Arrays.asList(validators));
	}

	/**
	 * 返回在每个绑定步骤之后应用的主要验证器，如果有的话。
	 */
	@Nullable
	public Validator getValidator() {
		return (!this.validators.isEmpty() ? this.validators.get(0) : null);
	}

	/**
	 * 返回数据绑定后要应用的验证器列表。
	 */
	public List<Validator> getValidators() {
		return Collections.unmodifiableList(this.validators);
	}


	//---------------------------------------------------------------------
	// Implementation of PropertyEditorRegistry/TypeConverter interface
	//---------------------------------------------------------------------

	/**
	 * 指定一个用于转换属性值的 Spring 3.0 ConversionService，作为 JavaBeans PropertyEditors 的替代方案。
	 */
	public void setConversionService(@Nullable ConversionService conversionService) {
		// 检查 DataBinder 是否已经使用 ConversionService 初始化
		Assert.state(this.conversionService == null, "DataBinder is already initialized with ConversionService");
		// 设置 ConversionService
		this.conversionService = conversionService;
		if (this.bindingResult != null && conversionService != null) {
			// 如果 bindingResult 不为空且 conversionService 不为空，则初始化 bindingResult 中的 conversionService
			this.bindingResult.initConversion(conversionService);
		}
	}

	/**
	 * 返回关联的 ConversionService（如果有）。
	 */
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * 添加自定义格式化程序，将其应用于所有与 {@link Formatter} 声明类型匹配的字段。
	 * <p>在底层注册相应的 {@link PropertyEditor} 适配器。
	 *
	 * @param formatter 用于特定类型的泛型声明的格式化程序
	 * @see #registerCustomEditor(Class, PropertyEditor)
	 * @since 4.2
	 */
	public void addCustomFormatter(Formatter<?> formatter) {
		// 创建一个 FormatterPropertyEditorAdapter 适配器对象，用于将 Formatter 转换为 PropertyEditor
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		// 将适配器注册到属性编辑器注册表中，以便于后续属性编辑器的使用
		getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
	}

	/**
	 * 添加自定义格式化器，将其应用于指定的字段，如果有的话，否则应用于所有字段。
	 * <p>在底层注册相应的 {@link PropertyEditor} 适配器。
	 *
	 * @param formatter 要添加的格式化器，通常声明为特定类型
	 * @param fields    要应用格式化器的字段，如果要应用于所有字段，则为空
	 * @see #registerCustomEditor(Class, String, PropertyEditor)
	 * @since 4.2
	 */
	public void addCustomFormatter(Formatter<?> formatter, String... fields) {
		// 创建 FormatterPropertyEditorAdapter 实例
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		// 获取字段的类型
		Class<?> fieldType = adapter.getFieldType();
		if (ObjectUtils.isEmpty(fields)) {
			// 如果字段数组为空，则将字段类型注册到 PropertyEditorRegistry 中
			getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
		} else {
			// 否则，将字段数组中的每个字段类型都注册到 PropertyEditorRegistry 中
			for (String field : fields) {
				getPropertyEditorRegistry().registerCustomEditor(fieldType, field, adapter);
			}
		}
	}

	/**
	 * 添加自定义格式化器，将其仅应用于指定的字段类型，如果有的话，否则应用于与 {@link Formatter} 声明的类型匹配的所有字段。
	 * <p>在底层注册相应的 {@link PropertyEditor} 适配器。
	 *
	 * @param formatter  要添加的格式化器（如果字段类型是显式指定的参数，则不需要通用地声明字段类型）
	 * @param fieldTypes 要应用格式化器的字段类型，如果要从给定的 {@link Formatter} 实现类派生，则为空
	 * @see #registerCustomEditor(Class, PropertyEditor)
	 * @since 4.2
	 */
	public void addCustomFormatter(Formatter<?> formatter, Class<?>... fieldTypes) {
		// 创建 FormatterPropertyEditorAdapter 实例
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		if (ObjectUtils.isEmpty(fieldTypes)) {
			// 如果字段类型数组为空，则将 FormatterPropertyEditorAdapter 实例的字段类型注册到 PropertyEditorRegistry 中
			getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
		} else {
			// 否则，将字段类型数组中的每个字段类型都注册到 PropertyEditorRegistry 中
			for (Class<?> fieldType : fieldTypes) {
				getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
			}
		}
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		getPropertyEditorRegistry().registerCustomEditor(requiredType, propertyEditor);
	}

	@Override
	public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String field, PropertyEditor propertyEditor) {
		getPropertyEditorRegistry().registerCustomEditor(requiredType, field, propertyEditor);
	}

	@Override
	@Nullable
	public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
		return getPropertyEditorRegistry().findCustomEditor(requiredType, propertyPath);
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
		return getTypeConverter().convertIfNecessary(value, requiredType);
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
									@Nullable MethodParameter methodParam) throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, methodParam);
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
			throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, field);
	}

	@Nullable
	@Override
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
									@Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, typeDescriptor);
	}


	/**
	 * 将给定的属性值绑定到此绑定器的目标。
	 * <p>此调用可以创建字段错误，表示基本绑定错误，如必需字段（代码“required”）或值与bean属性之间的类型不匹配（代码“typeMismatch”）。
	 * <p>请注意，给定的 PropertyValues 应该是一次性实例：为了效率，如果它实现了 MutablePropertyValues 接口，则它将被修改为仅包含允许的字段；
	 * 否则，将为此目的创建一个内部可变副本。如果您希望原始实例保持不修改，请传入 PropertyValues 的副本。
	 *
	 * @param pvs 要绑定的属性值
	 * @see #doBind(org.springframework.beans.MutablePropertyValues)
	 */
	public void bind(PropertyValues pvs) {
		// 如果传入的 PropertyValues 对象是 MutablePropertyValues 类型的，则直接使用，否则将其包装成 MutablePropertyValues 对象
		MutablePropertyValues mpvs = (pvs instanceof MutablePropertyValues ?
				(MutablePropertyValues) pvs : new MutablePropertyValues(pvs));
		// 调用 doBind 方法进行数据绑定
		doBind(mpvs);
	}

	/**
	 * 绑定过程的实际实现，使用传入的 MutablePropertyValues 实例。
	 *
	 * @param mpvs 要绑定的属性值，作为 MutablePropertyValues 实例
	 * @see #checkAllowedFields
	 * @see #checkRequiredFields
	 * @see #applyPropertyValues
	 */
	protected void doBind(MutablePropertyValues mpvs) {
		// 检查允许的字段
		checkAllowedFields(mpvs);
		// 检查必填字段
		checkRequiredFields(mpvs);
		// 应用属性值
		applyPropertyValues(mpvs);
	}

	/**
	 * 根据允许的字段检查给定的属性值，删除不允许的字段的值。
	 *
	 * @param mpvs 要绑定的属性值（可以修改）
	 * @see #getAllowedFields
	 * @see #isAllowed(String)
	 */
	protected void checkAllowedFields(MutablePropertyValues mpvs) {
		// 获取所有的属性值
		PropertyValue[] pvs = mpvs.getPropertyValues();
		// 遍历属性值数组
		for (PropertyValue pv : pvs) {
			// 获取属性的规范名称
			String field = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
			// 如果字段不在允许的字段列表中
			if (!isAllowed(field)) {
				// 从属性值中移除该属性
				mpvs.removePropertyValue(pv);
				// 记录被抑制的字段
				getBindingResult().recordSuppressedField(field);
				// 如果日志级别为DEBUG，则记录字段被移除的信息
				if (logger.isDebugEnabled()) {
					logger.debug("Field [" + field + "] has been removed from PropertyValues " +
							"and will not be bound, because it has not been found in the list of allowed fields");
				}
			}
		}
	}

	/**
	 * 确定给定字段是否允许绑定。
	 * <p>对于每个传入的属性值都会调用此方法。
	 * <p>检查配置的允许字段模式列表和不允许字段模式列表中的匹配项，支持{@code "xxx*"}、{@code "*xxx"}、{@code "*xxx*"}和{@code "xxx*yyy"}的匹配（带有任意数量的模式部分），以及直接相等的匹配。
	 * <p>与允许字段模式的匹配区分大小写；而与不允许字段模式的匹配不区分大小写。
	 * <p>即使字段匹配允许列表中的模式，如果它也匹配不允许列表中的模式，则不会接受匹配不允许列表中的模式。
	 * <p>可以在子类中重写此方法，但必须注意遵守上述约定。
	 *
	 * @param field 要检查的字段
	 * @return 如果字段允许绑定，则为 {@code true}
	 * @see #setAllowedFields
	 * @see #setDisallowedFields
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isAllowed(String field) {
		// 获取允许的字段和禁止的字段
		String[] allowed = getAllowedFields();
		String[] disallowed = getDisallowedFields();
		// 如果允许的字段为空或者字段匹配允许的字段列表中的任何一个，
		// 并且禁止的字段为空或者字段未匹配到禁止的字段列表中的任何一个，则返回true，否则返回false
		return ((ObjectUtils.isEmpty(allowed) || PatternMatchUtils.simpleMatch(allowed, field)) &&
				(ObjectUtils.isEmpty(disallowed) || !PatternMatchUtils.simpleMatch(disallowed, field.toLowerCase())));
	}

	/**
	 * 根据需要检查给定的属性值与必需字段，生成缺少字段错误。
	 *
	 * @param mpvs 要绑定的属性值（可以修改）
	 * @see #getRequiredFields
	 * @see #getBindingErrorProcessor
	 * @see BindingErrorProcessor#processMissingFieldError
	 */
	protected void checkRequiredFields(MutablePropertyValues mpvs) {
		// 获取必需的字段列表
		String[] requiredFields = getRequiredFields();
		if (!ObjectUtils.isEmpty(requiredFields)) {
			// 用于存储属性值的映射
			Map<String, PropertyValue> propertyValues = new HashMap<>();
			// 获取属性值数组
			PropertyValue[] pvs = mpvs.getPropertyValues();
			// 将属性值放入映射中
			for (PropertyValue pv : pvs) {
				// 获取属性的规范名称
				String canonicalName = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
				propertyValues.put(canonicalName, pv);
			}
			// 遍历必需的字段列表
			for (String field : requiredFields) {
				// 获取字段对应的属性值
				PropertyValue pv = propertyValues.get(field);
				// 检查属性值是否为空
				boolean empty = (pv == null || pv.getValue() == null);
				if (!empty) {
					// 如果属性值不为空，则进一步检查其是否为String或String[]类型，并检查其是否包含文本
					if (pv.getValue() instanceof String) {
						empty = !StringUtils.hasText((String) pv.getValue());
					} else if (pv.getValue() instanceof String[]) {
						String[] values = (String[]) pv.getValue();
						empty = (values.length == 0 || !StringUtils.hasText(values[0]));
					}
				}
				// 如果属性值为空，则创建字段丢失的错误，并从属性值中删除该属性
				if (empty) {
					// 使用绑定错误处理器创建字段错误
					getBindingErrorProcessor().processMissingFieldError(field, getInternalBindingResult());
					// 从属性值中删除属性，因为它已经导致了带有拒绝值的字段错误
					if (pv != null) {
						mpvs.removePropertyValue(pv);
						propertyValues.remove(field);
					}
				}
			}
		}
	}

	/**
	 * 将给定的属性值应用于目标对象。
	 * <p>默认实现将所有提供的属性值应用为bean属性值。默认情况下，未知字段将被忽略。
	 *
	 * @param mpvs 要绑定的属性值（可以修改）
	 * @see #getTarget
	 * @see #getPropertyAccessor
	 * @see #isIgnoreUnknownFields
	 * @see #getBindingErrorProcessor
	 * @see BindingErrorProcessor#processPropertyAccessException
	 */
	protected void applyPropertyValues(MutablePropertyValues mpvs) {
		try {
			// 将请求参数绑定到目标对象上
			getPropertyAccessor().setPropertyValues(mpvs, isIgnoreUnknownFields(), isIgnoreInvalidFields());
		} catch (PropertyBatchUpdateException ex) {
			// 使用绑定错误处理器创建字段错误
			for (PropertyAccessException pae : ex.getPropertyAccessExceptions()) {
				getBindingErrorProcessor().processPropertyAccessException(pae, getInternalBindingResult());
			}
		}
	}


	/**
	 * 调用指定的验证器（如果有）。
	 *
	 * @see #setValidator(Validator)
	 * @see #getBindingResult()
	 */
	public void validate() {
		// 获取目标对象
		Object target = getTarget();
		// 断言目标对象不为空
		Assert.state(target != null, "No target to validate");
		// 获取绑定结果
		BindingResult bindingResult = getBindingResult();
		// 使用相同的绑定结果调用每个验证器
		for (Validator validator : getValidators()) {
			validator.validate(target, bindingResult);
		}
	}

	/**
	 * 使用给定的验证提示调用指定的验证器（如果有）。
	 * <p>注意：实际目标验证器可能会忽略验证提示。
	 *
	 * @param validationHints 传递给 {@link SmartValidator} 的一个或多个提示对象
	 * @see #setValidator(Validator)
	 * @see SmartValidator#validate(Object, Errors, Object...)
	 * @since 3.1
	 */
	public void validate(Object... validationHints) {
		// 获取要验证的目标对象
		Object target = getTarget();
		Assert.state(target != null, "No target to validate");

		// 获取绑定结果
		BindingResult bindingResult = getBindingResult();

		// 对每个验证器调用相同的绑定结果
		for (Validator validator : getValidators()) {
			if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
				// 如果存在验证提示并且验证器是 SmartValidator，则使用验证提示进行验证
				((SmartValidator) validator).validate(target, bindingResult, validationHints);
			} else if (validator != null) {
				// 否则直接对目标对象进行验证
				validator.validate(target, bindingResult);
			}
		}
	}


	/**
	 * 关闭此 DataBinder，如果遇到任何错误可能会抛出 BindException。
	 *
	 * @return 包含目标对象和 Errors 实例的模型 Map
	 * @throws BindException 如果绑定操作中存在任何错误
	 * @see BindingResult#getModel()
	 */
	public Map<?, ?> close() throws BindException {
		// 如果存在绑定错误，则抛出 BindException
		if (getBindingResult().hasErrors()) {
			throw new BindException(getBindingResult());
		}
		// 返回绑定结果中的模型数据
		return getBindingResult().getModel();
	}

}
