USE gpconnect;

/* Destroy all existing data */
DROP TABLE IF EXISTS gpconnect.appointment_slots_organizations;
DROP TABLE IF EXISTS gpconnect.appointment_slots_orgType;
DROP TABLE IF EXISTS gpconnect.appointment_appointments_slots;
DROP TABLE IF EXISTS gpconnect.appointment_schedules;
DROP TABLE IF EXISTS gpconnect.appointment_slots;
DROP TABLE IF EXISTS gpconnect.organizations;
DROP TABLE IF EXISTS gpconnect.patients;
DROP TABLE IF EXISTS gpconnect.practitioners;
DROP TABLE IF EXISTS gpconnect.locations;
DROP TABLE IF EXISTS gpconnect.appointment_booking_orgz;
DROP TABLE IF EXISTS gpconnect.appointment_appointments;
DROP TABLE IF EXISTS gpconnect.medical_departments;
DROP TABLE IF EXISTS gpconnect.addresses;

/* Create new table schemas */

CREATE TABLE gpconnect.appointment_booking_orgz (
  id              BIGINT       NOT NULL,
  org_code        VARCHAR(30)  NULL,
  name            VARCHAR(100) NULL,
  telephone       VARCHAR(100) NULL,
  lastUpdated     DATETIME     NULL,
  PRIMARY KEY (id)
);

CREATE TABLE gpconnect.appointment_appointments (
  id                 BIGINT    NOT NULL AUTO_INCREMENT,
  cancellationReason TEXT(300) NULL,
  status             TEXT(50)  NULL,
  typeCode           BIGINT    NULL,
  typeDisplay        TEXT(100) NULL,
  typeText           TEXT(100) NULL,
  description        TEXT(300) NULL,
  startDateTime      DATETIME  NULL,
  endDateTime        DATETIME  NULL,
  commentText        TEXT(300) NULL,
  patientId          BIGINT    NULL NOT NULL,
  practitionerId     BIGINT    NULL,
  locationId         BIGINT    NULL NOT NULL,
  minutesDuration    BIGINT    NULL,
  created            DATETIME  NULL,
  priority    		   BIGINT    NULL,
  lastUpdated        DATETIME  NULL,
  PRIMARY KEY (id)
);

ALTER TABLE gpconnect.appointment_booking_orgz
ADD CONSTRAINT fk_appointment_appointments_booking_orgz
FOREIGN KEY (id) REFERENCES gpconnect.appointment_appointments(id);

CREATE TABLE gpconnect.appointment_schedules (
  id              BIGINT    NOT NULL AUTO_INCREMENT,
  practitionerId  BIGINT    NULL,
  identifier      TEXT(50)  NULL,
  typeCode        TEXT(50)  NULL,
  typeDescription TEXT(250) NULL,
  locationId      BIGINT    NULL,
  deliveryChannelCode	TEXT(50)  NULL,
  deliveryChannelDisplay  TEXT(50)  NULL,
  practitionerRoleCode	TEXT(50)  NULL,
  practitionerRoleDisplay  TEXT(50)  NULL,
  startDateTime   DATETIME  NULL,
  endDateTime     DATETIME  NULL,
  scheduleComment TEXT(300) NULL,
  lastUpdated     DATETIME  NULL,
  PRIMARY KEY (id)
);

CREATE TABLE gpconnect.appointment_slots (
  id                BIGINT    NOT NULL AUTO_INCREMENT,
  typeCode          BIGINT    NULL,
  typeDisplay       TEXT(300) NULL,
  scheduleReference BIGINT    NULL,
  freeBusyType      TEXT(50)  NULL,
  startDateTime     DATETIME  NULL,
  endDateTime       DATETIME  NULL,
  lastUpdated       DATETIME  NULL,
  gpConnectBookable BOOLEAN   NULL,
  PRIMARY KEY (id)
);

