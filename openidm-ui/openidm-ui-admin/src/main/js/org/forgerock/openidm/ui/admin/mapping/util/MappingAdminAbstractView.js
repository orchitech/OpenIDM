/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, _, Handlebars, form2js */

define("org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"

], function(AdminAbstractView,
            SyncDelegate,
            ConfigDelegate) {

    var currentMapping = {},
        syncConfig = {},
        numRepresentativeProps = 4,
        recon = null,
        syncCancelled = null,
        runSync = _.noop(),

        MappingAdminAbstractView = AdminAbstractView.extend({
            getCurrentMapping: function() {
                return _.clone(currentMapping, true);
            },

            getSyncConfig: function() {
                return _.clone(syncConfig, true);
            },

            getRecon: function() {
                return _.clone(recon, true);
            },

            getSyncNow: function() {
                return runSync;
            },

            getSyncCancelled: function() {
                return _.clone(syncCancelled, true);
            },

            getMappingName: function() {
                if (currentMapping) {
                    return currentMapping.name;
                } else {
                    return undefined;
                }
            },

            getNumRepresentativeProps: function() {
                return numRepresentativeProps;
            },

            setNumRepresentativeProps: function(num) {
                numRepresentativeProps = num;
                return num;
            },

            setCurrentMapping: function(mapping) {
                currentMapping = mapping;
                return mapping;
            },

            setSyncConfig: function(sync) {
                syncConfig = sync;
                return sync;
            },

            setRecon: function(data) {
                recon = data;
                return data;
            },

            setSyncNow: function(syncNow) {
                runSync = syncNow;
                return syncNow;
            },

            setSyncCancelled: function(cancelled) {
                syncCancelled = cancelled;
                return cancelled;
            },

            AbstractMappingSave: function(mapping, callback) {
                _.each(syncConfig.mappings, function(map, key) {
                    syncConfig.mappings[key] = _.omit(mapping, "recon");

                    if (map.name === this.getMappingName()) {
                        currentMapping = syncConfig.mappings[key];
                    }
                }, this);

                ConfigDelegate.updateEntity("sync", {"mappings" : syncConfig.mappings}).then(_.bind(callback, this));
            }
        });

    return MappingAdminAbstractView;
});
