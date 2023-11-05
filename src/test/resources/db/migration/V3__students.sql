
CREATE TABLE STUDENT (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	name varchar(255) not null
);

CREATE TABLE COURSE (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	name varchar(255) not null
);

CREATE TABLE COURSE_LIKE (
	student_id BIGINT not null,
	course_id BIGINT not null,
	PRIMARY KEY (student_id, course_id),
	FOREIGN KEY (student_id) REFERENCES STUDENT(id),
	FOREIGN KEY (course_id) REFERENCES COURSE(id)
);