/*
 * Copyright 2002-2016 the original author or authors.
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

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Converter;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * 允许使用 Spring {@link ApplicationContext} 进行自动装配的方式创建 Jackson {@link JsonSerializer}、
 * {@link JsonDeserializer}、{@link KeyDeserializer}、{@link TypeResolverBuilder}、{@link TypeIdResolver} bean。
 *
 * <p>从 Spring 4.3 开始，此类重写了 {@link HandlerInstantiator} 中的所有工厂方法，
 * 包括非抽象方法和最近引入的 Jackson 2.4 和 2.5 中的方法：
 * {@link ValueInstantiator}、{@link ObjectIdGenerator}、{@link ObjectIdResolver}、
 * {@link PropertyNamingStrategy}、{@link Converter}、{@link VirtualBeanPropertyWriter}。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @see Jackson2ObjectMapperBuilder#handlerInstantiator(HandlerInstantiator)
 * @see ApplicationContext#getAutowireCapableBeanFactory()
 * @see HandlerInstantiator
 * @since 4.1.3
 */
public class SpringHandlerInstantiator extends HandlerInstantiator {
	/**
	 * 自动装配Bean工厂
	 */
	private final AutowireCapableBeanFactory beanFactory;


	/**
	 * 使用给定的 BeanFactory 创建一个新的 SpringHandlerInstantiator。
	 *
	 * @param beanFactory 目标 BeanFactory
	 */
	public SpringHandlerInstantiator(AutowireCapableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public JsonDeserializer<?> deserializerInstance(DeserializationConfig config,
													Annotated annotated, Class<?> implClass) {

		return (JsonDeserializer<?>) this.beanFactory.createBean(implClass);
	}

	@Override
	public KeyDeserializer keyDeserializerInstance(DeserializationConfig config,
												   Annotated annotated, Class<?> implClass) {

		return (KeyDeserializer) this.beanFactory.createBean(implClass);
	}

	@Override
	public JsonSerializer<?> serializerInstance(SerializationConfig config,
												Annotated annotated, Class<?> implClass) {

		return (JsonSerializer<?>) this.beanFactory.createBean(implClass);
	}

	@Override
	public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config,
															  Annotated annotated, Class<?> implClass) {

		return (TypeResolverBuilder<?>) this.beanFactory.createBean(implClass);
	}

	@Override
	public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
		return (TypeIdResolver) this.beanFactory.createBean(implClass);
	}

	/**
	 * @since 4.3
	 */
	@Override
	public ValueInstantiator valueInstantiatorInstance(MapperConfig<?> config,
													   Annotated annotated, Class<?> implClass) {

		return (ValueInstantiator) this.beanFactory.createBean(implClass);
	}

	/**
	 * @since 4.3
	 */
	@Override
	public ObjectIdGenerator<?> objectIdGeneratorInstance(MapperConfig<?> config,
														  Annotated annotated, Class<?> implClass) {

		return (ObjectIdGenerator<?>) this.beanFactory.createBean(implClass);
	}

	/**
	 * @since 4.3
	 */
	@Override
	public ObjectIdResolver resolverIdGeneratorInstance(MapperConfig<?> config,
														Annotated annotated, Class<?> implClass) {

		return (ObjectIdResolver) this.beanFactory.createBean(implClass);
	}

	/**
	 * @since 4.3
	 */
	@Override
	public PropertyNamingStrategy namingStrategyInstance(MapperConfig<?> config,
														 Annotated annotated, Class<?> implClass) {

		return (PropertyNamingStrategy) this.beanFactory.createBean(implClass);
	}

	/**
	 * @since 4.3
	 */
	@Override
	public Converter<?, ?> converterInstance(MapperConfig<?> config,
											 Annotated annotated, Class<?> implClass) {

		return (Converter<?, ?>) this.beanFactory.createBean(implClass);
	}

	/**
	 * @since 4.3
	 */
	@Override
	public VirtualBeanPropertyWriter virtualPropertyWriterInstance(MapperConfig<?> config, Class<?> implClass) {
		return (VirtualBeanPropertyWriter) this.beanFactory.createBean(implClass);
	}

}
