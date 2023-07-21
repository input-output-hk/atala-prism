-- Fix the conventional "created_at" column
ALTER TABLE draft_credentials
    RENAME created_on TO created_at;

ALTER INDEX draft_credentials_created_on_index RENAME TO draft_credentials_created_at_index;