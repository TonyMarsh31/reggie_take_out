package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/common")
public class FileController {
    @Value("${project.pic-storage-path}")
    private String UPLOAD_PATH;

    private String randomFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = Objects.requireNonNull(originalFilename).substring(originalFilename.lastIndexOf("."));
        return UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    @RequestMapping("/upload")
    public R<String> upload(MultipartFile file) {
        File targetDir = new File(UPLOAD_PATH);
        if (!targetDir.exists()) targetDir.mkdirs();
        String newFileName = randomFileName(file);
        try {
            file.transferTo(new File(UPLOAD_PATH + newFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return R.success(newFileName);
    }

    /**
     * 文件下载
     *
     * @param name     文件名
     * @param response HttpServlet响应
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response) {
        //  先使用输入流读取对应文件到内存，然后使用输出流写入到response中返回给客户端浏览器
        try {
            FileInputStream fileInputStream = new FileInputStream(UPLOAD_PATH + name);
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("image/jpeg");
            // todo: 以下copy流的操作可以使用common io工具类优化
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                outputStream.flush();
            }
            outputStream.close();
            fileInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
