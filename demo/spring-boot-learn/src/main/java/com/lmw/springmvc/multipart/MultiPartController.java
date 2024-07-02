package com.lmw.springmvc.multipart;

import com.lmw.springmvc.entity.FileRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;

/**
 * 文件上传
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-01 22:52
 */
@Controller
public class MultiPartController {
	/**
	 * 上传文件保存的相对路径
	 */
	private static final String UPLOAD_DIR = "uploads/";

	/**
	 * 上传单个文件，使用MultipartFile实现
	 *
	 * @param file 文件参数
	 * @return 上传结果
	 */
	@ResponseBody
	@PostMapping("upload1")
	public String upload(@RequestParam MultipartFile file) {
		try {
			// 获取项目的相对路径
			String uploadPath = new File(UPLOAD_DIR).getAbsolutePath();

			// 创建目标文件
			File destFile = new File(uploadPath, file.getOriginalFilename());
			File parentFile = destFile.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			file.transferTo(destFile);
		} catch (IOException e) {
			throw new RuntimeException("上传文件失败，异常消息为：{}", e);
		}
		return "上传成功！";
	}

	/**
	 * 使用MultipartRequest，这种方式支持单个或多个文件上传
	 *
	 * @param files 文件上传请求
	 * @return 上传结果
	 */
	@ResponseBody
	@PostMapping("upload2")
	public String upload2(MultipartRequest files) {
		// 获得文件：
		MultipartFile file = files.getFile("file");
		// 获得文件名：
		String filename = file.getOriginalFilename();
		try {
			// 获取项目的相对路径
			String uploadPath = new File(UPLOAD_DIR).getAbsolutePath();

			// 创建目标文件
			File destFile = new File(uploadPath, filename);
			File parentFile = destFile.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			file.transferTo(destFile);
		} catch (IOException e) {
			throw new RuntimeException("上传文件失败，异常消息为：{}", e);
		}
		return "上传成功！";
	}

	/**
	 * 嵌套MultipartFile的实体类
	 *
	 * @param fileRequest 文件请求
	 * @return 上传结果
	 */
	@ResponseBody
	@PostMapping("upload3")
	public String upload3(FileRequest fileRequest) {
		// 获得文件：
		MultipartFile file = fileRequest.getFile();
		// 获得文件名：
		String filename = file.getOriginalFilename();
		try {
			// 获取项目的相对路径
			String uploadPath = new File(UPLOAD_DIR).getAbsolutePath();

			// 创建目标文件
			File destFile = new File(uploadPath, filename);
			File parentFile = destFile.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			file.transferTo(destFile);
		} catch (IOException e) {
			throw new RuntimeException("上传文件失败，异常消息为：{}", e);
		}
		return "上传成功！";
	}

	/**
	 * 批量上传文件
	 *
	 * @param files 多个文件列表
	 * @return 上传结果
	 */
	@ResponseBody
	@PostMapping("upload4")
	public String upload4(MultipartFile[] files) {
		for (MultipartFile file : files) {
			// 获得文件名：
			String filename = file.getOriginalFilename();
			try {
				// 获取项目的相对路径
				String uploadPath = new File(UPLOAD_DIR).getAbsolutePath();
				// 创建目标文件
				File destFile = new File(uploadPath, filename);
				File parentFile = destFile.getParentFile();
				if (!parentFile.exists()) {
					parentFile.mkdirs();
				}
				file.transferTo(destFile);
			} catch (IOException e) {
				throw new RuntimeException("上传文件失败，异常消息为：{}", e);
			}
		}
		return "上传成功！";
	}

	/**
	 * 使用Part实现单个文件上传
	 *
	 * @param file 上传的文件
	 * @return 上传结果
	 */
	@ResponseBody
	@PostMapping("upload5")
	public String upload5(Part file) {


		// 获取项目的相对路径
		String uploadPath = new File(UPLOAD_DIR).getAbsolutePath();

		// 创建目标文件
		File destFile = new File(uploadPath, file.getSubmittedFileName());
		File parentFile = destFile.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		try {
			file.write(destFile.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException("上传文件失败，异常消息为：{}", e);
		}

		return "上传成功！";
	}


	/**
	 * 上传单个文件，使用 CommonsMultipartFile 实现
	 * 需要引入 commons-fileupload 依赖，并且将 CommonsMultipartResolver 作为文件解析器
	 *
	 * @param file 文件参数
	 * @return 上传结果
	 * @see com.lmw.springmvc.config.FileUploadConfig
	 */
	@ResponseBody
	@PostMapping("upload6")
	public String upload6(@RequestParam CommonsMultipartFile file) {
		try {
			// 获取项目的相对路径
			String uploadPath = new File(UPLOAD_DIR).getAbsolutePath();

			// 创建目标文件
			File destFile = new File(uploadPath, file.getOriginalFilename());
			File parentFile = destFile.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			file.transferTo(destFile);
		} catch (IOException e) {
			throw new RuntimeException("上传文件失败，异常消息为：{}", e);
		}
		return "上传成功！";
	}
}
