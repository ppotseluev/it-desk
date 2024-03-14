CREATE TABLE bots_states (
  id           varchar(255) NOT NULL,
  value        varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE invitations(
    tg_username     varchar(100)      NOT NULL,
    role            SMALLINT          NOT NULL,
    valid_until     TIMESTAMP         NOT NULL,
    PRIMARY KEY     (tg_username, role)
);

CREATE TABLE users(
    id            BIGSERIAL      NOT NULL,
    tg_user_id    BIGINT         NOT NULL,
    role          SMALLINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT Role UNIQUE (tg_user_id, role)
);

CREATE TABLE experts(
    user_id                           INT NOT NULL,
    name            VARCHAR(100)      DEFAULT NULL,
    description     TEXT              DEFAULT NULL,
    status          SMALLINT          NOT NULL,
    photo           BYTEA             DEFAULT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT FK_UserId FOREIGN KEY (user_id) REFERENCES users(id)
);
