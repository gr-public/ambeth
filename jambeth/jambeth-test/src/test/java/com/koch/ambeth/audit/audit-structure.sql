CREATE OR REPLACE TYPE "STRING_ARRAY" AS VARRAY(4000) OF VARCHAR2(4000 CHAR);

CREATE TABLE "AUDIT_ENTRY"
  (
    "ID" 					NUMBER(9,0) NOT NULL,
    "VERSION"				NUMBER(9,0) NOT NULL,
    "USER_ID"				NUMBER(9,0),
	"USER_IDENTIFIER"		VARCHAR(64 CHAR),
	"SIGNATURE_OF_USER_ID"	NUMBER(9,0),
	"SIGNED_VALUE"			VARCHAR(140 CHAR),
	"PROTOCOL"				NUMBER(9,0),
	"TIMESTAMP"				NUMBER(18,0) NOT NULL,
	"REASON"				VARCHAR2(4000 CHAR),
	"CONTEXT"				VARCHAR2(256 CHAR),
	"HASH_ALGORITHM"		VARCHAR2(64 CHAR),
	CONSTRAINT "AUDIT_ENTRY_PK" PRIMARY KEY ("ID") USING INDEX,
    CONSTRAINT "AUDIT_ENTRY_FK_USER" FOREIGN KEY ("USER_ID") REFERENCES "USER" ("ID") DEFERRABLE INITIALLY DEFERRED,
	CONSTRAINT "AUDIT_ENTRY_FK_SIGNATURE" FOREIGN KEY ("SIGNATURE_OF_USER_ID") REFERENCES "SIGNATURE" ("ID") DEFERRABLE INITIALLY DEFERRED    
  );
CREATE SEQUENCE "AUDIT_ENTRY_SEQ"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE;

CREATE INDEX "AUDIT_ENTRY_FK_USER_IDX" ON "AUDIT_ENTRY" ("USER_ID");
CREATE INDEX "AUDIT_ENTRY_FK_SIGNATURE_IDX" ON "AUDIT_ENTRY" ("SIGNATURE_OF_USER_ID");

CREATE TABLE "AUDITED_ENTITY_REF"
  (
	"ID" 				NUMBER(9,0) NOT NULL,
	"VERSION"			NUMBER(9,0) NOT NULL,
	"ENTITY_ID"			NUMBER(9,0) NOT NULL,
	"ENTITY_TYPE"		VARCHAR2(256 CHAR) NOT NULL,
	"ENTITY_VERSION"	NUMBER(9,0),
	CONSTRAINT "AUDITED_ENTITY_REF_PK" PRIMARY KEY ("ID") USING INDEX
  );
CREATE SEQUENCE "AUDITED_ENTITY_REF_SEQ"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE;

CREATE TABLE "AUDITED_ENTITY"
  (
	"ID" 						NUMBER(9,0) NOT NULL,
	"VERSION"					NUMBER(9,0) NOT NULL,
	"AUDIT_ENTRY_ID"			NUMBER(9,0) NOT NULL,
	"ORDER"						NUMBER(9,0) NOT NULL,
	"REF_ID"					NUMBER(9,0) NOT NULL,
	"REF_PREVIOUS_VERSION"		NUMBER(9,0),
	"SIGNED_VALUE"				VARCHAR(140 CHAR),
	"CHANGE_TYPE"				VARCHAR2(6 CHAR) NOT NULL,
	CONSTRAINT "AUDITED_ENTITY_PK" PRIMARY KEY ("ID") USING INDEX,
	CONSTRAINT "AUDITED_ENTITY_UQ_AE_ORDER" UNIQUE ("AUDIT_ENTRY_ID", "ORDER"),
	CONSTRAINT "AUDITED_ENTITY_FK_AE_ID" FOREIGN KEY ("AUDIT_ENTRY_ID") REFERENCES "AUDIT_ENTRY" ("ID") DEFERRABLE INITIALLY DEFERRED,
	CONSTRAINT "AUDITED_ENTITY_FK_REF_ID" FOREIGN KEY ("REF_ID") REFERENCES "AUDITED_ENTITY_REF" ("ID") DEFERRABLE INITIALLY DEFERRED
  );
CREATE SEQUENCE "AUDITED_ENTITY_SEQ"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE;

CREATE INDEX "AUDITED_ENTITY_FK_AE_IDX" ON "AUDITED_ENTITY" ("AUDIT_ENTRY_ID");
CREATE INDEX "AUDITED_ENTITY_FK_REF_IDX" ON "AUDITED_ENTITY" ("REF_ID");

