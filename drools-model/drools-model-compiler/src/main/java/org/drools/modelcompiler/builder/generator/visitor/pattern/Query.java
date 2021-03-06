package org.drools.modelcompiler.builder.generator.visitor.pattern;

import java.util.List;

import org.drools.compiler.lang.descr.BaseDescr;
import org.drools.compiler.lang.descr.PatternDescr;
import org.drools.javaparser.ast.expr.Expression;
import org.drools.javaparser.ast.expr.MethodCallExpr;
import org.drools.javaparser.ast.expr.NameExpr;
import org.drools.modelcompiler.builder.PackageModel;
import org.drools.modelcompiler.builder.generator.DeclarationSpec;
import org.drools.modelcompiler.builder.generator.QueryGenerator;
import org.drools.modelcompiler.builder.generator.QueryParameter;
import org.drools.modelcompiler.builder.generator.RuleContext;
import org.drools.modelcompiler.builder.generator.visitor.DSLNode;

import static org.drools.modelcompiler.builder.generator.DslMethodNames.QUERY_INVOCATION_CALL;
import static org.drools.modelcompiler.builder.generator.DslMethodNames.VALUE_OF_CALL;
import static org.drools.modelcompiler.builder.generator.QueryGenerator.toQueryDef;

class Query implements DSLNode {

    private final RuleContext context;
    private final PackageModel packageModel;
    private PatternDescr pattern;
    private List<? extends BaseDescr> constraintDescrs;
    private String queryName;

    public Query(RuleContext context, PackageModel packageModel, PatternDescr pattern, List<? extends BaseDescr> constraintDescrs, String queryName) {
        this.context = context;
        this.packageModel = packageModel;
        this.pattern = pattern;
        this.constraintDescrs = constraintDescrs;
        this.queryName = queryName;
    }

    @Override
    public void buildPattern() {
        NameExpr queryCall = new NameExpr(toQueryDef(pattern.getObjectType()));
        MethodCallExpr callCall = new MethodCallExpr(queryCall, QUERY_INVOCATION_CALL);
        callCall.addArgument("" + !pattern.isQuery());

        if (!constraintDescrs.isEmpty()) {
            List<QueryParameter> queryParams = packageModel.queryVariables( queryName );
            Expression[] queryArgs = new Expression[queryParams.size()];

            for (int i = 0; i < constraintDescrs.size(); i++) {
                String itemText = constraintDescrs.get( i ).getText();
                int colonPos = itemText.indexOf( ':' );

                if ( colonPos > 0 ) {
                    String bindingId = itemText.substring( 0, colonPos ).trim();
                    String paramName = itemText.substring( colonPos + 1 ).trim();

                    for (int j = 0; j < queryParams.size(); j++) {
                        if ( queryParams.get( j ).getName().equals( paramName ) ) {
                            addQueryArg( queryParams, queryArgs, bindingId, j );
                            break;
                        } else if ( queryParams.get( j ).getName().equals( bindingId ) ) {
                            addQueryArg( queryParams, queryArgs, paramName, j );
                            break;
                        }
                    }

                } else {
                    addQueryArg( queryParams, queryArgs, itemText, i );
                }
            }

            for (Expression queryArg : queryArgs) {
                callCall.addArgument( queryArg );
            }
        }

        context.addExpression(callCall);
    }

    private void addQueryArg( List<QueryParameter> queryParams, Expression[] queryArgs, String itemText, int i ) {
        if ( QueryGenerator.isLiteral( itemText ) ) {
            MethodCallExpr valueOfMethod = new MethodCallExpr( null, VALUE_OF_CALL );
            valueOfMethod.addArgument( new NameExpr( itemText ) );
            queryArgs[i] = valueOfMethod;
        } else {
            context.addDeclaration( new DeclarationSpec( itemText, queryParams.get( i ).getType() ) );
            queryArgs[i] = context.getVarExpr( itemText );
        }
    }
}
