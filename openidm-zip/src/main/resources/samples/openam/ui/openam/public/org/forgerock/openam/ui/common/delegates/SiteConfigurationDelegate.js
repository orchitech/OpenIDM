/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global $, define, _ */

define("org/forgerock/openam/ui/common/delegates/SiteConfigurationDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate"
], function(constants, AbstractDelegate, configuration, eventManager, uiUtils, idmSiteDelegate) {

    var obj = new AbstractDelegate(constants.host + "/"+ constants.amContext ),
        lastKnownRealm = "/";

    obj.getConfiguration = function(successCallback, errorCallback) {
        
        var idmPromise = $.Deferred(),
            amPromise = $.Deferred();
    
        obj.serviceCall({
            url: "/json/serverinfo/*",
            headers: {
                "X-OpenIDM-Password": "anonymous",
                "X-OpenIDM-Username": "anonymous",
                "X-OpenIDM-NoSession": "true"
            },
            success: function (data) {
                amPromise.resolve(data);
            },
            error: function () {
                amPromise.reject();
            }
        });        

        idmSiteDelegate.getConfiguration(
            function (data) {
                idmPromise.resolve(data);
            }, 
            function () {
                idmPromise.reject();
            }
        );

        $.when(idmPromise, amPromise).then(function (idmData, amData) {
            successCallback(_.extend({}, amData, idmData));
        }, errorCallback);

    };

    return obj;
});



