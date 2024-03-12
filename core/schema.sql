CREATE TABLE `bots_states` (
  `id`           varchar(255) NOT NULL,
  `value`        varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE invitations(
    `tg_username`     varchar(100),
    `role`            TINYINT,
    `valid_until`     TIMESTAMP,
    PRIMARY KEY       (`tg_username`, `role`)
);

CREATE TABLE users(
    `id`            INT NOT NULL AUTO_INCREMENT,
    `tg_user_id`    INT,
    `role`          TINYINT,
    PRIMARY KEY (id),
    CONSTRAINT Role UNIQUE (`tg_user_id`, `role`)
);

CREATE TABLE experts(
    `user_id`         INT NOT NULL,
    `name`            VARCHAR(100),
    `description`     TEXT,
    `status`          TINYINT,
    PRIMARY KEY (`user_id`),
    CONSTRAINT FK_UserId FOREIGN KEY (`user_id`) REFERENCES users(`id`)
);
