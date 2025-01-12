/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
***/
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.misc.NotNull;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.lang.*;

public class TestPascalVisitor {
    
    public static class EvalVisitor extends PascalBaseVisitor<Value> {
    // used to compare floating point numbers
    public static final double SMALL_VALUE = 0.00000000001;

    // store variables (there's only one global scope!)
    // Symbol tables
    private Map<String, Value> memory = new HashMap<String, Value>();
    private Map<String, PascalParser.BlockContext> procMap = new HashMap<String, PascalParser.BlockContext>();
    private Map<String, PascalParser.BlockContext> funcMap = new HashMap<String, PascalParser.BlockContext>();
    private Map<String, Value> procedureVariableMap = new HashMap<String, Value>();
    private Map<String, Value> functionVariableMap = new HashMap<String, Value>();
    private Map<String, Integer> scopeLevelMap = new HashMap<String, Integer>();
    private Map<String, Integer> scopeNameMap = new HashMap<String, Integer>();
    private ArrayList<String> globalVariables = new ArrayList<String>();
    private ArrayList<String> procedureVariables = new ArrayList<String>();
    private ArrayList<String> functionVariables = new ArrayList<String>();
    private ArrayList<String> procedures = new ArrayList<String>();
    private ArrayList<String> functions = new ArrayList<String>();
    private int scopeTracker = 1; //This will basically be utilized to keep track of increasing scope level declaration names
    private int scopeLevel = 0; //This will be used to keep track of scope level
    private int inProcedures = 0; // This will be used to see if we are in a procedures function
    private int inFunctions = 0; //This will be used to see if we are in a function
    private int procCount = 0; //This will account for the variables in the procedure variables
    private int funcCount = 0; // This will acount for the variables in the function variables
    private int locator = 0; // This will keep track of the scope level for procedures, functions
    //private Boolean alreadyKey = false; // This will account for whether something is a key

    // @Override public Value visitMainBlock(PascalParser.MainBlockContext ctx) {
    //     scopeLevelMap.forEach((key, value) -> System.out.println(key + " " + value));
    //     return this.visit(ctx.statements());
    // }

    public Boolean breakStatus = false;
    public Boolean continueStatus = false;

    @Override public Value visitVisitContinue(PascalParser.VisitContinueContext ctx) {
        //CONTINUE
        //return new Value(String.valueOf(ctx.getText()));
        continueStatus = true;
        return null;
    }
    @Override public Value visitVisitBreak(PascalParser.VisitBreakContext ctx) {
        //BREAK
        //throw error only viewable by the program
        //check inside visitWhile visitDo to see if a statement returns this error
        //if it does then we break our loop
        //we might want to consider changing our loops
        //return an interrupt
        breakStatus = true;
        return null;
    }

