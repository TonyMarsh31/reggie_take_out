package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
}
