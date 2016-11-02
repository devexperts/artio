/*
 * Copyright 2015-2016 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.fix_gateway.dictionary.generation;

import org.agrona.LangUtil;
import org.agrona.collections.IntHashSet;
import org.agrona.generation.OutputManager;
import uk.co.real_logic.fix_gateway.dictionary.CharArraySet;
import uk.co.real_logic.fix_gateway.dictionary.ir.Dictionary;
import uk.co.real_logic.fix_gateway.dictionary.ir.Field;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toUpperCase;
import static java.util.stream.Collectors.joining;
import static uk.co.real_logic.fix_gateway.dictionary.generation.DecoderGenerator.addField;
import static uk.co.real_logic.fix_gateway.dictionary.generation.GenerationUtil.fileHeader;
import static uk.co.real_logic.fix_gateway.dictionary.generation.GenerationUtil.importFor;

public class ConstantGenerator
{
    public static final String CLASS_NAME = "Constants";

    public static final String BODY = "public class " + CLASS_NAME + "\n" + "{\n\n";
    public static final String VERSION = "VERSION";

    private final Dictionary dictionary;
    private final String builderPackage;
    private final OutputManager outputManager;

    public ConstantGenerator(
        final Dictionary dictionary, final String builderPackage, final OutputManager outputManager)
    {
        this.dictionary = dictionary;
        this.builderPackage = builderPackage;
        this.outputManager = outputManager;
    }

    public void generate()
    {
        outputManager.withOutput(CLASS_NAME, out ->
        {
            out.append(fileHeader(builderPackage));
            out.append(importFor(IntHashSet.class));
            out.append(importFor(CharArraySet.class));
            out.append(BODY);
            out.append(generateVersion());
            out.append(generateMessageTypes());
            out.append(generateFieldTags());
            out.append(generateAllFieldsDictionary());
            generateEnumDictionaries(out);
            out.append("}\n");
        });
    }

    private void generateEnumDictionaries(final Writer out)
    {
        for (final Field field : dictionary.fields().values())
        {
            final String name = field.name();
            final String valuesField = constantValuesOfField(name);
            final Field.Type type = field.type();
            final boolean isChar = type == Field.Type.CHAR;
            final boolean isPrimitive = type.isIntBased() || isChar;
            try
            {
                if (isPrimitive)
                {
                    final String addValues =
                        field.values()
                            .stream()
                            .map(Field.Value::representation)
                            .map(repr -> isChar ? "'" + repr + "'" : repr)
                            .map(repr -> String.format("        %1$s.add(%2$s);\n", valuesField, repr))
                            .collect(joining());

                    out.append(String.format(
                        "    public static final IntHashSet %1$s = new IntHashSet(%3$s, -1);\n" +
                        "    static\n" +
                        "    {\n" +
                        "%2$s" +
                        "    }\n\n",
                        valuesField,
                        addValues,
                        sizeHashSet(field.values())
                    ));
                }
                else if (type.isStringBased())
                {
                    final String addValues =
                        field.values()
                            .stream()
                            .map(value -> "\"" + value.representation() + '"')
                            .collect(joining(", "));

                    out.append(String.format(
                        "    public static final CharArraySet %1$s = new CharArraySet(%2$s);\n",
                        valuesField,
                        addValues));
                }
            }
            catch (final IOException ex)
            {
                LangUtil.rethrowUnchecked(ex);
            }
        }
    }

    static String constantValuesOfField(final String name)
    {
        return "VALUES_OF_" + name;
    }

    private String generateAllFieldsDictionary()
    {
        return generateFieldDictionary(dictionary.fields().values(), "ALL_FIELDS");
    }

    private String generateFieldDictionary(final Collection<Field> fields, final String name)
    {
        final String addFields = fields
            .stream()
            .map((field) -> addField(field, name))
            .collect(joining());

        final int hashMapSize = sizeHashSet(fields);
        return String.format(
            "    public static final IntHashSet %3$s = new IntHashSet(%1$d, -1);\n" +
            "    static\n" +
            "    {\n" +
            "%2$s" +
            "    }\n\n",
            hashMapSize,
            addFields,
            name);
    }

    private String generateVersion()
    {
        return String.format(
            "public static String VERSION = \"FIX.%d.%d\";\n" +
            "public static char[] VERSION_CHARS = VERSION.toCharArray();\n",
            dictionary.majorVersion(),
            dictionary.minorVersion());
    }

    public static int sizeHashSet(final Collection<?> objects)
    {
        return objects.size() * 2;
    }

    private String generateMessageTypes()
    {
        return dictionary
            .messages()
            .stream()
            .map(message ->
            {
                final int type = message.packedType();
                return generateMessageTypeConstant(type) + generateIntConstant(message.name(), type);
            })
            .collect(joining());
    }

    private String generateFieldTags()
    {
        return fields()
            .stream()
            .map(field -> generateIntConstant(field.name(), field.number()))
            .collect(joining());
    }

    private Collection<Field> fields()
    {
        return dictionary
            .fields()
            .values();
    }

    private String generateMessageTypeConstant(final int messageType)
    {
        final char[] chars;
        if (messageType > Byte.MAX_VALUE)
        {
            chars = new char[]{ (char)(byte)messageType, (char)(byte)(messageType >>> 8) };
        }
        else
        {
            chars = new char[]{ (char)(byte)messageType };
        }

        return String.format("    /** In Ascii - %1$s */\n", new String(chars));
    }

    private String generateIntConstant(final String name, final int number)
    {
        return String.format(
            "    public static final int %2$s = %1$d;\n\n",
            number,
            constantName(name));
    }

    private String constantName(String name)
    {
        name = name.replace("ID", "Id");
        return toUpperCase(name.charAt(0)) +
            name.substring(1)
                .chars()
                .mapToObj((codePoint) -> (isUpperCase(codePoint) ? "_" : "") + (char)toUpperCase(codePoint))
                .collect(joining());
    }
}
