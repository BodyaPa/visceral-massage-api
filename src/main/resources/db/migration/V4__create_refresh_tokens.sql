create table refresh_tokens (
                                id bigserial primary key,
                                user_id bigint not null references users(id) on delete cascade,
                                token_hash varchar(255) not null,
                                expires_at timestamptz not null,
                                created_at timestamptz not null default now(),
                                revoked_at timestamptz null
);

create index idx_refresh_user on refresh_tokens(user_id);
create index idx_refresh_expires on refresh_tokens(expires_at);