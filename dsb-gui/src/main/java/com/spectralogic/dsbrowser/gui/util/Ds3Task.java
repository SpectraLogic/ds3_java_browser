/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
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

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;
import javafx.concurrent.Task;

public class Ds3Task<T> extends Task<T> {

    private final Ds3Client client;
    protected String errorMsg;

    public Ds3Task() {
        this(null);
    }

    public Ds3Task(final Ds3Client client) {
        this.client = client;
    }

    @Override
    protected T call() throws Exception {
        throw new IllegalStateException("Implement this method");
    }

    public Ds3Client getClient() {
        return this.client;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public void setErrorMsg(final String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
