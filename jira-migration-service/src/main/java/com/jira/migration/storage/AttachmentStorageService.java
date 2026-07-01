package com.jira.migration.storage;

import com.jira.migration.dto.attachment.AttachmentMetadata;
import com.jira.migration.dto.attachment.StoredAttachment;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Interface for attachment storage operations.
 * Implementations can use local filesystem, S3, Azure Blob Storage, or other backends.
 */
public interface AttachmentStorageService {

    /**
     * Store a new attachment.
     *
     * @param issueId   The ID of the issue this attachment belongs to
     * @param fileName  The original file name
     * @param data      The file content as an input stream
     * @param size      The size of the file in bytes
     * @param mimeType  The MIME type of the file
     * @param metadata  Additional metadata to store with the file
     * @return Information about the stored attachment
     * @throws com.jira.migration.exception.StorageException if storage fails
     */
    StoredAttachment store(String issueId, String fileName, InputStream data,
                          long size, String mimeType, Map<String, String> metadata);

    /**
     * Retrieve an attachment's content.
     *
     * @param attachmentId The ID of the attachment to retrieve
     * @return An input stream containing the file content
     * @throws com.jira.migration.exception.StorageException if retrieval fails
     */
    InputStream retrieve(String attachmentId);

    /**
     * Get attachment metadata without retrieving content.
     *
     * @param attachmentId The ID of the attachment
     * @return The attachment metadata, or null if not found
     */
    AttachmentMetadata getMetadata(String attachmentId);

    /**
     * Delete an attachment.
     *
     * @param attachmentId The ID of the attachment to delete
     * @throws com.jira.migration.exception.StorageException if deletion fails
     */
    void delete(String attachmentId);

    /**
     * Check if an attachment exists.
     *
     * @param attachmentId The ID of the attachment to check
     * @return true if the attachment exists, false otherwise
     */
    boolean exists(String attachmentId);

    /**
     * Get a download URL for an attachment.
     * For local storage, this may return a path. For S3/Azure, it returns a presigned URL.
     *
     * @param attachmentId The ID of the attachment
     * @param validity    How long the URL should be valid
     * @return A URL or path that can be used to download the attachment
     * @throws com.jira.migration.exception.StorageException if URL generation fails
     */
    String getDownloadUrl(String attachmentId, Duration validity);

    /**
     * Copy an attachment to a new issue.
     *
     * @param sourceAttachmentId The ID of the attachment to copy
     * @param targetIssueId      The ID of the target issue
     * @return The ID of the newly created copy
     * @throws com.jira.migration.exception.StorageException if copy fails
     */
    String copy(String sourceAttachmentId, String targetIssueId);

    /**
     * Store multiple attachments in a batch.
     *
     * @param requests The list of attachment upload requests
     * @return List of stored attachment results
     * @throws com.jira.migration.exception.StorageException if any storage operation fails
     */
    List<StoredAttachment> storeBatch(List<AttachmentUploadRequest> requests);

    /**
     * Get the total size used by all attachments for a specific issue.
     *
     * @param issueId The ID of the issue
     * @return Total size in bytes
     */
    long getTotalSizeForIssue(String issueId);

    /**
     * Get the count of attachments for a specific issue.
     *
     * @param issueId The ID of the issue
     * @return Number of attachments
     */
    int getAttachmentCountForIssue(String issueId);
}