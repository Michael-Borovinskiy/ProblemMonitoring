DROP TABLE IF EXISTS ANALYTICAL_EVENT;
CREATE TABLE ANALYTICAL_EVENT (
id INT AUTO_INCREMENT  PRIMARY KEY,
type_event VARCHAR(20) NOT NULL,
kind_event VARCHAR(50) NOT NULL,
id_client VARCHAR(10) NOT NULL,
date_create TIMESTAMP NOT NULL,
approved_by VARCHAR(20) NOT NULL,
approved_datetime TIMESTAMP NOT NULL,
load_datetime TIMESTAMP NOT NULL
);