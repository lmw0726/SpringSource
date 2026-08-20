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

package org.springframework.context.index.processor;

import java.util.Set;

import javax.lang.model.element.Element;

/**
 * 提供匹配 {@link Element} 的刻板印象（Stereotypes）列表。
 *
 * <p>如果元素有一个或多个刻板印象，它将被引用在候选组件的索引中，
 * 并且每个刻板印象都可以单独查询。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
interface StereotypesProvider {

	/**
	 * 返回给定 {@link Element} 上存在的刻板印象。
	 * @param element 要处理的元素
	 * @return 刻板印象，如果没有找到则返回空集合
	 */
	Set<String> getStereotypes(Element element);

}
