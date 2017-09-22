/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.savedSessionStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.ds3client.models.common.Credentials;

public class SavedCredentials {
    @JsonProperty("accessId")
    private final String accessId;
    @JsonProperty("secretKey")
    private final String secretKey;

    @JsonCreator
    public SavedCredentials(@JsonProperty("accessId") final String accessId, @JsonProperty("secretKey") final String secretKey) {
        this.accessId = accessId;
        this.secretKey = secretKey;
    }

    public static SavedCredentials fromCredentials(final Credentials credentials) {
        return new SavedCredentials(credentials.getClientId(), credentials.getKey());
    }

    public String getAccessId() {
        return accessId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public Credentials toCredentials() {
        return new Credentials(this.accessId, this.secretKey);
    }

}
