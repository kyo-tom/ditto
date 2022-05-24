package com.kyotom.ditto.parser;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.kyotom.ditto.parser.bridging.Bridging;
import com.kyotom.ditto.parser.tree.Statement;
import com.kyotom.ditto.parser.tree.Statements;
import io.airlift.bootstrap.Bootstrap;
import io.trino.sql.SqlFormatter;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Test;

public class SqlParserTest {

    private SqlParser sqlParser;

    private Bridging bridging;

    private ParsingOptions parsingOptions;

    @Before
    public void init(){
        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(
                new ParserModule());

        Bootstrap app = new Bootstrap(modules.build());
        Injector injector = app.strictConfig().initialize();
        bridging = injector.getInstance(Bridging.class);
        parsingOptions = injector.getInstance(ParsingOptions.class);
        sqlParser = injector.getInstance(SqlParser.class);
    }

    @Test
    public void testStatements(){
        Statements statements = sqlParser.createStatements("refresh a.b; select * from a.b;", parsingOptions);
        io.trino.sql.tree.Node node = bridging.process(statements);
        String formatSql = SqlFormatter.formatSql(node);
        System.out.println(formatSql);
    }

    @Test
    public void testStatement(){
        Statement statement = sqlParser.createStatement("create table b.a(b bigint primary key,k varchar,name varchar(16),money decimal(4,8)) stored as orc location 'hdfs://abcsa' TBLPROPERTIES ('a'='1','b'='2')", parsingOptions);
        io.trino.sql.tree.Node node = bridging.process(statement);
        String formatSql = SqlFormatter.formatSql(node);
        System.out.println(formatSql);
    }

    @Test
    public void testRefresh(){
        Statement statement = sqlParser.createStatement("refresh a.b;", parsingOptions);
        System.out.println(statement);
    }

}