CREATE TABLE `appointment_slots_orgType` (
  `slotId` bigint(20) NOT NULL,
  `bookableOrgTypes` varchar(255) NOT NULL,
  KEY `slotId` (`slotId`),
  CONSTRAINT `appointment_slots_orgType_ibfk_1` FOREIGN KEY (`slotId`) REFERENCES `appointment_slots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE appointment_appointments_slots (
	appointmentId	BIGINT    NOT NULL,
	slotId			BIGINT    NOT NULL,
	FOREIGN KEY (appointmentId) REFERENCES gpconnect.appointment_appointments(id),
	FOREIGN KEY (slotId) 		REFERENCES gpconnect.appointment_slots(id)
);

CREATE TABLE gpconnect.practitioners (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  userid            VARCHAR(30)  NULL,
  p_role_ids        VARCHAR(30)  NOT NULL,
  p_name_family     VARCHAR(100) NULL,
  p_name_given      VARCHAR(100) NULL,
  p_name_prefix     VARCHAR(20)  NULL,
  p_gender          VARCHAR(10)  NULL,
  p_organization_id BIGINT       NULL,
  p_role_code       VARCHAR(30)  NULL,
  p_role_display    VARCHAR(100) NULL,
  p_com_code        VARCHAR(30)  NULL,
  p_com_display     VARCHAR(100) NULL,
  lastUpdated       DATETIME     NULL,
  PRIMARY KEY (id)
);

CREATE TABLE gpconnect.organizations (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  org_code    VARCHAR(30)  NULL,
  org_name    VARCHAR(100) NULL,
  lastUpdated DATETIME     NULL,
  PRIMARY KEY (id)
);

CREATE TABLE appointment_slots_organizations (
	slotId       	BIGINT    NOT NULL,
	organizationId	BIGINT    NOT NULL,
	FOREIGN KEY (organizationId) REFERENCES gpconnect.organizations(id),
	FOREIGN KEY (slotId) 		REFERENCES gpconnect.appointment_slots(id)
);

CREATE TABLE gpconnect.medical_departments (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  department  VARCHAR(150) NULL,
  lastUpdated DATETIME     NULL,
  PRIMARY KEY (id)
);

CREATE TABLE gpconnect.patients (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  title               VARCHAR(10)  NULL,
  first_name          VARCHAR(300) NULL,
  last_name           VARCHAR(300) NULL,
  address_1           VARCHAR(100) NULL,
  address_2           VARCHAR(100) NULL,
  address_3           VARCHAR(100) NULL,
  address_4           VARCHAR(100) NULL,
  address_5           VARCHAR(100) NULL,
  postcode            VARCHAR(10)  NULL,
  phone               VARCHAR(20)  NULL,
  date_of_birth       DATE         NULL,
  gender              VARCHAR(10)  NULL,
  nhs_number          VARCHAR(20)  NULL,
  pas_number          VARCHAR(20)  NULL,
  department_id       BIGINT       NULL,
  gp_id               BIGINT       NULL,
  lastUpdated         DATETIME     NULL,
  registration_start  DATETIME     NULL,
  registration_end    DATETIME     NULL,
  registration_status VARCHAR(10)  NULL,
  registration_type   VARCHAR(10)  NULL,
  sensitive_flag      BOOLEAN      NULL,
  multiple_birth      BOOLEAN      NULL,
  deceased			  DATETIME	   NULL,
  marital_status      VARCHAR(10)  NULL,
  managing_organization BIGINT     NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (department_id) REFERENCES gpconnect.medical_departments(id),
  FOREIGN KEY (gp_id) REFERENCES gpconnect.practitioners(id)
);

CREATE TABLE gpconnect.addresses (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  line   	  VARCHAR(100)NULL,
  city        VARCHAR(50) NULL,
  district    VARCHAR(50) NULL,
  state       VARCHAR(50) NULL,
  postalCode  VARCHAR(10) NULL,
  country     VARCHAR(50) NULL,
  PRIMARY KEY (id)
);

CREATE TABLE gpconnect.locations (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  name               VARCHAR(250) NOT NULL,
  org_ods_code       VARCHAR(250) NOT NULL,
  org_ods_code_name  VARCHAR(250) NOT NULL,
  site_ods_code      VARCHAR(250) NOT NULL,
  site_ods_code_name VARCHAR(250) NOT NULL,
  status             VARCHAR(100) NULL,
  lastUpdated        DATETIME     NULL,
  address_id         BIGINT       NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (address_id) REFERENCES gpconnect.addresses(id)
);
