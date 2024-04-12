/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.view.document;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 用于传统XLS格式的Excel文档视图的便捷超类。
 * 与Apache POI 3.5及更高版本兼容。
 *
 * <p>要在子类中使用工作簿，请参见
 * <a href="https://poi.apache.org">Apache的POI网站</a>
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public abstract class AbstractXlsView extends AbstractView {

	/**
	 * 默认构造函数。
	 * 将视图的内容类型设置为"application/vnd.ms-excel"。
	 */
	public AbstractXlsView() {
		setContentType("application/vnd.ms-excel");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	/**
	 * 渲染Excel视图，给定指定的模型。
	 */
	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// 为此渲染步骤创建一个新的工作簿实例。
		Workbook workbook = createWorkbook(model, request);

		// 委托给应用程序提供的文档代码。
		buildExcelDocument(model, workbook, request, response);

		// 设置内容类型。
		response.setContentType(getContentType());

		// 将字节数组刷新到servlet输出流。
		renderWorkbook(workbook, response);
	}


	/**
	 * 用于创建POI {@link Workbook}实例的模板方法。
	 * <p>默认实现创建传统的{@link HSSFWorkbook}。
	 * Spring提供的子类为基于OOXML的变体重写了此方法；
	 * 自定义子类可以为从文件中读取工作簿重写此方法。
	 * @param model 模型映射
	 * @param request 当前HTTP请求（用于考虑URL或标头）
	 * @return 新的{@link Workbook}实例
	 */
	protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new HSSFWorkbook();
	}

	/**
	 * 实际的渲染步骤：将POI {@link Workbook}渲染到给定的响应中。
	 * @param workbook 要渲染的POI工作簿
	 * @param response 当前HTTP响应
	 * @throws IOException 当我们委托的I/O方法抛出时
	 */
	protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
		ServletOutputStream out = response.getOutputStream();
		// 将工作簿写入响应输出流
		workbook.write(out);
		// 关闭工作簿
		workbook.close();
	}

	/**
	 * 应用程序提供的子类必须实现此方法，以填充Excel工作簿文档，给定模型。
	 * @param model 模型映射
	 * @param workbook 要填充的Excel工作簿
	 * @param request 如果我们需要区域设置等。不应查看属性。
	 * @param response 如果我们需要设置cookie。不应写入它。
	 */
	protected abstract void buildExcelDocument(
			Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
