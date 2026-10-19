-- Dashboard KPI and trend queries filter each scale by device measurement time.
CREATE INDEX IF NOT EXISTS idx_weighing_logs_scale_last_time
    ON weighing_logs (scale_id, last_time);
