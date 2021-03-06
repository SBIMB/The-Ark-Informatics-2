USE lims;
ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `DEPTH` `DEPTH` INT(11) NULL DEFAULT NULL  AFTER `STORED_IN` , CHANGE COLUMN `SAMPLE_TIME` `SAMPLE_TIME` DATETIME NULL DEFAULT NULL  AFTER `SAMPLEDATE` , CHANGE COLUMN `DATEEXTRACTED` `PROCESSED_DATE` DATETIME NULL DEFAULT NULL  AFTER `SAMPLE_TIME` , CHANGE COLUMN `EXTRACTED_TIME` `PROCESSED_TIME` DATETIME NULL DEFAULT NULL  ;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `SAMPLEDATE` `SAMPLE_DATE` DATETIME NULL DEFAULT NULL  ;

ALTER TABLE `lims`.`biospecimen` DROP FOREIGN KEY `fk_biospecimen_collection` ;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `LINK_SUBJECT_STUDY_ID` `LINK_SUBJECT_STUDY_ID` INT(11) NOT NULL  , CHANGE COLUMN `COLLECTION_ID` `COLLECTION_ID` INT(11) NOT NULL  ,
  ADD CONSTRAINT `fk_biospecimen_collection`
  FOREIGN KEY (`COLLECTION_ID` )
  REFERENCES `lims`.`collection` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `STORED_IN` `BIOSPECIMEN_STORAGE_ID` INT(11) NULL,
  ADD CONSTRAINT `fk_biospecimen_storage`
  FOREIGN KEY (`BIOSPECIMEN_STORAGE_ID` )
  REFERENCES `lims`.`biospecimen_storage` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `QUALITY` `BIOSPECIMEN_QUALITY_ID` INT(11) NULL,
  ADD CONSTRAINT `fk_biospecimen_quality`
  FOREIGN KEY (`BIOSPECIMEN_QUALITY_ID` )
  REFERENCES `lims`.`biospecimen_quality` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `GRADE` `BIOSPECIMEN_GRADE_ID` INT(11) NULL,
  ADD CONSTRAINT `fk_biospecimen_grade`
  FOREIGN KEY (`BIOSPECIMEN_GRADE_ID` )
  REFERENCES `lims`.`biospecimen_grade` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `STATUS` `BIOSPECIMEN_STATUS_ID` INT(11) NULL,
  ADD CONSTRAINT `fk_biospecimen_status`
  FOREIGN KEY (`BIOSPECIMEN_STATUS_ID` )
  REFERENCES `lims`.`biospecimen_status` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `PROTOCOL` `BIOSPECIMEN_PROTOCOL_ID` INT(11) NULL,
  ADD CONSTRAINT `fk_biospecimen_protocol`
  FOREIGN KEY (`BIOSPECIMEN_PROTOCOL_ID` )
  REFERENCES `lims`.`biospecimen_protocol` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `SPECIES` `BIOSPECIMEN_SPECIES_ID` INT(11) NULL,
  ADD CONSTRAINT `fk_biospecimen_species`
  FOREIGN KEY (`BIOSPECIMEN_SPECIES_ID` )
  REFERENCES `lims`.`biospecimen_species` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

/* This data DML was taken from 20111025_lims_bispeciment_SchemaAndDataPatch.sql. I had to move it here since there is reference to default value later in this
script that requires data in the referenced table biospecimen_species */

INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (1,'Human');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (2,'Baboon');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (3,'Cat');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (4,'Cow');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (5,'Dog');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (6,'Goat');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (7,'Mouse');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (8,'Pig');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (9,'Rabbit');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (10,'Rat');
INSERT INTO `lims`.`biospecimen_species` (ID,NAME) VALUES (11,'Sheep');


ALTER TABLE `lims`.`biospecimen` 	DROP COLUMN `WITHDRAWN`;

ALTER TABLE `lims`.`biospecimen` 	DROP FOREIGN KEY `fk_biospecimen_status` , 
					DROP FOREIGN KEY `fk_biospecimen_protocol` , 
					DROP FOREIGN KEY `fk_biospecimen_quality` ;

