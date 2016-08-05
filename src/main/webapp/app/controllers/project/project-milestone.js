(function () {

    'use strict';

    var module = angular.module('lavagna.controllers');

    module.controller('ProjectMilestoneCtrl', function ($rootScope, $scope, $state, $http,
                                                        project, milestone,
                                                        Label, BoardCache, LabelCache, Card, User, Notification, StompClient) {

        $scope.sidebarOpen = true;
        $scope.project = project;
        $scope.milestone = milestone;

        if(!milestone) {
            $state.go('404');
            return;
        }

        $scope.currentPage = 1;
        $scope.statuses = ["BACKLOG", "OPEN", "DEFERRED", "CLOSED"];

        var loadMilestoneDetail = function () {
            User.hasPermission('READ', project.shortName).then(function () {
                return Card.findCardsByMilestoneDetail(project.shortName, milestone.value, $scope.currentPage - 1).then(function (detail) {
                    $scope.detail = detail;
                });
            });
        };

        loadMilestoneDetail();

        var unbindMovedEvent = $rootScope.$on('card.moved.event', loadMilestoneDetail);
        $scope.$on('$destroy', unbindMovedEvent);

        var unbindRenamedEvent = $rootScope.$on('card.renamed.event', loadMilestoneDetail);
        $scope.$on('$destroy', unbindRenamedEvent);

        var reloadAll = function () {

            LabelCache.findLabelListValue($scope.milestone.cardLabelId, $scope.milestone.id).then(function (m) {

                if (m.value !== $scope.milestone.value) {
                    $state.go('projectMilestone', {projectName: project.shortName, milestone: m.value});
                } else {
                    $scope.milestone = m;
                    loadMilestoneDetail();
                }
            });

        };

        StompClient.subscribe($scope, '/event/project/' + project.shortName + '/label-value', reloadAll);

        StompClient.subscribe($scope, '/event/project/' + project.shortName + '/label', reloadAll);

        $scope.loadColumn = function (card) {
            BoardCache.column(card.columnId).then(function (col) {
                card.column = col;
            });
        };

        $scope.closeMilestone = function () {
            Label.updateLabelListValueMetadata($scope.milestone.id, 'status', 'CLOSED');
        };

        $scope.openMilestone = function () {
            Label.removeLabelListValueMetadata($scope.milestone.id, 'status');
        };

        $scope.orderCardByStatus = function (card) {
            return card.columnDefinition == "CLOSED" ? 1 : 0;
        };

        $scope.updateMilestone = function (newName) {
            var newLabelValue = jQuery.extend({}, $scope.milestone);
            newLabelValue.value = newName;
            Label.updateLabelListValue(newLabelValue).then(function () {
                $state.go('projectMilestone', {projectName: project.shortName, milestone: newName});
            }).catch(function (error) {
                Notification.addAutoAckNotification('error', {key: 'notification.project-milestones.update.error'}, false);
            });
        };

        $scope.updateMilestoneDate = function (newDate) {
            if (newDate) {
                Label.updateLabelListValueMetadata($scope.milestone.id, 'releaseDate', newDate).then(function(){
                    Notification.addAutoAckNotification('success', {key: 'notification.project-milestones.update.success'}, false);
                }).catch(function (error) {
                    Notification.addAutoAckNotification('error', {key: 'notification.project-milestones.update.error'}, false);
                });
            } else {
                Label.removeLabelListValueMetadata($scope.milestone.id, 'releaseDate').then(function(){
                    Notification.addAutoAckNotification('success', {key: 'notification.project-milestones.update.success'}, false);
                }).catch(function (error) {
                    Notification.addAutoAckNotification('error', {key: 'notification.project-milestones.update.error'}, false);
                });
            }
        };

    });

})();
