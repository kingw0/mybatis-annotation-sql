drop table count if exists;

create table count (
	type_ int default 0,
	count_ int default 0
);

insert into count values (1, 1);

commit;