    @Override public Value visitWhileDoStatement(PascalParser.WhileDoStatementContext ctx) {
        //WHILE expression DO statement
        Value value = this.visit(ctx.expression());
        scopeLevel++;
        scopeNameMap.put("While" + scopeTracker, scopeTracker);
        scopeTracker++;
        
        
        while(value.asBoolean() && !breakStatus) {
            
            //go through the whiledo parse tree
            Boolean check4Continue = false;
            PascalParser.StatementsContext ccc = ctx.statement().structuredStatement().compoundStatement().statements();
            int x = ctx.statement().structuredStatement().compoundStatement().statements().statement().size();
            int iter = 0;
            //find out how many statements there are
            while(iter<x)
            {
                this.visit(ccc.statement(iter));  //VISIT ONE STATEMENT AT A TIME
                iter++;
                if(continueStatus)
                {
                    continueStatus = false;
                    check4Continue = true;
                    //if we found a continue then lets break out of this while loop
                    break;
                }
            }
            if(check4Continue)
            {
                check4Continue = false;
                value = this.visit(ctx.expression());
                continue;
            }
            //evaluate the block
            //this.visit(ctx.statement());   
            
            value = this.visit(ctx.expression());
        }
        //reset breakStatus
        breakStatus = false;
        return Value.VOID;

    }   
    //for loops with DOWNTO
    @Override public Value visitVisitForDownto(PascalParser.VisitForDowntoContext ctx) {
        //FOR id LET initialVal TO finalVal DO statement
        String id = ctx.id().getText();
        Value initial = this.visit(ctx.initialVal);
        Value finalV = this.visit(ctx.finalVal);
        
        for(double i=initial.asDouble(); i>=finalV.asDouble();i--)
        {   
            //first update the value of the iterating variable
            //the iterating variable cannot be manipulated inside the for loop
            
            memory.replace(id, new Value(i));
            if(breakStatus)
                break;
            //System.out.println(memory.get(id));

            Boolean check4Continue = false;
            //visit statements one at a time within the compound statement block

            //List<PascalParser.StatementContext> csb = ctx.statement().structuredStatement().compoundStatement().statements().statement().size();
            PascalParser.StatementsContext ccc = ctx.statement().structuredStatement().compoundStatement().statements();
            int x = ctx.statement().structuredStatement().compoundStatement().statements().statement().size();
            int iter = 0;
            //find out how many statements there are
            while(iter<x)
            {
                this.visit(ccc.statement(iter));
                if(continueStatus)
                {
                    continueStatus = false;
                    check4Continue = true;
                    //if we found a continue then lets break out of this while loop
                    break;
                }
                iter++;
            }
            
            if(check4Continue)
            {
                check4Continue = false;
                continue;
            }
            //this.visit(ctx.statement());
            
        }
        breakStatus = false;
        return Value.VOID;
    }
    //for loops with TO
    @Override public Value visitVisitForTo(PascalParser.VisitForToContext ctx) {
        //FOR id LET initialVal TO finalVal DO statement
        String id = ctx.id().getText();
        Value initial = this.visit(ctx.initialVal);
        Value finalV = this.visit(ctx.finalVal);
        scopeLevel++;
        scopeNameMap.put("FOR" + scopeTracker, scopeTracker);
        scopeTracker++;
      
        
        
        for(double i=initial.asDouble(); i<=finalV.asDouble();i++)
        {   
            //first update the value of the iterating variable
            //the iterating variable cannot be manipulated inside the for loop
            if(inProcedures == 1 && procedureVariableMap.containsKey(id))
            {
                procedureVariableMap.replace(id, new Value(i));
            }
            else if(inFunctions == 1 && functionVariableMap.containsKey(id))
            {
                functionVariableMap.replace(id, new Value(i));
            }
            else
            {
                memory.replace(id, new Value(i));
            }
            //execute statement
          
            //memory.replace(id, new Value(i));
            if(breakStatus)
                break;
            //System.out.println(memory.get(id));

            Boolean check4Continue = false;
            //visit statements one at a time within the compound statement block

            //List<PascalParser.StatementContext> csb = ctx.statement().structuredStatement().compoundStatement().statements().statement().size();
            PascalParser.StatementsContext ccc = ctx.statement().structuredStatement().compoundStatement().statements();
            int x = ctx.statement().structuredStatement().compoundStatement().statements().statement().size();
            int iter = 0;
            //find out how many statements there are
            while(iter<x)
            {
                this.visit(ccc.statement(iter));
                if(continueStatus)
                {
                    continueStatus = false;
                    check4Continue = true;
                    //if we found a continue then lets break out of this while loop
                    break;
                }
                iter++;
            }

            if(check4Continue)
            {
                check4Continue = false;
                continue;
            }
            //this.visit(ctx.statement());
            
        }
        breakStatus = false;
        return Value.VOID;
    }
    @Override public Value visitFunctionExpression(PascalParser.FunctionExpressionContext ctx) {
        //function ( id )
        //first check if ctx.id() is in the symbol table
        String id = ctx.id().getText();
        Double oldVal;
        if(!memory.containsKey(id))
        {
            throw new RuntimeException("invalid symbol:");
        }
        else
        {
            oldVal = memory.get(id).asDouble();
        }
            Double newVal;
            switch(ctx.function.getType())
            {
                case PascalParser.SQRT:
                    newVal = Math.sqrt(oldVal);
                    memory.replace(id, new Value(newVal));
                    return new Value(newVal);
                case PascalParser.SIN:
                    newVal = Math.sin(oldVal);
                    memory.replace(id, new Value(newVal));
                    return new Value(newVal);
                case PascalParser.COS: 
                    newVal = Math.cos(oldVal);
                    memory.replace(id, new Value(newVal));
                    return new Value(newVal);
                case PascalParser.EXP:
                    newVal = Math.exp(oldVal);
                    memory.replace(id, new Value(newVal));
                    return new Value(newVal);
                case PascalParser.LN:
                    newVal = Math.log(oldVal);
                    memory.replace(id, new Value(newVal));
                    return new Value(newVal);
                default:
                    throw new RuntimeException("unknown operator: " + PascalParser.tokenNames[ctx.function.getType()]);
            }
        
    }

