create table channel
(
    id      uuid primary key,
    chat_id bigint not null,
    title   text   not null,
    status  text   not null
);

create unique index on channel (chat_id);

create table mirroring
(
    id             uuid primary key,
    src_channel_id uuid not null references channel (id)
);

create unique index on mirroring (src_channel_id);

create table mirroring_target
(
    channel_id uuid primary key references channel (id),
    mirroring_id   uuid not null references mirroring (id)
);

create unique index on mirroring_target (mirroring_id, channel_id);