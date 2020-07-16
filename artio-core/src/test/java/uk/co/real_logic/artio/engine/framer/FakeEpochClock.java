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
package uk.co.real_logic.artio.engine.framer;

import uk.co.real_logic.artio.fields.EpochFractionFormat;
import uk.co.real_logic.artio.util.EpochFractionClock;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

public class FakeEpochClock implements EpochFractionClock
{
    private long time;

    public FakeEpochClock()
    {
        time = 0;
    }

    public void advanceSeconds(final int timeInSeconds)
    {
        advanceMilliSeconds(SECONDS.toMillis(timeInSeconds));
    }

    public void advanceMilliSeconds(final long duration)
    {
        time += duration;
    }

    public long time()
    {
        return time;
    }

    @Override
    public long epochFractionTime(final EpochFractionFormat format)
    {
        switch (format)
        {
            case NANOSECONDS:
                return TimeUnit.MILLISECONDS.toNanos(time);

            case MICROSECONDS:
                return TimeUnit.MILLISECONDS.toMicros(time);

            case MILLISECONDS:
                return time;

            default:
                throw new RuntimeException("Unknown precision: " + format);
        }
    }
}
