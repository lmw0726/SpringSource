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

package org.springframework.validation;

/**
 * 针对应用程序特定对象的验证器。
 *
 * <p>该接口完全独立于任何基础设施或上下文；也就是说，它不与仅验证
 * Web 层、数据访问层或其他任何特定层的对象耦合。因此，它适用于
 * 应用程序的任何层，并支持将验证逻辑作为一等公民进行封装。
 *
 * <p>下面是一个简单但完整的 {@code Validator} 实现示例，
 * 它验证 {@code UserLogin} 实例的各个 {@link String} 属性
 * 不为空（即不为 {@code null} 且不完全由空白字符组成），
 * 并且任何存在的密码长度至少为 {@code 'MINIMUM_PASSWORD_LENGTH'} 个字符。
 *
 * <pre class="code">public class UserLoginValidator implements Validator {
 *
 *    private static final int MINIMUM_PASSWORD_LENGTH = 6;
 *
 *    public boolean supports(Class clazz) {
 *       return UserLogin.class.isAssignableFrom(clazz);
 *    }
 *
 *    public void validate(Object target, Errors errors) {
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userName", "field.required");
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "field.required");
 *       UserLogin login = (UserLogin) target;
 *       if (login.getPassword() != null
 *             &amp;&amp; login.getPassword().trim().length() &lt; MINIMUM_PASSWORD_LENGTH) {
 *          errors.rejectValue("password", "field.min.length",
 *                new Object[]{Integer.valueOf(MINIMUM_PASSWORD_LENGTH)},
 *                "The password must be at least [" + MINIMUM_PASSWORD_LENGTH + "] characters in length.");
 *       }
 *    }
 * }</pre>
 *
 * <p>另请参阅 Spring 参考手册，其中更全面地讨论了
 * {@code Validator} 接口及其在企业应用程序中的作用。
 *
 * @author Rod Johnson
 * @see SmartValidator
 * @see Errors
 * @see ValidationUtils
 */
public interface Validator {

	/**
	 * 此 {@link Validator} 能否对传入的 {@code clazz} 实例
	 * 进行 {@link #validate(Object, Errors) 验证}？
	 * <p>此方法的实现<i>通常</i>如下所示：
	 * <pre class="code">return Foo.class.isAssignableFrom(clazz);</pre>
	 * （其中 {@code Foo} 是将要被 {@link #validate(Object, Errors) 验证} 的
	 * 实际对象实例的类（或超类）。）
	 * @param clazz 被询问此 {@link Validator} 能否 {@link #validate(Object, Errors) 验证} 的 {@link Class}
	 * @return 如果此 {@link Validator} 确实能够
	 * {@link #validate(Object, Errors) 验证} 传入的 {@code clazz} 实例，则返回 {@code true}
	 */
	boolean supports(Class<?> clazz);

	/**
	 * 验证传入的 {@code target} 对象，该对象必须属于
	 * {@link #supports(Class)} 方法通常会（或将会）返回 {@code true} 的 {@link Class}。
	 * <p>传入的 {@link Errors errors} 实例可用于报告
	 * 产生的任何验证错误。
	 * @param target 要验证的对象
	 * @param errors 关于验证过程的上下文状态
	 * @see ValidationUtils
	 */
	void validate(Object target, Errors errors);

}
