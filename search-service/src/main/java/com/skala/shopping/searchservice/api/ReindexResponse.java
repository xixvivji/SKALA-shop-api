package com.skala.shopping.searchservice.api;

public final class ReindexResponse {

    private final long indexed;

    public ReindexResponse(long indexed) {
        this.indexed = indexed;
    }

    public long getIndexed() {
        return indexed;
    }
}
