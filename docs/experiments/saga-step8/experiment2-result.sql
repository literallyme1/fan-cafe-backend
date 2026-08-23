-- Set @run_id before SOURCE-ing this file.
SELECT
    before_snapshot.run_id,
    before_snapshot.captured_at AS started_at,
    after_snapshot.captured_at AS finished_at,
    after_snapshot.completed_saga_count - before_snapshot.completed_saga_count
        AS processed_saga_count,
    ROUND(TIMESTAMPDIFF(MICROSECOND,
        before_snapshot.captured_at, after_snapshot.captured_at) / 1000000.0, 6)
        AS elapsed_seconds,
    ROUND(
        (after_snapshot.completed_saga_count - before_snapshot.completed_saga_count)
        / NULLIF(TIMESTAMPDIFF(MICROSECOND,
            before_snapshot.captured_at, after_snapshot.captured_at) / 1000000.0, 0),
        3
    ) AS sagas_per_second,
    after_snapshot.innodb_row_lock_waits - before_snapshot.innodb_row_lock_waits
        AS innodb_row_lock_waits_delta,
    after_snapshot.innodb_row_lock_time_ms - before_snapshot.innodb_row_lock_time_ms
        AS innodb_row_lock_time_ms_delta,
    ROUND(
        (after_snapshot.innodb_row_lock_time_ms - before_snapshot.innodb_row_lock_time_ms)
        / NULLIF(after_snapshot.innodb_row_lock_waits - before_snapshot.innodb_row_lock_waits, 0),
        3
    ) AS average_row_lock_wait_ms
FROM saga_experiment_snapshot before_snapshot
JOIN saga_experiment_snapshot after_snapshot
  ON after_snapshot.run_id = before_snapshot.run_id
 AND after_snapshot.snapshot_phase = 'AFTER'
WHERE before_snapshot.run_id = @run_id
  AND before_snapshot.snapshot_phase = 'BEFORE';

SELECT status, COUNT(*) AS saga_count
FROM saga_instance
WHERE order_id BETWEEN 8100001 AND 8104000
GROUP BY status
ORDER BY status;
