create sequence hibernate_sequence;

CREATE TABLE PERSON_TABLE (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	name varchar(255) not null,
	aging INT not null,
	height INT,
	balancer BOOL DEFAULT TRUE
);

CREATE TABLE OBJECT_CON (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	name varchar(255) not null
);

CREATE TABLE NETWORK_OBJECT (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	ip_count DECIMAL(19,2),
	object_internal_type INT,
	object_con BIGINT,
	FOREIGN KEY (object_con) REFERENCES OBJECT_CON(id)
);

CREATE TABLE NETWORK_OBJECT_RANGE (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	first BIGINT,
	last BIGINT,
	network_obj BIGINT,
	FOREIGN KEY (network_obj) REFERENCES NETWORK_OBJECT(id)
);

insert into PERSON_TABLE (name, aging, balancer) values ('Dave', 345, 1);