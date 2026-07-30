package com.avionics_systems.comment.service;

import com.avionics_systems.comment.dto.CommentResponse;
import com.avionics_systems.comment.dto.UpdateCommentRequest;
import com.avionics_systems.comment.entity.Comment;
import com.avionics_systems.comment.exception.OptimisticLockException;
import com.avionics_systems.comment.exception.ResourceNotFoundException;
import com.avionics_systems.comment.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceOptimisticLockTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    private UUID commentId;
    private UUID userId;
    private UUID issueId;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        commentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        issueId = UUID.randomUUID();
        testComment = Comment.builder()
                .id(commentId)
                .issueId(issueId)
                .userId(userId)
                .content("Original comment content")
                .deleted(false)
                .version(0L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Optimistic Locking Tests")
    class OptimisticLockingTests {

        @Test
        @DisplayName("Should update comment when version matches")
        void updateComment_withMatchingVersion_shouldSucceed() {
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated comment content")
                    .version(0L)
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            CommentResponse response = commentService.updateComment(commentId, request, userId);

            assertThat(response.getContent()).isEqualTo("Updated comment content");
            assertThat(response.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw OptimisticLockException when version mismatch")
        void updateComment_withMismatchedVersion_shouldThrowException() {
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated comment content")
                    .version(5L) // Stale version, current is 0
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.updateComment(commentId, request, userId))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("was modified by another user")
                    .hasMessageContaining("Expected version: 0")
                    .hasMessageContaining("provided: 5");
        }

        @Test
        @DisplayName("Should succeed when version is null (skip check)")
        void updateComment_withNullVersion_shouldSucceed() {
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated comment content")
                    .version(null) // No version check
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            CommentResponse response = commentService.updateComment(commentId, request, userId);

            assertThat(response.getContent()).isEqualTo("Updated comment content");
        }

        @Test
        @DisplayName("Should handle concurrent update scenario")
        void concurrentUpdateScenario_staleUserAUpdate_shouldFail() {
            // User A loads comment at version 0
            Comment userALoadedComment = Comment.builder()
                    .id(commentId)
                    .issueId(issueId)
                    .userId(userId)
                    .content("Original")
                    .version(0L)
                    .deleted(false)
                    .build();

            // User B updates comment (version 0 -> 1)
            Comment userBUpdatedComment = Comment.builder()
                    .id(commentId)
                    .issueId(issueId)
                    .userId(userId)
                    .content("User B's update")
                    .version(1L)
                    .deleted(false)
                    .build();

            // User A tries to update with stale version 0
            UpdateCommentRequest userARequest = UpdateCommentRequest.builder()
                    .content("User A's update")
                    .version(0L) // Stale
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(userBUpdatedComment));

            assertThatThrownBy(() -> commentService.updateComment(commentId, userARequest, userId))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("Expected version: 1, provided: 0");
        }

        @Test
        @DisplayName("Should return comment with version in response")
        void updateComment_shouldIncludeNewVersionInResponse() {
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated content")
                    .version(0L)
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setVersion(c.getVersion() + 1);
                return c;
            });

            CommentResponse response = commentService.updateComment(commentId, request, userId);

            assertThat(response.getVersion()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should throw exception when user tries to update another user's comment")
        void updateComment_differentUser_shouldThrow() {
            UUID differentUserId = UUID.randomUUID();
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated content")
                    .version(0L)
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.updateComment(commentId, request, differentUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not authorized");
        }
    }

    @Nested
    @DisplayName("Resource Not Found Tests")
    class ResourceNotFoundTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when comment not found")
        void updateComment_notFound_shouldThrow() {
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated content")
                    .version(0L)
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.updateComment(commentId, request, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when comment is deleted")
        void updateComment_deleted_shouldThrow() {
            testComment.setDeleted(true);
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated content")
                    .version(0L)
                    .build();

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.updateComment(commentId, request, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("deleted");
        }
    }

    @Nested
    @DisplayName("Response Version Tests")
    class ResponseVersionTests {

        @Test
        @DisplayName("Should include version in response for tree structure")
        void getCommentsByIssueId_shouldIncludeVersion() {
            when(commentRepository.findByIssueIdAndDeletedFalse(issueId)).thenReturn(java.util.List.of(testComment));

            var responses = commentService.getCommentsByIssueId(issueId);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getVersion()).isEqualTo(0L);
        }
    }
}