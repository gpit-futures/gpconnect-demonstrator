'use strict';

angular.module('gpConnect')
        .controller('AppointmentsCtrl', ['$scope', '$http', '$stateParams', '$state', '$modal', '$q', 'PatientService', 'usSpinnerService', 'Appointment', 'ProviderRouting', 'gpcResource', function ($scope, $http, $stateParams, $state, $modal, $q, PatientService, usSpinnerService, Appointment, ProviderRouting, gpcResource) {

			$scope.appointment = {};

            $scope.currentPage = 1;

            $scope.pageChangeHandler = function (newPage) {
                $scope.currentPage = newPage;
            };

            if ($stateParams.page) {
                $scope.currentPage = $stateParams.page;
            }

            if ($stateParams.filter) {
                $scope.query = $stateParams.filter;
            }
            
            $scope.openDatePicker = function ($event, name) {
      			$event.preventDefault();
      			$event.stopPropagation();
     			$scope.geStartDate = false;
     			$scope.leStartDate = false;
      			$scope[name] = true;
    		};
            
            $scope.initaliseDates = function() {
            	var today = new Date();
            	var lower = new Date();
            	var upper = new Date();
            	$scope.appointment.geStartDate = lower.setDate(today.getDate());
            	$scope.appointment.leStartDate = upper.setDate(today.getDate() + 14);
        		$scope.appointment.minGeStartDate = new Date();
        		$scope.appointment.minLeStartDate = false;
        		$scope.appointment.geStartDateInvalid = false;
        		$scope.appointment.leStartDateInvalid = false;
    		};
    		$scope.initaliseDates();
    		
    		$scope.onGeStartDate = function() {
    			var geStartDate = $scope.appointment.geStartDate;
    			$scope.appointment.minLeStartDate = geStartDate;
    		};
    
    		$scope.onLeStartDate = function() {
    			var leStartDate = $scope.appointment.leStartDate;
    		};
  
    		$scope.$watch("geStartDate", function(newValue, oldValue) {
        		$scope.onGeStartDate();
    		}); 
    
    		$scope.$watch("leStartDate", function(newValue, oldValue) {
        		$scope.onLeStartDate();
    		});

            initAppointments();

            function initAppointments() {
                $scope.appointments = [];
                $scope.allPractices = ProviderRouting.practices;
                $.each($scope.allPractices, function (key, practice) {
                    if ($scope.searchCount == undefined) {
                        $scope.searchCount = 1;
                    } else {
                        $scope.searchCount++;
                    }
                    practice.statusMsg = "Searching";
                    practice.status = "Searching";
                    PatientService.getFhirPatient(practice.odsCode, $stateParams.patientId).then(
                            function (patient) {
                                if (patient == undefined) {
                                    practice.statusMsg = "Failed to find patient";
                                    practice.status = "Failed";
                                    onFindAppointmentDone();
                                } else {
                                    $scope.fhirPatient = patient;
                                    Appointment.findAllAppointments($stateParams.patientId, patient.id, practice.odsCode, $scope.appointment.geStartDate, $scope.appointment.leStartDate).then(
                                            function (findAllAppointmentsSuccess) {
                                                var appointments = findAllAppointmentsSuccess.data.entry;
                                                if (appointments != undefined) {
                                                    $.each(appointments, function (key, appointment) {
                                                        appointment.appointmentPractice = practice.name;
                                                        appointment.appointmentPracticeOdsCode = practice.odsCode;
                                                    });
                                                    $scope.appointments = $scope.appointments.concat(appointments); // Add appointments to total list
                                                    practice.statusMsg = "Appointments found";
                                                    practice.status = "Success";
                                                    onFindAppointmentDone();
                                                } else {
                                                    practice.statusMsg = "No appointments found";
                                                    practice.status = "Success";
                                                    onFindAppointmentDone();
                                                }
                                            },
                                            function (findAllAppointmentsFailure) {
                                                practice.statusMsg = "Failed appointment search";
                                                practice.status = "Failed";
                                                onFindAppointmentDone();
                                            }
                                    );
                                }
                            },
                            function (getPatientFhirIdFailure) {
                                practice.statusMsg = "Failed to connect";
                                practice.status = "Failed";
                                onFindAppointmentDone();
                            }
                    );
                });
            }

            function onFindAppointmentDone() {
                $scope.searchCount--;
                if ($scope.searchCount <= 0) {
                    if ($scope.appointments != undefined) {
                        $scope.appointments = $scope.appointments.sort(function (a, b) {
                            return a.resource.start.localeCompare(b.resource.start);
                        });
                        $.each($scope.appointments, function (key, appointment) {
                            if (appointment.resource.extension != undefined) {
                                for (var i = 0; i < appointment.resource.extension.length; i++) {
                                    if (gpcResource.getConst("SD_EXT_GPC_APPOINT_CANC_REAS") == appointment.resource.extension[i].url) {
                                        appointment.cancellationReason = appointment.resource.extension[i].valueId;
                                        i = appointment.resource.extension.length;
                                    }
                                }
                            }
                        });
                    }
                    usSpinnerService.stop('patientSummary-spinner');
                }
            }

            $scope.go = function (id, practice) {
                usSpinnerService.spin('patientSummary-spinner');
                $scope.appointmentDetail = undefined;
                $scope.appointmentLocation = undefined;
                $scope.practitionerName = undefined;

                var appointment;
                for (var index = 0; index < $scope.appointments.length; ++index) {
                    appointment = $scope.appointments[index];
                    if (appointment.resource.id == id && appointment.appointmentPractice == practice) {
                        $scope.appointmentDetail = appointment;
                        break;
                    }
                }

                $.each($scope.appointmentDetail.resource.participant, function (key, value) {
                    var reference = value.actor.reference.toString();
                    if (reference.indexOf("Location") != -1) {
                        $scope.appointmentLocation = "[Searching...]";
                        Appointment.findResourceByReference($scope.appointmentDetail.appointmentPracticeOdsCode, $stateParams.patientId, reference).then(function (response) {
                            if(response.data == undefined || response.data == null){
                                $scope.appointmentLocation = "[Lookup failed]";
                            } else {
                            	var addressStr = response.data.name;
                            	if (response.data.address != undefined) {
		                        	if (response.data.address.line != undefined) {
		                                addressStr += ", " + response.data.address.line;
		                            }
		                            if (response.data.address.city != undefined) {
		                                addressStr += ", " + response.data.address.city;
		                            }
		                            if (response.data.address.district != undefined) {
		                                addressStr += ", " + response.data.address.district;
		                            }
		                            if (response.data.address.state != undefined) {
		                                addressStr += ", " + response.data.address.state;
		                            }
		                            if (response.data.address.postalCode != undefined) {
		                                addressStr += ", " + response.data.address.postalCode;
		                            }
		                            if (response.data.address.country != undefined) {
		                                addressStr += ", " + response.data.address.country;
		                            }
	                            }
                                $scope.appointmentLocation = addressStr;
                            }
                        },
                        function (failedResponse) {
                            $scope.appointmentLocation = "[Lookup failed]";
                        });
                    } else if (reference.indexOf("Practitioner") != -1) {
                        $scope.practitionerName = "[Searching...]";
                        Appointment.findResourceByReference($scope.appointmentDetail.appointmentPracticeOdsCode, $stateParams.patientId, reference).then(function (response) {
                            if(response.data == undefined || response.data == null){
                                $scope.practitionerName = "[Lookup failed]";
                            } else {
                                $scope.practitionerName = response.data.name[0].prefix[0] + " " + response.data.name[0].given[0] + " " + response.data.name[0].family;
                            }
                        },
                        function (failedResponse) {
                            $scope.practitionerName = "[Lookup failed]";
                        });
                    }

                });

                usSpinnerService.stop('patientSummary-spinner');
            };

            $scope.selected = function (appointmentIndex) {
                return appointmentIndex === $stateParams.appointmentIndex;
            };

            $scope.cancelAppointment = function () {
                $modal.open({
                    templateUrl: 'views/appointments/appointments-cancel-modal.html',
                    size: 'md',
                    controller: 'AppointmentsCancelModalCtrl',
                    resolve: {
                        appointment: function () {
                            return {
                                patient: $scope.fhirPatient,
                                appointmentResource: $scope.appointmentDetail,
                                practitionerName: $scope.practitionerName,
                                appointmentLocation: $scope.appointmentLocation
                            };
                        }
                    }
                });
            };

            $scope.amendAppointment = function () {
                $modal.open({
                    templateUrl: 'views/appointments/appointments-amend-modal.html',
                    size: 'md',
                    controller: 'AppointmentsAmendModalCtrl',
                    resolve: {
                        appointment: function () {
                            return {
                                patient: $scope.fhirPatient,
                                appointmentResource: $scope.appointmentDetail,
                                practitionerName: $scope.practitionerName,
                                appointmentLocation: $scope.appointmentLocation
                            };
                        }
                    }
                });
            };

            $scope.create = function () {
                $modal.open({
                    templateUrl: 'views/appointments/appointments-search-modal.html',
                    size: 'md',
                    controller: 'AppointmentsSearchModalCtrl',
                    resolve: {
                        modal: function () {
                            return {
                                title: 'Appointment Search'
                            };
                        }
                    }
                });
            };

            $scope.closeAppointmentDetail = function(){
                $scope.appointmentDetail = undefined;
                $scope.appointmentLocation = undefined;
                $scope.practitionerName = undefined;
            };
            
            $scope.searchAppointments = function () {
            	$scope.appointment.geStartDateInvalid = false;
				$scope.appointment.leStartDateInvalid = false;
	            
	            var startDate = $scope.appointment.geStartDate;
	            var endDate = $scope.appointment.leStartDate;
	    		
	    		if (!startDate || moment(startDate).diff(new Date(), 'days') < 0) {
	    			$scope.appointment.geStartDateInvalid = true;
	    		} 
	    		else if (!endDate || moment(endDate).diff(startDate, 'days') < 0) 
	    		{
	    			$scope.appointment.leStartDateInvalid = true;
	    		} 
				else
				{
					$scope.closeAppointmentDetail();
	            	initAppointments();
	            }
            };

        }]);
