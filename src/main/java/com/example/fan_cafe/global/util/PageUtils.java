package com.example.fan_cafe.global.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
}
