package com.zhli.baymd.rag.service.impl;

import cn.hutool.core.lang.Assert;
import com.zhli.baymd.rag.dto.StoredFileDTO;
import com.zhli.baymd.rag.service.FileStorageService;
import com.zhli.baymd.rag.util.FileTypeDetector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件存储实现
 * 开发/测试阶段使用，文件存储在本地磁盘，避免对 S3 服务的依赖
 */
@Slf4j
@Service
@Primary
public class LocalFileStorageService implements FileStorageService {

    private final Path baseDir;
    private static final Tika TIKA = new Tika();

    public LocalFileStorageService(@Value("${storage.local.dir:./data/baymd-files}") String baseDirPath) {
        this.baseDir = Paths.get(baseDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
            log.info("本地文件存储目录: {}", this.baseDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建本地存储目录: " + this.baseDir, e);
        }
    }

    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, MultipartFile file) {
        Assert.isFalse(file == null || file.isEmpty(), "上传文件不能为空");
        String originalFilename = file.getOriginalFilename();
        long size = file.getSize();
        String detectedContentType = TIKA.detect(file.getInputStream(), originalFilename);
        try (InputStream is = file.getInputStream()) {
            return writeToLocal(bucketName, is, size, originalFilename, detectedContentType);
        }
    }

    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, InputStream content, long size, String originalFilename, String contentType) {
        Assert.notNull(content, "上传内容不能为空");
        String detected = resolveContentType(originalFilename, contentType);
        return writeToLocal(bucketName, content, size, originalFilename, detected);
    }

    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType) {
        Assert.notNull(content, "上传内容不能为空");
        String detected = resolveContentType(originalFilename, contentType);
        return writeToLocal(bucketName, new ByteArrayInputStream(content), content.length, originalFilename, detected);
    }

    @Override
    @SneakyThrows
    public StoredFileDTO reliableUpload(String bucketName, InputStream content, long size, String originalFilename, String contentType) {
        return upload(bucketName, content, size, originalFilename, contentType);
    }

    @Override
    public InputStream openStream(String url) {
        // url 格式: local://bucketName/fileName.ext
        String path = url.replaceFirst("^local://", "");
        try {
            return new FileInputStream(baseDir.resolve(path).toFile());
        } catch (IOException e) {
            throw new RuntimeException("无法读取本地文件: " + url, e);
        }
    }

    @Override
    public void deleteByUrl(String url) {
        String path = url.replaceFirst("^local://", "");
        try {
            Files.deleteIfExists(baseDir.resolve(path));
        } catch (IOException e) {
            log.warn("删除本地文件失败: {}", url, e);
        }
    }

    private StoredFileDTO writeToLocal(String bucketName, InputStream inputStream, long size,
                                        String originalFilename, String contentType) throws IOException {
        String fileName = generateFileName(originalFilename);
        Path bucketDir = baseDir.resolve(bucketName);
        Files.createDirectories(bucketDir);
        Path targetPath = bucketDir.resolve(fileName);

        Files.copy(inputStream, targetPath);

        String url = "local://" + bucketName + "/" + fileName;
        String detectedType = FileTypeDetector.detectType(originalFilename, contentType);
        return StoredFileDTO.builder()
                .url(url)
                .detectedType(detectedType)
                .size(size)
                .originalFilename(originalFilename)
                .build();
    }

    private String generateFileName(String originalFilename) {
        String suffix = extractSuffix(originalFilename);
        UUID uuid = UUID.randomUUID();
        String key = String.format("%016x%016x", uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        return suffix.isBlank() ? key : key + "." + suffix;
    }

    private String extractSuffix(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return (idx < 0 || idx == filename.length() - 1) ? "" : filename.substring(idx + 1).trim();
    }

    private String resolveContentType(String originalFilename, String contentType) {
        if (contentType != null && !contentType.isBlank()) return contentType;
        if (originalFilename != null && !originalFilename.isBlank()) return TIKA.detect(originalFilename);
        return null;
    }
}
