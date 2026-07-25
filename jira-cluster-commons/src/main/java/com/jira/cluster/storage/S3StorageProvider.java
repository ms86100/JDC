package com.jira.cluster.storage;

import com.jira.cluster.config.ClusterProperties;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Slf4j
public class S3StorageProvider implements StorageProvider {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3StorageProvider(ClusterProperties properties) {
        ClusterProperties.StorageConfig.S3Config s3Config = properties.getStorage().getS3();
        this.bucket = s3Config.getBucket();

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                s3Config.getAccessKey(), s3Config.getSecretKey());

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(s3Config.getEndpoint()))
                .region(Region.of(s3Config.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3Config.isPathStyleAccess())
                        .build())
                .forcePathStyle(s3Config.isPathStyleAccess())
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(s3Config.getEndpoint()))
                .region(Region.of(s3Config.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3Config.isPathStyleAccess())
                        .build())
                .build();

        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created S3 bucket: {}", bucket);
        }
    }

    @Override
    public void store(String path, InputStream data, long size) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(path)
                        .contentLength(size)
                        .build(),
                RequestBody.fromInputStream(data, size));
        log.debug("Stored object s3://{}/{}", bucket, path);
    }

    @Override
    public InputStream retrieve(String path) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(path)
                        .build());
    }

    @Override
    public void delete(String path) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(path)
                        .build());
    }

    @Override
    public boolean exists(String path) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public String getUrl(String path, Duration validity) {
        return presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(validity)
                        .getObjectRequest(r -> r.bucket(bucket).key(path))
                        .build())
                .url()
                .toString();
    }
}
