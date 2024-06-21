/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.converter.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.KotlinDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.util.xml.StaxUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 用于使用流畅的 API 创建 {@link ObjectMapper} 实例的构建器。
 *
 * <p>它自定义了 Jackson 的默认属性如下：
 * <ul>
 * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} 被禁用</li>
 * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} 被禁用</li>
 * </ul>
 *
 * <p>如果它们存在于类路径中，它还会自动注册以下已知模块：
 * <ul>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk8">jackson-datatype-jdk8</a>:
 * 支持其他 Java 8 类型如 {@link java.util.Optional}</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jsr310">jackson-datatype-jsr310</a>:
 * 支持 Java 8 日期和时间 API 类型</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-joda">jackson-datatype-joda</a>:
 * 支持 Joda-Time 类型</li>
 * <li><a href="https://github.com/FasterXML/jackson-module-kotlin">jackson-module-kotlin</a>:
 * 支持 Kotlin 类和数据类</li>
 * </ul>
 *
 * <p>与 Jackson 2.9 到 2.12 兼容，截至 Spring 5.3。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Tadaya Tsuyukubo
 * @author Eddú Meléndez
 * @see #build()
 * @see #configure(ObjectMapper)
 * @see Jackson2ObjectMapperFactoryBean
 * @since 4.1.1
 */
public class Jackson2ObjectMapperBuilder {

	/**
	 * 混合注解 与 增强指定类或接口 映射
	 */
	private final Map<Class<?>, Class<?>> mixIns = new LinkedHashMap<>();

	/**
	 * 类型与Json序列化器映射
	 */
	private final Map<Class<?>, JsonSerializer<?>> serializers = new LinkedHashMap<>();

	/**
	 * 类型与Json反序列化器映射
	 */
	private final Map<Class<?>, JsonDeserializer<?>> deserializers = new LinkedHashMap<>();

	/**
	 * 属性访问器 —— 可见性 映射
	 */
	private final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities = new LinkedHashMap<>();

	/**
	 * JSON特性映射
	 */
	private final Map<Object, Boolean> features = new LinkedHashMap<>();

	/**
	 * 是否创建XML映射器
	 */
	private boolean createXmlMapper = false;

	/**
	 * Json工厂
	 */
	@Nullable
	private JsonFactory factory;

	/**
	 * 日期格式化器
	 */
	@Nullable
	private DateFormat dateFormat;

	/**
	 * 区域设置
	 */
	@Nullable
	private Locale locale;

	/**
	 * 时区
	 */
	@Nullable
	private TimeZone timeZone;

	/**
	 * 注解自省器
	 */
	@Nullable
	private AnnotationIntrospector annotationIntrospector;

	/**
	 * 属性命名策略
	 */
	@Nullable
	private PropertyNamingStrategy propertyNamingStrategy;

	/**
	 * 指定用于 Jackson 默认类型处理的 {@link TypeResolverBuilder}。
	 */
	@Nullable
	private TypeResolverBuilder<?> defaultTyping;

	/**
	 * 自定义的序列化包含策略
	 */
	@Nullable
	private JsonInclude.Value serializationInclusion;

	/**
	 * 过滤器提供者
	 */
	@Nullable
	private FilterProvider filters;

	/**
	 * 模块列表
	 */
	@Nullable
	private List<Module> modules;

	/**
	 * 模块类
	 */
	@Nullable
	private Class<? extends Module>[] moduleClasses;

	/**
	 * 是否通过 JDK 服务加载器 查找可用的 Jackson 模块
	 */
	private boolean findModulesViaServiceLoader = false;

	/**
	 * 是否查找知名模块
	 */
	private boolean findWellKnownModules = true;

	/**
	 * 模块类加载器
	 */
	private ClassLoader moduleClassLoader = getClass().getClassLoader();

	/**
	 * 处理程序实例化器
	 */
	@Nullable
	private HandlerInstantiator handlerInstantiator;

	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;

	/**
	 * 是否在默认情况下会使用包装器来包装索引（List、array）属性
	 */
	@Nullable
	private Boolean defaultUseWrapper;

	/**
	 * 要应用的配置器。
	 */
	@Nullable
	private Consumer<ObjectMapper> configurer;


	/**
	 * 如果设置为 {@code true}，将使用其默认构造函数创建一个 {@link XmlMapper}。
	 * 这仅适用于 {@link #build()} 调用，不适用于 {@link #configure} 调用。
	 */
	public Jackson2ObjectMapperBuilder createXmlMapper(boolean createXmlMapper) {
		this.createXmlMapper = createXmlMapper;
		return this;
	}

