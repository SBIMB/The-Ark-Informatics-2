/*
-- Query: SELECT * FROM lims.unit
LIMIT 0, 2000

-- Date: 2013-08-09 14:17


DELETE FROM lims.UNIT where id <> 17
*/
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (0,'unit',NULL,1,-1,'UNKNOWN');
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (1,'mm',NULL,0.001,301,'DISTANCE');
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (2,'ul',NULL,1,3,'VOLUME');
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (3,'nl',NULL,0.001,2,'VOLUME');
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (10,'kg',NULL,1000000,105,'MASS');
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (12,'g',NULL,1000,104,'MASS');
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (14,'ug',NULL,0.001,102,'MASS');
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (15,'mg',NULL,1,103,'MASS');
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (16,'cm',NULL,1,301,'DISTANCE');
-- INSERT INTO `lims.UNIT` (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (17,'mL',NULL,1000,4,'VOLUME');
INSERT INTO lims.UNIT (`ID`,`NAME`,`DESCRIPTION`,`FACTOR`,`ORDER`,`TYPE`) VALUES (38,'L',NULL,1000000,5,'VOLUME');