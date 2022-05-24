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

import com.kyotom.ditto.parser.tree.Node;
import com.kyotom.ditto.parser.tree.Statement;
import com.kyotom.ditto.parser.tree.Statements;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import javax.inject.Inject;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class SqlParser
{

    private static final BaseErrorListener LEXER_ERROR_LISTENER = new BaseErrorListener()
    {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String message, RecognitionException e)
        {
            throw new ParsingException(message, e, line, charPositionInLine + 1);
        }
    };
    private static final BiConsumer<HiveLexer, HiveParser> DEFAULT_PARSER_INITIALIZER = (HiveLexer lexer, HiveParser parser) -> {};

    private static final ErrorHandler PARSER_ERROR_HANDLER = ErrorHandler.builder()
            .specialRule(HiveParser.RULE_expression, "<expression>")
            .ignoredRule(HiveParser.RULE_nonReserved)
            .build();

    private final BiConsumer<HiveLexer, HiveParser> initializer;

    private final ParsingOptions defaultParsingOptions;

    public SqlParser()
    {
        this(DEFAULT_PARSER_INITIALIZER, null);
    }

    @Inject
    public SqlParser(ParsingOptions defaultParsingOptions){
        this(DEFAULT_PARSER_INITIALIZER, defaultParsingOptions);
    }

    public SqlParser(BiConsumer<HiveLexer, HiveParser> initializer, ParsingOptions defaultParsingOptions)
    {
        this.initializer = requireNonNull(initializer, "initializer is null");
        this.defaultParsingOptions = defaultParsingOptions;
    }

    public Statements createStatements(String sql)
    {
        return createStatements(sql, defaultParsingOptions);
    }

    public Statements createStatements(String sql, ParsingOptions parsingOptions)
    {
        return (Statements) invokeParser("statements", sql, HiveParser::statements, parsingOptions);
    }

    public Statement createStatement(String sql)
    {
        return createStatement(sql, defaultParsingOptions);
    }


    public Statement createStatement(String sql, ParsingOptions parsingOptions)
    {
        return (Statement) invokeParser("statement", sql, HiveParser::statement, parsingOptions);
    }

    private Node invokeParser(String name, String sql, Function<HiveParser, ParserRuleContext> parseFunction, ParsingOptions parsingOptions)
    {
        requireNonNull(parsingOptions, "parsingOptions is null");
        try {
            HiveLexer lexer = new HiveLexer(new CaseInsensitiveStream(CharStreams.fromString(sql)));
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            HiveParser parser = new HiveParser(tokenStream);
            initializer.accept(lexer, parser);

            // Override the default error strategy to not attempt inserting or deleting a token.
            // Otherwise, it messes up error reporting
            parser.setErrorHandler(new DefaultErrorStrategy()
            {
                @Override
                public Token recoverInline(Parser recognizer)
                        throws RecognitionException
                {
                    if (nextTokensContext == null) {
                        throw new InputMismatchException(recognizer);
                    }
                    else {
                        throw new InputMismatchException(recognizer, nextTokensState, nextTokensContext);
                    }
                }
            });

//            parser.addParseListener(new PostProcessor(Arrays.asList(parser.getRuleNames()), parser));

            lexer.removeErrorListeners();
            lexer.addErrorListener(LEXER_ERROR_LISTENER);

            parser.removeErrorListeners();
            parser.addErrorListener(PARSER_ERROR_HANDLER);

            ParserRuleContext tree;
            try {
                // first, try parsing with potentially faster SLL mode
                parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
                tree = parseFunction.apply(parser);
            }
            catch (ParseCancellationException ex) {
                // if we fail, parse with LL mode
                tokenStream.seek(0); // rewind input stream
                parser.reset();

                parser.getInterpreter().setPredictionMode(PredictionMode.LL);
                tree = parseFunction.apply(parser);
            }

            return new AstBuilder(parsingOptions).visit(tree);
        }
        catch (StackOverflowError e) {
            throw new ParsingException(name + " is too large (stack overflow while parsing)");
        }
    }

    /*private static class PostProcessor
            extends HiveParserListener
    {
        private final List<String> ruleNames;
        private final HiveParser parser;

        public PostProcessor(List<String> ruleNames, HiveParser parser)
        {
            this.ruleNames = ruleNames;
            this.parser = parser;
        }

        @Override
        public void exitQuotedIdentifier(HiveParser.QuotedIdentifierContext context)
        {
            Token token = context.QUOTED_IDENTIFIER().getSymbol();
            if (token.getText().length() == 2) { // empty identifier
                throw new ParsingException("Zero-length delimited identifier not allowed", null, token.getLine(), token.getCharPositionInLine() + 1);
            }
        }

        @Override
        public void exitBackQuotedIdentifier(HiveParser.BackQuotedIdentifierContext context)
        {
            Token token = context.BACKQUOTED_IDENTIFIER().getSymbol();
            throw new ParsingException(
                    "backquoted identifiers are not supported; use double quotes to quote identifiers",
                    null,
                    token.getLine(),
                    token.getCharPositionInLine() + 1);
        }

        @Override
        public void exitDigitIdentifier(SqlBaseParser.DigitIdentifierContext context)
        {
            Token token = context.DIGIT_IDENTIFIER().getSymbol();
            throw new ParsingException(
                    "identifiers must not start with a digit; surround the identifier with double quotes",
                    null,
                    token.getLine(),
                    token.getCharPositionInLine() + 1);
        }

        @Override
        public void exitNonReserved(SqlBaseParser.NonReservedContext context)
        {
            // we can't modify the tree during rule enter/exit event handling unless we're dealing with a terminal.
            // Otherwise, ANTLR gets confused and fires spurious notifications.
            if (!(context.getChild(0) instanceof TerminalNode)) {
                int rule = ((ParserRuleContext) context.getChild(0)).getRuleIndex();
                throw new AssertionError("nonReserved can only contain tokens. Found nested rule: " + ruleNames.get(rule));
            }

            // replace nonReserved words with IDENT tokens
            context.getParent().removeLastChild();

            Token token = (Token) context.getChild(0).getPayload();
            Token newToken = new CommonToken(
                    new Pair<>(token.getTokenSource(), token.getInputStream()),
                    SqlBaseLexer.IDENTIFIER,
                    token.getChannel(),
                    token.getStartIndex(),
                    token.getStopIndex());

            context.getParent().addChild(parser.createTerminalNode(context.getParent(), newToken));
        }
    }*/
}