    @Override public Value visitFunctionNumber(PascalParser.FunctionNumberContext ctx) {
        
        Double oldVal = Double.valueOf(ctx.Uinput.getText());
        Double newVal;
        switch(ctx.function.getType())
            {
                case PascalParser.SQRT:
                    newVal = Math.sqrt(oldVal);
                    return new Value(newVal);
                case PascalParser.SIN:
                    newVal = Math.sin(oldVal);
                    return new Value(newVal);
                case PascalParser.COS: 
                    newVal = Math.cos(oldVal);
                    return new Value(newVal);
                case PascalParser.EXP:
                    newVal = Math.exp(oldVal);
                    return new Value(newVal);
                case PascalParser.LN:
                    newVal = Math.log(oldVal);
                    return new Value(newVal);
                default:
                    throw new RuntimeException("unknown operator: " + PascalParser.tokenNames[ctx.function.getType()]);
            }
        
    }

    @Override public Value visitVisitWriteId(PascalParser.VisitWriteIdContext ctx) {
        //check if id is in symbol table
        String id = ctx.id().getText();
        if(memory.containsKey(id))
        {
            //then we can get the symbol
            Value s1 = new Value(memory.get(id));
            System.out.println(s1.asString());
            return new Value(s1.asString());
        }
        return null;

    }
   
    @Override public Value visitVisitWriteEmpty(PascalParser.VisitWriteEmptyContext ctx) {
        //in this scenario of writeln there is nothing inside the parentheses
        System.out.println();
        String newline = "\n";
        return new Value(newline);
    }

    @Override public Value visitVisitWriteExpr(PascalParser.VisitWriteExprContext ctx) {
        //Value value = this.visit(ctx.expression());
        Value temp = this.visit(ctx.expression());
         System.out.println(temp.asString());
         return new Value(temp.asString());
    }
    
    @Override public Value visitVisitReadId(PascalParser.VisitReadIdContext ctx) {
        //System.out.println(memory.values());    //returns symbol table, for debugging
        //get userIO
        Scanner IOScanner = new Scanner(System.in);
        //System.out.print(">");
        String userIO = IOScanner.nextLine();
       
        //find the variable in symbol table to be updated by userIO
        String id = ctx.id().getText(); // the variable id being replaced

        if (memory.containsKey(id))
        {
            //then its in the symbol table
            //now check if userIO is a double or a boolean
            //FIRST WE SHOULD CHECK THE SYMBOL IN THE SYMBOL TABLE TO FIND OUT WHAT ITS TYPE IS!!!!
            Value oldVal = memory.get(id);
            if(oldVal.isDouble())
            {
                //now check if user input is correct
                Value userDouble = new Value(Double.valueOf(userIO));
                return new Value(memory.replace(id, userDouble));
            }
            if(oldVal.isBoolean())
            {
                Value userBoolean = new Value(Boolean.valueOf(userIO));
                return new Value(memory.replace(id, userBoolean));
            }

        }
        
         return null;
    }

    

