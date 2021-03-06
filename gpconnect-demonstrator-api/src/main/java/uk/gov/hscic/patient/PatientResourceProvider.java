package uk.gov.hscic.patient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Address.AddressType;
import org.hl7.fhir.dstu3.model.Address.AddressUse;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.HumanName.NameUse;
import org.hl7.fhir.dstu3.model.Identifier.IdentifierUse;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueType;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import uk.gov.hscic.OperationOutcomeFactory;
import uk.gov.hscic.SystemCode;
import uk.gov.hscic.SystemURL;
import uk.gov.hscic.appointments.AppointmentResourceProvider;
import uk.gov.hscic.common.helpers.StaticElementsHelper;
import uk.gov.hscic.common.validators.IdentifierValidator;
import uk.gov.hscic.model.patient.PatientDetails;
import uk.gov.hscic.patient.details.PatientSearch;
import uk.gov.hscic.patient.details.PatientStore;
import uk.gov.hscic.practitioner.PractitionerResourceProvider;
import uk.gov.hscic.util.NhsCodeValidator;

@Component
public class PatientResourceProvider implements IResourceProvider {
    private static final String REGISTER_PATIENT_OPERATION_NAME = "$gpc.registerpatient";
    private static final String GET_CARE_RECORD_OPERATION_NAME = "$gpc.getcarerecord";

    private static final String TEMPORARY_RESIDENT_REGISTRATION_TYPE = "T";
    private static final String ACTIVE_REGISTRATION_STATUS = "A";

    @Autowired
    private PractitionerResourceProvider practitionerResourceProvider;

    @Autowired
    private AppointmentResourceProvider appointmentResourceProvider;

    @Autowired
    private PatientStore patientStore;

    @Autowired
    private PatientSearch patientSearch;

    @Autowired
    private StaticElementsHelper staticElHelper;

    private NhsNumber nhsNumber;

    private Map<String, Boolean> getCareRecordParams;
    private Map<String, Boolean> registerPatientParams;

    public static Set<String> getCustomReadOperations() {
        Set<String> customReadOperations = new HashSet<String>();
        customReadOperations.add(GET_CARE_RECORD_OPERATION_NAME);

        return customReadOperations;
    }

