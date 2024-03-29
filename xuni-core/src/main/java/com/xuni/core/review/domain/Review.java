package com.xuni.core.review.domain;

import com.xuni.core.common.exception.NotPermissionException;
import com.xuni.core.common.exception.CommonExceptionMessage;
import com.xuni.core.review.domain.exception.ReviewDeletedException;
import com.xuni.core.review.domain.exception.ReviewExceptionMessage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "study_product_review")
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;
    @Embedded
    private Reviewer reviewer;
    private String studyProductId;
    @Embedded
    private Content content;
    private Integer likeTotalCnt;
    private LocalDateTime lastModifiedTime;
    private Boolean isDeleted;

    @Builder
    public Review(Reviewer reviewer, String studyProductId, Content content) {
        this.reviewer = reviewer;
        this.studyProductId = studyProductId;
        this.content = content;
        this.likeTotalCnt = 0;
        this.lastModifiedTime = LocalDateTime.now();
        this.isDeleted = false;
    }

    public void update(Long reviewerId, Integer rating, String comment) {
        // 본인 검증
        checkReviewer(reviewerId);
        this.content.update(rating, comment);
        updateLastModifiedTime();
    }

    public void delete(Long reviewerId) {
        checkReviewer(reviewerId);
        this.isDeleted = true;
    }

    private void checkReviewer(Long reviewerId) {
        if (!this.receiveReviewerId().equals(reviewerId)) {
            throw new NotPermissionException(CommonExceptionMessage.PRIVATE_ACCESSIBLE);
        }
    }

    private void updateLastModifiedTime() {
        this.lastModifiedTime = LocalDateTime.now();
    }

    public Integer receiveRating() {
        return this.content.getRating();
    }

    public String receiveComment() {
        return this.content.getComment();
    }

    public Long receiveReviewerId() {
        return this.reviewer.getId();
    }

    public Progress receiveProgress() {
        return this.reviewer.getProgress();
    }

    public String receiveReviewerName() {
        return this.reviewer.getName();
    }

    protected void onLikeEvent() {
        likeTotalCnt += 1;
    }

    protected void onLikeCancelEvent() {
        likeTotalCnt -= 1;
    }

    protected void checkReviewDeleted() {
        if (isDeleted) {
            throw new ReviewDeletedException(ReviewExceptionMessage.REVIEW_DELETED);
        }
    }
}