	/**
	 * 定义用于创建 {@link ObjectMapper} 实例的 {@link JsonFactory}。
	 *
	 * @since 5.0
	 */
	public Jackson2ObjectMapperBuilder factory(JsonFactory factory) {
		this.factory = factory;
		return this;
	}

	/**
	 * 使用给定的 {@link DateFormat} 定义日期/时间的格式。
	 * <p>注意：根据 Jackson 的线程安全规则，设置此属性会使得暴露的 {@link ObjectMapper} 非线程安全。
	 *
	 * @see #simpleDateFormat(String)
	 */
	public Jackson2ObjectMapperBuilder dateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
		return this;
	}

	/**
	 * 使用 {@link SimpleDateFormat} 定义日期/时间格式。
	 * <p>注意：根据 Jackson 的线程安全规则，设置此属性会使得暴露的 {@link ObjectMapper} 非线程安全。
	 *
	 * @see #dateFormat(DateFormat)
	 */
	public Jackson2ObjectMapperBuilder simpleDateFormat(String format) {
		this.dateFormat = new SimpleDateFormat(format);
		return this;
	}

	/**
	 * 覆盖默认的 {@link Locale} 以用于格式化。
	 * 默认使用 {@link Locale#getDefault()}。
	 *
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder locale(Locale locale) {
		this.locale = locale;
		return this;
	}

	/**
	 * 覆盖默认的 {@link Locale} 以用于格式化。
	 * 默认使用 {@link Locale#getDefault()}。
	 *
	 * @param localeString 作为字符串表示的区域设置ID
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder locale(String localeString) {
		this.locale = StringUtils.parseLocale(localeString);
		return this;
	}

	/**
	 * 覆盖默认的 {@link TimeZone} 以用于格式化。
	 * 默认值为 UTC（而非本地时区）。
	 *
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder timeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
		return this;
	}

	/**
	 * 重写默认的 {@link TimeZone} 以用于格式化。
	 * 默认使用的是 UTC 时区（不是本地时区）。
	 *
	 * @param timeZoneString 时区ID的字符串表示形式
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder timeZone(String timeZoneString) {
		this.timeZone = StringUtils.parseTimeZoneString(timeZoneString);
		return this;
	}

	/**
	 * 设置一个 {@link AnnotationIntrospector} 用于序列化和反序列化。
	 */
	public Jackson2ObjectMapperBuilder annotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.annotationIntrospector = annotationIntrospector;
		return this;
	}

	/**
	 * 替代 {@link #annotationIntrospector(AnnotationIntrospector)} 方法，
	 * 允许与当前设置的 introspector 结合而不是替换它，例如通过
	 * {@link AnnotationIntrospectorPair#pair(AnnotationIntrospector, AnnotationIntrospector)} 方法。
	 *
	 * @param pairingFunction 一个函数，用于应用于当前设置的 introspector（可能为 {@code null}）；
	 *                        函数的结果将成为新的 introspector。
	 * @since 5.2.4
	 */
	public Jackson2ObjectMapperBuilder annotationIntrospector(
			Function<AnnotationIntrospector, AnnotationIntrospector> pairingFunction) {

		this.annotationIntrospector = pairingFunction.apply(this.annotationIntrospector);
		return this;
	}

	/**
	 * 指定一个 {@link com.fasterxml.jackson.databind.PropertyNamingStrategy} 来配置 {@link ObjectMapper}。
	 */
	public Jackson2ObjectMapperBuilder propertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
		this.propertyNamingStrategy = propertyNamingStrategy;
		return this;
	}

	/**
	 * 指定用于 Jackson 默认类型处理的 {@link TypeResolverBuilder}。
	 *
	 * @since 4.2.2
	 */
	public Jackson2ObjectMapperBuilder defaultTyping(TypeResolverBuilder<?> typeResolverBuilder) {
		this.defaultTyping = typeResolverBuilder;
		return this;
	}

	/**
	 * 设置自定义的序列化包含策略。
	 *
	 * @see com.fasterxml.jackson.annotation.JsonInclude.Include
	 */
	public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Include inclusion) {
		return serializationInclusion(JsonInclude.Value.construct(inclusion, inclusion));
	}

	/**
	 * 设置自定义的序列化包含策略。
	 *
	 * @see com.fasterxml.jackson.annotation.JsonInclude.Value
	 * @since 5.3
	 */
	public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Value serializationInclusion) {
		this.serializationInclusion = serializationInclusion;
		return this;
	}

	/**
	 * 设置全局过滤器以支持带有 {@link JsonFilter @JsonFilter} 注解的 POJO。
	 *
	 * @see MappingJacksonValue#setFilters(FilterProvider)
	 * @since 4.2
	 */
	public Jackson2ObjectMapperBuilder filters(FilterProvider filters) {
		this.filters = filters;
		return this;
	}

	/**
	 * 添加混合注解以用于增强指定类或接口。
	 *
	 * @param target      要有效覆盖其注解的类（或接口）
	 * @param mixinSource 要作为值添加到目标注解中的类（或接口）的注解
	 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixIn(Class, Class)
	 * @since 4.1.2
	 */
	public Jackson2ObjectMapperBuilder mixIn(Class<?> target, Class<?> mixinSource) {
		this.mixIns.put(target, mixinSource);
		return this;
	}

	/**
	 * 添加混合注解以用于增强指定类或接口。
	 *
	 * @param mixIns 包含目标类（或接口）作为键和要添加到目标注解中的混合类（或接口）作为值的条目的映射。
	 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixIn(Class, Class)
	 * @since 4.1.2
	 */
	public Jackson2ObjectMapperBuilder mixIns(Map<Class<?>, Class<?>> mixIns) {
		this.mixIns.putAll(mixIns);
		return this;
	}

	/**
	 * 配置自定义序列化器。每个序列化器都注册到 {@link JsonSerializer#handledType()} 返回的类型，
	 * 该类型不能为 {@code null}。
	 *
	 * @see #serializersByType(Map)
	 */
	public Jackson2ObjectMapperBuilder serializers(JsonSerializer<?>... serializers) {
		// 遍历所有的序列化器
		for (JsonSerializer<?> serializer : serializers) {
			// 获取当前序列化器处理的类型
			Class<?> handledType = serializer.handledType();
			// 检查处理的类型是否合法
			if (handledType == null || handledType == Object.class) {
				// 如果处理类型不存在，或者处理类型是Object类型，抛出 非法参数异常
				throw new IllegalArgumentException("Unknown handled type in " + serializer.getClass().getName());
			}
			// 将处理的类型和序列化器放入映射中
			this.serializers.put(serializer.handledType(), serializer);
		}
		// 返回当前对象的引用，支持链式调用
		return this;
	}

	/**
	 * 为给定类型配置自定义序列化器。
	 *
	 * @see #serializers(JsonSerializer...)
	 * @since 4.1.2
	 */
	public Jackson2ObjectMapperBuilder serializerByType(Class<?> type, JsonSerializer<?> serializer) {
		this.serializers.put(type, serializer);
		return this;
	}

	/**
	 * 为给定类型配置自定义序列化器集合。
	 *
	 * @see #serializers(JsonSerializer...)
	 */
	public Jackson2ObjectMapperBuilder serializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
		this.serializers.putAll(serializers);
		return this;
	}

	/**
	 * 配置自定义反序列化器。每个反序列化器都注册到 {@link JsonDeserializer#handledType()} 返回的类型，
	 * 该类型不能为 {@code null}。
	 *
	 * @see #deserializersByType(Map)
	 * @since 4.3
	 */
	public Jackson2ObjectMapperBuilder deserializers(JsonDeserializer<?>... deserializers) {
		// 遍历所有的反序列化器
		for (JsonDeserializer<?> deserializer : deserializers) {
			// 获取当前反序列化器处理的类型
			Class<?> handledType = deserializer.handledType();
			// 检查处理的类型是否合法
			if (handledType == null || handledType == Object.class) {
				// 如果处理类型不存在，或者处理类型是Object类型，抛出 非法参数异常
				throw new IllegalArgumentException("Unknown handled type in " + deserializer.getClass().getName());
			}
			// 将处理的类型和反序列化器放入映射中
			this.deserializers.put(deserializer.handledType(), deserializer);
		}
		// 返回当前对象的引用，支持链式调用
		return this;
	}

	/**
	 * 为给定类型配置自定义反序列化器。
	 *
	 * @since 4.1.2
	 */
	public Jackson2ObjectMapperBuilder deserializerByType(Class<?> type, JsonDeserializer<?> deserializer) {
		this.deserializers.put(type, deserializer);
		return this;
	}

	/**
	 * 为给定类型配置自定义反序列化器集合。
	 */
	public Jackson2ObjectMapperBuilder deserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
		this.deserializers.putAll(deserializers);
		return this;
	}

	/**
	 * {@link MapperFeature#AUTO_DETECT_FIELDS} 选项的快捷方式。
	 */
	public Jackson2ObjectMapperBuilder autoDetectFields(boolean autoDetectFields) {
		this.features.put(MapperFeature.AUTO_DETECT_FIELDS, autoDetectFields);
		return this;
	}

	/**
	 * {@link MapperFeature#AUTO_DETECT_SETTERS}/{@link MapperFeature#AUTO_DETECT_GETTERS}/{@link MapperFeature#AUTO_DETECT_IS_GETTERS}
	 * 选项的快捷方式。
	 */
	public Jackson2ObjectMapperBuilder autoDetectGettersSetters(boolean autoDetectGettersSetters) {
		// 添加自动检测Getter的特性
		this.features.put(MapperFeature.AUTO_DETECT_GETTERS, autoDetectGettersSetters);
		// 添加自动检测Setter的特性
		this.features.put(MapperFeature.AUTO_DETECT_SETTERS, autoDetectGettersSetters);
		// 添加自动检测IsGetter的特性
		this.features.put(MapperFeature.AUTO_DETECT_IS_GETTERS, autoDetectGettersSetters);
		return this;
	}

	/**
	 * 快捷方式用于 {@link MapperFeature#DEFAULT_VIEW_INCLUSION} 选项。
	 */
	public Jackson2ObjectMapperBuilder defaultViewInclusion(boolean defaultViewInclusion) {
		this.features.put(MapperFeature.DEFAULT_VIEW_INCLUSION, defaultViewInclusion);
		return this;
	}

	/**
	 * 快捷方式用于 {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} 选项。
	 */
	public Jackson2ObjectMapperBuilder failOnUnknownProperties(boolean failOnUnknownProperties) {
		this.features.put(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
		return this;
	}

	/**
	 * 快捷方式用于 {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} 选项。
	 */
	public Jackson2ObjectMapperBuilder failOnEmptyBeans(boolean failOnEmptyBeans) {
		this.features.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, failOnEmptyBeans);
		return this;
	}

	/**
	 * 快捷方式用于 {@link SerializationFeature#INDENT_OUTPUT} 选项。
	 */
	public Jackson2ObjectMapperBuilder indentOutput(boolean indentOutput) {
		this.features.put(SerializationFeature.INDENT_OUTPUT, indentOutput);
		return this;
	}

	/**
	 * 定义是否默认情况下会使用包装器来包装索引（List、array）属性（仅适用于 {@link XmlMapper}）。
	 *
	 * @since 4.3
	 */
	public Jackson2ObjectMapperBuilder defaultUseWrapper(boolean defaultUseWrapper) {
		this.defaultUseWrapper = defaultUseWrapper;
		return this;
	}

	/**
	 * 指定可见性以限制自动检测的属性类型。
	 *
	 * @see com.fasterxml.jackson.annotation.PropertyAccessor
	 * @see com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
	 * @since 5.1
	 */
	public Jackson2ObjectMapperBuilder visibility(PropertyAccessor accessor, JsonAutoDetect.Visibility visibility) {
		this.visibilities.put(accessor, visibility);
		return this;
	}

	/**
	 * 指定要启用的特性。
	 *
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public Jackson2ObjectMapperBuilder featuresToEnable(Object... featuresToEnable) {
		// 遍历要启用的特性
		for (Object feature : featuresToEnable) {
			// 添加启用特性
			this.features.put(feature, Boolean.TRUE);
		}
		return this;
	}

	/**
	 * 指定要禁用的特性。
	 *
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public Jackson2ObjectMapperBuilder featuresToDisable(Object... featuresToDisable) {
		// 遍历要禁用的特性
		for (Object feature : featuresToDisable) {
			// 添加禁用特性
			this.features.put(feature, Boolean.FALSE);
		}
		return this;
	}

	/**
	 * 指定要注册到 {@link ObjectMapper} 的一个或多个模块。
	 * <p>多次调用不是累加的，最后一次调用定义了要注册的模块。
	 * <p>注意：如果设置了此选项，将不会发生任何模块的发现 - 不是通过 Jackson，也不是通过 Spring（参见 {@link #findModulesViaServiceLoader}）。
	 * 因此，在这里指定一个空列表将会禁止任何类型的模块检测。
	 * <p>请指定 {@link #modules(List)} 或 {@link #modulesToInstall(Module[])} 中的一个，不要同时使用两者。
	 *
	 * @see #modules(List)
	 * @see com.fasterxml.jackson.databind.Module
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder modules(Module... modules) {
		return modules(Arrays.asList(modules));
	}

	/**
	 * 设置要注册到 {@link ObjectMapper} 的完整模块列表。
	 * <p>多次调用不是累加的，最后一次调用定义了要注册的模块。
	 * <p>注意：如果设置了此选项，将不会发生任何模块的发现 - 不是通过 Jackson，也不是通过 Spring（参见 {@link #findModulesViaServiceLoader}）。
	 * 因此，在这里指定一个空列表将会禁止任何类型的模块检测。
	 * <p>请指定 {@link #modules(Module...)} 或 {@link #modulesToInstall(Module[])} 中的一个，不要同时使用两者。
	 *
	 * @see #modules(Module...)
	 * @see com.fasterxml.jackson.databind.Module
	 */
	public Jackson2ObjectMapperBuilder modules(List<Module> modules) {
		this.modules = new ArrayList<>(modules);
		this.findModulesViaServiceLoader = false;
		this.findWellKnownModules = false;
		return this;
	}

	/**
	 * 指定要注册到 {@link ObjectMapper} 的一个或多个模块。
	 * <p>多次调用不是累加的，最后一次调用定义了要注册的模块。
	 * <p>在此指定的模块将在 Spring 自动检测 JSR-310 和 Joda-Time 或 Jackson
	 * 发现模块之后进行注册（参见 {@link #findModulesViaServiceLoader}），
	 * 允许最终覆盖其配置。
	 * <p>请指定 {@link #modules(Module...)} 或 {@link #modulesToInstall(Class[])} 中的一个，不要同时使用两者。
	 *
	 * @see #modulesToInstall(Class...)
	 * @see com.fasterxml.jackson.databind.Module
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder modulesToInstall(Module... modules) {
		this.modules = Arrays.asList(modules);
		this.findWellKnownModules = true;
		return this;
	}

	/**
	 * 指定要注册到 {@link ObjectMapper} 的一个或多个模块类。
	 * <p>多次调用不是累加的，最后一次调用定义了要注册的模块。
	 * <p>在此指定的模块将在 Spring 自动检测 JSR-310 和 Joda-Time 或 Jackson
	 * 发现模块之后进行注册（参见 {@link #findModulesViaServiceLoader}），
	 * 允许最终覆盖它们的配置。
	 * <p>请指定 {@link #modules(Module...)} 或 {@link #modulesToInstall(Class[])} 中的一个，不要同时使用两者。
	 *
	 * @see #modulesToInstall(Module...)
	 * @see com.fasterxml.jackson.databind.Module
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public final Jackson2ObjectMapperBuilder modulesToInstall(Class<? extends Module>... modules) {
		this.moduleClasses = modules;
		this.findWellKnownModules = true;
		return this;
	}

	/**
	 * 设置是否通过 JDK ServiceLoader 查找可用的 Jackson 模块，
	 * 基于类路径中 META-INF 元数据。
	 * <p>如果未设置此模式，Spring 的 Jackson2ObjectMapperBuilder 本身
	 * 将尝试在类路径上找到 JSR-310 和 Joda-Time 支持模块 -
	 * 前提是 Java 8 和 Joda-Time 本身分别可用。
	 *
	 * @see com.fasterxml.jackson.databind.ObjectMapper#findModules()
	 */
	public Jackson2ObjectMapperBuilder findModulesViaServiceLoader(boolean findModules) {
		this.findModulesViaServiceLoader = findModules;
		return this;
	}

	/**
	 * 设置用于加载 Jackson 扩展模块的类加载器。
	 */
	public Jackson2ObjectMapperBuilder moduleClassLoader(ClassLoader moduleClassLoader) {
		this.moduleClassLoader = moduleClassLoader;
		return this;
	}

	/**
	 * 自定义 Jackson 处理器（{@link JsonSerializer}, {@link JsonDeserializer},
	 * {@link KeyDeserializer}, {@code TypeResolverBuilder} 和 {@code TypeIdResolver}）的构建。
	 *
	 * @see Jackson2ObjectMapperBuilder#applicationContext(ApplicationContext)
	 * @since 4.1.3
	 */
	public Jackson2ObjectMapperBuilder handlerInstantiator(HandlerInstantiator handlerInstantiator) {
		this.handlerInstantiator = handlerInstantiator;
		return this;
	}

	/**
	 * 设置 Spring {@link ApplicationContext} 以自动装配 Jackson 处理器（{@link JsonSerializer},
	 * {@link JsonDeserializer}, {@link KeyDeserializer}, {@code TypeResolverBuilder} 和 {@code TypeIdResolver}）。
	 *
	 * @see SpringHandlerInstantiator
	 * @since 4.1.3
	 */
	public Jackson2ObjectMapperBuilder applicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		return this;
	}

	/**
	 * 一个选项，用于在构建器的所有其他配置属性应用之后，直接对
	 * {@code ObjectMapper} 实例应用额外的自定义设置。
	 *
	 * @param configurer 要应用的配置器。如果注册了多个配置器，
	 *                   它们将按注册顺序应用。
	 * @since 5.3
	 */
	public Jackson2ObjectMapperBuilder postConfigurer(Consumer<ObjectMapper> configurer) {
		this.configurer = (this.configurer != null ? this.configurer.andThen(configurer) : configurer);
		return this;
	}


	/**
	 * 构建一个新的 {@link ObjectMapper} 实例。
	 * <p>每次构建操作都会产生一个独立的 {@link ObjectMapper} 实例。
	 * 构建器的设置可以修改，随后的构建操作
	 * 将基于最新的设置生成新的 {@link ObjectMapper}。
	 *
	 * @return 新构建的 ObjectMapper
	 */
	@SuppressWarnings("unchecked")
	public <T extends ObjectMapper> T build() {
		// 创建一个 ObjectMapper 对象
		ObjectMapper mapper;
		// 如果需要创建 XML Mapper
		if (this.createXmlMapper) {
			// 根据条件创建 XML ObjectMapper
			mapper = (this.defaultUseWrapper != null ?
					new XmlObjectMapperInitializer().create(this.defaultUseWrapper, this.factory) :
					new XmlObjectMapperInitializer().create(this.factory));
		} else {
			// 否则，根据条件创建普通 ObjectMapper
			mapper = (this.factory != null ? new ObjectMapper(this.factory) : new ObjectMapper());
		}
		// 配置 ObjectMapper
		configure(mapper);
		// 返回配置后的 ObjectMapper 对象
		return (T) mapper;
	}

	/**
	 * 使用此构建器的设置配置现有的 {@link ObjectMapper} 实例。
	 * 这可以应用于任意数量的 {@code ObjectMappers}。
	 *
	 * @param objectMapper 要配置的 ObjectMapper
	 */
	public void configure(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");

		// 创建一个 MultiValueMap 用于存储要注册的模块
		MultiValueMap<Object, Module> modulesToRegister = new LinkedMultiValueMap<>();
		// 如果通过 服务加载器 查找模块
		if (this.findModulesViaServiceLoader) {
			// 使用 ObjectMapper 的静态方法找到并注册模块
			ObjectMapper.findModules(this.moduleClassLoader).forEach(module -> registerModule(module, modulesToRegister));
		} else if (this.findWellKnownModules) {
			// 否则，如果需要注册已知的模块
			registerWellKnownModulesIfAvailable(modulesToRegister);
		}

		// 注册用户自定义的模块
		if (this.modules != null) {
			this.modules.forEach(module -> registerModule(module, modulesToRegister));
		}
		// 注册用户提供的模块类
		if (this.moduleClasses != null) {
			// 遍历所有模块类
			for (Class<? extends Module> moduleClass : this.moduleClasses) {
				// 注册 模块类 到 要注册的模块列表 中
				registerModule(BeanUtils.instantiateClass(moduleClass), modulesToRegister);
			}
		}
		// 将所有注册的模块收集到一个列表中
		List<Module> modules = new ArrayList<>();
		// 遍历要注册的模块
		for (List<Module> nestedModules : modulesToRegister.values()) {
			// 将 嵌套的模块列表 添加进 模块列表 中
			modules.addAll(nestedModules);
		}
		// 将收集到的模块注册到 ObjectMapper 中
		objectMapper.registerModules(modules);

		// 设置日期格式
		if (this.dateFormat != null) {
			objectMapper.setDateFormat(this.dateFormat);
		}
		// 设置地区
		if (this.locale != null) {
			objectMapper.setLocale(this.locale);
		}
		// 设置时区
		if (this.timeZone != null) {
			objectMapper.setTimeZone(this.timeZone);
		}

		// 设置注解自省器
		if (this.annotationIntrospector != null) {
			objectMapper.setAnnotationIntrospector(this.annotationIntrospector);
		}
		// 设置属性命名策略
		if (this.propertyNamingStrategy != null) {
			objectMapper.setPropertyNamingStrategy(this.propertyNamingStrategy);
		}
		// 设置默认的类型序列化与反序列化
		if (this.defaultTyping != null) {
			objectMapper.setDefaultTyping(this.defaultTyping);
		}
		// 设置默认的属性包含规则
		if (this.serializationInclusion != null) {
			objectMapper.setDefaultPropertyInclusion(this.serializationInclusion);
		}

		// 设置过滤器提供者
		if (this.filters != null) {
			objectMapper.setFilterProvider(this.filters);
		}

		// 添加 MixIns 配置
		this.mixIns.forEach(objectMapper::addMixIn);

		// 如果有自定义的序列化器或反序列化器，则创建 SimpleModule 并注册
		if (!this.serializers.isEmpty() || !this.deserializers.isEmpty()) {
			// 创建简单模块
			SimpleModule module = new SimpleModule();
			// 添加序列化器
			addSerializers(module);
			// 添加反序列化器
			addDeserializers(module);
			// 注册简单模块
			objectMapper.registerModule(module);
		}

		// 设置可见性配置
		this.visibilities.forEach(objectMapper::setVisibility);

		// 自定义默认的特性配置
		customizeDefaultFeatures(objectMapper);
		// 配置每个特性的启用状态
		this.features.forEach((feature, enabled) -> configureFeature(objectMapper, feature, enabled));

		// 设置 处理程序实例化器
		if (this.handlerInstantiator != null) {
			objectMapper.setHandlerInstantiator(this.handlerInstantiator);
		} else if (this.applicationContext != null) {
			// 否则，如果有 应用程序上下文，则使用 Spring处理程序实例化器 创建 处理程序实例化器
			objectMapper.setHandlerInstantiator(
					new SpringHandlerInstantiator(this.applicationContext.getAutowireCapableBeanFactory()));
		}

		// 如果有自定义配置器，则接受并应用配置
		if (this.configurer != null) {
			this.configurer.accept(objectMapper);
		}
	}

	private void registerModule(Module module, MultiValueMap<Object, Module> modulesToRegister) {
		// 如果模块的类型编号为空，则将 SimpleModule 和 模块 添加 要注册的模块 中
		if (module.getTypeId() == null) {
			modulesToRegister.add(SimpleModule.class.getName(), module);
		} else {
			// 否则，使用类型编号 和 模块 添加到 要注册的模块 中
			modulesToRegister.set(module.getTypeId(), module);
		}
	}


	/**
	 * 对此方法的任何更改都应同时应用于 spring-jms 和 spring-messaging 的
	 * MappingJackson2MessageConverter 默认构造函数
	 */
	private void customizeDefaultFeatures(ObjectMapper objectMapper) {
		// 如果 特性列表 中不包含MapperFeature.DEFAULT_VIEW_INCLUSION，则配置该特性为false
		if (!this.features.containsKey(MapperFeature.DEFAULT_VIEW_INCLUSION)) {
			configureFeature(objectMapper, MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		}

		// 如果 特性列表 中不包含DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES，则配置该特性为false
		if (!this.features.containsKey(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
			configureFeature(objectMapper, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void addSerializers(SimpleModule module) {
		this.serializers.forEach((type, serializer) ->
				module.addSerializer((Class<? extends T>) type, (JsonSerializer<T>) serializer));
	}

	@SuppressWarnings("unchecked")
	private <T> void addDeserializers(SimpleModule module) {
		this.deserializers.forEach((type, deserializer) ->
				module.addDeserializer((Class<T>) type, (JsonDeserializer<? extends T>) deserializer));
	}

	@SuppressWarnings("deprecation")  // on Jackson 2.13: configure(MapperFeature, boolean)
	private void configureFeature(ObjectMapper objectMapper, Object feature, boolean enabled) {
		// 如果 特性 是JsonParser.Feature类型，则配置JsonParser.Feature
		if (feature instanceof JsonParser.Feature) {
			objectMapper.configure((JsonParser.Feature) feature, enabled);
		} else if (feature instanceof JsonGenerator.Feature) {
			// 如果 特性 是JsonGenerator.Feature类型，则配置JsonGenerator.Feature
			objectMapper.configure((JsonGenerator.Feature) feature, enabled);
		} else if (feature instanceof SerializationFeature) {
			// 如果 特性 是SerializationFeature类型，则配置SerializationFeature
			objectMapper.configure((SerializationFeature) feature, enabled);
		} else if (feature instanceof DeserializationFeature) {
			// 如果 特性 是DeserializationFeature类型，则配置DeserializationFeature
			objectMapper.configure((DeserializationFeature) feature, enabled);
		} else if (feature instanceof MapperFeature) {
			// 如果 特性 是MapperFeature类型，则配置MapperFeature
			objectMapper.configure((MapperFeature) feature, enabled);
		} else {
			// 如果无法识别 特性 的类型，则抛出 致命的Bean异常
			throw new FatalBeanException("Unknown feature class: " + feature.getClass().getName());
		}
	}

	@SuppressWarnings("unchecked")
	private void registerWellKnownModulesIfAvailable(MultiValueMap<Object, Module> modulesToRegister) {
		try {
			// 尝试加载jackson-datatype-jdk8 模块
			Class<? extends Module> jdk8ModuleClass = (Class<? extends Module>)
					ClassUtils.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", this.moduleClassLoader);
			// 实例化 jdk8 模块类
			Module jdk8Module = BeanUtils.instantiateClass(jdk8ModuleClass);
			// 将 jdk8 模块添加到模块列表
			modulesToRegister.set(jdk8Module.getTypeId(), jdk8Module);
		} catch (ClassNotFoundException ex) {
			// 如果 jackson-datatype-jdk8 不可用，捕获 未找到类异常
		}

		try {
			// 尝试加载jackson-datatype-jsr310 Java Time 模块
			Class<? extends Module> javaTimeModuleClass = (Class<? extends Module>)
					ClassUtils.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", this.moduleClassLoader);
			// 实例化  Java Time 模块类
			Module javaTimeModule = BeanUtils.instantiateClass(javaTimeModuleClass);
			// 将 Java Time 模块添加到模块列表
			modulesToRegister.set(javaTimeModule.getTypeId(), javaTimeModule);
		} catch (ClassNotFoundException ex) {
			// 如果 jackson-datatype-jsr310 不可用，捕获 未找到类异常
		}

		// 检查是否存在 Joda-Time 2.x
		if (ClassUtils.isPresent("org.joda.time.YearMonth", this.moduleClassLoader)) {
			try {
				// 尝试加载 jackson-datatype-joda Joda-Time 模块
				Class<? extends Module> jodaModuleClass = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.datatype.joda.JodaModule", this.moduleClassLoader);
				// 实例化 joda 模块类
				Module jodaModule = BeanUtils.instantiateClass(jodaModuleClass);
				// 将 joda 模块添加到模块列表
				modulesToRegister.set(jodaModule.getTypeId(), jodaModule);
			} catch (ClassNotFoundException ex) {
				// 如果 jackson-datatype-joda 不可用，捕获 未找到类异常
			}
		}

		// 检查 Kotlin 是否存在
		if (KotlinDetector.isKotlinPresent()) {
			try {
				// 尝试加载  jackson-module-kotlin Kotlin 模块
				Class<? extends Module> kotlinModuleClass = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", this.moduleClassLoader);
				// 实例化 kotlin 模块类
				Module kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
				// 将 kotlin 模块添加到模块列表
				modulesToRegister.set(kotlinModule.getTypeId(), kotlinModule);
			} catch (ClassNotFoundException ex) {
				// 如果 jackson-module-kotlin 不可用，捕获 未找到类异常
			}
		}
	}


	// 便利工厂方法

	/**
	 * 获取一个 {@link Jackson2ObjectMapperBuilder} 实例，以便构建常规的 JSON {@link ObjectMapper} 实例。
	 */
	public static Jackson2ObjectMapperBuilder json() {
		return new Jackson2ObjectMapperBuilder();
	}

	/**
	 * 获取一个 {@link Jackson2ObjectMapperBuilder} 实例，以便构建一个 {@link XmlMapper} 实例。
	 */
	public static Jackson2ObjectMapperBuilder xml() {
		return new Jackson2ObjectMapperBuilder().createXmlMapper(true);
	}

	/**
	 * 获取一个 {@link Jackson2ObjectMapperBuilder} 实例，以便构建一个 Smile 数据格式的 {@link ObjectMapper} 实例。
	 *
	 * @since 5.0
	 */
	public static Jackson2ObjectMapperBuilder smile() {
		return new Jackson2ObjectMapperBuilder().factory(new SmileFactoryInitializer().create());
	}

	/**
	 * 获取一个 {@link Jackson2ObjectMapperBuilder} 实例，以便构建一个 CBOR 数据格式的 {@link ObjectMapper} 实例。
	 *
	 * @since 5.0
	 */
	public static Jackson2ObjectMapperBuilder cbor() {
		return new Jackson2ObjectMapperBuilder().factory(new CborFactoryInitializer().create());
	}


	private static class XmlObjectMapperInitializer {

		public ObjectMapper create(@Nullable JsonFactory factory) {
			// 如果提供了Json工厂，则使用提供的XmlFactory创建XmlMapper实例
			if (factory != null) {
				return new XmlMapper((XmlFactory) factory);
			} else {
				// 否则，创建一个防御性的XmlInputFactory，并使用其创建XmlMapper实例
				return new XmlMapper(StaxUtils.createDefensiveInputFactory());
			}
		}

		public ObjectMapper create(boolean defaultUseWrapper, @Nullable JsonFactory factory) {
			// 创建一个JacksonXmlModule实例
			JacksonXmlModule module = new JacksonXmlModule();

			// 设置是否默认使用包装器
			module.setDefaultUseWrapper(defaultUseWrapper);

			// 如果提供了factory，则使用提供的XmlFactory创建XmlMapper实例
			if (factory != null) {
				return new XmlMapper((XmlFactory) factory, module);
			} else {
				// 否则，创建一个新的XmlFactory，并使用其创建XmlMapper实例
				return new XmlMapper(new XmlFactory(StaxUtils.createDefensiveInputFactory()), module);
			}
		}
	}


	private static class SmileFactoryInitializer {

		public JsonFactory create() {
			return new SmileFactory();
		}
	}


	private static class CborFactoryInitializer {

		public JsonFactory create() {
			return new CBORFactory();
		}
	}

}
