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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
/**
 * Office 2007 XLSX格式的Excel文档视图的便捷超类
 * (由POI-OOXML支持)。与Apache POI 3.5及更高版本兼容。
 *
 * <p>要在子类中使用工作簿，请参见
 * <a href="https://poi.apache.org">Apache的POI网站</a>。
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public abstract class AbstractXlsxView extends AbstractXlsView {

	/**
	 * 默认构造函数。
	 * <p>将视图的内容类型设置为
	 * {@code "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}。
	 */
	public AbstractXlsxView() {
		setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	/**
	 * 此实现为XLSX格式创建一个{@link XSSFWorkbook}。
	 */
	@Override
	protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new XSSFWorkbook();
	}

}
