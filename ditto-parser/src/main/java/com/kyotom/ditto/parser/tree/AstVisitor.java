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
package com.kyotom.ditto.parser.tree;

import javax.annotation.Nullable;

public abstract class AstVisitor<R, C>
{
    public R process(Node node)
    {
        return process(node, null);
    }

    public R process(Node node, @Nullable C context)
    {
        return node.accept(this, context);
    }

    protected R visitNode(Node node, C context)
    {
        return null;
    }

    protected R visitStatements(Statements node, C context)
    {

        return visitNode(node, context);
    }

    protected R visitStatement(Statement node, C context)
    {
        return visitNode(node, context);
    }

    protected R visitRefresh(Refresh node, C context)
    {
        return visitNode(node, context);
    }

    protected R visitTable(Table node, C context)
    {
        return visitNode(node, context);
    }


    public R visitIdentifier(Identifier node, C context) {
        return visitNode(node, context);
    }

    public R visitExpression(Expression node, C context) {
        return visitNode(node, context);
    }

    public R visitQueryBody(QueryBody node, C context) {
        return visitNode(node, context);
    }

    public R visitRelation(Relation node, C context) {
        return visitNode(node, context);
    }

    public R visitTableElement(TableElement node, C context) {
        return visitNode(node, context);
    }

    public R visitProperty(Property node, C context) {
        return visitNode(node, context);
    }

    public R visitCreateTable(CreateTable node, C context) {
        return visitNode(node, context);
    }

    public R visitLikeClause(LikeClause node, C context) {
        return visitNode(node, context);
    }

    public R visitRowDataType(RowDataType node, C context) {
        return visitNode(node, context);
    }

    public R visitColumnDefinition(ColumnDefinition node, C context) {
        return visitNode(node, context);
    }

    public R visitDateTimeType(DateTimeDataType node, C context) {
        return visitNode(node, context);
    }

    public R visitGenericDataType(GenericDataType node, C context) {
        return visitNode(node, context);
    }

    public R visitIntervalDataType(IntervalDayTimeDataType node, C context) {
        return visitNode(node, context);
    }

    public R visitDataTypeParameter(DataTypeParameter node, C context) {
        return visitNode(node, context);
    }

    public R visitNumericTypeParameter(NumericParameter node, C context) {
        return visitNode(node, context);
    }

    public R visitTypeParameter(TypeParameter node, C context) {
        return visitNode(node, context);
    }

    public R visitRowField(RowDataType.Field node, C context) {
        return visitNode(node, context);
    }

    public R visitLiteral(Literal node, C context) {
        return visitNode(node, context);
    }

    public R visitBooleanLiteral(BooleanLiteral node, C context) {
        return visitNode(node, context);
    }

    public R visitFileFormat(FileFormat node, C context) {
        return visitNode(node, context);
    }

    public R visitStringLiteral(StringLiteral node, C context) {
        return visitNode(node, context);
    }
}