    @Override public Value visitBoolInput(PascalParser.BoolInputContext ctx) {
        return new Value(Boolean.valueOf(ctx.getText()));
    }
    @Override public Value visitNumInput(PascalParser.NumInputContext ctx) {
        return new Value(Double.valueOf(ctx.getText()));
    }
    @Override public Value visitStringInput(PascalParser.StringInputContext ctx) {
        return new Value(String.valueOf(ctx.getText()));
    }
    @Override public Value visitVariableDeclaration(PascalParser.VariableDeclarationContext ctx) {
        //add a variable with an input to the symbol table
        int x = ctx.getChildCount();
        //if x=3 then there is an idList declaration of variables
        //if x=5 then there is a regular variable declaration happening
        if(x==5) {
        //prints the id for a single variable dec
        String id = ctx.id().getText();
        Value value = this.visit(ctx.input());
        //System.out.println(value.asDouble());

        if(scopeLevel == 0)
        {
            globalVariables.add(id);
        }

        if(inProcedures == 1 && memory.containsKey(id) && scopeLevelMap.get(id) == 0 ||
                inFunctions == 1 && memory.containsKey(id) && scopeLevelMap.get(id) == 0)
        {
            System.out.println("ALREADY A GLOBAL VARIABLE!");
        }
        else if(inProcedures == 1 && memory.containsKey(id) == false)
        {
            procCount++;
            scopeLevelMap.put(id, scopeLevel);
            procedureVariableMap.put(id, value);
        }
        else if(inFunctions == 1 && memory.containsKey(id) == false)
        {
            funcCount++;
            scopeLevelMap.put(id, scopeLevel);
            functionVariableMap.put(id, value);
        }
        else
        {
            scopeLevelMap.put(id, scopeLevel);
            return memory.put(id, value);
        }
        } if(x==3)
        {
            //idList : type   cases
            //get the type as a string
            String type = ctx.type().getText();
            //check if type is REAL or BOOLEAN
            String realSTR = "REAL";
            String booleanSTR = "BOOLEAN";
            String strStr = "STRING";
            if(type.toUpperCase().equals(realSTR))
            {
                //we want to declare all of the variables inside idList as 0.0
                //get the list of ids from idList

                //loop through the idList
                int xd = ctx.idList().getChildCount();
                for(int i=0;i<xd;i=i+2)
                {
                    String newIDEN = ctx.idList().getChild(i).getText();
                    if(scopeLevel == 0)
                    {
                        globalVariables.add(newIDEN);
                    }

                    if(inProcedures == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0 ||
                            inFunctions == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0 )
                    {
                        System.out.println("ALREADY A GLOBAL VARIABLE!");
                    }
                    else if(inProcedures == 1 && memory.containsKey(newIDEN) == false)
                    {
                        procCount++;
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        procedureVariables.add(newIDEN);
                        procedureVariableMap.put(newIDEN, new Value(0.0));
                    }
                    else if(inFunctions == 1 && memory.containsKey(newIDEN) == false)
                    {
                        funcCount++;
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        functionVariables.add(newIDEN);
                        functionVariableMap.put(newIDEN, new Value(0.0));
                    }
                    else
                    {
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        memory.put(newIDEN, new Value(0.0));
                    }
                }
            }
            else if(type.toUpperCase().equals(booleanSTR))
            {
                //we want to declare all of the variables inside idList as TRUE
                //loop through the idList just like in the above if statement
                int xd = ctx.idList().getChildCount();
                for(int i=0;i<xd;i=i+2)
                {
                    String newIDEN = ctx.idList().getChild(i).getText();
                    if(scopeLevel == 0)
                    {
                        globalVariables.add(newIDEN);
                    }
                    if(inProcedures == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0 ||
                        inFunctions == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0)
                    {
                        System.out.println("ALREADY A GLOBAL VARIABLE!");
                    }
                    else if(inProcedures == 1 && memory.containsKey(newIDEN) == false)
                    {
                        procCount++;
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        procedureVariables.add(newIDEN);
                        procedureVariableMap.put(newIDEN, new Value(true));
                    }
                    else if(inFunctions == 1 && memory.containsKey(newIDEN) == false)
                    {
                        funcCount++;
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        functionVariables.add(newIDEN);
                        functionVariableMap.put(newIDEN, new Value(true));
                    }
                    else
                    {
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        memory.put(newIDEN, new Value(true));
                    }
                    
                }
            } else if(type.toUpperCase().equals(strStr))
            {
                int xd = ctx.idList().getChildCount();
                for(int i=0;i<xd;i=i+2)
                {
                    String newIDEN = ctx.idList().getChild(i).getText();
                    if(scopeLevel == 0)
                    {
                        globalVariables.add(newIDEN);
                    }
                    if(inProcedures == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0 ||
                            inFunctions == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0)
                    {
                        System.out.println("ALREADY A GLOBAL VARIABLE!");
                    }
                    else if(inProcedures == 1 && memory.containsKey(newIDEN) == false)
                    {
                        procCount++;
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        procedureVariables.add(newIDEN);
                        procedureVariableMap.put(newIDEN, new Value(""));
                    }
                    else if(inFunctions == 1 && memory.containsKey(newIDEN) == false)
                    {
                        funcCount++;
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        functionVariables.add(newIDEN);
                        functionVariableMap.put(newIDEN, new Value(""));
                    }
                    else
                    {
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        memory.put(newIDEN, new Value(""));
                    }
                }
            }

        }
        
         return null;
    }

