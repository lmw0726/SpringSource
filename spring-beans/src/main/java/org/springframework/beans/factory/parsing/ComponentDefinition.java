/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * Interface that describes the logical view of a set of {@link BeanDefinition BeanDefinitions}
 * and {@link BeanReference BeanReferences} as presented in some configuration context.
 *
 * <p>With the introduction of {@link org.springframework.beans.factory.xml.NamespaceHandler pluggable custom XML tags},
 * it is now possible for a single logical configuration entity, in this case an XML tag, to
 * create multiple {@link BeanDefinition BeanDefinitions} and {@link BeanReference RuntimeBeanReferences}
 * in order to provide more succinct configuration and greater convenience to end users. As such, it can
 * no longer be assumed that each configuration entity (e.g. XML tag) maps to one {@link BeanDefinition}.
 * For tool vendors and other users who wish to present visualization or support for configuring Spring
 * applications it is important that there is some mechanism in place to tie the {@link BeanDefinition BeanDefinitions}
 * in the {@link org.springframework.beans.factory.BeanFactory} back to the configuration data in a way
 * that has concrete meaning to the end user. As such, {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * implementations are able to publish events in the form of a {@code ComponentDefinition} for each
 * logical entity being configured. Third parties can then {@link ReaderEventListener subscribe to these events},
 * allowing for a user-centric view of the bean metadata.
 *
 * <p>Each {@code ComponentDefinition} has a {@link #getSource source object} which is configuration-specific.
 * In the case of XML-based configuration this is typically the {@link org.w3c.dom.Node} which contains the user
 * supplied configuration information. In addition to this, each {@link BeanDefinition} enclosed in a
 * {@code ComponentDefinition} has its own {@link BeanDefinition#getSource() source object} which may point
 * to a different, more specific, set of configuration data. Beyond this, individual pieces of bean metadata such
 * as the {@link org.springframework.beans.PropertyValue PropertyValues} may also have a source object giving an
 * even greater level of detail. Source object extraction is handled through the
 * {@link SourceExtractor} which can be customized as required.
 *
 * <p>Whilst direct access to important {@link BeanReference BeanReferences} is provided through
 * {@link #getBeanReferences}, tools may wish to inspect all {@link BeanDefinition BeanDefinitions} to gather
 * the full set of {@link BeanReference BeanReferences}. Implementations are required to provide
 * all {@link BeanReference BeanReferences} that are required to validate the configuration of the
 * overall logical entity as well as those required to provide full user visualisation of the configuration.
 * It is expected that certain {@link BeanReference BeanReferences} will not be important to
 * validation or to the user view of the configuration and as such these may be omitted. A tool may wish to
 * display any additional {@link BeanReference BeanReferences} sourced through the supplied
 * {@link BeanDefinition BeanDefinitions} but this is not considered to be a typical case.
 *
 * <p>Tools can determine the important of contained {@link BeanDefinition BeanDefinitions} by checking the
 * {@link BeanDefinition#getRole role identifier}. The role is essentially a hint to the tool as to how
 * important the configuration provider believes a {@link BeanDefinition} is to the end user. It is expected
 * that tools will <strong>not</strong> display all {@link BeanDefinition BeanDefinitions} for a given
 * {@code ComponentDefinition} choosing instead to filter based on the role. Tools may choose to make
 * this filtering user configurable. Particular notice should be given to the
 * {@link BeanDefinition#ROLE_INFRASTRUCTURE INFRASTRUCTURE role identifier}. {@link BeanDefinition BeanDefinitions}
 * classified with this role are completely unimportant to the end user and are required only for
 * internal implementation reasons.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see AbstractComponentDefinition
 * @see CompositeComponentDefinition
 * @see BeanComponentDefinition
 * @see ReaderEventListener#componentRegistered(ComponentDefinition)
 * @since 2.0
 */
public interface ComponentDefinition extends BeanMetadataElement {

	/**
	 * 获取此 {@code ComponentDefinition} 的用户可见名称。
	 * <p> 这应该直接链接回给定上下文中此组件的相应配置数据。
	 */
	String getName();

	/**
	 * 返回描述组件的友好描述。
	 * <p> 鼓励实现从 {@code toString()} 返回相同的值。
	 */
	String getDescription();

	/**
	 * 返回注册形成此 {@code ComponentDefinition} 的 {@link BeanDefinition}。
	 * <p> 应该注意的是，通过 {@link BeanReference 引用}，
	 * {@code ComponentDefinition} 很可能与其他 {@link BeanDefinition BeanDefinition} 相关，
	 * 但是这些 <strong> 不</strong>包括在内，因为它们可能无法立即使用。
	 * 重要的 {@link BeanReference BeanReferences} 可从 {@link #getBeanReferences()} 获得。
	 *
	 * @return BeanDefinitions的数组，如果没有，则为空数组
	 */
	BeanDefinition[] getBeanDefinitions();

	/**
	 * 返回表示此组件中所有相关内部bean的 {@link BeanDefinition BeanDefinitions}。
	 * <p> 关联的 {@link BeanDefinition BeanDefinitions} 中可能存在其他内部bean，
	 * 但是，对于验证或用户可视化，不需要这些内部bean。
	 *
	 * @return BeanDefinitions的数组，如果没有，则为空数组
	 */
	BeanDefinition[] getInnerBeanDefinitions();

	/**
	 * 返回被认为对这个 {@code ComponentDefinition} 重要的 {@link BeanReference} 的集合。
	 * <p> 其他 {@link BeanReference BeanReferences} 可能存在于关联的 {@link BeanDefinition BeanDefinitions} 中，
	 * 但是对于验证或用户可视化而言，不需要这些。
	 *
	 * @return BeanReferences的数组，如果没有，则为空数组
	 */
	BeanReference[] getBeanReferences();

}