ALTER TABLE `lims`.`biospecimen` 	DROP COLUMN `BIOSPECIMEN_STATUS_ID` , 
					DROP COLUMN `BIOSPECIMEN_QUALITY_ID` , 
					DROP COLUMN `DNA_BANK` , 
					DROP COLUMN `BIOSPECIMEN_PROTOCOL_ID` , 
					DROP COLUMN `ANTICOAG` , 
					DROP COLUMN `PURITY` , 
					DROP COLUMN `DNACONC` ,
					DROP COLUMN `COLLABORATOR` , 
					DROP COLUMN `DATEDISTRIBUTED` , 
					DROP COLUMN `GESTAT` , 
					DROP COLUMN `SUBTYPEDESC` ,
					DROP COLUMN `LOCATION` , 
					CHANGE COLUMN `DEPTH` `DEPTH` INT(11) NOT NULL ,
					DROP INDEX `fk_biospecimen_quality` ,
					DROP INDEX `fk_biospecimen_protocol` ,
					DROP INDEX `fk_biospecimen_status` ;


-- Script stoppped here because it is trying to re-add the same column 'BIOSPECIMEN_QUALITY_ID'(Nov 2nd 2011)
-- With the re-sequencing of the SQL scripts here i.e with the DROP COLUMN it should not error any more (NN Nov 3 2011)
ALTER TABLE `lims`.`biospecimen` ADD COLUMN `BIOSPECIMEN_QUALITY_ID` INT(11) NULL DEFAULT NULL  AFTER `BARCODED`, 
  ADD CONSTRAINT `fk_biospecimen_quality`
  FOREIGN KEY (`BIOSPECIMEN_QUALITY_ID` )
  REFERENCES `lims`.`biospecimen_quality` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION
, ADD INDEX `fk_biospecimen_quality` (`BIOSPECIMEN_QUALITY_ID` ASC) ;



ALTER TABLE `lims`.`biospecimen` ADD COLUMN `BIOSPECIMEN_ANTICOAGULANT_ID` INT(11) NULL DEFAULT NULL  AFTER `BIOSPECIMEN_QUALITY_ID` , 
  ADD CONSTRAINT `fk_biospecimen_anticoagulant`
  FOREIGN KEY (`BIOSPECIMEN_ANTICOAGULANT_ID` )
  REFERENCES `lims`.`biospecimen_anticoagulant` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION
, ADD INDEX `fk_biospecimen_anticoagulant` (`BIOSPECIMEN_ANTICOAGULANT_ID` ASC) ;

ALTER TABLE `lims`.`biospecimen` DROP FOREIGN KEY `fk_biospecimen_species` ;


UPDATE `lims`.`biospecimen` SET biospecimen_species_id=1 WHERE id >0;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `BIOSPECIMEN_SPECIES_ID` `BIOSPECIMEN_SPECIES_ID` INT(11) NOT NULL DEFAULT '1'  , 
  ADD CONSTRAINT `fk_biospecimen_species`
  FOREIGN KEY (`BIOSPECIMEN_SPECIES_ID` )
  REFERENCES `lims`.`biospecimen_species` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;


ALTER TABLE `lims`.`biospecimen` ADD COLUMN `BIOSPECIMEN_STATUS_ID` INT(11) NULL DEFAULT NULL  AFTER `BIOSPECIMEN_ANTICOAGULANT_ID` , 
  ADD CONSTRAINT `fk_biospecimen_status`
  FOREIGN KEY (`BIOSPECIMEN_STATUS_ID` )
  REFERENCES `lims`.`biospecimen_status` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION
, ADD INDEX `fk_biospecimen_status` (`BIOSPECIMEN_STATUS_ID` ASC) ;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `DEPTH` `DEPTH` INT(11) NULL DEFAULT '1'  ;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `BIOSPECIMEN_GRADE_ID` `BIOSPECIMEN_GRADE_ID` INT(11) NULL;

ALTER TABLE `lims`.`biospecimen` CHANGE COLUMN `BARCODED` `BARCODED` TINYINT(1) NOT NULL DEFAULT '0';


