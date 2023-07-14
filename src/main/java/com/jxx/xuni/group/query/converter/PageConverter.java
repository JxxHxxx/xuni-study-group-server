package com.jxx.xuni.group.query.converter;

import com.jxx.xuni.group.dto.response.PageInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PageConverter<T> {

    public PageInfo toPageInfo(Pageable pageable, long totalElements, int totalPage) {
        int pageNumber = createAdjustedPageNumber(pageable);
        boolean last = pageNumber == totalPage ? true : false;
        return PageInfo.of(pageable.getOffset(), pageable.getPageSize(), pageNumber, totalElements, totalPage, last);
    }

    public PageInfo toPageInto(Page<T> page) {

        int totalPages = page.getTotalPages();
        long totalElements = page.getTotalElements();

        Pageable pageable = page.getPageable();
        int pageNumber = createAdjustedPageNumber(pageable);

        boolean last = pageNumber == totalPages ? true : false;
        return PageInfo.of(pageable.getOffset(), pageable.getPageSize(), pageNumber, totalElements, totalPages, last);
    }

    /**
     * pageable 인터페이스 PageNumber는 0부터 시작한다. 이를 1로 시작하도록 조정한다.
     */
    private int createAdjustedPageNumber(Pageable pageable) {
        return pageable.getPageNumber() + 1;
    }
}
