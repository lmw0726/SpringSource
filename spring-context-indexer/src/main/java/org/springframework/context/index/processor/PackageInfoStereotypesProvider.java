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

package org.springframework.context.index.processor;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * 一个 {@link StereotypesProvider} 实现，为每个 package-info
 * 提供 {@value #STEREOTYPE} 刻板印象（stereotype）。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
class PackageInfoStereotypesProvider implements StereotypesProvider {

	public static final String STEREOTYPE = "package-info";


	@Override
	public Set<String> getStereotypes(Element element) {
		Set<String> stereotypes = new HashSet<>();
		if (element.getKind() == ElementKind.PACKAGE) {
			stereotypes.add(STEREOTYPE);
		}
		return stereotypes;
	}

}
