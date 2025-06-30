package com.example.fan_cafe.global.s3;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.S3ErrorCode;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Template s3Template;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file, String dir) {
        String filename = createFileName(Objects.requireNonNull(file.getOriginalFilename()), dir);

        try {
            S3Resource resource = s3Template.upload(
                    bucket,
                    filename,
                    file.getInputStream()
            );
            return resource.getURL().toString();
        } catch (IOException e) {
            throw new CustomException(S3ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void delete(String fileKey) {
        try {
            s3Template.deleteObject(bucket, fileKey);
        } catch (Exception e) {
            throw new CustomException(S3ErrorCode.FILE_DELETE_FAILED);
        }
    }

    private String createFileName(String originalName, String dir) {
        String ext = originalName.substring(originalName.lastIndexOf("."));
        return dir + "/" + UUID.randomUUID() + ext;
    }

//    private String getUrl(String filename) {
//        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + filename;
//    }
}
