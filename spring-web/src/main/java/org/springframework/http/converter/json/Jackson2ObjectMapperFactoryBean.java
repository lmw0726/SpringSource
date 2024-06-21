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

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * 用于创建 Jackson 2.x {@link ObjectMapper}（默认）或 {@link XmlMapper}（{@code createXmlMapper} 属性设置为 true）的 {@link FactoryBean}，
 * 可通过 XML 配置启用或禁用 Jackson 功能。
 *
 * <p>它自定义了 Jackson 默认属性如下：
 * <ul>
 * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} 被禁用</li>
 * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} 被禁用</li>
 * </ul>
 *
 * <p>示例用法与 {@link MappingJackson2HttpMessageConverter}：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter"&gt;
 *   &lt;property name="objectMapper"&gt;
 *     &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"
 *       p:autoDetectFields="false"
 *       p:autoDetectGettersSetters="false"
 *       p:annotationIntrospector-ref="jaxbAnnotationIntrospector" /&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>与 MappingJackson2JsonView 的示例用法：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.web.servlet.view.json.MappingJackson2JsonView"&gt;
 *   &lt;property name="objectMapper"&gt;
 *     &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"
 *       p:failOnEmptyBeans="false"
 *       p:indentOutput="true"&gt;
 *       &lt;property name="serializers"&gt;
 *         &lt;array&gt;
 *           &lt;bean class="org.mycompany.MyCustomSerializer" /&gt;
 *         &lt;/array&gt;
 *       &lt;/property&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>如果某些选项没有提供特定的 setter 方法（例如某些不常用的选项），你仍然可以使用更通用的方法 {@link #setFeaturesToEnable} 和
 * {@link #setFeaturesToDisable}。
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"&gt;
 *   &lt;property name="featuresToEnable"&gt;
 *     &lt;array&gt;
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.SerializationFeature.WRAP_ROOT_VALUE"/&gt;
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.SerializationFeature.CLOSE_CLOSEABLE"/&gt;
 *     &lt;/array&gt;
 *   &lt;/property&gt;
 *   &lt;property name="featuresToDisable"&gt;
 *     &lt;array&gt;
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.MapperFeature.USE_ANNOTATIONS"/&gt;
 *     &lt;/array&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>如果你想要使用自定义的 {@link Module} 配置 Jackson 的 {@link ObjectMapper}，你可以通过类名注册一个或多个这样的模块，使用 {@link #setModulesToInstall}：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"&gt;
 *   &lt;property name="modulesToInstall" value="myapp.jackson.MySampleModule,myapp.jackson.MyOtherModule"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>如果它们存在于类路径中，它还会自动注册以下已知模块：
 * <ul>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk7">jackson-datatype-jdk7</a>:
 * 支持 Java 7 类型如 {@link java.nio.file.Path}</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk8">jackson-datatype-jdk8</a>:
 * 支持其他 Java 8 类型如 {@link java.util.Optional}</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jsr310">jackson-datatype-jsr310</a>:
 * 支持 Java 8 日期和时间 API 类型</li>
 * <li><a href="https://github.com/FasterXML/jackson-module-kotlin">jackson-module-kotlin</a>:
 * 支持 Kotlin 类和数据类</li>
 * </ul>
 *
 * <p>与 Jackson 2.9 到 2.12 兼容，截至 Spring 5.3。
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Tadaya Tsuyukubo
 * @author Sebastien Deleuze
 * @since 3.2
 */
public class Jackson2ObjectMapperFactoryBean implements FactoryBean<ObjectMapper>, BeanClassLoaderAware,
		ApplicationContextAware, InitializingBean {
	/**
	 * Jackson2对象映射器生成器
	 */
	private final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

	/**
	 * 对象映射
	 */
	@Nullable
	private ObjectMapper objectMapper;


	/**
	 * 设置要使用的 {@link ObjectMapper} 实例。如果未设置，则将使用其默认构造函数创建 {@link ObjectMapper}。
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * 如果设置为 true，并且未设置自定义 {@link ObjectMapper}，将使用其默认构造函数创建 {@link XmlMapper}。
	 *
	 * @since 4.1
	 */
	public void setCreateXmlMapper(boolean createXmlMapper) {
		this.builder.createXmlMapper(createXmlMapper);
	}

	/**
	 * 设置用于创建 {@link ObjectMapper} 实例的 {@link JsonFactory}。
	 *
	 * @since 5.0
	 */
	public void setFactory(JsonFactory factory) {
		this.builder.factory(factory);
	}

	/**
	 * 使用给定的 {@link DateFormat} 定义日期/时间格式。
	 * <p>注意：根据 Jackson 的线程安全规则，设置此属性会使暴露的 {@link ObjectMapper} 非线程安全。
	 *
	 * @see #setSimpleDateFormat(String)
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.builder.dateFormat(dateFormat);
	}

	/**
	 * 使用 {@link SimpleDateFormat} 定义日期/时间格式。
	 * <p>注意：根据 Jackson 的线程安全规则，设置此属性会使暴露的 {@link ObjectMapper} 非线程安全。
	 *
	 * @see #setDateFormat(DateFormat)
	 */
	public void setSimpleDateFormat(String format) {
		this.builder.simpleDateFormat(format);
	}

	/**
	 * 覆盖默认的 {@link Locale} 用于格式化。
	 * 默认值为 {@link Locale#getDefault()}。
	 *
	 * @since 4.1.5
	 */
	public void setLocale(Locale locale) {
		this.builder.locale(locale);
	}

	/**
	 * 覆盖默认的 {@link TimeZone} 用于格式化。
	 * 默认值为 UTC（而非本地时区）。
	 *
	 * @since 4.1.5
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.builder.timeZone(timeZone);
	}

	/**
	 * 设置用于序列化和反序列化的 {@link AnnotationIntrospector}。
	 */
	public void setAnnotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.builder.annotationIntrospector(annotationIntrospector);
	}

	/**
	 * 指定 {@link com.fasterxml.jackson.databind.PropertyNamingStrategy} 来配置 {@link ObjectMapper}。
	 *
	 * @since 4.0.2
	 */
	public void setPropertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
		this.builder.propertyNamingStrategy(propertyNamingStrategy);
	}

	/**
	 * 指定 {@link TypeResolverBuilder} 用于配置 Jackson 的默认类型处理。
	 *
	 * @since 4.2.2
	 */
	public void setDefaultTyping(TypeResolverBuilder<?> typeResolverBuilder) {
		this.builder.defaultTyping(typeResolverBuilder);
	}

	/**
	 * 设置自定义的序列化包含策略。
	 *
	 * @see com.fasterxml.jackson.annotation.JsonInclude.Include
	 */
	public void setSerializationInclusion(JsonInclude.Include serializationInclusion) {
		this.builder.serializationInclusion(serializationInclusion);
	}

	/**
	 * 设置全局过滤器以支持 {@link JsonFilter @JsonFilter} 注解的 POJO。
	 *
	 * @see Jackson2ObjectMapperBuilder#filters(FilterProvider)
	 * @since 4.2
	 */
	public void setFilters(FilterProvider filters) {
		this.builder.filters(filters);
	}

	/**
	 * 设置用于增强指定类或接口的混合注解。
	 *
	 * @param mixIns 一个映射，其中目标类（或接口）作为键，要添加到目标注解中的混合类（或接口）作为值。
	 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixInAnnotations(Class, Class)
	 * @since 4.1.2
	 */
	public void setMixIns(Map<Class<?>, Class<?>> mixIns) {
		this.builder.mixIns(mixIns);
	}

	/**
	 * 配置自定义序列化器。每个序列化器都注册到 {@link JsonSerializer#handledType()} 返回的类型上，该类型不能为 {@code null}。
	 *
	 * @see #setSerializersByType(Map)
	 */
	public void setSerializers(JsonSerializer<?>... serializers) {
		this.builder.serializers(serializers);
	}

	/**
	 * 为给定类型配置自定义序列化器。
	 *
	 * @see #setSerializers(JsonSerializer...)
	 */
	public void setSerializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
		this.builder.serializersByType(serializers);
	}

	/**
	 * 配置自定义反序列化器。每个反序列化器都注册到 {@link JsonDeserializer#handledType()} 返回的类型上，该类型不能为 {@code null}。
	 *
	 * @see #setDeserializersByType(Map)
	 * @since 4.3
	 */
	public void setDeserializers(JsonDeserializer<?>... deserializers) {
		this.builder.deserializers(deserializers);
	}

	/**
	 * 为给定类型配置自定义反序列化器。
	 */
	public void setDeserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
		this.builder.deserializersByType(deserializers);
	}

	/**
	 * 快捷设置 {@link MapperFeature#AUTO_DETECT_FIELDS} 选项。
	 */
	public void setAutoDetectFields(boolean autoDetectFields) {
		this.builder.autoDetectFields(autoDetectFields);
	}

	/**
	 * 快捷设置 {@link MapperFeature#AUTO_DETECT_SETTERS}/
	 * {@link MapperFeature#AUTO_DETECT_GETTERS}/{@link MapperFeature#AUTO_DETECT_IS_GETTERS}
	 * 选项。
	 */
	public void setAutoDetectGettersSetters(boolean autoDetectGettersSetters) {
		this.builder.autoDetectGettersSetters(autoDetectGettersSetters);
	}

	/**
	 * 快捷设置 {@link MapperFeature#DEFAULT_VIEW_INCLUSION} 选项。
	 *
	 * @since 4.1
	 */
	public void setDefaultViewInclusion(boolean defaultViewInclusion) {
		this.builder.defaultViewInclusion(defaultViewInclusion);
	}

	/**
	 * 快捷设置 {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} 选项。
	 *
	 * @since 4.1.1
	 */
	public void setFailOnUnknownProperties(boolean failOnUnknownProperties) {
		this.builder.failOnUnknownProperties(failOnUnknownProperties);
	}

	/**
	 * 快捷设置 {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} 选项。
	 */
	public void setFailOnEmptyBeans(boolean failOnEmptyBeans) {
		this.builder.failOnEmptyBeans(failOnEmptyBeans);
	}

	/**
	 * 快捷设置 {@link SerializationFeature#INDENT_OUTPUT} 选项。
	 */
	public void setIndentOutput(boolean indentOutput) {
		this.builder.indentOutput(indentOutput);
	}

	/**
	 * 定义是否默认使用包装器来处理索引化（List、数组）属性（仅适用于 {@link XmlMapper}）。
	 *
	 * @since 4.3
	 */
	public void setDefaultUseWrapper(boolean defaultUseWrapper) {
		this.builder.defaultUseWrapper(defaultUseWrapper);
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
	public void setFeaturesToEnable(Object... featuresToEnable) {
		this.builder.featuresToEnable(featuresToEnable);
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
	public void setFeaturesToDisable(Object... featuresToDisable) {
		this.builder.featuresToDisable(featuresToDisable);
	}

	/**
	 * 设置要注册到 {@link ObjectMapper} 的完整模块列表。
	 * <p>注意：如果设置了此项，Jackson 和 Spring 都不会执行模块的查找（参见 {@link #setFindModulesViaServiceLoader}）。
	 * 因此，如果这里指定了空列表，将会禁止任何类型的模块检测。
	 * <p>请指定此项或 {@link #setModulesToInstall}，而不是同时指定两者。
	 *
	 * @see com.fasterxml.jackson.databind.Module
	 * @since 4.0
	 */
	public void setModules(List<Module> modules) {
		this.builder.modules(modules);
	}

	/**
	 * 指定要注册到 {@link ObjectMapper} 的一个或多个模块（通过类或类名在 XML 中）。
	 * <p>在此处指定的模块将在 Spring 自动检测 JSR-310 和 Joda-Time 或 Jackson 的模块查找之后注册
	 * （参见 {@link #setFindModulesViaServiceLoader}），允许最终覆盖它们的配置。
	 * <p>请指定此项或 {@link #setModules}，而不是同时指定两者。
	 *
	 * @see com.fasterxml.jackson.databind.Module
	 * @since 4.0.1
	 */
	@SafeVarargs
	public final void setModulesToInstall(Class<? extends Module>... modules) {
		this.builder.modulesToInstall(modules);
	}

	/**
	 * 设置是否通过 JDK ServiceLoader 让 Jackson 查找可用模块，基于类路径中的 META-INF 元数据。
	 * 要求使用 Jackson 2.2 或更高版本。
	 * <p>如果未设置此模式，Spring 的 Jackson2ObjectMapperFactoryBean 将尝试在类路径上找到 JSR-310 和 Joda-Time
	 * 支持模块（前提是 Java 8 和 Joda-Time 本身也可用）。
	 *
	 * @see com.fasterxml.jackson.databind.ObjectMapper#findModules()
	 * @since 4.0.1
	 */
	public void setFindModulesViaServiceLoader(boolean findModules) {
		this.builder.findModulesViaServiceLoader(findModules);
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.builder.moduleClassLoader(beanClassLoader);
	}

	/**
	 * 自定义构建 Jackson 处理器（{@link JsonSerializer}、{@link JsonDeserializer}、{@link KeyDeserializer}、
	 * {@code TypeResolverBuilder} 和 {@code TypeIdResolver}）的方式。
	 *
	 * @see Jackson2ObjectMapperFactoryBean#setApplicationContext(ApplicationContext)
	 * @since 4.1.3
	 */
	public void setHandlerInstantiator(HandlerInstantiator handlerInstantiator) {
		this.builder.handlerInstantiator(handlerInstantiator);
	}

	/**
	 * 设置构建器 {@link ApplicationContext}，以便自动装配 Jackson 处理器
	 * （{@link JsonSerializer}、{@link JsonDeserializer}、{@link KeyDeserializer}、
	 * {@code TypeResolverBuilder} 和 {@code TypeIdResolver}）。
	 *
	 * @see Jackson2ObjectMapperBuilder#applicationContext(ApplicationContext)
	 * @see SpringHandlerInstantiator
	 * @since 4.1.3
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.builder.applicationContext(applicationContext);
	}


	@Override
	public void afterPropertiesSet() {
		// 如果已经设置了 对象映射
		if (this.objectMapper != null) {
			// 使用已有的 对象映射 配置 Jackson2对象映射器生成器
			this.builder.configure(this.objectMapper);
		} else {
			// 否则，构建新的 对象映射 并保存到 Jackson2对象映射器生成器 中
			this.objectMapper = this.builder.build();
		}
	}

	/**
	 * 返回单例的ObjectMapper实例。
	 */
	@Override
	@Nullable
	public ObjectMapper getObject() {
		return this.objectMapper;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.objectMapper != null ? this.objectMapper.getClass() : null);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
