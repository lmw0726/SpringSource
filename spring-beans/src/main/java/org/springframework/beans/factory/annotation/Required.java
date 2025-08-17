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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法（通常是 JavaBean 的 setter 方法）为“必需”的：
 * 即该 setter 方法必须被配置为通过依赖注入获得一个值。
 *
 * <p>请参考 {@link RequiredAnnotationBeanPostProcessor} 类的 javadoc
 *（默认情况下，该类会检查此注解的存在）。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see RequiredAnnotationBeanPostProcessor
 * @deprecated 自 5.1 起，推荐使用构造器注入来设置必需属性
 * （或使用自定义 {@link org.springframework.beans.factory.InitializingBean} 实现）
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Required {

}
