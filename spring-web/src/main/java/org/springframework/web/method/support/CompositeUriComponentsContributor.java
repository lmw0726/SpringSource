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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * 一个{@link UriComponentsContributor}，包含要委托的其他贡献者列表，并封装了用于将方法参数值格式化为字符串的特定{@link ConversionService}。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class CompositeUriComponentsContributor implements UriComponentsContributor {
	/**
	 * URL组件贡献者或处理器方法参数解析器列表集合
	 */
	private final List<Object> contributors;

	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	/**
	 * 从{@link UriComponentsContributor UriComponentsContributors}或{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}集合创建一个实例。
	 * 由于这两者通常由相同的类实现，因此最方便的选项是获取{@code RequestMappingHandlerAdapter}中配置的{@code HandlerMethodArgumentResolvers}并将其提供给此构造函数。
	 *
	 * @param contributors 一组{@link UriComponentsContributor}或{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
	 */
	public CompositeUriComponentsContributor(UriComponentsContributor... contributors) {
		this.contributors = Arrays.asList((Object[]) contributors);
		this.conversionService = new DefaultFormattingConversionService();
	}

	/**
	 * 从{@link UriComponentsContributor UriComponentsContributors}或{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}集合创建一个实例。
	 * 由于这两者通常由相同的类实现，因此最方便的选项是获取{@code RequestMappingHandlerAdapter}中配置的{@code HandlerMethodArgumentResolvers}并将其提供给此构造函数。
	 *
	 * @param contributors 一组{@link UriComponentsContributor}或{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
	 */
	public CompositeUriComponentsContributor(Collection<?> contributors) {
		this(contributors, null);
	}

	/**
	 * 从{@link UriComponentsContributor UriComponentsContributors}或{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}集合创建一个实例。
	 * 由于这两者通常由相同的类实现，因此最方便的选项是获取{@code RequestMappingHandlerAdapter}中配置的{@code HandlerMethodArgumentResolvers}并将其提供给此构造函数。
	 * <p>如果{@link ConversionService}参数为{@code null}，则默认使用{@link org.springframework.format.support.DefaultFormattingConversionService}。
	 *
	 * @param contributors 一组{@link UriComponentsContributor}或{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
	 * @param cs           用于在将方法参数值作为字符串添加到URI之前对其进行格式化的ConversionService
	 */
	public CompositeUriComponentsContributor(@Nullable Collection<?> contributors, @Nullable ConversionService cs) {
		this.contributors = (contributors != null ? new ArrayList<>(contributors) : Collections.emptyList());
		this.conversionService = (cs != null ? cs : new DefaultFormattingConversionService());
	}

	/**
	 * 确定此{@code CompositeUriComponentsContributor}是否有任何贡献者。
	 *
	 * @return 如果此{@code CompositeUriComponentsContributor}是使用委托的贡献者创建的，则返回{@code true}
	 */
	public boolean hasContributors() {
		return !this.contributors.isEmpty();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 遍历所有贡献者
		for (Object contributor : this.contributors) {
			// 如果贡献者是 UriComponentsContributor 类型
			if (contributor instanceof UriComponentsContributor) {
				if (((UriComponentsContributor) contributor).supportsParameter(parameter)) {
					// 如果当前贡献者支持该参数，返回 true
					return true;
				}
			} else if (contributor instanceof HandlerMethodArgumentResolver) {
				// 如果贡献者是 HandlerMethodArgumentResolver 类型
				if (((HandlerMethodArgumentResolver) contributor).supportsParameter(parameter)) {
					// 如果当前贡献者支持该参数，返回 false
					return false;
				}
			}
		}
		// 如果没有找到支持该参数的贡献者，返回 false
		return false;
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
										 UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		// 遍历所有贡献者
		for (Object contributor : this.contributors) {
			// 如果贡献者是 UriComponentsContributor 的实例
			if (contributor instanceof UriComponentsContributor) {
				UriComponentsContributor ucc = (UriComponentsContributor) contributor;
				// 如果贡献者支持该参数
				if (ucc.supportsParameter(parameter)) {
					// 贡献方法参数
					ucc.contributeMethodArgument(parameter, value, builder, uriVariables, conversionService);
					// 终止循环
					break;
				}
			} else if (contributor instanceof HandlerMethodArgumentResolver) {
				// 如果贡献者是 HandlerMethodArgumentResolver 的实例并且支持该参数
				if (((HandlerMethodArgumentResolver) contributor).supportsParameter(parameter)) {
					// 终止循环
					break;
				}
			}
		}
	}

	/**
	 * 使用在构造时创建的ConversionService的重载方法。
	 */
	public void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
										 Map<String, Object> uriVariables) {

		contributeMethodArgument(parameter, value, builder, uriVariables, this.conversionService);
	}

}
