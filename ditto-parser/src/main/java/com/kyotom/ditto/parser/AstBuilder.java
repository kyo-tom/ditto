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

import com.google.common.collect.ImmutableList;
import com.kyotom.ditto.parser.tree.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

class AstBuilder
        extends HiveParserBaseVisitor<Node>
{
    private int parameterPosition;
    private final ParsingOptions parsingOptions;

    AstBuilder(ParsingOptions parsingOptions)
    {
        this.parsingOptions = requireNonNull(parsingOptions, "parsingOptions is null");
    }

    @Override
    public Node visitStatements(HiveParser.StatementsContext ctx) {
        return visit(ctx);
    }

    @Override
    public Node visitExec(HiveParser.ExecContext ctx) {
        return visit(ctx.execStatement());
    }

    @Override
    public Node visitQueryStatementExpression(HiveParser.QueryStatementExpressionContext ctx) {
        return visit(ctx.queryStatementExpressionBody());
    }

    @Override
    public Node visitCreateTableStatement(HiveParser.CreateTableStatementContext ctx) {
        Table table = (Table)visit(ctx.tableName(0));

        List<TableElement> tableElementList = new ArrayList<>();
        if (ctx.KW_LIKE() != null) {
            Table likeTarget = (Table)visit(ctx.tableName(1));
            LikeClause likeClause = new LikeClause(getLocation(ctx), likeTarget.getName(), Optional.empty());
            tableElementList.add(likeClause);
        }
        else {
            tableElementList = visit(ctx.columnNameTypeOrConstraintList().columnNameTypeOrConstraint(), TableElement.class);
        }

        boolean isNotExist = ctx.ifNotExists() != null;
        ImmutableList.Builder<Property> propertyBuilder = ImmutableList.<Property>builder();
        Optional<String> comment = Optional.empty();
        if (ctx.tableComment() != null) {
        }

        if(ctx.tablePropertiesPrefixed() != null
                && ctx.tablePropertiesPrefixed().tableProperties() != null
                && ctx.tablePropertiesPrefixed().tableProperties().tablePropertiesList() != null){
            List<Property> propertyList = visit(ctx.tablePropertiesPrefixed().tableProperties().tablePropertiesList().keyValueProperty(), Property.class);
            propertyBuilder.addAll(propertyList);
        }

        Property location = null;
        if(ctx.tableLocation() != null) {
            location = (Property)visit(ctx.tableLocation());
        }

        QualifiedName tableName = table.getName();
        if (ctx.tableFileFormat() != null) {
            FileFormat fileFormat = (FileFormat)visit(ctx.tableFileFormat());
            if (fileFormat.isKuduTable()) {
                List<Identifier> originalParts = tableName.getOriginalParts();
                List<Identifier> newOriginalParts = ImmutableList.<Identifier>builder()
                        .add(new Identifier(parsingOptions.getParserConfig().getKuduDefaultCatalog()))
                        .addAll(originalParts).build();
                tableName = QualifiedName.of(newOriginalParts);
                if (location != null) {
                    throw new ParsingException("Unexpected location for kudu: " + location);
                }
            }
            else {
                List<Identifier> originalParts = tableName.getOriginalParts();
                List<Identifier> newOriginalParts = ImmutableList.<Identifier>builder()
                        .add(new Identifier(parsingOptions.getParserConfig().getHiveDefaultCatalog()))
                        .addAll(originalParts).build();
                tableName = QualifiedName.of(newOriginalParts);
                Expression tableFileFormat;
                switch (fileFormat.getStoredAs().getValue().toUpperCase(Locale.ENGLISH)) {
                    case "PARQUET" :
                        tableFileFormat = new StringLiteral("Parquet");
                        break;
                    case "ORC":
                        tableFileFormat = new StringLiteral("ORC");
                        break;
                    case "AVRO":
                        tableFileFormat = new StringLiteral("Avro");
                        break;
                    case "RCFILE":
                        tableFileFormat = new StringLiteral("RCText");
                        break;
                    case "SEQUENCEFILE":
                        tableFileFormat = new StringLiteral("SequenceFile");
                        break;
                    case "HUDI":
                    case "TEXT":
                    default:
                        tableFileFormat = new StringLiteral("TextFile");
                }
                propertyBuilder.add(new Property(new Identifier("format"), tableFileFormat));
            }
            if (location != null) {
                propertyBuilder.add(location);
            }
        }
        else {
            List<Identifier> originalParts = tableName.getOriginalParts();
            List<Identifier> newOriginalParts = ImmutableList.<Identifier>builder()
                    .add(new Identifier(parsingOptions.getParserConfig().getHiveDefaultCatalog()))
                    .addAll(originalParts).build();
            tableName = QualifiedName.of(newOriginalParts);
            Expression tableFileFormat = new StringLiteral("TextFile");
            propertyBuilder.add(new Property(new Identifier("format"), tableFileFormat));
            if (location != null) {
                propertyBuilder.add(location);
            }
        }

        CreateTable createTable = new CreateTable(getLocation(ctx), tableName, tableElementList, isNotExist, propertyBuilder.build(), comment);
        return createTable;
    }

    @Override
    public Node visitKeyValueProperty(HiveParser.KeyValuePropertyContext ctx) {
        String key = ctx.StringLiteral(0).getText();
        String value = ctx.StringLiteral(1).getText();
        return new Property(new Identifier(key.substring(1, value.length() - 1)), new StringLiteral(value.substring(1, value.length() - 1)));
    }

    @Override
    public Node visitTableLocation(HiveParser.TableLocationContext ctx) {
        String location = ctx.StringLiteral().getText();
        return new Property(new Identifier("location"), new StringLiteral(location.substring(1, location.length() - 1)));
    }

    @Override
    public Node visitFileFormatAs(HiveParser.FileFormatAsContext ctx) {
        Identifier storedAs = (Identifier)visit(ctx.identifier());
        return new FileFormat(getLocation(ctx), storedAs);
    }

    @Override
    public Node visitColumnNameTypeOrConstraint(HiveParser.ColumnNameTypeOrConstraintContext ctx) {
        Optional<String> comment = Optional.empty();
        if(ctx.tableConstraint() != null){
            return visit(ctx.tableConstraint());
        }
        else {
            return visit(ctx.columnNameTypeConstraint());
        }
    }

    @Override
    public Node visitColumnNameTypeConstraint(HiveParser.ColumnNameTypeConstraintContext ctx) {
        DataType dataType = (DataType)visit(ctx.colType());
        Optional<String> comment = Optional.empty();
        if(ctx.KW_COMMENT() != null){

        }
        Boolean nullable = true;
        if(ctx.KW_NOT() != null && ctx.KW_NULL() != null){
            nullable = false;
        }
        List<Property> propertyList = new ArrayList<>();
        if(ctx.columnConstraint() != null){
            Property property = (Property)visit(ctx.columnConstraint());
            propertyList.add(property);
        }
        ColumnDefinition columnDefinition = new ColumnDefinition(getLocation(ctx), (Identifier)visit(ctx.identifier()), dataType, nullable, propertyList, comment);
        return columnDefinition;
    }


    @Override
    public Node visitColConstraintRule(HiveParser.ColConstraintRuleContext ctx) {
        return visit(ctx.colConstraint());
    }

    @Override
    public Node visitColConstraint(HiveParser.ColConstraintContext ctx) {
        BooleanLiteral isPrimaryKey;
        if (ctx.tableConstraintPrimaryKey() != null) {
            isPrimaryKey = new BooleanLiteral("true");
        }
        else {
            isPrimaryKey = new BooleanLiteral("false");
        }
        Property property = new Property(new Identifier(getLocation(ctx), "primary_key", false), isPrimaryKey);
        return property;
    }

    @Override
    public Node visitColType(HiveParser.ColTypeContext ctx) {
        return visit(ctx.type_db_col());
    }

    @Override
    public Node visitPrimitiveDataType(HiveParser.PrimitiveDataTypeContext ctx) {
        return visit(ctx.primitiveType());

    }

    @Override
    public Node visitPrimitiveType(HiveParser.PrimitiveTypeContext ctx) {
        NodeLocation location = getLocation(ctx);
        ImmutableList.Builder<DataTypeParameter> builder = ImmutableList.builder();
        if (ctx.KW_BIGINT() != null) {
            return new GenericDataType(location, new Identifier("BIGINT"), ImmutableList.of());
        }
        else if (ctx.KW_VARCHAR() != null) {
            TerminalNode length = ctx.Number(0);
            if (length != null) {
                builder.add(new NumericParameter(location, length.getText()));

            }
            return new GenericDataType(location, new Identifier("VARCHAR"), builder.build());
        }
        else if (ctx.KW_DECIMAL() != null) {
            TerminalNode precision = ctx.Number(0);
            TerminalNode scale = ctx.Number(1);
            if (precision != null) {
                builder.add(new NumericParameter(location, precision.getText()));
            }
            if (scale != null) {
                builder.add(new NumericParameter(location, scale.getText()));
            }
            return new GenericDataType(location, new Identifier("DECIMAL"), builder.build());
        }
        else {
            return null;
        }
    }

    @Override
    public Node visitListDataType(HiveParser.ListDataTypeContext ctx) {
        return super.visitListDataType(ctx);
    }

    @Override
    public Node visitStructDataType(HiveParser.StructDataTypeContext ctx) {
        return super.visitStructDataType(ctx);
    }

    @Override
    public Node visitMapType(HiveParser.MapTypeContext ctx) {
        return super.visitMapType(ctx);
    }

    @Override
    public Node visitUnionType(HiveParser.UnionTypeContext ctx) {
        return super.visitUnionType(ctx);
    }

    @Override
    public Node visitRefresh(HiveParser.RefreshContext ctx) {
        HiveParser.RefreshStatementContext refreshStatementContext = ctx.refreshStatement();
        Table table = (Table) visit(refreshStatementContext.tableName());
        Refresh refresh = new Refresh(Optional.of(getLocation(ctx)), table);
        return refresh;
    }

    @Override
    public Node visitTableName(HiveParser.TableNameContext ctx) {
        return new Table(getLocation(ctx), QualifiedName.of(visit(ctx.identifier(), Identifier.class)));
    }

    @Override
    public Node visitIdentifier(HiveParser.IdentifierContext ctx) {
        return new Identifier(getLocation(ctx), ctx.getText(), false);
    }

    public static NodeLocation getLocation(TerminalNode terminalNode)
    {
        requireNonNull(terminalNode, "terminalNode is null");
        return getLocation(terminalNode.getSymbol());
    }

    public static NodeLocation getLocation(ParserRuleContext parserRuleContext)
    {
        requireNonNull(parserRuleContext, "parserRuleContext is null");
        return getLocation(parserRuleContext.getStart());
    }

    public static NodeLocation getLocation(Token token)
    {
        requireNonNull(token, "token is null");
        return new NodeLocation(token.getLine(), token.getCharPositionInLine() + 1);
    }

    private <T> List<T> visit(List<? extends ParserRuleContext> contexts, Class<T> clazz)
    {
        return contexts.stream()
                .map(this::visit)
                .map(clazz::cast)
                .collect(toList());
    }

    private <T> List<T> visit(ParserRuleContext contexts, Class<T> clazz)
    {
        return contexts.children.stream()
                .map(this::visit)
                .map(clazz::cast)
                .collect(toList());
    }
}