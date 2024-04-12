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

package org.springframework.web.servlet.view.document;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 用于Office 2007 XLSX格式的Excel文档视图的便捷超类，
 * 使用POI的流式变体。与Apache POI 3.9及更高版本兼容。
 *
 * <p>要在子类中使用工作簿，请参见
 * <a href="https://poi.apache.org">Apache的POI网站</a>。
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public abstract class AbstractXlsxStreamingView extends AbstractXlsxView {

	/**
	 * 此实现为流式传输XLSX格式创建一个{@link SXSSFWorkbook}。
	 */
	@Override
	protected SXSSFWorkbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new SXSSFWorkbook();
	}

	/**
	 * 此实现在渲染完成后处置{@link SXSSFWorkbook}。
	 * @see org.apache.poi.xssf.streaming.SXSSFWorkbook#dispose()
	 */
	@Override
	protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
		super.renderWorkbook(workbook, response);

		// 处置临时文件，以防流式变体...
		((SXSSFWorkbook) workbook).dispose();
	}

}
