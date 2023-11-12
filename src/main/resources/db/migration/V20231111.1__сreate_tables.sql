create table channel(
    id uuid primary key,
    name text not null
);

create table mirroring(
    id uuid primary key,
    src_channel_id uuid not null references channel(id)
);

create table mirroring_target(
    id uuid primary key,
    mirroring_id uuid not null references mirroring(id),
    tgt_channel_id uuid not null references channel(id)
);