-- ATA-4656: We deleted the code related to the bitcoin network.
-- As a consequence, we do not need the "blocks" table anymore

DROP TABLE blocks;