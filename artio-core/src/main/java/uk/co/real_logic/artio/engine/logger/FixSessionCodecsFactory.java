/*
 * Copyright 2019 Monotonic Ltd.
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
package uk.co.real_logic.artio.engine.logger;

import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import uk.co.real_logic.artio.dictionary.FixDictionary;
import uk.co.real_logic.artio.messages.ManageSessionDecoder;
import uk.co.real_logic.artio.messages.MessageHeaderDecoder;

import java.util.HashMap;
import java.util.Map;

import static io.aeron.logbuffer.ControlledFragmentHandler.Action.CONTINUE;
import static uk.co.real_logic.artio.messages.MessageHeaderDecoder.ENCODED_LENGTH;

public class FixSessionCodecsFactory implements ControlledFragmentHandler
{
    private final MessageHeaderDecoder messageHeader = new MessageHeaderDecoder();
    private final ManageSessionDecoder manageSession = new ManageSessionDecoder();

    private final Map<String, FixSessionCodecs> fixDictionaryClassToIndex = new HashMap<>();
    private final Long2ObjectHashMap<FixSessionCodecs> sessionIdToFixDictionaryIndex = new Long2ObjectHashMap<>();

    public Action onFragment(
        final DirectBuffer buffer, final int offset, final int length, final Header header)
    {
        messageHeader.wrap(buffer, offset);

        final int blockLength = messageHeader.blockLength();
        final int version = messageHeader.version();

        if (messageHeader.templateId() == ManageSessionDecoder.TEMPLATE_ID)
        {
            manageSession.wrap(buffer, offset + ENCODED_LENGTH, blockLength, version);

            // Skip over variable length fields
            manageSession.localCompId();
            manageSession.localSubId();
            manageSession.localLocationId();
            manageSession.remoteCompId();
            manageSession.remoteSubId();
            manageSession.remoteLocationId();
            manageSession.address();
            manageSession.username();
            manageSession.password();

            onDictionary(manageSession.session(), manageSession.fixDictionary());
        }

        return CONTINUE;
    }

    private void onDictionary(final long sessionId, final String fixDictionaryClassName)
    {
        final FixSessionCodecs fixSessionCodecs = fixDictionaryClassToIndex.computeIfAbsent(fixDictionaryClassName,
            fixDictionaryName -> new FixSessionCodecs(FixDictionary.find(fixDictionaryName)));
        final FixSessionCodecs previousIndex = sessionIdToFixDictionaryIndex.get(sessionId);
        // NB: this could potentially changes over time.
        if (previousIndex != fixSessionCodecs)
        {
            sessionIdToFixDictionaryIndex.put(sessionId, fixSessionCodecs);
        }
    }

    FixSessionCodecs get(final long sessionId)
    {
        return sessionIdToFixDictionaryIndex.get(sessionId);
    }
}
