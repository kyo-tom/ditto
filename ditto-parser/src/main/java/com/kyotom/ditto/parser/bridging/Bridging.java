package com.kyotom.ditto.parser.bridging;

import com.kyotom.ditto.parser.tree.*;
import io.trino.sql.tree.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class Bridging extends AstVisitor<Node, Void> {

    @Override
    public Node visitCreateTable(CreateTable node, Void context) {
        List<io.trino.sql.tree.Property> propertyList = new ArrayList<>();
        for (Property property : node.getProperties()) {
            propertyList.add((io.trino.sql.tree.Property)process(property, context));
        }
        List<io.trino.sql.tree.TableElement> tableElementList = new ArrayList<>();
        for (TableElement element : node.getElements()) {
            tableElementList.add((io.trino.sql.tree.TableElement)process(element, context));
        }

        io.trino.sql.tree.QualifiedName qualifiedName = visitQualifiedName(node.getName(), context);
        io.trino.sql.tree.NodeLocation location = visitNodeLocation(node.getLocation(), context);
        io.trino.sql.tree.CreateTable createTable;
        if (location != null) {
            createTable = new io.trino.sql.tree.CreateTable(location, qualifiedName, tableElementList, node.isNotExists(), propertyList, node.getComment());
        }
        else {
            createTable = new io.trino.sql.tree.CreateTable(qualifiedName, tableElementList, node.isNotExists(), propertyList, node.getComment());
        }
        return createTable;
    }

    @Override
    public Node visitLikeClause(LikeClause node, Void context) {
        io.trino.sql.tree.NodeLocation location = visitNodeLocation(node.getLocation(), context);
        io.trino.sql.tree.QualifiedName qualifiedName = visitQualifiedName(node.getTableName(), context);
        Optional<io.trino.sql.tree.LikeClause.PropertiesOption> propertiesOption = visitPropertiesOption(node.getPropertiesOption());
        io.trino.sql.tree.LikeClause likeClause;
        if (location != null) {
            likeClause = new io.trino.sql.tree.LikeClause(location, qualifiedName, propertiesOption);
        }
        else {
            likeClause = new io.trino.sql.tree.LikeClause(qualifiedName, propertiesOption);
        }
        return likeClause;
    }

    @Override
    public Node visitColumnDefinition(ColumnDefinition node, Void context) {
        io.trino.sql.tree.NodeLocation location = visitNodeLocation(node.getLocation(), context);
        io.trino.sql.tree.Identifier identifier = (io.trino.sql.tree.Identifier)process(node.getName(), context);
        io.trino.sql.tree.DataType dataType = visitDataType(node.getType(), context);
        List<io.trino.sql.tree.Property> propertyList = new ArrayList<>();
        for (Property property : node.getProperties()) {
            propertyList.add((io.trino.sql.tree.Property)process(property, context));
        }
        io.trino.sql.tree.ColumnDefinition columnDefinition;
        if(location != null){
            columnDefinition = new io.trino.sql.tree.ColumnDefinition(location, identifier, dataType,
                    node.isNullable(), propertyList, node.getComment());
        }
        else {
            columnDefinition = new io.trino.sql.tree.ColumnDefinition(identifier, dataType,
                    node.isNullable(), propertyList, node.getComment());
        }
        return columnDefinition;
    }

    private io.trino.sql.tree.DataType visitDataType(DataType type, Void context) {
        return (io.trino.sql.tree.DataType)process(type,context);
    }

    @Override
    public Node visitGenericDataType(GenericDataType node, Void context) {
        Identifier name = node.getName();
        return new io.trino.sql.tree.GenericDataType(visitNodeLocation(node.getLocation(), context), new io.trino.sql.tree.Identifier(name.getValue()), process(node.getArguments(), io.trino.sql.tree.DataTypeParameter.class));
    }

    @Override
    public Node visitNumericTypeParameter(NumericParameter node, Void context) {
        io.trino.sql.tree.NodeLocation location = visitNodeLocation(node.getLocation(), context);
        io.trino.sql.tree.NumericParameter numericParameter;
        if (location != null){
            numericParameter = new io.trino.sql.tree.NumericParameter(location, node.getValue());
        }
        else {
            numericParameter = new io.trino.sql.tree.NumericParameter(Optional.empty(), node.getValue());
        }
        return numericParameter;
    }

    @Override
    public Node visitRowDataType(RowDataType node, Void context) {
        return null;
    }

    private Node visitIntervalDayTimeDataType(IntervalDayTimeDataType type, Void context) {
        return null;
    }

    private Node visitDataTimeDataType(DateTimeDataType type, Void context) {
        return null;
    }

    private Optional<io.trino.sql.tree.LikeClause.PropertiesOption> visitPropertiesOption(Optional<LikeClause.PropertiesOption> propertiesOption) {
        return Optional.empty();
    }


    @Override
    public Node visitProperty(Property node, Void context) {
        io.trino.sql.tree.Identifier identifier = (io.trino.sql.tree.Identifier)process(node.getName(), context);
        io.trino.sql.tree.Expression expression = (io.trino.sql.tree.Expression)process(node.getValue(), context);
        io.trino.sql.tree.NodeLocation location = visitNodeLocation(node.getLocation(), context);
        io.trino.sql.tree.Property property;
        if (location != null) {
            property = new io.trino.sql.tree.Property(location, identifier, expression);
        }
        else {
            property = new io.trino.sql.tree.Property(identifier, expression);
        }
        return property;
    }

    @Override
    public Node visitIdentifier(Identifier node, Void context) {
        io.trino.sql.tree.NodeLocation location = visitNodeLocation(node.getLocation(), context);
        io.trino.sql.tree.Identifier identifier;
        if (location != null) {
           identifier = new io.trino.sql.tree.Identifier(location, node.getValue(), node.isDelimited());
        }
        else {
            identifier = new io.trino.sql.tree.Identifier(node.getValue(), node.isDelimited());
        }
        return identifier;
    }

    @Override
    public Node visitBooleanLiteral(BooleanLiteral node, Void context) {
        io.trino.sql.tree.NodeLocation location = visitNodeLocation(node.getLocation(),context);
        if (location!= null) {
            return new io.trino.sql.tree.BooleanLiteral(location, node.toString());
        }
        else {
            return new io.trino.sql.tree.BooleanLiteral(node.toString());
        }
    }

    @Override
    public Node visitStringLiteral(StringLiteral node, Void context) {
        io.trino.sql.tree.NodeLocation location = visitNodeLocation(node.getLocation(),context);
        if (location!= null) {
            return new io.trino.sql.tree.StringLiteral(location, node.toString());
        }
        else {
            return new io.trino.sql.tree.StringLiteral(node.toString());
        }
    }

    public io.trino.sql.tree.QualifiedName visitQualifiedName(QualifiedName node, Void context) {
        List<io.trino.sql.tree.Identifier> identifierList = new ArrayList<>();
        for (Identifier identifier : node.getOriginalParts()) {
            identifierList.add((io.trino.sql.tree.Identifier)process(identifier, context));
        }
        io.trino.sql.tree.QualifiedName qualifiedName = io.trino.sql.tree.QualifiedName.of(identifierList);
        return qualifiedName;
    }

    public io.trino.sql.tree.NodeLocation visitNodeLocation(NodeLocation node, Void context) {
        io.trino.sql.tree.NodeLocation location = new io.trino.sql.tree.NodeLocation(node.getLineNumber(), node.getColumnNumber());
        return location;
    }

    public io.trino.sql.tree.NodeLocation visitNodeLocation(Optional<NodeLocation> node, Void context) {
        if(node.isPresent()){
            io.trino.sql.tree.NodeLocation location = new io.trino.sql.tree.NodeLocation(node.get().getLineNumber(), node.get().getColumnNumber());
            return location;
        }
        else {
            return null;
        }
    }

    private <T> List<T> process(List<? extends com.kyotom.ditto.parser.tree.Node> contexts, Class<T> clazz)
    {
        return contexts.stream()
                .map(this::process)
                .map(clazz::cast)
                .collect(toList());
    }
}
