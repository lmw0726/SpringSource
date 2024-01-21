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

package org.springframework.beans.factory;

/**
 * 一个标记性的超级接口，表示一个bean有资格通过回调式方法被Spring容器通知特定的框架对象。
 * 实际的方法签名由各个子接口确定，但通常应该只包含一个接受单一参数且返回void的方法。
 *
 * <p>注意，仅仅实现 {@link Aware} 提供的接口并不提供默认功能。
 * 相反，处理必须在显式进行，例如在 {@link org.springframework.beans.factory.config.BeanPostProcessor} 中。
 * 参考 {@link org.springframework.context.support.ApplicationContextAwareProcessor} 以查看处理特定的 {@code *Aware} 接口回调的示例。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public interface Aware {

}