    public static Set<String> getCustomWriteOperations() {
        Set<String> customWriteOperations = new HashSet<String>();
        customWriteOperations.add(REGISTER_PATIENT_OPERATION_NAME);

        return customWriteOperations;
    }

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }

    @PostConstruct
    public void postConstruct() {
        nhsNumber = new NhsNumber();

        boolean mandatory = true;
        boolean optional = false;

        getCareRecordParams = new HashMap<>();
        getCareRecordParams.put("patientNHSNumber", mandatory);
        getCareRecordParams.put("recordSection", mandatory);
        getCareRecordParams.put("timePeriod", optional);

        registerPatientParams = new HashMap<>();
        registerPatientParams.put("registerPatient", true);
    }

    @Read(version = true)
    public Patient getPatientById(@IdParam IdType internalId) throws FHIRException {
        PatientDetails patientDetails = patientSearch.findPatientByInternalID(internalId.getIdPart());
      
        if (patientDetails == null) {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new ResourceNotFoundException("No patient details found for patient ID: " + internalId.getIdPart()),
                    SystemCode.PATIENT_NOT_FOUND, IssueType.NOTFOUND);
        }

        Patient patient =  IdentifierValidator.versionComparison(internalId,
                patientDetailsToPatientResourceConverter(patientDetails));
        if(null != patient){
            addPreferredBranchSurgeryExtension(patient);
        }
        return patient;
    }

    @Search
	public List<Patient> getPatientsByPatientId(@RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam tokenParam)
			throws FHIRException {

		Patient patient = getPatientByPatientId(nhsNumber.fromToken(tokenParam));
		if (null != patient) {
			addPreferredBranchSurgeryExtension(patient);
		}
		return null == patient || patient.getDeceased() != null ? Collections.emptyList()
				: Collections.singletonList(patient);
	}

	private void addPreferredBranchSurgeryExtension(Patient patient) {
		List<Extension> regDetailsEx = patient.getExtensionsByUrl(SystemURL.SD_EXTENSION_CC_REG_DETAILS);
		Extension branchSurgeryEx = regDetailsEx.get(0).addExtension();
		branchSurgeryEx.setUrl("preferredBranchSurgery");
		branchSurgeryEx.setValue(new Reference("Location/1"));
	}

    private Patient getPatientByPatientId(String patientId) throws FHIRException {
        PatientDetails patientDetails = patientSearch.findPatient(patientId);

        return null == patientDetails ? null : patientDetailsToPatientResourceConverter(patientDetails);
    }

    private void validateParameterNames(Parameters parameters, Map<String, Boolean> parameterDefinitions) {
        List<String> parameterNames = parameters.getParameter().stream().map(ParametersParameterComponent::getName)
                .collect(Collectors.toList());

        Set<String> parameterDefinitionNames = parameterDefinitions.keySet();

        if (parameterNames.isEmpty() == false) {
            for (String parameterDefinition : parameterDefinitionNames) {
                boolean mandatory = parameterDefinitions.get(parameterDefinition);

                if (mandatory) {
                    if (parameterNames.contains(parameterDefinition) == false) {
                        throw OperationOutcomeFactory.buildOperationOutcomeException(
                                new InvalidRequestException("Not all mandatory parameters have been provided"),
                                SystemCode.INVALID_PARAMETER, IssueType.INVALID);
                    }
                }
            }

            if (parameterDefinitionNames.containsAll(parameterNames) == false) {
                parameterNames.removeAll(parameterDefinitionNames);
                throw OperationOutcomeFactory.buildOperationOutcomeException(
                        new InvalidRequestException(
                                "Unrecognised parameters have been provided - " + parameterNames.toString()),
                        SystemCode.BAD_REQUEST, IssueType.INVALID);
            }
        } else {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new InvalidRequestException("Not all mandatory parameters have been provided"),
                    SystemCode.INVALID_PARAMETER, IssueType.INVALID);
        }
    }

    @Search(compartmentName = "Appointment")
    public List<Appointment> getPatientAppointments(@IdParam IdType patientLocalId, @Sort SortSpec sort,
            @Count Integer count, @RequiredParam(name= "start") DateAndListParam startDates) {
        return appointmentResourceProvider.getAppointmentsForPatientIdAndDates(patientLocalId, sort, count, startDates);
    }

    @Operation(name = REGISTER_PATIENT_OPERATION_NAME)
    public Bundle registerPatient(@ResourceParam Parameters params) {
        Patient registeredPatient = null;

        validateParameterNames(params, registerPatientParams);

        Patient unregisteredPatient = params.getParameter().stream()
                .filter(param -> "registerPatient".equalsIgnoreCase(param.getName()))
                .map(ParametersParameterComponent::getResource).map(Patient.class::cast).findFirst().orElse(null);

        if (unregisteredPatient != null) {
            validatePatient(unregisteredPatient);

            // check if the patient already exists
            PatientDetails patientDetails = patientSearch
                    .findPatient(nhsNumber.fromPatientResource(unregisteredPatient));

            if (patientDetails == null || IsInactiveTemporaryPatient(patientDetails)) {

                if (patientDetails == null) {
                    patientDetails = registerPatientResourceConverterToPatientDetail(unregisteredPatient);
                    
                    patientStore.create(patientDetails);
                } else {
                    patientDetails.setRegistrationStatus(ACTIVE_REGISTRATION_STATUS);
                    updateAddressAndTelecom(unregisteredPatient, patientDetails);
                    
                    patientStore.update(patientDetails);
                }
                try {
                    registeredPatient = patientDetailsToRegisterPatientResourceConverter(
                            patientSearch.findPatient(unregisteredPatient.getIdentifierFirstRep().getValue()));
                    
                    addPreferredBranchSurgeryExtension(registeredPatient);
                } catch (FHIRException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                throw OperationOutcomeFactory.buildOperationOutcomeException(
                        new InvalidRequestException(String.format("Patient (NHS number - %s) already exists",
                                nhsNumber.fromPatientResource(unregisteredPatient))),
                        SystemCode.BAD_REQUEST, IssueType.INVALID);
            }
        } else {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new UnprocessableEntityException("Patient record not found"), SystemCode.INVALID_PARAMETER,
                    IssueType.INVALID);
        }

        Bundle bundle = new Bundle().setType(BundleType.SEARCHSET);
        bundle.getMeta().addProfile(SystemURL.SD_GPC_SRCHSET_BUNDLE);
        bundle.addEntry().setResource(registeredPatient);
        
        return bundle;
    }

	private void updateAddressAndTelecom(Patient unregisteredPatient, PatientDetails patientDetails) {
		if (unregisteredPatient.getTelecom().size() > 0) {
			patientDetails.setTelephone(unregisteredPatient.getTelecom().get(0).getValue());
		}
		if (unregisteredPatient.getAddress().size() > 0) {
			List<StringType> addressLineList = unregisteredPatient.getAddress().get(0).getLine();
			StringBuilder addressBuilder = new StringBuilder();
			for(StringType addressLine: addressLineList) {
				addressBuilder.append(addressLine);
				addressBuilder.append(",");
			}
			addressBuilder.append(unregisteredPatient.getAddress().get(0).getCity()).append(",");
			addressBuilder.append(unregisteredPatient.getAddress().get(0).getPostalCode());

			patientDetails.setAddress(addressBuilder.toString());
		}
	}

    private Boolean IsInactiveTemporaryPatient(PatientDetails patientDetails) {

        return patientDetails.getRegistrationType() != null
                && TEMPORARY_RESIDENT_REGISTRATION_TYPE.equals(patientDetails.getRegistrationType())
                && patientDetails.getRegistrationStatus() != null
                && ACTIVE_REGISTRATION_STATUS.equals(patientDetails.getRegistrationStatus()) == false;
    }

    public String getNhsNumber(Object source) {
        return nhsNumber.getNhsNumber(source);
    }

    private void validatePatient(Patient patient) {
        validateIdentifiers(patient);
        validateTelecomAndAddress(patient);
        validateConstrainedOutProperties(patient);
        checkValidExtensions(patient.getExtension());
        validateNames(patient);
        validateDateOfBirth(patient);
        valiateGender(patient);
    }

    private void validateTelecomAndAddress(Patient patient) {
    	//Only a single telecom with type temp may be sent
    	if (patient.getTelecom().size() > 1) {
    		throw OperationOutcomeFactory.buildOperationOutcomeException(
    				new InvalidRequestException(
    						"Only a single telecom can be sent in a register patient request."),
    				SystemCode.BAD_REQUEST, IssueType.INVALID);
    	} else if (patient.getTelecom().size() == 1) {
    		if (patient.getTelecom().get(0).getUse() != ContactPointUse.TEMP) {
    			throw OperationOutcomeFactory.buildOperationOutcomeException(
        				new InvalidRequestException(
        						"The telecom use must be set to temp."),
        				SystemCode.BAD_REQUEST, IssueType.INVALID);
    		}
    	}
    	 
    	//Only a single address with type temp may be sent
    	if (patient.getAddress().size() > 1) {
    		throw OperationOutcomeFactory.buildOperationOutcomeException(
    				new InvalidRequestException(
    						"Only a single address can be sent in a register patient request."),
    				SystemCode.BAD_REQUEST, IssueType.INVALID);
    	} else if (patient.getAddress().size() == 1) {
    		if (patient.getAddress().get(0).getUse() != AddressUse.TEMP) {
    			throw OperationOutcomeFactory.buildOperationOutcomeException(
        				new InvalidRequestException(
        						"The address use must be set to temp."),
        				SystemCode.BAD_REQUEST, IssueType.INVALID);
    		}
    	}
    }

    private void valiateGender(Patient patient) {
        AdministrativeGender gender = patient.getGender();

        if (gender != null) {

            EnumSet<AdministrativeGender> genderList = EnumSet.allOf(AdministrativeGender.class);
            Boolean valid = false;
            for (AdministrativeGender genderItem : genderList) {

                if (genderItem.toCode().equalsIgnoreCase(gender.toString())) {
                    valid = true;
                    break;
                }
            }

            if (!valid) {
                throw OperationOutcomeFactory.buildOperationOutcomeException(
                        new InvalidRequestException(
                                String.format("The supplied Patient gender %s is an unrecognised type.", gender)),
                        SystemCode.BAD_REQUEST, IssueType.INVALID);
            }
        }
    }

    private void validateDateOfBirth(Patient patient) {
        Date birthDate = patient.getBirthDate();

        if (birthDate == null) {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new InvalidRequestException("The Patient date of birth must be supplied"), SystemCode.BAD_REQUEST,
                    IssueType.INVALID);
        }
    }

    private void validateIdentifiers(Patient patient) {
        List<Identifier> identifiers = patient.getIdentifier();
        if (identifiers.isEmpty() == false) {
            boolean identifiersValid = identifiers.stream()
                    .allMatch(identifier -> identifier.getSystem() != null && identifier.getValue() != null);

            if (identifiersValid == false) {
                throw OperationOutcomeFactory.buildOperationOutcomeException(
                        new InvalidRequestException(
                                "One or both of the system and/or value on some of the provided identifiers is null"),
                        SystemCode.BAD_REQUEST, IssueType.INVALID);
            }
        } else {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new InvalidRequestException("At least one identifier must be supplied on a Patient resource"),
                    SystemCode.BAD_REQUEST, IssueType.INVALID);
        }
    }

    private void checkValidExtensions(List<Extension> undeclaredExtensions) {

        List<String> extensionURLs = undeclaredExtensions.stream().map(Extension::getUrl).collect(Collectors.toList());

        extensionURLs.remove(SystemURL.SD_EXTENSION_CC_REG_DETAILS);
        extensionURLs.remove(SystemURL.SD_CC_EXT_ETHNIC_CATEGORY);
        extensionURLs.remove(SystemURL.SD_CC_EXT_RELIGIOUS_AFFILI);
        extensionURLs.remove(SystemURL.SD_PATIENT_CADAVERIC_DON);
        extensionURLs.remove(SystemURL.SD_CC_EXT_RESIDENTIAL_STATUS);
        extensionURLs.remove(SystemURL.SD_CC_EXT_TREATMENT_CAT);
        extensionURLs.remove(SystemURL.SD_CC_EXT_NHS_COMMUNICATION);

        if (!extensionURLs.isEmpty()) {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new UnprocessableEntityException(
                            "Invalid/multiple patient extensions found. The following are in excess or invalid: "
                                    + extensionURLs.stream().collect(Collectors.joining(", "))),
                    SystemCode.INVALID_RESOURCE, IssueType.INVALID);
        }
    }

    private void validateConstrainedOutProperties(Patient patient) {

        Set<String> invalidFields = new HashSet<String>();

        // ## The above can exist in the patient resource but can be ignored. If
        // they are saved by the provider then they should be returned in the
        // response!

        if (patient.getPhoto().isEmpty() == false)
            invalidFields.add("photo");
        if (patient.getAnimal().isEmpty() == false)
            invalidFields.add("animal");
        if (patient.getCommunication().isEmpty() == false)
            invalidFields.add("communication");
        if (patient.getLink().isEmpty() == false)
            invalidFields.add("link");
        if (patient.getDeceased() != null)
            invalidFields.add("deceased");

        if (invalidFields.isEmpty() == false) {
            String message = String.format(
                    "The following properties have been constrained out on the Patient resource - %s",
                    String.join(", ", invalidFields));
            throw OperationOutcomeFactory.buildOperationOutcomeException(new InvalidRequestException(message),
                    SystemCode.BAD_REQUEST, IssueType.NOTSUPPORTED);
        }
    }

    private void validateNames(Patient patient) {
        List<HumanName> names = patient.getName();

        if (names.size() < 1) {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new InvalidRequestException("The patient must have at least one Name."), SystemCode.BAD_REQUEST,
                    IssueType.INVALID);
        }

        List<HumanName> activeOfficialNames = names
                .stream()
                .filter(nm -> IsActiveName(nm))
                .filter(nm -> NameUse.OFFICIAL.equals(nm.getUse()))
                .collect(Collectors.toList());

        if (activeOfficialNames.size() != 1) {
            InvalidRequestException exception = new InvalidRequestException("The patient must have one Active Name with a Use of OFFICIAL");
            
            throw OperationOutcomeFactory.buildOperationOutcomeException(exception, SystemCode.BAD_REQUEST, IssueType.INVALID);
        }

        List<String> officialFamilyNames = new ArrayList<>();
        
        for (HumanName humanName : activeOfficialNames) {
            if (humanName.getFamily() != null) {
                officialFamilyNames.add(humanName.getFamily());
            }
        }

        validateNameCount(officialFamilyNames, "family");
    }

    private void validateNameCount(List<String> names, String nameType) {
        if (names.size() != 1) {
            String message = String.format("The patient must have one and only one %s name property. Found %s",
                    nameType, names.size());
            throw OperationOutcomeFactory.buildOperationOutcomeException(new InvalidRequestException(message),
                    SystemCode.BAD_REQUEST, IssueType.INVALID);
        }
    }

    private Boolean IsActiveName(HumanName name) {

        Period period = name.getPeriod();

        if (null == period) {
            return true;
        }

        Date start = period.getStart();
        Date end = period.getEnd();

        if ((null == end || end.after(new Date()))
                && (null == start || start.equals(new Date()) || start.before(new Date()))) {
            return true;
        }

        return false;
    }

    private PatientDetails registerPatientResourceConverterToPatientDetail(Patient patientResource) {
        PatientDetails patientDetails = new PatientDetails();
        HumanName name = patientResource.getNameFirstRep();

        String givenNames = name.getGiven().stream().map(n -> n.getValue()).collect(Collectors.joining(","));

        patientDetails.setForename(givenNames);

        patientDetails.setSurname(name.getFamily());
        patientDetails.setDateOfBirth(patientResource.getBirthDate());
        if (patientResource.getGender() != null) {
            patientDetails.setGender(patientResource.getGender().toString());
        }
        patientDetails.setNhsNumber(patientResource.getIdentifierFirstRep().getValue());

        Type multipleBirth = patientResource.getMultipleBirth();
        if (multipleBirth != null) {
            try {
                patientDetails.setMultipleBirth((multipleBirth));
            } catch (ClassCastException cce) {
                throw OperationOutcomeFactory.buildOperationOutcomeException(
                        new UnprocessableEntityException("The multiple birth property is expected to be a boolean"),
                        SystemCode.INVALID_RESOURCE, IssueType.INVALID);
            }
        }

        DateTimeType deceased = (DateTimeType) patientResource.getDeceased();
        if (deceased != null) {
            try {
                patientDetails.setDeceased((deceased.getValue()));
            } catch (ClassCastException cce) {
                throw OperationOutcomeFactory.buildOperationOutcomeException(
                        new UnprocessableEntityException("The multiple deceased property is expected to be a datetime"),
                        SystemCode.INVALID_RESOURCE, IssueType.INVALID);
            }
        }

        patientDetails.setRegistrationStartDateTime(new Date());
        patientDetails.setRegistrationStatus(ACTIVE_REGISTRATION_STATUS);
        patientDetails.setRegistrationType(TEMPORARY_RESIDENT_REGISTRATION_TYPE);
        updateAddressAndTelecom(patientResource, patientDetails);

        return patientDetails;
    }

    // a cut-down Patient
    private Patient patientDetailsToRegisterPatientResourceConverter(PatientDetails patientDetails)
            throws FHIRException {
        Patient patient = patientDetailsToMinimalPatient(patientDetails);

        HumanName name = getPatientNameFromPatientDetails(patientDetails);

        patient.addName(name);

        patient = setStaticPatientData(patient);

        return patient;
    }

    private Patient setStaticPatientData(Patient patient) {

        patient.setLanguage(("en-GB"));

        patient.addExtension(createCodingExtension("CG", "Greek Cypriot", SystemURL.CS_CC_ETHNIC_CATEGORY_STU3,
                SystemURL.SD_CC_EXT_ETHNIC_CATEGORY));
        patient.addExtension(createCodingExtension("SomeSnomedCode", "Some Snomed Code",
                SystemURL.CS_CC_RELIGIOUS_AFFILI, SystemURL.SD_CC_EXT_RELIGIOUS_AFFILI));
        patient.addExtension(new Extension(SystemURL.SD_PATIENT_CADAVERIC_DON, new BooleanType(false)));
        patient.addExtension(createCodingExtension("H", "UK Resident", SystemURL.CS_CC_RESIDENTIAL_STATUS_STU3,
                SystemURL.SD_CC_EXT_RESIDENTIAL_STATUS));
        patient.addExtension(createCodingExtension("3", "To pay hotel fees only", SystemURL.CS_CC_TREATMENT_CAT_STU3,
                SystemURL.SD_CC_EXT_TREATMENT_CAT));

        Extension nhsCommExtension = new Extension();
        nhsCommExtension.setUrl(SystemURL.SD_CC_EXT_NHS_COMMUNICATION);
        nhsCommExtension.addExtension(
                createCodingExtension("en", "English", SystemURL.CS_CC_HUMAN_LANG_STU3, SystemURL.SD_CC_EXT_COMM_LANGUAGE));
        nhsCommExtension.addExtension(new Extension(SystemURL.SD_CC_COMM_PREFERRED, new BooleanType(false)));
        nhsCommExtension.addExtension(createCodingExtension("RWR", "Received written",
                SystemURL.CS_CC_LANG_ABILITY_MODE_STU3, SystemURL.SD_CC_MODE_OF_COMM));
        nhsCommExtension.addExtension(createCodingExtension("E", "Excellent", SystemURL.CS_CC_LANG_ABILITY_PROFI_STU3,
                SystemURL.SD_CC_COMM_PROFICIENCY));
        nhsCommExtension.addExtension(new Extension(SystemURL.SD_CC_INTERPRETER_REQUIRED, new BooleanType(false)));

        patient.addExtension(nhsCommExtension);

        Identifier localIdentifier = new Identifier();
        localIdentifier.setUse(IdentifierUse.USUAL);
        localIdentifier.setSystem(SystemURL.ID_LOCAL_PATIENT_IDENTIFIER);
        localIdentifier.setValue("123456");

        CodeableConcept liType = new CodeableConcept();
        Coding liTypeCoding = new Coding();
        liTypeCoding.setCode("EN");
        liTypeCoding.setDisplay("Employer number");
        liTypeCoding.setSystem(SystemURL.VS_IDENTIFIER_TYPE);
        liType.addCoding(liTypeCoding);
        localIdentifier.setType(liType);

        localIdentifier.setAssigner(new Reference("Organization/1"));
        patient.addIdentifier(localIdentifier);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, 1, 1);
        calendar.set(2016, 1, 1);
        Period pastPeriod = new Period().setStart(calendar.getTime()).setEnd(calendar.getTime());

        patient.addName()
                .setFamily("AnotherOfficialFamilyName")
                .addGiven("AnotherOfficialGivenName")
                .setUse(NameUse.OFFICIAL)
                .setPeriod(pastPeriod);

        patient.addName()
                .setFamily("AdditionalFamily")
                .addGiven("AdditionalGiven")
                .setUse(NameUse.TEMP);

        patient.addTelecom(staticElHelper.getValidTelecom());
        patient.addAddress(staticElHelper.getValidAddress());

        return patient;
    }

    private Extension createCodingExtension(String code, String display, String vsSystem, String extSystem) {

        Extension ext = new Extension(extSystem, createCoding(code, display, vsSystem));

        return ext;
    }

    private CodeableConcept createCoding(String code, String display, String vsSystem) {

        Coding coding = new Coding();
        coding.setCode(code);
        coding.setDisplay(display);
        coding.setSystem(vsSystem);
        CodeableConcept concept = new CodeableConcept();
        concept.addCoding(coding);

        return concept;
    }

    private Patient patientDetailsToMinimalPatient(PatientDetails patientDetails) throws FHIRException {
        Patient patient = new Patient();

        Date lastUpdated = patientDetails.getLastUpdated() == null 
                ? new Date() 
                : patientDetails.getLastUpdated();
        
        String resourceId = String.valueOf(patientDetails.getId());
        String versionId = String.valueOf(lastUpdated.getTime());
        String resourceType = patient.getResourceType().toString();
        
        IdType id = new IdType(resourceType, resourceId, versionId);

        patient.setId(id);
        patient.getMeta().setVersionId(versionId);
        patient.getMeta().setLastUpdated(lastUpdated);
        patient.getMeta().addProfile(SystemURL.SD_GPC_PATIENT);


        Identifier patientNhsNumber = new Identifier()
                .setSystem(SystemURL.ID_NHS_NUMBER)
                .setValue(patientDetails.getNhsNumber());
        
        Extension extension = createCodingExtension(
                "01", 
                "Number present and verified",
                SystemURL.CS_CC_NHS_NUMBER_VERIF_STU3,
                SystemURL.SD_CC_EXT_NHS_NUMBER_VERIF);
        
        patientNhsNumber.addExtension(extension);
        
        patient.addIdentifier(patientNhsNumber);

        patient.setBirthDate(patientDetails.getDateOfBirth());

        String gender = patientDetails.getGender();
        if (gender != null) {
            patient.setGender(AdministrativeGender.fromCode(gender.toLowerCase(Locale.UK)));
        }

        Date registrationEndDateTime = patientDetails.getRegistrationEndDateTime();
        Date registrationStartDateTime = patientDetails.getRegistrationStartDateTime();

        Extension regDetailsExtension = new Extension(SystemURL.SD_EXTENSION_CC_REG_DETAILS);

        Period registrationPeriod = new Period().setStart(registrationStartDateTime);
        if (registrationEndDateTime != null) {
        	registrationPeriod.setEnd(registrationEndDateTime);
        }

        Extension regPeriodExt = new Extension(SystemURL.SD_CC_EXT_REGISTRATION_PERIOD, registrationPeriod);
        regDetailsExtension.addExtension(regPeriodExt);

        String registrationStatusValue = patientDetails.getRegistrationStatus();
        patient.setActive(
                ACTIVE_REGISTRATION_STATUS.equals(registrationStatusValue) || null == registrationStatusValue);

        String registrationTypeValue = patientDetails.getRegistrationType();
        if (registrationTypeValue != null) {

            Coding regTypeCode = new Coding();
            regTypeCode.setCode(registrationTypeValue);
            regTypeCode.setDisplay("Temporary"); // Should always be Temporary
            regTypeCode.setSystem(SystemURL.CS_REGISTRATION_TYPE);
            CodeableConcept regTypeConcept = new CodeableConcept();
            regTypeConcept.addCoding(regTypeCode);

            Extension regTypeExt = new Extension(SystemURL.SD_CC_EXT_REGISTRATION_TYPE, regTypeConcept);
            regDetailsExtension.addExtension(regTypeExt);
        }

        patient.addExtension(regDetailsExtension);

        CodeableConcept marital = new CodeableConcept();
        Coding maritalCoding = new Coding();
        if (patientDetails.getMaritalStatus() != null) {
            maritalCoding.setSystem(SystemURL.CS_MARITAL_STATUS);
            maritalCoding.setCode(patientDetails.getMaritalStatus());
            maritalCoding.setDisplay("Married"); //TODO needs to actually match the marital code
        } else {
        	maritalCoding.setSystem(SystemURL.CS_NULL_FLAVOUR);
        	maritalCoding.setCode("UNK");
        	maritalCoding.setDisplay("unknown");
        }
        marital.addCoding(maritalCoding);
        patient.setMaritalStatus(marital);


        patient.setMultipleBirth(patientDetails.isMultipleBirth());

        if (patientDetails.isDeceased()) {
            DateTimeType decesed = new DateTimeType(patientDetails.getDeceased());
            patient.setDeceased(decesed);
        }

        return patient;
    }

    private Patient patientDetailsToPatientResourceConverter(PatientDetails patientDetails) throws FHIRException {
        Patient patient = patientDetailsToMinimalPatient(patientDetails);

        HumanName name = getPatientNameFromPatientDetails(patientDetails);

        patient.addName(name);

        String addressLines = patientDetails.getAddress();
        if (addressLines != null) {
            patient.addAddress().setUse(AddressUse.HOME).setType(AddressType.PHYSICAL).setText(addressLines);
        }

        Long gpId = patientDetails.getGpId();
        if (gpId != null) {
            Practitioner prac = practitionerResourceProvider.getPractitionerById(new IdType(gpId));
            HumanName practitionerName = prac.getNameFirstRep();
            Reference practitionerReference = new Reference("Practitioner/" + gpId)
                    .setDisplay(practitionerName.getPrefix().get(0) + " " + practitionerName.getGivenAsSingleString() + " "
                            + practitionerName.getFamily());
            patient.setGeneralPractitioner(Collections.singletonList(practitionerReference));
        }

        String telephoneNumber = patientDetails.getTelephone();
        if (telephoneNumber != null) {
            ContactPoint telephone = new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue(telephoneNumber)
                    .setUse(ContactPointUse.HOME);

            patient.setTelecom(Collections.singletonList(telephone));
        }

        String managingOrganization = patientDetails.getManagingOrganization();

        if (managingOrganization != null) {
            patient.setManagingOrganization(new Reference("Organization/" + managingOrganization));
        }

        return patient;
    }

    private HumanName getPatientNameFromPatientDetails(PatientDetails patientDetails) {
        HumanName name = new HumanName();

        name.setText(patientDetails.getName()).setFamily(patientDetails.getSurname())
            .addPrefix(patientDetails.getTitle())
            .setUse(NameUse.OFFICIAL);

        List<String> givenNames = patientDetails.getForenames();

        givenNames.forEach((givenName) -> {
            name.addGiven(givenName);
        });

        return name;
    }

    private class NhsNumber {

        private NhsNumber() {
            super();
        }

        private String getNhsNumber(Object source) {
            String nhsNumber = fromIdDt(source);

            if (nhsNumber == null) {
                nhsNumber = fromToken(source);

                if (nhsNumber == null) {
                    nhsNumber = fromParameters(source);

                    if (nhsNumber == null) {
                        nhsNumber = fromIdentifier(source);
                    }
                }
            }

            if (nhsNumber != null && !NhsCodeValidator.nhsNumberValid(nhsNumber)) {
                throw OperationOutcomeFactory.buildOperationOutcomeException(
                        new InvalidRequestException("Invalid NHS number submitted: " + nhsNumber),
                        SystemCode.INVALID_NHS_NUMBER, IssueType.INVALID);
            }

            return nhsNumber;
        }

        private String fromIdentifier(Object source) {
            String nhsNumber = null;

            if (source instanceof Identifier) {
                Identifier Identifier = (Identifier) source;

                String identifierSystem = Identifier.getSystem();
                if (identifierSystem != null && SystemURL.ID_NHS_NUMBER.equals(identifierSystem)) {
                    nhsNumber = Identifier.getValue();
                } else {
                    String message = String.format(
                            "The given identifier system code (%s) does not match the expected code - %s",
                            identifierSystem, SystemURL.ID_NHS_NUMBER);

                    throw OperationOutcomeFactory.buildOperationOutcomeException(new InvalidRequestException(message),
                            SystemCode.INVALID_IDENTIFIER_SYSTEM, IssueType.INVALID);
                }
            }

            return nhsNumber;
        }

        private String fromIdDt(Object source) {
            String nhsNumber = null;

            if (source instanceof IdDt) {
                IdDt idDt = (IdDt) source;

                PatientDetails patientDetails = patientSearch.findPatientByInternalID(idDt.getIdPart());
                if (patientDetails != null) {
                    nhsNumber = patientDetails.getNhsNumber();
                }
            }

            return nhsNumber;
        }

        private String fromToken(Object source) {
            String nhsNumber = null;

            if (source instanceof TokenParam) {
                TokenParam tokenParam = (TokenParam) source;

                if (!SystemURL.ID_NHS_NUMBER.equals(tokenParam.getSystem())) {
                    throw OperationOutcomeFactory.buildOperationOutcomeException(
                            new InvalidRequestException("Invalid system code"), SystemCode.INVALID_PARAMETER,
                            IssueType.INVALID);
                }

                nhsNumber = tokenParam.getValue();
            }

            return nhsNumber;
        }

        private String fromParameters(Object source) {
            String nhsNumber = null;

            if (source instanceof Parameters) {
                Parameters parameters = (Parameters) source;
                List<ParametersParameterComponent> params = new ArrayList<>();
                params.addAll(parameters.getParameter());

                ParametersParameterComponent parameter = getParameterByName(params, "patientNHSNumber");
                if (parameter != null) {
                    nhsNumber = fromIdentifier(parameter.getValue());
                } else {
                    parameter = getParameterByName(parameters.getParameter(), "registerPatient");
                    if (parameter != null) {
                        nhsNumber = fromPatientResource(parameter.getResource());
                    } else {
                        throw OperationOutcomeFactory.buildOperationOutcomeException(
                                new InvalidRequestException(
                                        "Unable to read parameters. Expecting one of patientNHSNumber or registerPatient both of which are case-sensitive"),
                                SystemCode.INVALID_PARAMETER, IssueType.INVALID);
                    }
                }
            }

            return nhsNumber;
        }

        private String fromPatientResource(Object source) {
            String nhsNumber = null;

            if (source instanceof Patient) {
                Patient patient = (Patient) source;

                nhsNumber = patient.getIdentifierFirstRep().getValue();
            }

            return nhsNumber;
        }

        private ParametersParameterComponent getParameterByName(List<ParametersParameterComponent> parameters,
                String parameterName) {
            ParametersParameterComponent parameter = null;

            List<ParametersParameterComponent> filteredParameters = parameters.stream()
                    .filter(currentParameter -> parameterName.equals(currentParameter.getName()))
                    .collect(Collectors.toList());

            if (filteredParameters != null) {
                if (filteredParameters.size() == 1) {
                    parameter = filteredParameters.iterator().next();
                } else if (filteredParameters.size() > 1) {
                    throw OperationOutcomeFactory.buildOperationOutcomeException(
                            new InvalidRequestException(
                                    "The parameter " + parameterName + " cannot be set more than once"),
                            SystemCode.BAD_REQUEST, IssueType.INVALID);
                }
            }

            return parameter;
        }
    }
}
