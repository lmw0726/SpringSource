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

package org.springframework.web.servlet;

import org.springframework.util.Assert;

import javax.servlet.ServletException;

/**
 * 在应该转发到特定视图和特定模型的错误条件下抛出的异常。
 *
 * <p>可以在处理程序处理的任何时候抛出此异常。
 * 这包括预构建控制器的任何模板方法。
 * 例如，如果某些参数不允许使用正常工作流程进行继续，则表单控制器可能会中止到特定的错误页面。
 *
 * @author Juergen Hoeller
 * @since 22.11.2003
 */
@SuppressWarnings("serial")
public class ModelAndViewDefiningException extends ServletException {

	private final ModelAndView modelAndView;


	/**
	 * 使用给定的 ModelAndView 创建一个新的 ModelAndViewDefiningException，通常表示特定的错误页面。
	 *
	 * @param modelAndView 包含要转发到的视图和要公开的模型的 ModelAndView
	 */
	public ModelAndViewDefiningException(ModelAndView modelAndView) {
		Assert.notNull(modelAndView, "ModelAndView must not be null in ModelAndViewDefiningException");
		this.modelAndView = modelAndView;
	}

	/**
	 * 返回此异常包含的用于转发的 ModelAndView。
	 */
	public ModelAndView getModelAndView() {
		return this.modelAndView;
	}

}