    @Override public Value visitParameterGroup(PascalParser.ParameterGroupContext ctx) {
        //add a variable with an input to the symbol table
        int x = ctx.getChildCount();
        //if x=3 then there is an idList declaration of variables
        if(x==3)
        {
            //idList : type cases
            //get the type as a string
            String type = ctx.type().getText();
            //check if type is REAL or BOOLEAN
            String realSTR = "REAL";
            String booleanSTR = "BOOLEAN";
            String strStr = "STRING";
            if(type.toUpperCase().equals(realSTR))
            {
                //we want to declare all of the variables inside idList as 0.0
                //get the list of ids from idList

                //loop through the idList
                int xd = ctx.idList().getChildCount();
                for(int i=0;i<xd;i=i+2)
                {
                    String newIDEN = ctx.idList().getChild(i).getText();
                    if(inProcedures == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0 || 
                    inFunctions == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0 )
                    {
                        System.out.println("ALREADY A GLOBAL VARIABLE!");
                    }
                    else if(inProcedures == 1)
                    {
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        procCount++;
                        procedureVariables.add(newIDEN);
                        procedureVariableMap.put(newIDEN, new Value(0.0));
                    }
                    else if(inFunctions == 1)
                    {
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        funcCount++;
                        functionVariables.add(newIDEN);
                        functionVariableMap.put(newIDEN, new Value(0.0));
                    }
                }
            }
            else if(type.toUpperCase().equals(booleanSTR))
            {
                //we want to declare all of the variables inside idList as TRUE
                //loop through the idList just like in the above if statement
                int xd = ctx.idList().getChildCount();
                for(int i=0;i<xd;i=i+2)
                {
                    String newIDEN = ctx.idList().getChild(i).getText();
                    if(inProcedures == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0 ||
                    inFunctions == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0)
                    {
                        System.out.println("ALREADY A GLOBAL VARIABLE!");
                    }
                    else if(inProcedures == 1)
                    {
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        procCount++;
                        procedureVariables.add(newIDEN);
                        procedureVariableMap.put(newIDEN, new Value(true));
                    }
                    else if(inFunctions == 1)
                    {
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        funcCount++;
                        functionVariables.add(newIDEN);
                        functionVariableMap.put(newIDEN, new Value(true));
                    }
                }
            } else if(type.toUpperCase().equals(strStr))
            {
                int xd = ctx.idList().getChildCount();
                for(int i=0;i<xd;i=i+2)
                {
                    String newIDEN = ctx.idList().getChild(i).getText();
                    if(inProcedures == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0 ||
                    inFunctions == 1 && memory.containsKey(newIDEN) && scopeLevelMap.get(newIDEN) == 0 )
                    {
                        System.out.println("ALREADY A GLOBAL VARIABLE!");
                    }
                    else if(inProcedures == 1)
                    {
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        procCount++;
                        procedureVariables.add(newIDEN);
                        procedureVariableMap.put(newIDEN, new Value(""));
                    }
                    else if(inFunctions == 1)
                    {
                        scopeLevelMap.put(newIDEN, scopeLevel);
                        funcCount++;
                        functionVariables.add(newIDEN);
                        functionVariableMap.put(newIDEN, new Value(""));
                    }
                }
            }
        }
        
         return null;
    }

    @Override public Value visitNotExpression(PascalParser.NotExpressionContext ctx) {
        Value value = this.visit(ctx.expression());
        return new Value(!value.asBoolean());
    }
    @Override public Value visitParentExpression(PascalParser.ParentExpressionContext ctx) {
        return this.visit(ctx.expression());
    }
    @Override public Value visitMultExpression(PascalParser.MultExpressionContext ctx) {
        Value left = this.visit(ctx.expression(0));
        Value right = this.visit(ctx.expression(1));

        switch(ctx.op.getType())
        {
            case PascalParser.MULT:
                return new Value(left.asDouble() * right.asDouble());
            case PascalParser.DIV:
                return new Value(left.asDouble() / right.asDouble());
            case PascalParser.AND:
                return new Value(left.asBoolean() == right.asBoolean());
        
        default:
                throw new RuntimeException("unknown operator: " + PascalParser.tokenNames[ctx.op.getType()]);
    }
}
    @Override public Value visitPlusExpression(PascalParser.PlusExpressionContext ctx) {
        Value left = this.visit(ctx.expression(0));
        Value right = this.visit(ctx.expression(1));

        switch(ctx.op.getType())
        {
            case PascalParser.OR:
                return new Value(left.asBoolean() || right.asBoolean());
            case PascalParser.PLUS:
                return left.isDouble() && right.isDouble() ? 
                    new Value(left.asDouble() + right.asDouble()):
                    new Value(left.asString() + right.asString());
            case PascalParser.MINUS:
                    return new Value(left.asDouble() - right.asDouble());
                default:
                    throw new RuntimeException("unknown operator: " + PascalParser.tokenNames[ctx.op.getType()]);
            }
    }

