CREATE TABLE it_desk_bots_states (
  id           varchar(255) NOT NULL,
  value        varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE it_desk_invitations(
    tg_username     varchar(100)      NOT NULL,
    role            SMALLINT          NOT NULL,
    valid_until     TIMESTAMP         NOT NULL,
    PRIMARY KEY     (tg_username, role)
);

CREATE TABLE it_desk_users(
    id            BIGSERIAL      NOT NULL,
    tg_user_id    BIGINT         NOT NULL,
    role          SMALLINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT Role UNIQUE (tg_user_id, role)
);

CREATE TABLE it_desk_experts(
    user_id                           INT NOT NULL,
    name            VARCHAR(100)      DEFAULT NULL,
    description     TEXT              DEFAULT NULL,
    status          SMALLINT          NOT NULL,
    photo           BYTEA             DEFAULT NULL,
    skills          INTEGER[]        DEFAULT '{}'::INTEGER[],
    PRIMARY KEY (user_id),
    CONSTRAINT FK_UserId FOREIGN KEY (user_id) REFERENCES it_desk_users(id)
);
