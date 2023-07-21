-- Create function to notify on table events
CREATE OR REPLACE FUNCTION notify_event() RETURNS TRIGGER AS $$
    DECLARE
        channel VARCHAR;
        row JSON;
        notification JSON;
    BEGIN
        channel = TG_ARGV[0];

        IF (TG_OP = 'DELETE') THEN
            -- Use the deleted row
            row = row_to_json(OLD);
        ELSE
            -- Use the inserted or updated row
            row = row_to_json(NEW);
        END IF;

        -- Construct the notification as a JSON string
        notification = json_build_object('operation', TG_OP, 'row', row);

        -- Execute pg_notify(channel, notification)
        PERFORM pg_notify(channel, notification::text);

        -- Result is ignored since this is an AFTER trigger
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

-- Add trigger to notify when a new message is inserted
DROP TRIGGER IF EXISTS messages_notify_event ON messages;
CREATE TRIGGER messages_notify_event
    AFTER INSERT ON messages
    FOR EACH ROW EXECUTE PROCEDURE notify_event('new_messages');
