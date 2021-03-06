use study;
ALTER TABLE `study`.`custom_field_group` ADD COLUMN `STUDY_ID` INT NULL  AFTER `DESCRIPTION` , 
  ADD CONSTRAINT `FK_CUSTOM_FIELD_GROUP_STUDY_ID`
  FOREIGN KEY (`STUDY_ID` )
  REFERENCES `study`.`study` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION
, ADD INDEX `FK_CUSTOM_FIELD_GROUP_STUDY_ID` (`STUDY_ID` ASC) ;


ALTER TABLE `study`.`custom_field_group` CHANGE COLUMN `STUDY_ID` `STUDY_ID` INT(11) NOT NULL  ;

/* Use this script if the FK was dropped for some reason after adding STUDY_ID not null 
ALTER TABLE `study`.`custom_field_group` 
  ADD CONSTRAINT `FK_CUSTOM_FIELD_GROUP_STUDY_ID`
  FOREIGN KEY (`STUDY_ID` )
  REFERENCES `study`.`study` (`ID` )
  ON DELETE NO ACTION
  ON UPDATE NO ACTION
, DROP PRIMARY KEY 
, ADD PRIMARY KEY (`ID`) ;


*/

/* We need this field to stop users from using the Questionnaire. When published field is set to true, then it should be avaialble to users to data entry */

ALTER TABLE `study`.`custom_field_group` ADD COLUMN `PUBLISHED` TINYINT(1)  NULL  AFTER `STUDY_ID` ;



