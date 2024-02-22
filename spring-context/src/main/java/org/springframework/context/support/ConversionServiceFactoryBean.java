/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.lang.Nullable;

import java.util.Set;

/**
 * ConversionServiceFactoryBean 是一个工厂，提供了方便访问的 ConversionService，配置了适用于大多数环境的转换器。
 * 设置 "converters" 属性以补充默认转换器。
 * <p>
 * 此实现创建一个 DefaultConversionService。子类可以覆盖 createConversionService() 方法，以返回所选的 GenericConversionService 实例。
 * <p>
 * 像所有 FactoryBean 实现一样，此类适用于使用 Spring <beans> XML 配置 Spring 应用程序上下文。
 * 在使用 org.springframework.context.annotation.Configuration @Configuration 类配置容器时，
 * 只需从 @Bean 方法中实例化、配置并返回适当的 ConversionService 对象即可。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 */
public class ConversionServiceFactoryBean implements FactoryBean<ConversionService>, InitializingBean {

	/**
	 * 转换器集合
	 */
	@Nullable
	private Set<?> converters;
	/**
	 * 通用转换器服务
	 */
	@Nullable
	private GenericConversionService conversionService;


	/**
	 * 配置应添加的自定义转换器对象集合：
	 * 实现 {@link org.springframework.core.convert.converter.Converter}、
	 * {@link org.springframework.core.convert.converter.ConverterFactory} 或
	 * {@link org.springframework.core.convert.converter.GenericConverter} 接口的对象。
	 *
	 * @param converters 应添加的自定义转换器对象集合
	 */
	public void setConverters(Set<?> converters) {
		this.converters = converters;
	}

	@Override
	public void afterPropertiesSet() {
		// 创建转换服务
		this.conversionService = createConversionService();
		// 将转换器注册到转换服务中
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
	}

	/**
	 * 创建此工厂bean返回的 ConversionService 实例。
	 * <p>默认情况下创建一个简单的 {@link GenericConversionService} 实例。
	 * 子类可以重写此方法以定制要创建的 ConversionService 实例。
	 *
	 * @return 此工厂bean返回的 ConversionService 实例
	 */
	protected GenericConversionService createConversionService() {
		return new DefaultConversionService();
	}


	// implementing FactoryBean

	@Override
	@Nullable
	public ConversionService getObject() {
		return this.conversionService;
	}

	@Override
	public Class<? extends ConversionService> getObjectType() {
		return GenericConversionService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
