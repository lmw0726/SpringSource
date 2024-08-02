package com.lmw.springmvc.argument;

import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.MatrixVariableMethodArgumentResolver;
import org.springframework.web.util.UrlPathHelper;

import java.util.List;
import java.util.Map;

/**
 * 方法上标注@MaxtrixVariable注解的示例
 * 这个控制器是 {@link MatrixVariableMethodArgumentResolver} 参数解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-01 21:23
 */
@Controller
@RequestMapping("/argument")
public class MatrixVariableMethodController {

	@Bean
	public WebMvcConfigurer webMvcConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void configurePathMatch(PathMatchConfigurer configurer) {
				UrlPathHelper urlPathHelper = new UrlPathHelper();
				urlPathHelper.setRemoveSemicolonContent(false);
				configurer.setUrlPathHelper(urlPathHelper);
			}
		};
	}


	/**
	 * 矩阵变量列表示例，使用@MatrixVariable 修饰 List<String>
	 * 访问：/argument/matrixVariable/list/list;matrixVariable=111,123
	 * 可以接收到参数
	 *
	 * @param matrixVariable 矩阵变量列表
	 * @return 解析结果
	 */
	@GetMapping("/matrixVariable/list/{list}")
	public ResponseEntity<List<String>> matrixVariableList(@PathVariable String list, @MatrixVariable List<String> matrixVariable) {
		return ResponseEntity.ok(matrixVariable);
	}

	/**
	 * 矩阵变量映射示例，使用@MatrixVariable 修饰  Map<String, Object>
	 * 访问 /argument/matrixVariable/map/map;name=LMW;age=18
	 * 不能使用Map接收，因为返回的类型实际上是 List<String>，导致参数转换错误
	 *
	 * @param map 矩阵变量映射
	 * @return 解析结果
	 */
	@GetMapping("/matrixVariable/map/{map}")
	public ResponseEntity<String> matrixVariableMap(@PathVariable String map, @MatrixVariable(name = "name", pathVar = "map") Map<String, Object> names) {
		return ResponseEntity.ok(names.toString());
	}

	/**
	 * 使用@MatrixVariable 修饰 String 示例
	 * 访问 /argument/matrixVariable/string/map;name=LMW;age=18
	 * 可以接收到结果
	 *
	 * @param name 名称
	 * @param age  年龄
	 * @return 解析结果
	 */
	@GetMapping("/matrixVariable/string/{pathName}")
	public ResponseEntity<String> matrixVariableMap(@PathVariable String pathName,
													@MatrixVariable String name,
													@MatrixVariable String age) {
		return ResponseEntity.ok(pathName + " " + name + " " + age);
	}

	/**
	 * 多个矩阵变量，指定变量归属的路径段
	 * 访问路径：/argument/matrixVariable/multi/firstname;name=lmw/age;age=18
	 * 可以接收到参数
	 *
	 * @param name 名称
	 * @param age  年龄
	 * @return 解析结果
	 */
	@GetMapping("/matrixVariable/multi/{type}/{pathName}")
	public ResponseEntity<String> multiMatrixVariable(@MatrixVariable(name = "name", pathVar = "type") String name,
													  @MatrixVariable(name = "age", pathVar = "pathName") String age) {
		return ResponseEntity.ok(name + " " + age);
	}

}
