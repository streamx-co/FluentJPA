
CREATE TABLE COMPANY (
	id BIGINT NOT NULL,
	name VARCHAR(255),
	PRIMARY KEY (id)
);

CREATE TABLE EMPLOYEE (
	company_id BIGINT NOT NULL,
	employee_number BIGINT NOT NULL,
	name VARCHAR(255),
	PRIMARY KEY (company_id, employee_number),
	FOREIGN KEY (company_id) REFERENCES COMPANY(id)
);

CREATE TABLE PHONE (
	company_id BIGINT NOT NULL,
	employee_number BIGINT NOT NULL,
	num VARCHAR(255) NOT NULL PRIMARY KEY,
	FOREIGN KEY (company_id, employee_number) REFERENCES EMPLOYEE(company_id, employee_number),
	FOREIGN KEY (company_id) REFERENCES COMPANY(id)
);