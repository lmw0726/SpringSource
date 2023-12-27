/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.result.view.freemarker;

import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;

/**
 * 用于解析 {@link FreeMarkerView} 实例的 {@code ViewResolver}，即 FreeMarker 模板及其自定义子类。
 *
 * <p>此解析器生成的所有视图的视图类都可以通过 "viewClass" 属性指定。有关详细信息，请参阅 {@link UrlBasedViewResolver}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FreeMarkerViewResolver extends UrlBasedViewResolver {

	/**
	 * 简单的构造函数。
	 */
	public FreeMarkerViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * 带有前缀和后缀的便利构造函数。
	 *
	 * @param prefix 视图名称前缀
	 * @param suffix 视图名称后缀
	 */
	public FreeMarkerViewResolver(String prefix, String suffix) {
		setViewClass(requiredViewClass());
		setPrefix(prefix);
		setSuffix(suffix);
	}


	/**
	 * 需要 {@link FreeMarkerView}。
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return FreeMarkerView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == FreeMarkerView.class ? new FreeMarkerView() : super.instantiateView());
	}

}
