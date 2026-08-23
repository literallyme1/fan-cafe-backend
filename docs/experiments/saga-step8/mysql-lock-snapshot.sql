-- Set these in the same mysql session before SOURCE-ing this file:
-- SET @run_id = 'concurrency-1-run-1';
-- SET @snapshot_phase = 'BEFORE'; -- or 'AFTER'

CREATE TABLE IF NOT EXISTS saga_experiment_snapshot (
    run_id VARCHAR(100) NOT NULL,
    snapshot_phase VARCHAR(10) NOT NULL,
    captured_at DATETIME(6) NOT NULL,
    innodb_row_lock_waits BIGINT UNSIGNED NOT NULL,
    innodb_row_lock_time_ms BIGINT UNSIGNED NOT NULL,
    completed_saga_count BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (run_id, snapshot_phase)
);

DELETE FROM saga_experiment_snapshot
WHERE run_id = @run_id AND snapshot_phase = @snapshot_phase;

INSERT INTO saga_experiment_snapshot (
    run_id, snapshot_phase, captured_at,
    innodb_row_lock_waits, innodb_row_lock_time_ms, completed_saga_count
)
SELECT
    @run_id,
    @snapshot_phase,
    NOW(6),
    COALESCE(MAX(CASE WHEN VARIABLE_NAME = 'INNODB_ROW_LOCK_WAITS'
        THEN CAST(VARIABLE_VALUE AS UNSIGNED) END), 0),
    COALESCE(MAX(CASE WHEN VARIABLE_NAME = 'INNODB_ROW_LOCK_TIME'
        THEN CAST(VARIABLE_VALUE AS UNSIGNED) END), 0),
    (SELECT COUNT(*)
     FROM saga_instance
     WHERE order_id BETWEEN 8100001 AND 8104000
       AND status = 'COMPLETED')
FROM performance_schema.global_status
WHERE VARIABLE_NAME IN ('INNODB_ROW_LOCK_WAITS', 'INNODB_ROW_LOCK_TIME');

SELECT *
FROM saga_experiment_snapshot
WHERE run_id = @run_id AND snapshot_phase = @snapshot_phase;
