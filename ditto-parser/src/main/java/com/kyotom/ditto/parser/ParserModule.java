package com.kyotom.ditto.parser;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.kyotom.ditto.parser.bridging.Bridging;

public class ParserModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(ParsingOptions.class).in(Scopes.SINGLETON);
        binder.bind(SqlParser.class).in(Scopes.SINGLETON);
        binder.bind(Bridging.class).in(Scopes.SINGLETON);
    }
}
