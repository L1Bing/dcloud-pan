package com.bing.dcloudpan.component;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MinioFileStoreEngine implements StoreEngine {

    @Resource
    private AmazonS3Client amazonS3Client;


    @Override
    public boolean bucketExists(String bucketName) {
        return amazonS3Client.doesBucketExistV2(bucketName);
    }

    @Override
    public boolean removeBucket(String bucketName) {
        try {
            if (bucketExists(bucketName)) {
                List<S3ObjectSummary> objects = listObjects(bucketName);
                if (!objects.isEmpty()) {
                    return false;
                }
                amazonS3Client.deleteBucket(bucketName);
                return !bucketExists(bucketName);
            }
        } catch (Exception e) {
            log.error("errorMsg={}", e.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public void createBucket(String bucketName) {

        if (bucketExists(bucketName)) {
            log.info("Bucket {} already exists.", bucketName);
            return;
        }
        try {
            Bucket bucket = amazonS3Client.createBucket(bucketName);
            log.info("Bucket {} created.", bucketName);
        } catch (Exception e) {
            log.error("errorMsg={}", e.getMessage());
        }
    }

    @Override
    public List<Bucket> getAllBucket() {
        return amazonS3Client.listBuckets();
    }

    @Override
    public List<S3ObjectSummary> listObjects(String bucketName) {
        if (bucketExists(bucketName)) {
            ListObjectsV2Result result = amazonS3Client.listObjectsV2(bucketName);
            return result.getObjectSummaries();
        }
        return List.of();
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectKey) {
        return amazonS3Client.doesObjectExist(bucketName, objectKey);

    }

    @Override
    public boolean upload(String bucketName, String objectName, String localFileName) {
        try {
            File file = new File(localFileName);
            amazonS3Client.putObject(bucketName, objectName, file);
            return true;
        } catch (Exception e) {
            log.error("errorMsg={}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean upload(String bucketName, String objectKey, MultipartFile file) {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());
            amazonS3Client.putObject(bucketName, objectKey, file.getInputStream(), objectMetadata);

            return true;
        } catch (Exception e) {
            log.error("errorMsg={}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(String bucketName, String objectKey) {
        try {
            amazonS3Client.deleteObject(bucketName, objectKey);
            return true;
        } catch (Exception e) {
            log.error("errorMsg={}", e);
            return false;
        }
    }

    @Override
    public String getDownloadUrl(String bucketName, String remoteFileName, long timeout, TimeUnit unit) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + unit.toMillis(timeout));
            return amazonS3Client.generatePresignedUrl(bucketName, remoteFileName, expiration).toString();
        } catch (Exception e) {
            log.error("errorMsg {}", e);
            return null;
        }
    }

    @Override
    @SneakyThrows
    public void download2Response(String bucketName, String objectKey, HttpServletResponse response) {
        S3Object s3Object = amazonS3Client.getObject(bucketName, objectKey);
        response.setHeader("Content-Disposition", "attachment;filename=" + objectKey.substring(objectKey.lastIndexOf("/") + 1));
        response.setContentType("application/force-download");
        response.setCharacterEncoding("UTF-8");
        IOUtils.copy(s3Object.getObjectContent(), response.getOutputStream());
    }

}
