/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @AliasFor} 是一个注解，用于声明注解属性的别名。
 *
 * <h3>使用场景</h3>
 * <ul>
 * <li><strong>注解内的显式别名</strong>：在单个注解中，可以在一对属性上声明 {@code @AliasFor}，
 * 表明它们是彼此可互换的别名。</li>
 * <li><strong>元注解中属性的显式别名</strong>：如果 {@code @AliasFor} 的 {@link #annotation}
 * 属性设置为与声明它的注解不同的注解，则 {@link #attribute} 被解释为元注解中属性的别名
 * （即，显式元注解属性覆盖）。这使得可以对注解层次结构中究竟哪些属性被覆盖进行细粒度控制。
 * 事实上，使用 {@code @AliasFor} 甚至可以声明元注解的 {@code value} 属性的别名。</li>
 * <li><strong>注解内的隐式别名</strong>：如果注解内的一个或多个属性被声明为同一元注解属性的属性覆盖
 * （直接或间接），则这些属性将被视为彼此的<em>隐式</em>别名集，
 * 产生与注解内显式别名类似的行为。</li>
 * </ul>
 *
 * <h3>使用要求</h3>
 * <p>与 Java 中的任何注解一样，仅存在 {@code @AliasFor} 本身并不会强制执行别名语义。
 * 要强制执行别名语义，注解必须通过 {@link MergedAnnotations} 进行<em>加载</em>。
 *
 * <h3>实现要求</h3>
 * <ul>
 * <li><strong>注解内的显式别名</strong>：
 * <ol>
 * <li>构成别名对的每个属性都应该用 {@code @AliasFor} 注解，并且 {@link #attribute} 或
 * {@link #value} 必须引用该对中的<em>另一个</em>属性。自 Spring Framework 5.2.1 起，
 * 技术上可以在别名对中只注解其中一个属性；但是，建议注解别名对中的两个属性，
 * 以便更好地文档化并与 Spring Framework 的早期版本兼容。</li>
 * <li>别名属性必须声明相同的返回类型。</li>
 * <li>别名属性必须声明默认值。</li>
 * <li>别名属性必须声明相同的默认值。</li>
 * <li>不应声明 {@link #annotation}。</li>
 * </ol>
 * </li>
 * <li><strong>元注解中属性的显式别名</strong>：
 * <ol>
 * <li>作为元注解中属性别名的属性必须用 {@code @AliasFor} 注解，并且 {@link #attribute}
 * 必须引用元注解中的属性。</li>
 * <li>别名属性必须声明相同的返回类型。</li>
 * <li>{@link #annotation} 必须引用元注解。</li>
 * <li>引用的元注解必须在声明 {@code @AliasFor} 的注解类上<em>元存在</em>。</li>
 * </ol>
 * </li>
 * <li><strong>注解内的隐式别名</strong>：
 * <ol>
 * <li>属于隐式别名集中的每个属性都必须用 {@code @AliasFor} 注解，并且 {@link #attribute}
 * 必须引用同一元注解中的相同属性（直接或通过注解层次结构中的其他显式元注解属性覆盖）。</li>
 * <li>别名属性必须声明相同的返回类型。</li>
 * <li>别名属性必须声明默认值。</li>
 * <li>别名属性必须声明相同的默认值。</li>
 * <li>{@link #annotation} 必须引用适当的元注解。</li>
 * <li>引用的元注解必须在声明 {@code @AliasFor} 的注解类上<em>元存在</em>。</li>
 * </ol>
 * </li>
 * </ul>
 *
 * <h3>示例：注解内的显式别名</h3>
 * <p>在 {@code @ContextConfiguration} 中，{@code value} 和 {@code locations} 彼此是显式别名。
 *
 * <pre class="code"> public &#064;interface ContextConfiguration {
 *
 *    &#064;AliasFor("locations")
 *    String[] value() default {};
 *
 *    &#064;AliasFor("value")
 *    String[] locations() default {};
 *
 *    // ...
 * }</pre>
 *
 * <h3>示例：元注解中属性的显式别名</h3>
 * <p>在 {@code @XmlTestConfig} 中，{@code xmlFiles} 是 {@code @ContextConfiguration} 中
 * {@code locations} 的显式别名。换句话说，{@code xmlFiles} 覆盖了
 * {@code @ContextConfiguration} 中的 {@code locations} 属性。
 *
 * <pre class="code"> &#064;ContextConfiguration
 * public &#064;interface XmlTestConfig {
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xmlFiles();
 * }</pre>
 *
 * <h3>示例：注解内的隐式别名</h3>
 * <p>在 {@code @MyTestConfig} 中，{@code value}、{@code groovyScripts} 和 {@code xmlFiles}
 * 都是 {@code @ContextConfiguration} 中 {@code locations} 属性的显式元注解属性覆盖。
 * 因此，这三个属性也是彼此的隐式别名。
 *
 * <pre class="code"> &#064;ContextConfiguration
 * public &#064;interface MyTestConfig {
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] value() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] groovyScripts() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xmlFiles() default {};
 * }</pre>
 *
 * <h3>示例：注解内的传递性隐式别名</h3>
 * <p>在 {@code @GroovyOrXmlTestConfig} 中，{@code groovy} 是 {@code @MyTestConfig} 中
 * {@code groovyScripts} 属性的显式覆盖；而 {@code xml} 是 {@code @ContextConfiguration} 中
 * {@code locations} 属性的显式覆盖。此外，{@code groovy} 和 {@code xml} 彼此是传递性隐式别名，
 * 因为它们都有效地覆盖了 {@code @ContextConfiguration} 中的 {@code locations} 属性。
 *
 * <pre class="code"> &#064;MyTestConfig
 * public &#064;interface GroovyOrXmlTestConfig {
 *
 *    &#064;AliasFor(annotation = MyTestConfig.class, attribute = "groovyScripts")
 *    String[] groovy() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xml() default {};
 * }</pre>
 *
 * <h3>支持属性别名的 Spring 注解</h3>
 * <p>自 Spring Framework 4.2 起，Spring 核心中的几个注解已更新为使用 {@code @AliasFor}
 * 来配置其内部属性别名。有关详细信息，请查阅各个注解的 Javadoc 和参考手册。
 *
 * @author Sam Brannen
 * @since 4.2
 * @see MergedAnnotations
 * @see SynthesizedAnnotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface AliasFor {

	/**
	 * {@link #attribute} 的别名。
	 * <p>旨在当未声明 {@link #annotation} 时代替 {@link #attribute} 使用
	 * &mdash; 例如：{@code @AliasFor("value")} 而不是 {@code @AliasFor(attribute = "value")}。
	 */
	@AliasFor("attribute")
	String value() default "";

	/**
	 * <em>此</em>属性为其别名的属性名称。
	 * @see #value
	 */
	@AliasFor("value")
	String attribute() default "";

	/**
	 * 声明别名 {@link #attribute} 的注解类型。
	 * <p>默认为 {@link Annotation}，表示别名属性声明在与<em>此</em>属性相同的注解中。
	 */
	Class<? extends Annotation> annotation() default Annotation.class;

}
