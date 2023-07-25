-- Speed up queries on atala_object_tx_submissions
CREATE INDEX atala_object_tx_submissions_latest_index ON atala_object_tx_submissions USING BTREE (atala_object_id, submission_timestamp);
CREATE INDEX atala_object_tx_submissions_filter_index ON atala_object_tx_submissions USING BTREE (submission_timestamp, status, ledger);
