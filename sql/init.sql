CREATE TABLE `rooms` (
                         `id`	VARCHAR(36)	NOT NULL,
                         `host_id`	VARCHAR(36)	NOT NULL,
                         `category_id`	VARCHAR(36)	NULL,
                         `region_id`	VARCHAR(36)	NULL,
                         `name`	VARCHAR(255)	NOT NULL,
                         `short_description`	VARCHAR(255)	NULL,
                         `description`	TEXT	NULL,
                         `max_guests`	INT	NOT NULL,
                         `max_adults`	INT	NULL,
                         `max_children`	INT	NULL,
                         `max_infants`	INT	NULL,
                         `max_pets`	INT	NULL,
                         `price_per_night`	DECIMAL(10, 2)	NOT NULL,
                         `weekend_price_per_night`	DECIMAL(10, 2)	NULL,
                         `country`	VARCHAR(127)	NULL,
                         `city`	VARCHAR(127)	NULL,
                         `state`	VARCHAR(127)	NULL,
                         `latitude`	DECIMAL(10, 7)	NULL,
                         `longitude`	DECIMAL(10, 7)	NULL,
                         `thumbnail_url`	TEXT	NULL,
                         `created_at`	DATETIME	NOT NULL
);

CREATE TABLE `reservations` (
                                `id`	VARCHAR(36)	NOT NULL,
                                `room_id`	VARCHAR(36)	NOT NULL,
                                `user_id`	VARCHAR(36)	NOT NULL,
                                `check_in`	DATE	NOT NULL,
                                `check_out`	DATE	NOT NULL,
                                `guest_count`	INT	NOT NULL,
                                `total_price`	DECIMAL(10, 2)	NOT NULL,
                                `price_snapshot`	JSON	NULL,
                                `status`	ENUM('PENDING', 'CONFIRMED', 'CANCELLED')	NULL,
                                `created_at`	DATETIME	NOT NULL
);

CREATE TABLE `reviews` (
                           `id`	VARCHAR(36)	NOT NULL,
                           `score`	INT	NOT NULL,
                           `content`	TEXT	NULL,
                           `created_at`	DATETIME	NOT NULL,
                           `updated_at`	DATETIME	NULL,
                           `reservation_id`	VARCHAR(36)	NOT NULL
);

CREATE TABLE `hosts` (
                         `id`	VARCHAR(36)	NOT NULL,
                         `user_id`	VARCHAR(36)	NOT NULL,
                         `name`	VARCHAR(255)	NOT NULL,
                         `description`	TEXT	NULL,
                         `thumbnail_url`	TEXT	NULL,
                         `started_at`	DATETIME	NULL,
                         `host_type`	ENUM('SUPERHOST', 'NORMAL')	NULL
);

CREATE TABLE `categories` (
                              `id`	VARCHAR(36)	NOT NULL,
                              `name`	VARCHAR(127)	NOT NULL,
                              `icon_url`	VARCHAR(255)	NULL
);

CREATE TABLE `room_prices` (
                               `id`	VARCHAR(36)	NOT NULL,
                               `room_id`	VARCHAR(36)	NOT NULL,
                               `date`	DATE	NOT NULL,
                               `price`	DECIMAL(10, 2)	NOT NULL
);

CREATE TABLE `room_discounts` (
                                  `id`	VARCHAR(36)	NOT NULL,
                                  `room_id`	VARCHAR(36)	NOT NULL,
                                  `min_nights`	INT	NOT NULL,
                                  `discount_rate`	DECIMAL(5, 2)	NOT NULL
);

CREATE TABLE `regions` (
                           `id`	VARCHAR(36)	NOT NULL,
                           `name`	VARCHAR(127)	NOT NULL,
                           `lat_min`	DECIMAL(10, 7)	NOT NULL,
                           `lat_max`	DECIMAL(10, 7)	NOT NULL,
                           `lng_min`	DECIMAL(10, 7)	NOT NULL,
                           `lng_max`	DECIMAL(10, 7)	NOT NULL
);

CREATE TABLE `room_images` (
                               `id`	VARCHAR(36)	NOT NULL,
                               `room_id`	VARCHAR(36)	NOT NULL,
                               `url`	TEXT	NOT NULL,
                               `caption`	VARCHAR(255)	NULL,
                               `order_num`	INT	NULL
);

CREATE TABLE `users` (
                         `id`	VARCHAR(36)	NOT NULL,
                         `login_id`	VARCHAR(255)	NOT NULL,
                         `password`	VARCHAR(255)	NOT NULL,
                         `name`	VARCHAR(255)	NOT NULL,
                         `user_type`	ENUM('HOST', 'GUEST')	NULL,
                         `created_at`	DATETIME	NOT NULL
);

CREATE TABLE `review_images` (
                                 `id`	VARCHAR(36)	NOT NULL,
                                 `review_id`	VARCHAR(36)	NOT NULL,
                                 `url`	TEXT	NOT NULL,
                                 `caption`	VARCHAR(255)	NULL,
                                 `order_num`	INT	NULL
);

ALTER TABLE `rooms` ADD CONSTRAINT `PK_ROOMS` PRIMARY KEY (
                                                           `id`
    );

ALTER TABLE `reservations` ADD CONSTRAINT `PK_RESERVATIONS` PRIMARY KEY (
                                                                         `id`
    );

ALTER TABLE `reviews` ADD CONSTRAINT `PK_REVIEWS` PRIMARY KEY (
                                                               `id`
    );

ALTER TABLE `hosts` ADD CONSTRAINT `PK_HOSTS` PRIMARY KEY (
                                                           `id`
    );

ALTER TABLE `categories` ADD CONSTRAINT `PK_CATEGORIES` PRIMARY KEY (
                                                                     `id`
    );

ALTER TABLE `room_prices` ADD CONSTRAINT `PK_ROOM_PRICES` PRIMARY KEY (
                                                                       `id`
    );

ALTER TABLE `room_discounts` ADD CONSTRAINT `PK_ROOM_DISCOUNTS` PRIMARY KEY (
                                                                             `id`
    );

ALTER TABLE `regions` ADD CONSTRAINT `PK_REGIONS` PRIMARY KEY (
                                                               `id`
    );

ALTER TABLE `room_images` ADD CONSTRAINT `PK_ROOM_IMAGES` PRIMARY KEY (
                                                                       `id`
    );

ALTER TABLE `users` ADD CONSTRAINT `PK_USERS` PRIMARY KEY (
                                                           `id`
    );

ALTER TABLE `review_images` ADD CONSTRAINT `PK_REVIEW_IMAGES` PRIMARY KEY (
                                                                           `id`
    );