    @Override public Value visitRelationExpression(PascalParser.RelationExpressionContext ctx) {
        Value left = this.visit(ctx.expression(0));
        Value right = this.visit(ctx.expression(1));

        switch (ctx.op.getType()) {
            case PascalParser.EQUAL:
                if(left.isDouble() && right.isDouble())
                {
                    return left.isDouble() && right.isDouble() ?
                    new Value(Math.abs(left.asDouble() - right.asDouble()) < SMALL_VALUE) :
                    new Value(left.equals(right));
                } else if(left.isBoolean() && right.isBoolean()) {
                    return new Value(left.asBoolean() == right.asBoolean());
                } throw new RuntimeException("need booleans or double to check if EQUAL(=)");
            case PascalParser.NOT_EQUAL:
                if(left.isDouble() && right.isDouble())
                {
                    return left.isDouble() && right.isDouble() ?
                     new Value(Math.abs(left.asDouble() - right.asDouble()) >= SMALL_VALUE) :
                    new Value(!left.equals(right));
                } else if(left.isBoolean() && right.isBoolean()) {
                    return new Value(left.asBoolean() != right.asBoolean());
                } throw new RuntimeException("need booleans or double to check if NOT_EQUAL");
                
            case PascalParser.LT:
                return new Value(left.asDouble() < right.asDouble());
            case PascalParser.LE:
                return new Value(left.asDouble() <= right.asDouble());
            case PascalParser.GT:
                return new Value(left.asDouble() > right.asDouble());
            case PascalParser.GE:
                return new Value(left.asDouble() >= right.asDouble());
            default:
                throw new RuntimeException("unknown operator: " + PascalParser.tokenNames[ctx.op.getType()]);
        }
    }
    @Override public Value visitNumExpression(PascalParser.NumExpressionContext ctx) {
        return new Value(Double.valueOf(ctx.getText()));
    }
    @Override public Value visitNumConstant(PascalParser.NumConstantContext ctx) {
        return new Value(Double.valueOf(ctx.getText()));
    }
    @Override public Value visitBoolExpression(PascalParser.BoolExpressionContext ctx) {
        return new Value(Boolean.valueOf(ctx.getText()));
    }
    @Override public Value visitBoolConstant(PascalParser.BoolConstantContext ctx) {
        return new Value(Boolean.valueOf(ctx.getText()));
    }
    @Override public Value visitStrConstant(PascalParser.StrConstantContext ctx) {
        return new Value(String.valueOf(ctx.getText()));
    }
    @Override public Value visitIdExpression(PascalParser.IdExpressionContext ctx) {
        String id = ctx.getText();
        Value value = null;
        if(inProcedures == 1)
        {
            if(locator == scopeLevelMap.get(id))
            {
                value = procedureVariableMap.get(id);
            }

            if(value == null && scopeLevelMap.get(id) == 0)
            {
                value = memory.get(id);
            }
            else if(value == null)
            {
                System.out.println("No such variable: " + id);
            }
        }
        else if(inFunctions == 1)
        {
            if(locator == scopeLevelMap.get(id))
            {
                value = functionVariableMap.get(id);
            }
            else if(value == null && scopeLevelMap.get(id) == 0)
            {
                value = memory.get(id);
            }
            else if(value == null)
            {
                System.out.println("No such variable: " + id);
            }
        }
        else
        {
            //System.out.println("The scope level is currently: " + scopeLevel);
            // if(scopeLevelMap.get(id) == null)
            // {
            //     scopeLevelMap.put(id, scopeLevel);
            // }

            if(scopeLevelMap.get(id) == 0)
            {
                value = memory.get(id);
            }
            else if(scopeLevelMap.get(id) == scopeLevel)
            {
                value = memory.get(id);
            }
            else if(value == null)
            {
                System.out.println("No such variable: " + id);
            }

        }
        return value;
    }
    
    @Override public Value visitIdConstant(PascalParser.IdConstantContext ctx) {
        String id = ctx.getText();
        Value value = memory.get(id);
        String scope = id + scopeLevel;
        if(scopeLevel != 0 && memory.containsKey(scope)) 
        {
            value = memory.get(scope);
        }
        else if(value == null) {
            throw new RuntimeException("no such variable: " + id);
        }
        else
        {
            value = memory.get(id);
        }
        return value;
    }
    @Override public Value visitStringExpression(PascalParser.StringExpressionContext ctx) {
        String str = ctx.getText();
        return new Value(str);
    }

   @Override public Value visitAssignStatement(PascalParser.AssignStatementContext ctx) {
       //id = expression
       String id = ctx.id().getText();
       Value value = this.visit(ctx.expression());

       if(inProcedures == 1)
        {
            if(procedureVariableMap.containsKey(id) && locator == scopeLevelMap.get(id))
            {
                return new Value(procedureVariableMap.put(id, value));
            }
            else if(scopeLevelMap.get(id) == 0)
            {
                return new Value(memory.put(id, value));
            }
            else if(value == null)
            {

                throw new RuntimeException("no such variable: " + id);
            }

            return value;
        }
        else if(inFunctions == 1)
        {
            if(functionVariableMap.containsKey(id) && locator == scopeLevelMap.get(id))
            {
                return new Value(functionVariableMap.put(id, value));
            }
            else if(scopeLevelMap.get(id) == 0)
            {
                return new Value(memory.put(id, value));
            }
            else if(value == null)
            {
                throw new RuntimeException("no such variable: " + id);
            }

            return value;
        }
        else
        {
            //System.out.println("The value of id: " + id + " is -> " + value);
            return new Value(memory.put(id,value));
        }
   }

