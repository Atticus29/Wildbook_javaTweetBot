/* global angular, alertplus */
'use strict';

angular.module('wildbook.admin').directive(
    'wbIndividualView',
    ["$http", function($http) {
        return {
            restrict: 'E',
            scope: {
                data: "=indData"
            },
            templateUrl: 'encounters/individual_view.html',
            replace: true,
            link: function($scope, ele, attr) {
              $http.get("api/individual/photos/"+$scope.data.id)
              .then(function(photos) {
                  $scope.photos = photos.data;
              });
            }
        };
    }]
);

angular.module('wildbook.admin').directive(
    'wbIndividualEdit',
    ["$http", "$exceptionHandler", "wbConfig", "$mdDialog",
     function($http, $exceptionHandler, wbConfig, $mdDialog) {
        return {
            restrict: 'E',
            scope: {
                data: "=indData",
                editIndividualDone: "&"
            },
            templateUrl: 'encounters/individual_edit.html',
            replace: true,
            controller: function($scope) {
                $scope.module = {};

                wbConfig.config()
                .then(function(config) {
                    $scope.allSpecies = config.species;
                });

                $scope.save = function() {
                    $http.post('admin/api/individual/save', $scope.data)
                    .then(function(result) {
                        $scope.data.id = result.data.id;
                        //
                        // Reset the display name to the new one.
                        //
                        //$scope.data.displayName = getDisplayName($scope.data);
                        $scope.data.displayName = result.data.displayName;

                        $scope.editIndividualDone({individual: $scope.data, deleted: false});
                    }, $exceptionHandler);
                };

                $scope.delete = function() {
                    return alertplus.confirm("You're about to delete this individual, are you sure?", "Delete Individual")
                    .then(function() {
                        $http.post("admin/api/individual/delete", $scope.data)
                        .then(function() {
                            $scope.editIndividualDone({individual: $scope.data, deleted: true});
                        }, $exceptionHandler);
                    });
                };

                //
                // wb-key-handler-form
                //
                $scope.cancel = function() {
                    $scope.editIndividualDone({individual: null, deleted: false});
                };

                $scope.cmdEnter = function() {
                    $scope.save();
                };
            }
        };
    }]
);