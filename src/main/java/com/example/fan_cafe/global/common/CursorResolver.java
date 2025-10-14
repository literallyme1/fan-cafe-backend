package com.example.fan_cafe.global.common;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;


public class CursorResolver {

    public static Cursor resolve(Long cursorId, LocalDateTime cursorCreatedAt, Supplier<Optional<? extends HasCreatedAt>> latestSupplier) {
        if (isProvided(cursorId, cursorCreatedAt)) {
            return new Cursor(cursorId, cursorCreatedAt);
        }
        return resolveFromLatest(latestSupplier);
    }

    private static boolean isProvided(Long cursorId, LocalDateTime cursorCreatedAt) {
        return cursorId != null && cursorCreatedAt != null;
    }

    private static Cursor resolveFromLatest(Supplier<Optional<? extends HasCreatedAt>> latestSupplier) {
        return latestSupplier.get()
                .map(entity -> new Cursor(entity.getId() + 1, entity.getCreatedAt().plusNanos(1)))
                .orElseGet(() -> new Cursor(Long.MAX_VALUE, LocalDateTime.now().plusNanos(1)));
    }

}
