/*
 * Copyright 2015-2020 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.artio.util;

import org.agrona.concurrent.EpochClock;
import uk.co.real_logic.artio.fields.EpochFractionFormat;

/**
 * A clock which provides time with requested resolution.
 */
public interface EpochFractionClock extends EpochClock
{
    /**
     * Time corresponding to number of requested units since epoch.
     *
     * @param format Format needed for result
     * @return time corresponding to number of requested units since epoch
     */
    long epochFractionTime(EpochFractionFormat format);
}
