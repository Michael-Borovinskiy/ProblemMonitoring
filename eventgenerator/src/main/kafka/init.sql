-- create a table ANALYTICAL_EVENT
CREATE TABLE ANALYTICAL_EVENT(
  id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  type_event varchar(20) ,
  kind_event varchar(50) ,
  id_client  varchar(10) ,
  date_create timestamp ,
  approved_by varchar(20) ,
  approved_datetime timestamp
);
