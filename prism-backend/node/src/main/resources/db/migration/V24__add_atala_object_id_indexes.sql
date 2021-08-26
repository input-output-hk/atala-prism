CREATE INDEX atala_objects_atala_object_status_index ON atala_objects USING BTREE (atala_object_status);

CREATE INDEX atala_object_tx_submissions_atala_object_id_index ON atala_object_tx_submissions USING HASH (atala_object_id);
CREATE INDEX atala_object_txs_atala_object_id_index ON atala_object_txs USING HASH (atala_object_id);
