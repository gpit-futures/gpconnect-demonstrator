"use strict";

angular
    .module("gpConnect")
    .controller("AppointmentsStructuredCtrl", function(
        $scope,
        $http,
        PatientService,
        $stateParams
    ) {
        $scope.MedicationListList = [];
        $scope.AllMedications = [];
        $scope.IsVisible = false;
        $scope.ActiveAllergiesList = [];
        $scope.ResolvedAllergiesList = [];

        initGetMedicationData();

         function initGetMedicationData() {
             PatientService.medication($stateParams.patientId).then(function(
                 patientSummaryResponse
             ) {
                 for (
                     var i = 0; i < patientSummaryResponse.entry.length;i++) {
                     var resource = patientSummaryResponse.entry[i].resource;

                     if (resource.resourceType == "Medication") {
                         $scope.MedicationListList.push(resource);
                     }

                 }
             });
         }

        $scope.openDatePicker = function ($event, name) {
            $event.preventDefault();
            $event.stopPropagation();
            $scope.startDate = false;
            $scope.endDate = false;
            $scope[name] = true;
            highlightMonth();
        };

        var highlightMonth = function(){
            $(".text-muted").parent().addClass("otherMonth");
            $("button").unbind('click', highlightMonth);
            $("button").bind('click', highlightMonth);
        }




        $scope.getAllergyData = function() {
            {
                $scope.patient = {
                    name: "",
                    id: "",
                    birthDate: ""
                };
                $scope.practitioner = {
                    name: "",
                    id: ""
                };
                $scope.organization = {
                    name: "",
                    id: ""
                };


                $scope.allergyIntolerance = {
                    assertedDate: "",
                    category: "",
                    clinicalStatus: "",
                    patient: "",
                    reaction: "",
                    resolvedDate: ""
                };
                PatientService.structured($stateParams.patientId).then(function(
                    patientSummaryResponse
                ) {
                    for (
                        var i = 0;
                        i < patientSummaryResponse.entry.length;
                        i++
                    ) {
                        var resource = patientSummaryResponse.entry[i].resource;

                        if (resource.resourceType == "Patient") {
                            $scope.patient = {
                                name: resource.name[0].text,
                                id: resource.id,
                                birthDate: resource.birthDate
                            };
                        } else if (
                            patientSummaryResponse.entry[i].resource
                                .resourceType == "Organization"
                        ) {
                            $scope.organization = {
                                name: resource.name,
                                id: resource.id
                            };
                        } else if (resource.resourceType == "Practitioner") {
                            $scope.practitioner = {
                                name: resource.name,
                                id: resource.id
                            };
                        } else if (
                            resource.resourceType == "AllergyIntolerance"
                        ) {
                            if (resource.clinicalStatus == "active") {
                                $scope.ActiveAllergiesList.push(resource);
                            } else if(resource.clinicalStatus == "resolved") {
                                $scope.ResolvedAllergiesList.push(resource);
                            }


                        }
                    }
                });
            }
        };

        $scope.showHIde = function() {
            $scope.IsVisible = true;
            PatientService.allMedications().then(function (mediacations) {
                for(var i =0; i<mediacations.length;i++) {
                    $scope.AllMedications.push(mediacations[i]);
                }
            });
        }
    });

