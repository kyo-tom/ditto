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
package com.kyotom.ditto.client.cli;

import com.google.common.collect.ImmutableSet;
import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.SyntaxError;
import org.jline.reader.impl.DefaultParser;

import java.util.Set;

import static com.google.common.base.CharMatcher.whitespace;
import static java.util.Locale.ENGLISH;

public class InputParser
        implements Parser
{
    private static final Set<String> SPECIAL = ImmutableSet.of("exit", "quit", "history", "help", "clear","transform");

    @Override
    public ParsedLine parse(String line, int cursor, ParseContext context)
            throws SyntaxError
    {
        String command = whitespace().trimFrom(line);
        if (command.isEmpty() || SPECIAL.contains(command.toLowerCase(ENGLISH))) {
            return new DefaultParser().parse(line, cursor, context);
        }
        if(line.endsWith(";")){
            return new DefaultParser().parse(line, cursor, context);
        }
        throw new EOFError(-1, -1, null);
    }

    @Override
    public boolean isEscapeChar(char ch)
    {
        return false;
    }
}
