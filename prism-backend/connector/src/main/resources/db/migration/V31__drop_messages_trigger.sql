-- Drop trigger as it's not usable because it cannot store messages larger than 8000 bytes
DROP TRIGGER messages_notify_event ON messages;
-- Drop the function used by the trigger above
DROP FUNCTION notify_event;
