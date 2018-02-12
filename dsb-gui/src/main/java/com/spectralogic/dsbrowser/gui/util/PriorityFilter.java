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

import com.spectralogic.ds3client.models.Priority;

public final class PriorityFilter {

    /*
       Do we really need to do this at all? We only use this two places?
       Both times we use this we take the full list of possabilities
       and remove BACKGROUND and CRITICAL.
       A better solution would be to statically provide the list
       Or remove the entries from the enum and skip this wholesale.
     */

    private static final Priority[] priorities = { Priority.URGENT, Priority.HIGH, Priority.NORMAL, Priority.LOW };

    public static Priority[] filterPriorities() {
        return priorities;
    }

}
