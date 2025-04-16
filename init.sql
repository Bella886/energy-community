CREATE DATABASE energy_db;

CREATE TABLE energy_usage (
                              id SERIAL PRIMARY KEY,
                              hour TIMESTAMP NOT NULL,
                              community_produced DOUBLE PRECISION NOT NULL,
                              community_used DOUBLE PRECISION NOT NULL,
                              grid_used DOUBLE PRECISION NOT NULL
);

INSERT INTO energy_usage (hour, community_produced, community_used, grid_used)
VALUES
    (NOW(), 120.5, 85.3, 35.2),
    (NOW() + INTERVAL '1 hour', 125.7, 90.1, 30.6);
