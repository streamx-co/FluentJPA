CREATE TABLE t1 (
 id long NOT NULL PRIMARY KEY,
 bcolor VARCHAR,
 fcolor VARCHAR
);

CREATE TABLE rental (
 id long NOT NULL PRIMARY KEY,
 customer_id long,
 return_date TIMESTAMP
);

CREATE TABLE employee_man (
 employee_id INT PRIMARY KEY,
 first_name VARCHAR (255) NOT NULL,
 last_name VARCHAR (255) NOT NULL,
 manager_id INT,
 FOREIGN KEY (manager_id) 
 REFERENCES employee_man (employee_id) 
 ON DELETE CASCADE
);
INSERT INTO employee_man (
 employee_id,
 first_name,
 last_name,
 manager_id
)
VALUES
 (1, 'Windy', 'Hays', NULL),
 (2, 'Ava', 'Christensen', 1),
 (3, 'Hassan', 'Conner', 1),
 (4, 'Anna', 'Reeves', 2),
 (5, 'Sau', 'Norman', 2),
 (6, 'Kelsie', 'Hays', 3),
 (7, 'Tory', 'Goff', 3),
 (8, 'Salley', 'Lester', 3);