   @Override public Value visitIfStatement(PascalParser.IfStatementContext ctx) {

    PascalParser.ConditionBlockContext conditions = ctx.conditionBlock();
    //conditions: expression THEN statement
    //conditions.expression() conditions.statement()

    boolean evaluatedBlock = false;

    Value evaluated = this.visit(conditions.expression());
    if(evaluated.asBoolean())
    {
        evaluatedBlock = true;
        PascalParser.StatementContext then_statement = conditions.statement();
        
        return this.visit(then_statement);
        
    }

    if(!evaluatedBlock && ctx.statement() != null) 
    {
        // evaluate the else-stat_block (if present, the else-stat_block == not null)
        return this.visit(ctx.statement());
    }


    return null;
   }
    
    @Override public Value visitCaseStatement(PascalParser.CaseStatementContext ctx) {
            String id = ctx.id().getText(); //GPA, num1, etc
            String variableValue = id + scopeLevel;
            Value expected;
            //retrieve Value from symbol table
            if(!memory.containsKey(id))
            {
                throw new RuntimeException("Use a variable inside your case(variable) expression"); 
            }
            else if (memory.containsKey(variableValue))
            {
                expected = new Value(memory.get(variableValue));
            }
            else
            {
                expected = new Value(memory.get(id)); //GPA=3.0
            }
            //for each constList
            //if a constant within constList is equal to expression
            //then evaluate the statement related to the constList
            //ctx.getChild(3) is the first constList
            boolean evaluatedCase = false;
            List<PascalParser.CaseNumberContext> cases = ctx.caseNumber();
            for(PascalParser.CaseNumberContext caseNUM : cases) {
                PascalParser.ConstListContext cList = caseNUM.constList();
                int x = cList.getChildCount();
                //number of children inside a constList
                x = x + 1;
                x = x / 2;
                int newX = (int)x;
                for(int i=0;i<newX;i++)
                {
                    Value evaluated = this.visit(cList.constant(i));
                    //System.out.println(evaluated.asString());
                    //System.out.println(cList.constant());
                    if(evaluated.asString().equals(expected.asString()))
                    {
                        evaluatedCase = true;
                        return this.visit(caseNUM.statement());
                    }
                }
            }
            if(!evaluatedCase && ctx.statements() != null) 
            {
                // evaluate the else-stat_block (if present == not null)
                return this.visit(ctx.statements());
            }
            return null;
        }

    @Override public Value visitProcedureCall(PascalParser.ProcedureCallContext ctx)
    {
        String name = ctx.id().getText();
        String params = ctx.parameterList().getText();
        String[] elements = params.split(",");
        int index = 0; //This is to keep track of global variables
        int correctPosition = 0; //Keeps track of the correct position of procedure variables
        if(procedures.contains(name))
        {
            locator = scopeNameMap.get(name);
            int location = procedures.indexOf(name);
            if(location > 0)
            {
                location--;
            }
            while(location > 0)
            {
                correctPosition += Integer.parseInt(procedures.get(location));
            }
            location = correctPosition;
            inProcedures = 1;
            for(String names: procedureVariables)
            {
                procedureVariableMap.replace(procedureVariables.get(location), memory.get(elements[index]));
                location++;
                index++;
            }
            // System.out.println("The Procedure Variable Map - Before");
            // procedureVariableMap.forEach((key, value) -> System.out.println(key + " " + value));
            PascalParser.BlockContext block = procMap.get(name);
            this.visit(block);
            // System.out.println("The Procedure Variable Map - After");
            // procedureVariableMap.forEach((key, value) -> System.out.println(key + " " + value));
            // System.out.println("The arraylist (after) is: ");
            // for(String names: procedureVariables)
            // {
            //     System.out.println(names);
            // }
            int space = procedures.indexOf(name) + 1;
            int totalNumberOfVariables = Integer.parseInt(procedures.get(space)) + procCount;
            procedures.set(space, Integer.toString(totalNumberOfVariables));   

            location = correctPosition;
            //System.out.println("The value of location is: " + location);
            index = 0;
            int size = elements.length;
            //This will check to see if the procedure was called with no parameters!
            if(elements.length == 1 && elements[0].isBlank())
            {
                size = 0;
            }
            if(size != 0)
            {
                for(String names: elements)
                {
                memory.replace(elements[index], procedureVariableMap.get(procedureVariables.get(location)));
                location++;
                index++;
                }
            }
            
            procCount = 0;
            inProcedures = 0;
        }
        else
        {
            System.out.println("This procedure was not defined!");
        }
        return null;
    }

