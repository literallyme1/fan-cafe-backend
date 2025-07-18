package com.example.fan_cafe.global.util;

import org.springframework.data.domain.*;

import java.util.List;

public class PageUtils {

    public static Pageable createPageRequest(int size) {
        return PageRequest.of(0, size);
    }

    public static Pageable createPageRequest(int page, int size) {
        return PageRequest.of(page, size);
    }


    public static Pageable createPageRequest(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

    public static <T> Slice<T> toSlice(List<T> results, Pageable pageable) {
        boolean hasNext = false;
        if (results.size() > pageable.getPageSize()) {
            hasNext = true;
            results.remove(pageable.getPageSize());
        }
        return new SliceImpl<>(results, pageable, hasNext);
    }
}