CREATE TABLE "AE_PRIMITIVE"
  (
	"ID" 				NUMBER(9,0) NOT NULL,
	"VERSION"			NUMBER(9,0) NOT NULL,
	"AUDITED_ENTITY_ID"	NUMBER(9,0) NOT NULL,
	"ORDER"				NUMBER(9,0) NOT NULL,
	"NAME"				VARCHAR2(64 CHAR) NOT NULL,
	"NEW_VALUE"			CLOB,
	CONSTRAINT "AE_PRIMITIVE_PK" PRIMARY KEY ("ID") USING INDEX,
	CONSTRAINT "AE_PRIMITIVE_UQ_AE_ORDER" UNIQUE ("AUDITED_ENTITY_ID", "ORDER"),
	CONSTRAINT "AE_PRIMITIVE_FK_AE_ID" FOREIGN KEY ("AUDITED_ENTITY_ID") REFERENCES "AUDITED_ENTITY" ("ID") DEFERRABLE INITIALLY DEFERRED
  );
CREATE SEQUENCE "AE_PRIMITIVE_SEQ"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE;

CREATE INDEX "AE_PRIMITIVE_FK_AE_IDX" ON "AE_PRIMITIVE" ("AUDITED_ENTITY_ID");

CREATE TABLE "AE_RELATION"
  (
	"ID" 				NUMBER(9,0) NOT NULL,
	"VERSION"			NUMBER(9,0) NOT NULL,
	"AUDITED_ENTITY_ID"	NUMBER(9,0) NOT NULL,
	"ORDER"				NUMBER(9,0) NOT NULL,
	"NAME"				VARCHAR2(64 CHAR) NOT NULL,
	CONSTRAINT "AE_RELATION_PK" PRIMARY KEY ("ID") USING INDEX,
	CONSTRAINT "AE_RELATION_UQ_AE_ORDER" UNIQUE ("AUDITED_ENTITY_ID", "ORDER"),
	CONSTRAINT "AE_RELATION_FK_AE_ID" FOREIGN KEY ("AUDITED_ENTITY_ID") REFERENCES "AUDITED_ENTITY" ("ID") DEFERRABLE INITIALLY DEFERRED
  );
CREATE SEQUENCE "AE_RELATION_SEQ"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE;

CREATE INDEX "AE_RELATION_FK_AE_IDX" ON "AE_RELATION" ("AUDITED_ENTITY_ID");

CREATE TABLE "AE_RELATION_ITEM"
  (
	"ID" 				NUMBER(9,0) NOT NULL,
	"VERSION"			NUMBER(9,0) NOT NULL,
	"AE_RELATION_ID"	NUMBER(9,0) NOT NULL,
	"ORDER"				NUMBER(9,0) NOT NULL,
	"CHANGE_TYPE"		VARCHAR2(6 CHAR) NOT NULL,
	"REF_ID"			NUMBER(9,0) NOT NULL,
	CONSTRAINT "AE_RELATION_ITEM_PK" PRIMARY KEY ("ID") USING INDEX,
	CONSTRAINT "AE_RELATION_ITEM_UQ_AE_ORDER" UNIQUE ("AE_RELATION_ID", "ORDER"),
	CONSTRAINT "AE_RELATION_ITEM_FK_REL_ID" FOREIGN KEY ("AE_RELATION_ID") REFERENCES "AE_RELATION" ("ID") DEFERRABLE INITIALLY DEFERRED,
	CONSTRAINT "AE_RELATION_ITEM_FK_REF_ID" FOREIGN KEY ("REF_ID") REFERENCES "AUDITED_ENTITY_REF" ("ID") DEFERRABLE INITIALLY DEFERRED
  );
CREATE SEQUENCE "AE_RELATION_ITEM_SEQ"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE;

CREATE INDEX "AE_RELATION_ITEM_FK_REL_IDX" ON "AE_RELATION_ITEM" ("AE_RELATION_ID");
CREATE INDEX "AE_RELATION_ITEM_FK_REF_IDX" ON "AE_RELATION_ITEM" ("REF_ID");

CREATE TABLE "AUDITED_SERVICE"
  (
	"ID" 				NUMBER(9,0) NOT NULL,
	"VERSION"			NUMBER(9,0) NOT NULL,
	"AUDIT_ENTRY_ID"	NUMBER(9,0) NOT NULL,
	"ORDER"				NUMBER(9,0) NOT NULL,	
	"SERVICE_TYPE"		VARCHAR2(256 CHAR) NOT NULL,
	"METHOD_NAME"		VARCHAR2(100 CHAR) NOT NULL,
	"ARGUMENTS"			STRING_ARRAY,
	"SPENT_TIME"		NUMBER(12,0) NOT NULL,
	CONSTRAINT "AUDITED_SERVICE_PK" PRIMARY KEY ("ID") USING INDEX,
	CONSTRAINT "AUDITED_SERVICE_UQ_AE_ORDER" UNIQUE ("AUDIT_ENTRY_ID", "ORDER"),
	CONSTRAINT "AUDITED_SERVICE_FK_AE_ID" FOREIGN KEY ("AUDIT_ENTRY_ID") REFERENCES "AUDIT_ENTRY" ("ID") DEFERRABLE INITIALLY DEFERRED	
  );
CREATE SEQUENCE "AUDITED_SERVICE_SEQ"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE;

CREATE INDEX "AUDITED_SERVICE_FK_AE_IDX" ON "AUDITED_SERVICE" ("AUDIT_ENTRY_ID");