    @Override public Value visitProcedureDeclaration(PascalParser.ProcedureDeclarationContext ctx) {
        //PROCEDURE procName=id (procVariable=formalParameterList)? ';' block
        String procName = ctx.id().getText();

        if(procedures.contains(procName))
        {
            System.out.println("Procedure name already exits!");
        }
        else
        {
            scopeLevel++;
            scopeNameMap.put(procName, scopeTracker);
            scopeTracker++;
            procedures.add(procName);
            procMap.put(procName, ctx.block());

            inProcedures = 1;
            this.visit(ctx.formalParameterList());
            inProcedures = 0;
    
            procedures.add(Integer.toString(procCount));
    
            procCount = 0;
        }

        return null;

    }

   @Override public Value visitFunctionCall(PascalParser.FunctionCallContext ctx) {
    //id '(' parameterList ')'
    String name = ctx.id().getText();
    String params = ctx.parameterList().getText();
    String[] elements = params.split(",");
    int index = 0;
    int correctPosition = 0;

    if(functions.contains(name))
    {
        locator = scopeNameMap.get(name);
        int location = functions.indexOf(name);
        correctPosition = location;
        inFunctions = 1;
        //System.out.println("I got to before the function call!");

        for(int i = 0; i < elements.length; i++)
        {
            functionVariableMap.replace(functionVariables.get(location), memory.get(elements[i]));
            location++;
        }

        PascalParser.BlockContext block = funcMap.get(name);
        this.visit(block);

        //System.out.println("I got to after the function call!");

        // System.out.println("All Global Variables");

        // memory.forEach((key, value) -> System.out.println(key + " " + value));

        // System.out.println("All Function Variables");

        // functionVariableMap.forEach((key, value) -> System.out.println(key + " " + value));

        // System.out.println("The value of " + name + " is " + functionVariableMap.get(name));
        
        // System.out.println("The scope level right now is: " + scopeLevel);

        // System.out.println("The ArrayList order!");

        // for(String names : functionVariables)
        // {
        //     System.out.println(names);
        // }

        int space = functions.indexOf(name) + 1;
        int totalNumberOfVariables = Integer.parseInt(functions.get(space)) + funcCount;
        functions.set(space, Integer.toString(totalNumberOfVariables));   
        
        funcCount = 0;
        inFunctions = 0;

        return functionVariableMap.get(name);
    }
    else
    {
        System.out.println("This function was not defined!");
        return null;
    }
   }

   @Override public Value visitFunctionDeclaration(PascalParser.FunctionDeclarationContext ctx) {
       // FUNCTION functionName=id (functionVariables=formalParameterList)? ':' type ';' block
       String funcName = ctx.id().getText();

       if(functions.contains(funcName))
       {
           System.out.println("Function Name already exists");
       }
       else
       {
           scopeLevel++;
           scopeNameMap.put(funcName, scopeTracker);
           scopeTracker++;

           functions.add(funcName);
           funcMap.put(funcName, ctx.block());

           inFunctions = 1;
           this.visit(ctx.formalParameterList());
           inFunctions = 0;
    
           String realSTR = "REAL";
           String booleanSTR = "BOOLEAN";
           functionVariables.add(funcName);
           funcCount++;
           if(ctx.type().getText().toUpperCase().equals(realSTR))
           {
                functionVariableMap.put(funcName, new Value(0.0));
           }
           if(ctx.type().getText().toUpperCase().equals(booleanSTR))
           {
                functionVariableMap.put(funcName, new Value(true));
           }

           scopeLevelMap.put(funcName, scopeLevel);
           functions.add(Integer.toString(funcCount));
    
           funcCount = 0;
       }

       return null;
   }
}
    
    public static void main(String[] args) throws Exception {
        String inputFile = null;
        if ( args.length>0 ) inputFile = args[0];
        InputStream is = System.in;
        if ( inputFile!=null ) {
            is = new FileInputStream(inputFile);
        }
        //CharStreams.fromFileName("myinputfile")
        ANTLRInputStream input = new ANTLRInputStream(is);

        PascalLexer lexer = new PascalLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PascalParser parser = new PascalParser(tokens);
        
        ParseTree tree = parser.program();
        EvalVisitor visitor = new EvalVisitor();
        visitor.visit(tree);
        /*
        parser.setBuildParseTree(true);      // tell ANTLR to build a parse tree
        ParseTree tree = parser.s(); // parse
        // show tree in text form
        System.out.println(tree.toStringTree(parser));

        EvalVisitor evalVisitor = new EvalVisitor();
        //int result = evalVisitor.visit(tree);
        //System.out.println("visitor result = "+result);
        evalVisitor.visit(tree);
        */
    }
}
