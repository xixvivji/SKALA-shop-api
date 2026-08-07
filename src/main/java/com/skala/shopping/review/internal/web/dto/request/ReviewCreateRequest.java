package com.skala.shopping.review.internal.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class ReviewCreateRequest {

    @Min(1)
    @Max(5)
    private int rating;

    @Size(max = 2000)
    private String comment;

    public ReviewCreateRequest() {
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
