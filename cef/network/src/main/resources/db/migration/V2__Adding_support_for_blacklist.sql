create table if not exists cef.blacklist_node (
    node_id char(256) not null primary key,
    blacklist_since timestamp not null,
    blacklist_until timestamp not null,
    foreign key (node_id) references cef.known_node(id)
);