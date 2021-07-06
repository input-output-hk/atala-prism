-- Notify on new messages inserted
CREATE OR REPLACE FUNCTION notify_new_message() RETURNS TRIGGER AS $$
    BEGIN
        PERFORM pg_notify('new_messages', NEW.id::text);

        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

-- Add trigger to notify when a new message is inserted
DROP TRIGGER IF EXISTS messages_notify_event ON messages;
CREATE TRIGGER messages_notify_event
    AFTER INSERT ON messages
    FOR EACH ROW EXECUTE PROCEDURE notify_new_message();
