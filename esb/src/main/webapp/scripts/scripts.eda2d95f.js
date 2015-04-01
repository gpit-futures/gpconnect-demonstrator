"use strict";angular.module("openehrPocApp",["ngResource","ngTouch","ngAnimate","ui.router","ui.bootstrap","angular-loading-bar","growlNotifications","angularUtils.directives.dirPagination"]).config(["$stateProvider","$urlRouterProvider",function(a,b){b.otherwise("/"),a.state("patients-list",{url:"/patients?ageRange&department&order&reverse",views:{actions:{templateUrl:"views/home-sidebar.html"},main:{templateUrl:"views/patients/patients-list.html",controller:"PatientsListCtrl"}}}).state("patients-charts",{url:"/",views:{actions:{templateUrl:"views/home-sidebar.html"},main:{templateUrl:"views/patients/patients-charts.html",controller:"PatientsChartsCtrl"}}}).state("patients-lookup",{url:"/lookup",views:{actions:{templateUrl:"views/home-sidebar.html"},main:{controller:"PatientsLookupCtrl"}}}).state("diagnoses-list",{url:"/patients/{patientId:int}/diagnoses",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/diagnoses/diagnoses-list.html",controller:"DiagnosesListCtrl"}}}).state("diagnoses-detail",{url:"/patients/{patientId:int}/diagnoses/{diagnosisIndex:int}",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/diagnoses/diagnoses-list.html",controller:"DiagnosesListCtrl"},detail:{templateUrl:"views/diagnoses/diagnoses-detail.html",controller:"DiagnosesDetailCtrl"}}}).state("allergies",{url:"/patients/{patientId:int}/allergies",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/allergies/allergies-list.html",controller:"AllergiesListCtrl"}}}).state("allergies-detail",{url:"/patients/{patientId:int}/allergies/{allergyIndex:int}",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/allergies/allergies-list.html",controller:"AllergiesListCtrl"},detail:{templateUrl:"views/allergies/allergies-detail.html",controller:"AllergiesDetailCtrl"}}}).state("medications",{url:"/patients/{patientId:int}/medications",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/medications/medications-list.html",controller:"MedicationsListCtrl"}}}).state("medications-detail",{url:"/patients/{patientId:int}/medications/{medicationIndex:int}",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/medications/medications-list.html",controller:"MedicationsListCtrl"},detail:{templateUrl:"views/medications/medications-detail.html",controller:"MedicationsDetailCtrl"}}}).state("contacts",{url:"/patients/{patientId:int}/contacts",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/contacts/contacts-list.html",controller:"ContactsListCtrl"}}}).state("contacts-detail",{url:"/patients/{patientId:int}/contacts/{contactIndex:int}",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/contacts/contacts-list.html",controller:"ContactsListCtrl"},detail:{templateUrl:"views/contacts/contacts-detail.html",controller:"ContactsDetailCtrl"}}}).state("transferOfCare",{url:"/patients/{patientId:int}/transfer-of-care-list",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/transfer-of-care/transfer-of-care-list.html",controller:"TransferOfCareListCtrl"}}}).state("transferOfCare-detail",{url:"/patients/{patientId:int}/transfer-of-care-detail/{transferOfCareIndex:int}",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{templateUrl:"views/transfer-of-care/transfer-of-care-list.html",controller:"TransferOfCareListCtrl"},detail:{templateUrl:"views/transfer-of-care/transfer-of-care-detail.html",controller:"TransferOfCareDetailCtrl"}}}).state("transferOfCare-create",{url:"/patients/{patientId:int}/transfer-of-care-create",views:{"user-context":{templateUrl:"views/patients/patients-context.html",controller:"PatientsDetailCtrl"},actions:{templateUrl:"views/patients/patients-sidebar.html",controller:"PatientsDetailCtrl"},main:{controller:"TransferOfCareCtrl"}}})}]).config(["datepickerConfig","datepickerPopupConfig","cfpLoadingBarProvider",function(a,b,c){a.startingDay=1,b.showButtonBar=!1,b.datepickerPopup="dd-MMM-yyyy",c.includeSpinner=!1}]).config(["paginationTemplateProvider",function(a){a.setPath("views/dirPagination.tpl.html")}]),angular.module("openehrPocApp").factory("PatientService",["$http","Patient",function(a,b){var c=function(){return a.get("/api/patients").then(function(a){var c=[];return angular.forEach(a.data,function(a){a=new b(a),c.push(a)}),c})},d=function(c){return a.get("/api/patients/"+c,{cache:!0}).then(function(a){return new b(a.data)})},e=function(b){var c={address1:b.address1,address2:b.address2,address3:b.address3,address4:b.address4,address5:b.address5,born:b.born,department:b.department,firstname:b.firstname,gPPractice:b.gPPractice,gender:b.gender,id:b.id,lastname:b.lastname,nhsNo:b.nhsNo,phone:b.phone,postCode:b.postCode,title:b.title};return a.put("/api/patients",c)},f=function(a,b){var c=_.findWhere(a.diagnoses,{id:b.id});angular.extend(c,b)},g=function(){return c().then(function(a){var b={};return b.age=_.chain(a).filter(function(a){return!!a.age}).countBy(function(a){return a.ageRange}).map(function(a,b){return{series:b,value:a}}).sortBy(function(a){return a.ageRange}).reverse().value(),b.department=_.chain(a).filter(function(a){return!!a.department}).countBy(function(a){return a.department}).map(function(a,b){return{series:b,value:a}}).sortBy(function(a){return a.department}).value(),b})};return{all:c,get:d,update:f,summaries:g,create:e}}]),angular.module("openehrPocApp").factory("Diagnosis",["$http",function(a){var b=9999999e3,c=function(c){return a.get("/api/patients/"+(b||c)+"/diagnoses")},d=function(c,d){return a.put("/api/patients/"+(b||c)+"/diagnoses",d)};return{all:c,update:d}}]),angular.module("openehrPocApp").factory("Allergy",["$http",function(a){var b=9999999e3,c=function(c){return a.get("/api/patients/"+(b||c)+"/allergies")},d=function(c,d){return a.put("/api/patients/"+(b||c)+"/allergies",d)};return{all:c,update:d}}]),angular.module("openehrPocApp").factory("Medication",["$http",function(a){var b=9999999e3,c=function(c){return a.get("/api/patients/"+(b||c)+"/medications")},d=function(c,d){return a.put("/api/patients/"+(b||c)+"/medications",d)};return{all:c,update:d}}]),angular.module("openehrPocApp").factory("Contact",["$http",function(a){var b=9999999e3,c=function(c){return a.get("/api/patients/"+(b||c)+"/contacts")},d=function(c,d){return a.put("/api/patients/"+(b||c)+"/contacts",d)};return{all:c,update:d}}]),angular.module("openehrPocApp").factory("SmspLookup",["$http","Patient",function(a,b){var c=function(c,d){return a.post("/api/smsp-search",{firstname:c,lastname:d}).then(function(a){var c=[];return angular.forEach(a.data,function(a){a=new b(a),c.push(a)}),c})};return{byName:c}}]),angular.module("openehrPocApp").factory("TransferOfCare",["$http",function(a){var b=9999999e3,c=function(c){return a.get("/api/patients/"+(b||c)+"/transfer-of-care/summary")},d=function(c){return a.get("/api/patients/"+(b||c)+"/transfer-of-care/")},e=function(c,d){return console.log("transferOfCareComposition: "),console.log(d),a.post("/api/patients/"+(b||c)+"/transfer-of-care",d)},f=function(c,d){return console.log("transferOfCareComposition: "),console.log(d),a.put("/api/patients/"+(b||c)+"/transfer-of-care",d)};return{get:c,getComposition:d,create:e,update:f}}]),angular.module("openehrPocApp").factory("Patient",["$window",function(a){var b=function(b){var c=this;_.extend(this,b),c.fullname=function(){return c.lastname.toUpperCase()+", "+c.firstname+(c.title?" ("+c.title+")":"")},c.address=function(){return[c.address1,c.address2,c.address3,c.address4,c.address5,c.postCode].join(", ")}(),c.age=function(){return a.moment().diff(c.born,"years")},c.ageRange=function(){var a=c.age();switch(!0){case a>=0&&10>=a:return"0-10";case a>=11&&18>=a:return"11-18";case a>=19&&30>=a:return"19-30";case a>=31&&60>=a:return"31-60";case a>=60&&80>=a:return"60-80";case a>80:return">80";default:return}}()};return b}]),angular.module("openehrPocApp").controller("PatientsListCtrl",["$scope","$state","$stateParams","PatientService",function(a,b,c,d){d.all().then(function(b){a.patients=b}),a.order=c.order||"lastname",a.reverse="true"===c.reverse,a.filters={department:c.department,ageRange:c.ageRange},a.sort=function(d){var e=a.reverse;a.order===d&&(e=!e),b.transitionTo(b.current,_.extend(c,{order:d,reverse:e}))},a.sortClass=function(b){return a.order===b?a.reverse?"sort-desc":"sort-asc":void 0},a.go=function(a){b.go("diagnoses-list",{patientId:a.id})},a.patientFilter=function(b){return a.filters.department?b.department===a.filters.department:a.filters.ageRange?b.ageRange===a.filters.ageRange:!0}}]),angular.module("openehrPocApp").controller("PatientsChartsCtrl",["$scope","$window","$state","PatientService",function(a,b,c,d){d.summaries().then(function(a){e(a),f(a)});var e=function(a){b.Morris.Bar({element:"age-chart",data:a.age,ykeys:["value"],xkey:"series",labels:["Patients"],hideHover:!0,ymin:0,ymax:40}).on("click",function(a,b){c.go("patients-list",{ageRange:b.series})})},f=function(a){b.Morris.Bar({element:"department-chart",data:a.department,ykeys:["value"],xkey:"series",labels:["Patients"],hideHover:!0,barColors:["#0BA462"],ymin:0,ymax:40}).on("click",function(a,b){"All"===b.series&&(b.series=null),c.go("patients-list",{department:b.series})})}}]),angular.module("openehrPocApp").controller("PatientsLookupCtrl",["$modal","$state",function(a,b){a.open({templateUrl:"views/patients/patients-lookup.html",size:"lg",controller:["$scope","SmspLookup","PatientService",function(a,b,c){a.search=function(c,d){a.searching=!0,b.byName(c,d).then(function(b){a.patients=b,a.searching=!1},function(){a.searching=!1})},a.create=function(b){c.create(b).then(function(){a.save()})},a.dismiss=function(){a.$dismiss()},a.save=function(){a.$close(!0)}}]}).result.finally(function(){b.go("patients-list")})}]),angular.module("openehrPocApp").controller("PatientsDetailCtrl",["$scope","$stateParams","PatientService",function(a,b,c){c.get(b.patientId).then(function(b){a.patient=b})}]),angular.module("openehrPocApp").controller("DiagnosesListCtrl",["$scope","$stateParams","$location","$modal","PatientService","Diagnosis",function(a,b,c,d,e,f){e.get(b.patientId).then(function(b){a.patient=b}),f.all(b.patientId).then(function(b){a.result=b.data,a.diagnoses=a.result.problems}),a.go=function(a){c.path(a)},a.selected=function(a){return a===b.diagnosisIndex},a.create=function(){var b=d.open({templateUrl:"views/diagnoses/diagnoses-modal.html",size:"lg",controller:"DiagnosesModalCtrl",resolve:{modal:function(){return{title:"Create Diagnosis"}},diagnosis:function(){return{}},patient:function(){return a.patient}}});b.result.then(function(b){a.result.problems.push(b),f.update(a.patient.id,a.result).then(function(b){a.patient.diagnoses.push(b.data)})})}}]),angular.module("openehrPocApp").controller("DiagnosesDetailCtrl",["$scope","$stateParams","$location","$modal","PatientService","Diagnosis",function(a,b,c,d,e,f){a.UnlockedSources=["handi.ehrscape.com"],e.get(b.patientId).then(function(b){a.patient=b}),f.all(b.patientId).then(function(c){a.result=c.data,a.diagnosis=a.result.problems[b.diagnosisIndex]}),a.formDisabled=!0,a.edit=function(){var e=d.open({templateUrl:"views/diagnoses/diagnoses-modal.html",size:"lg",controller:"DiagnosesModalCtrl",resolve:{modal:function(){return{title:"Edit Problem"}},diagnosis:function(){return angular.copy(a.diagnosis)},patient:function(){return a.patient}}});e.result.then(function(d){a.result.problems[b.diagnosisIndex]=d,f.update(a.patient.id,a.result).then(function(d){a.diagnoses=d.data,c.path("/patients/"+a.patient.id+"/contacts/"+b.diagnosisIndex)})})},a.isLocked=function(b){if(!b||!b.id)return!0;var c=b.id.toString().split("::");return c.length>1?a.UnlockedSources.indexOf(c[1])<0:!0},a.convertToLabel=function(a){var b=a.replace(/([A-Z])/g," $1");return b.charAt(0).toUpperCase()+b.slice(1)}}]),angular.module("openehrPocApp").controller("DiagnosesModalCtrl",["$scope","$modalInstance","diagnosis","patient","modal",function(a,b,c,d,e){a.diagnosis=c,a.patient=d,a.modal=e,a.ok=function(c,d){a.formSubmitted=!0,c.$valid&&b.close(d)},a.cancel=function(){b.dismiss("cancel")},a.openDatepicker=function(b,c){b.preventDefault(),b.stopPropagation(),a[c]=!0}}]),angular.module("openehrPocApp").controller("AllergiesListCtrl",["$scope","$location","$stateParams","$modal","PatientService","Allergy",function(a,b,c,d,e,f){e.get(c.patientId).then(function(b){a.patient=b}),f.all(c.patientId).then(function(b){a.result=b.data,a.allergies=a.result.allergies}),a.go=function(a){b.path(a)},a.selected=function(a){return a===c.allergyIndex},a.create=function(){var b=d.open({templateUrl:"views/allergies/allergies-modal.html",size:"lg",controller:"AllergiesModalCtrl",resolve:{modal:function(){return{title:"Create Allergy"}},allergy:function(){return{}},patient:function(){return a.patient}}});b.result.then(function(b){a.result.allergies.push(b),f.update(a.patient.id,a.result).then(function(b){a.patient.allergies.push(b.data)})})}}]),angular.module("openehrPocApp").controller("AllergiesDetailCtrl",["$scope","$stateParams","$modal","$location","PatientService","Allergy",function(a,b,c,d,e,f){e.get(b.patientId).then(function(b){a.patient=b}),f.all(b.patientId).then(function(c){a.result=c.data,a.allergy=a.result.allergies[b.allergyIndex]}),a.edit=function(){var e=c.open({templateUrl:"views/allergies/allergies-modal.html",size:"lg",controller:"AllergiesModalCtrl",resolve:{modal:function(){return{title:"Edit Allergy"}},allergy:function(){return angular.copy(a.allergy)},patient:function(){return a.patient}}});e.result.then(function(c){a.result.allergies[b.allergyIndex]=c,f.update(a.patient.id,a.result).then(function(c){a.result=c.data,d.path("/patients/"+a.patient.id+"/allergies/"+b.allergyIndex)})})}}]),angular.module("openehrPocApp").controller("AllergiesModalCtrl",["$scope","$modalInstance","allergy","patient","modal",function(a,b,c,d,e){a.allergy=c,a.patient=d,a.modal=e,a.ok=function(c,d){a.formSubmitted=!0,c.$valid&&b.close(d)},a.cancel=function(){b.dismiss("cancel")},a.openDatepicker=function(b,c){b.preventDefault(),b.stopPropagation(),a[c]=!0}}]),angular.module("openehrPocApp").controller("MedicationsListCtrl",["$scope","$location","$stateParams","$modal","PatientService","Medication",function(a,b,c,d,e,f){e.get(c.patientId).then(function(b){a.patient=b}),f.all(c.patientId).then(function(b){a.result=b.data,a.medications=a.result.medications}),a.go=function(a){b.path(a)},a.selected=function(a){return a===c.medicationIndex},a.create=function(){var b=d.open({templateUrl:"views/medications/medications-modal.html",size:"lg",controller:"MedicationsModalCtrl",resolve:{modal:function(){return{title:"Create Medication"}},medication:function(){return{}},patient:function(){return a.patient}}});b.result.then(function(b){a.result.medications.push(b),f.update(a.patient.id,a.result).then(function(b){a.patient.medications.push(b.data)})})}}]),angular.module("openehrPocApp").controller("MedicationsDetailCtrl",["$scope","$stateParams","$modal","$location","PatientService","Medication",function(a,b,c,d,e,f){e.get(b.patientId).then(function(b){a.patient=b}),f.all(b.medicationIndex).then(function(c){a.result=c.data,a.medication=a.result.medications[b.medicationIndex]}),a.edit=function(){var e=c.open({templateUrl:"views/medications/medications-modal.html",size:"lg",controller:"MedicationsModalCtrl",resolve:{modal:function(){return{title:"Edit Medication"}},medication:function(){return angular.copy(a.medication)},patient:function(){return a.patient}}});e.result.then(function(c){a.result.medications[b.medicationIndex]=c,f.update(a.patient.id,a.result).then(function(b){a.medication=b.data,d.path("/patients/"+a.patient.id+"/medications/"+a.medication.id)})})}}]),angular.module("openehrPocApp").controller("MedicationsModalCtrl",["$scope","$modalInstance","medication","patient","modal",function(a,b,c,d,e){a.medication=c,a.patient=d,a.modal=e,a.ok=function(c,d){a.formSubmitted=!0,c.$valid&&b.close(d)},a.cancel=function(){b.dismiss("cancel")},a.openDatepicker=function(b,c){b.preventDefault(),b.stopPropagation(),a[c]=!0}}]),angular.module("openehrPocApp").controller("ContactsListCtrl",["$scope","$location","$stateParams","$modal","PatientService","Contact",function(a,b,c,d,e,f){e.get(c.patientId).then(function(b){a.patient=b}),f.all(c.patientId).then(function(b){a.result=b.data,a.contacts=a.result.contacts}),a.go=function(a){b.path(a)},a.selected=function(a){return a===c.allergyIndex},a.create=function(){var b=d.open({templateUrl:"views/contacts/contacts-modal.html",size:"lg",controller:"ContactsModalCtrl",resolve:{modal:function(){return{title:"Create Contact"}},contact:function(){return{}},patient:function(){return a.patient}}});b.result.then(function(b){a.result.contacts.push(b),f.update(a.patient.id,a.result).then(function(b){a.patient.contacts.push(b.data)})})}}]),angular.module("openehrPocApp").controller("ContactsDetailCtrl",["$scope","$stateParams","$modal","$location","PatientService","Contact",function(a,b,c,d,e,f){e.get(b.patientId).then(function(b){a.patient=b}),f.all(b.contactId).then(function(c){a.result=c.data,a.contact=a.result.contacts[b.contactIndex]}),a.edit=function(){var e=c.open({templateUrl:"views/contacts/contacts-modal.html",size:"lg",controller:"ContactsModalCtrl",resolve:{modal:function(){return{title:"Edit Contact"}},contact:function(){return angular.copy(a.contact)},patient:function(){return a.patient}}});e.result.then(function(c){a.result.contacts[b.contactIndex]=c,f.update(a.patient.id,a.result).then(function(c){a.contact=c.data,d.path("/patients/"+a.patient.id+"/contacts/"+b.contactIndex)})})}}]),angular.module("openehrPocApp").controller("ContactsModalCtrl",["$scope","$modalInstance","contact","patient","modal",function(a,b,c,d,e){a.contact=c,a.patient=d,a.modal=e,a.ok=function(c,d){a.formSubmitted=!0,c.$valid&&b.close(d)},a.cancel=function(){b.dismiss("cancel")},a.openDatepicker=function(b,c){b.preventDefault(),b.stopPropagation(),a[c]=!0}}]),angular.module("openehrPocApp").controller("TransferOfCareCtrl",["$modal","$state","$scope","$stateParams","PatientService","TransferOfCare",function(a,b,c,d,e,f){function g(){a.open({templateUrl:"views/transfer-of-care/transfer-of-care-confirmation.html",size:"md",controller:["$scope",function(a){a.ok=function(){a.$close()}}]}).result.finally(function(){b.go("transferOfCare",{patientId:c.patient.id})})}a.open({templateUrl:"views/transfer-of-care/transfer-of-care-modal.html",size:"lg",resolve:{patient:function(){return e.get(d.patientId).then(function(a){return c.patient=a,c.patient})},transferOfCare:function(){return f.get(d.patientId).then(function(a){return c.transferOfCare=a.data,c.transferOfCare})},transferOfCareCompositions:function(){return f.getComposition(d.patientId).then(function(a){return c.transferOfCareCompositions=a.data,c.transferOfCareCompositions})}},controller:["$scope","patient","transferOfCare","transferOfCareCompositions",function(a,c,d,e){function g(){var b=jQuery.extend(!0,{},a.transferOfCare.allergies,a.transferOfCare.contacts,a.transferOfCare.medication,a.transferOfCare.problems);delete b.compositionId;for(var c in a.selectedItems)switch(c){case"allergies":case"contacts":case"medications":case"problems":for(var d=b[c].length;d--;){var e=!1;angular.forEach(a.selectedItems[c],function(a){var b=a;d===b&&(e=!0)}),e===!1&&b[c].splice(d,1)}}return b}a.patient=c,a.transferOfCare=d,a.allergies=a.transferOfCare.allergies.allergies,a.contacts=a.transferOfCare.contacts.contacts,a.problems=a.transferOfCare.problems.problems,a.medications=a.transferOfCare.medication.medications,a.transferOfCareComposition=e,a.siteFrom=a.transferOfCareComposition.transfers[0].transferDetail.site.siteFrom,a.selectedItems={allergies:[],contacts:[],medications:[],problems:[]},a.transferDetail={site:{}},a.selectTransferOfCareItem=function(b,c){-1!==a.selectedItems[c].indexOf(b)?a.selectedItems[c].splice(a.selectedItems[c].indexOf(b),1):a.selectedItems[c].push(b)},a.isItemSelected=function(b,c){return-1!==a.selectedItems[c].indexOf(b)?"transfer-of-care-green":"transfer-of-care-red"},a.isItemSelectedIcon=function(b,c){return-1!==a.selectedItems[c].indexOf(b)?"glyphicon glyphicon-ok":"glyphicon glyphicon-remove"},a.getSelectedItItemsForSummary=function(b){a.formSubmitted=!0,b.$valid&&(a.selectedItemsForSummary=g(),a.selectedAllergies=a.selectedItemsForSummary.allergies,a.selectedContacts=a.selectedItemsForSummary.contacts,a.selectedProblems=a.selectedItemsForSummary.problems,a.selectedMedications=a.selectedItemsForSummary.medications,a.transferDetail.reasonForContact=a.details.reasonForContact?a.details.reasonForContact:"No reason specified",a.transferDetail.clinicalSummary=a.details.clinicalSummary?a.details.clinicalSummary:"No clinical summary",a.transferDetail.site.siteFrom=a.siteFrom?a.siteFrom:"No site from",a.transferDetail.site.siteTo=a.details.siteTo?a.details.siteTo:"No site to",a.transferDetail.site.patientId=9999999e3,a.toggleDetailView())},a.displayDetail=!1,a.toggleDetailView=function(){a.displayDetail=!a.displayDetail},a.dismiss=function(){a.$dismiss(),b.go("transferOfCare",{patientId:a.patient.id})},a.ok=function(){a.transferOfCare=g(),a.transferOfCare.medication=a.transferOfCare.medications,delete a.transferOfCare.medications,a.transferOfCare.transferDetail=a.transferDetail,a.transferOfCareComposition.transfers.push(a.transferOfCare),f.create(a.patient.id,a.transferOfCareComposition).then(function(){a.$close()}),a.$close()}}]}).result.then(function(){g()})}]),angular.module("openehrPocApp").controller("TransferOfCareListCtrl",["$scope","$location","$stateParams","$modal","$state","PatientService","TransferOfCare",function(a,b,c,d,e,f,g){f.get(c.patientId).then(function(b){a.patient=b}),g.getComposition(c.patientId).then(function(b){a.transferofCareComposition=b.data,console.log(a.transferofCareComposition)}),a.go=function(a){b.path(a)},a.selected=function(a){return a===c.transferOfCareIndex},a.create=function(){e.go("transferOfCare-create",{patientId:a.patient.id})}}]),angular.module("openehrPocApp").controller("TransferOfCareDetailCtrl",["$scope","$stateParams","$modal","$location","PatientService","TransferOfCare",function(a,b,c,d,e,f){e.get(b.patientId).then(function(b){a.patient=b}),f.getComposition(b.patientId).then(function(c){a.transferofCareComposition=c.data,a.transferOfCare=a.transferofCareComposition.transfers[b.transferOfCareIndex],a.transferOfCareSelectionNumber=b.transferOfCareIndex+1,a.allergies=a.transferOfCare.allergies,a.contacts=a.transferOfCare.contacts,a.problems=a.transferOfCare.problems,a.medications=a.transferOfCare.medication,a.transferDetail=a.transferOfCare.transferDetail,a.site=a.transferOfCare.transferDetail.site})}]);