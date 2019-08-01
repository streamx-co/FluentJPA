
CREATE TABLE Parent(
  "co de" varchar(4) not null,
  id int not null,
  primary key("co de",id)
);

CREATE TABLE Child(
  code varchar(4) not null,
  id int not null,
  index int not null,
  primary key(code, id, index),
  foreign key(code, id) references Parent("co de",id)
);