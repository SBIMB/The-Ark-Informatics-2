use pheno;
CREATE  TABLE `pheno`.`pheno_collection` (
  `ID` INT NOT NULL AUTO_INCREMENT ,
  `NAME` VARCHAR(255) NULL ,
  `LINK_SUBJECT_STUDY_ID` INT NOT NULL ,
  `QUESTIONNAIRE_STATUS_ID` INT NOT NULL ,
  `RECORDED_DATE` DATE NULL ,
  `CUSTOM_FIELD_GROUP_ID` INT NOT NULL ,
  `REVIEWED_DATE` DATE NULL ,
  `REVIEWED_BY_ID` INT NULL ,
  PRIMARY KEY (`ID`) ,
  INDEX `FK_PHENO_COLLECTION_LINK_SUBJECT_STUDY_ID` (`LINK_SUBJECT_STUDY_ID` ASC) ,
  INDEX `FK_PHENO_QUESTIONNAIRE_STATUS_ID` (`QUESTIONNAIRE_STATUS_ID` ASC) ,
  INDEX `FK_PHENO_CUSTOM_FIELD_GROUP_ID` (`CUSTOM_FIELD_GROUP_ID` ASC) ,
  INDEX `FK_REVIEWED_BY_ARK_USER_ID` (`REVIEWED_BY_ID` ASC) ,
  CONSTRAINT `FK_PHENO_COLLECTION_LINK_SUBJECT_STUDY_ID`
    FOREIGN KEY (`LINK_SUBJECT_STUDY_ID` )
    REFERENCES `study`.`link_subject_study` (`ID` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_PHENO_QUESTIONNAIRE_STATUS_ID`
    FOREIGN KEY (`QUESTIONNAIRE_STATUS_ID` )
    REFERENCES `pheno`.`questionnaire_status` (`ID` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_PHENO_CUSTOM_FIELD_GROUP_ID`
    FOREIGN KEY (`CUSTOM_FIELD_GROUP_ID` )
    REFERENCES `study`.`custom_field_group` (`ID` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_REVIEWED_BY_ARK_USER_ID`
    FOREIGN KEY (`REVIEWED_BY_ID` )
    REFERENCES `study`.`ark_user` (`ID` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

