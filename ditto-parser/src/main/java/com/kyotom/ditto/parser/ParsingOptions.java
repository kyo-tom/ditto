/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kyotom.ditto.parser;

import com.kyotom.ditto.client.ParserConfig;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ParsingOptions
{

    private Map<String, String> properties = new HashMap<>();
    private final ParserConfig parserConfig;
    public enum DecimalLiteralTreatment
    {
        AS_DOUBLE,
        AS_DECIMAL,
        REJECT
    }

    private final DecimalLiteralTreatment decimalLiteralTreatment;

    @Inject
    public ParsingOptions(ParserConfig parserConfig)
    {
        this(parserConfig, DecimalLiteralTreatment.REJECT);
    }

    public ParsingOptions(ParserConfig parserConfig,DecimalLiteralTreatment decimalLiteralTreatment)
    {
        this.parserConfig = parserConfig;
        this.decimalLiteralTreatment = requireNonNull(decimalLiteralTreatment, "decimalLiteralTreatment is null");
    }

    public DecimalLiteralTreatment getDecimalLiteralTreatment()
    {
        return decimalLiteralTreatment;
    }

    public ParserConfig getParserConfig() {
        return parserConfig;
    }
}
