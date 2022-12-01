CREATE TABLE service_endpoints
(
    service_endpoint_id     ID_TYPE PRIMARY KEY      NOT NULL,
    service_id              ID_TYPE                  NOT NULL,
    url                     TEXT                     NOT NULL,


    CONSTRAINT service_endpoints_service_id_fk
        FOREIGN KEY (service_id) REFERENCES services (service_id)

);
