package apps;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {

	/**
	 * Expression to be evaluated
	 */
	String expr;                
    
	/**
	 * Scalar symbols in the expression 
	 */
	ArrayList<ScalarSymbol> scalars;   
	
	/**
	 * Array symbols in the expression
	 */
	ArrayList<ArraySymbol> arrays;
    
    /**
     * String containing all delimiters (characters other than variables and constants), 
     * to be used with StringTokenizer
     */
    public static final String delims = " \t*+-/()[]";
    
    /**
     * Initializes this Expression object with an input expression. Sets all other
     * fields to null.
     * 
     * @param expr Expression
     */
    public Expression(String expr) {
        this.expr = expr;
    }

    /**
     * Populates the scalars and arrays lists with symbols for scalar and array
     * variables in the expression. For every variable, a SINGLE symbol is created and stored,
     * even if it appears more than once in the expression.
     * At this time, values for all variables are set to
     * zero - they will be loaded from a file in the loadSymbolValues method.
     */
    public void buildSymbols() {
    	scalars = new ArrayList<ScalarSymbol>();
    	arrays = new ArrayList<ArraySymbol>();
    	int count = 0;
    	while (count < expr.length()) {
    		String temp = "";
    		while (count < expr.length() && Character.isLetter(expr.charAt(count))) {
    			temp = temp + expr.charAt(count);
    			count++;
    		}
    		if (temp != "") {
    			if (count < expr.length() && expr.charAt(count) == '[') {
    				ArraySymbol tempArray = new ArraySymbol(temp);
    				arrays.add(tempArray);
    			} 
    			else {
    				ScalarSymbol tempScalars = new ScalarSymbol(temp);
    				scalars.add(tempScalars);
    			}
    		}
    		count++;
    	}
    }
    
    /**
     * Loads values for symbols in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     */
    public void loadSymbolValues(Scanner sc) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String sym = st.nextToken();
            ScalarSymbol ssymbol = new ScalarSymbol(sym);
            ArraySymbol asymbol = new ArraySymbol(sym);
            int ssi = scalars.indexOf(ssymbol);
            int asi = arrays.indexOf(asymbol);
            if (ssi == -1 && asi == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                scalars.get(ssi).value = num;
            } else { // array symbol
            	asymbol = arrays.get(asi);
            	asymbol.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    asymbol.values[index] = val;              
                }
            }
        }
    }
    
    
    /**
     * Evaluates the expression, using RECURSION to evaluate subexpressions and to evaluate array 
     * subscript expressions.
     * 
     * @return Result of evaluation
     */
    public float evaluate() {
        String e = this.expr;
        int count = 0;
        while (count < e.length()) {
            if (e.charAt(count) == ' ') {
                e = e.substring(0,count) + e.substring (count+1,e.length());
                count--;
            }
            count++;
        }
        for (int i = 0; i < this.scalars.size(); i++) {
            e = e.replace(this.scalars.get(i).name,Integer.toString(this.scalars.get(i).value));
        }
        e = "("+e+")";
        Stack<String> stk = new Stack<String>();
        stk = merge(e);
        Stack<String> oper = new Stack<String>();
        Stack<String> result = new Stack<String>();
        try {
            return evaluate(stk, result, oper);
        }
        catch(Exception t) {
            return 0;
        }
    }

    private Stack<String> merge(String expr) {
    	Stack <String> stktemp = new Stack<String>();
    	Stack <String> result = new Stack<String>();
    	int count = 0;
    	while(count < expr.length()) {
	    	if(Character.isLetter(expr.charAt(count))) {
	    		String temp = "";
		    	while(count<expr.length() && Character.isLetter(expr.charAt(count))) {
			    	temp = temp + expr.charAt(count);
			    	count++;
		    	}
		    	stktemp.push(temp);
	    	}
	    	else if(Character.isDigit(expr.charAt(count))) {
	    		String temp = "";
	    		while(count<expr.length() && Character.isDigit(expr.charAt(count))) {
		    		temp = temp + expr.charAt(count);
		    		count++;
	    		}
	    		stktemp.push(temp);
	    	}
    		else {
	    		stktemp.push(String.valueOf(expr.charAt(count)));
	    		count++;
    		}
    	}
    	while (!stktemp.isEmpty()) {
    		result.push(stktemp.pop());
    	}
    	return result;
    }

    private float evaluate(Stack<String> stk, Stack<String> result, Stack<String> oper) {
        if (stk.isEmpty() && result.size() == 1) {
            return Float.valueOf(result.pop());
        }
        else if (result.isEmpty() && !stk.isEmpty()) {
            if (stk.peek().equals("+") || stk.peek().equals("-") || stk.peek().equals("*") || stk.peek().equals("/")) {
                if (!oper.isEmpty() && oper.peek().equals("~")) {oper.pop();}
                oper.push(stk.pop());
            }
            else {result.push(stk.pop());}
            return evaluate(stk,result,oper);
        }
        else {
            if (stk.size()>1 && !result.peek().equals(")") && !oper.isEmpty() && (oper.peek().equals("*") || oper.peek().equals("/"))) {
                if(stk.peek().equals("(")) {
                    result.push(stk.pop());
                    oper.push("~");
                    return evaluate(stk, result, oper);
                    } 
                    else {
                        String first = stk.pop(), mark = oper.pop(), second = result.pop();
                    result.push("(");
                    result.push(second);
                    oper.push(mark);
                    result.push(first);
                    result.push(")");
                    return evaluate(stk, result, oper);
                }
            }
            else if (!result.peek().equals(")") && !result.peek().equals("]")) {
                if (stk.peek().equals("+") || stk.peek().equals("-") || stk.peek().equals("*") || stk.peek().equals("/")) {
                    if (!oper.isEmpty() && oper.peek().equals("~")) {oper.pop();}
                    oper.push(stk.pop());
                }
                else {result.push(stk.pop());}
                return evaluate(stk,result,oper);
            }
            else if (result.peek().equals(")")) {
                result.pop();
                String first = result.pop();
                if (result.peek().equals("(")) {
                    result.pop();
                    if(!oper.isEmpty()&&(oper.peek().equals("*")||oper.peek().equals("/"))) {
                        String afterT = result.pop();
                        String mark = oper.pop();
                        if(mark.equals("*")) {
                            float answer = Float.valueOf(first)*Float.valueOf(afterT);
                            result.push(String.valueOf(answer));
                            return evaluate(stk, result, oper);
                        }
                        else if (mark.equals("/")) {
                            float answer = Float.valueOf(afterT)/Float.valueOf(first);
                            result.push(String.valueOf(answer));
                            return evaluate(stk, result, oper);
                        }
                    }
                    result.push(first);
                    return evaluate(stk,result,oper);
                }
                else {
                    String mark = oper.pop();
                    String second = result.pop();
                    if (!oper.isEmpty() && oper.peek().equals("-") && !result.peek().equals("(") && !result.peek().equals("[")) {
                        if(mark.equals("+")) {
                            result.push("(");
                            result.push(second);
                            oper.push("-");
                            result.push(first);
                            result.push(")");
                            stk.push(")");
                            return evaluate(stk, result, oper);
                        }
                        else if(mark.equals("-")) {
                            result.push("(");
                            result.push(second);
                            oper.push("+");
                            result.push(first);
                            result.push(")");
                            stk.push(")");
                            return evaluate(stk, result, oper);
                        }
                    }
                    if(mark.equals("+")) {
                        float temp = Float.valueOf(first)+Float.valueOf(second);
                        result.push(String.valueOf(temp));
                    }
                    else if(mark.equals("-")) {
                        float temp = Float.valueOf(second)-Float.valueOf(first);
                        result.push(String.valueOf(temp));
                    }
                    else if(mark.equals("*")) {
                        float temp = Float.valueOf(second)*Float.valueOf(first);
                        result.push(String.valueOf(temp));
                    }
                    else if(mark.equals("/")) {
                        float temp = Float.valueOf(second)/Float.valueOf(first);
                        result.push(String.valueOf(temp));
                    }
                    result.push(")");
                    return evaluate (stk, result, oper);
                }
            }
            else if(result.peek().equals("]")) {
                result.pop();
                String first = result.pop();
                if(result.peek().equals("[")) {
                    result.pop();
                    String name = result.pop();
                    int[] tempArray = null;
                    for (int i = 0; i < arrays.size(); i++) {
                        if (arrays.get(i).name.equals(name)) {
                            tempArray = arrays.get(i).values;
                            break;
                        }
                    }
                    float temp = Float.valueOf(first);
                    int tempInt = (int) temp;
                    result.push(String.valueOf(tempArray[tempInt]));
                    return evaluate (stk, result, oper);
                }
                else {
                    String mark = oper.pop();
                    String second = result.pop();
                    if(mark.equals("+")) {
                        float temp = Float.valueOf(first)+Float.valueOf(second);
                        result.push(String.valueOf(temp));
                    }
                    else if(mark.equals("-")) {
                        float temp = Float.valueOf(second)-Float.valueOf(first);
                        result.push(String.valueOf(temp));
                    }
                    else if(mark.equals("*")) {
                        float temp = Float.valueOf(second)*Float.valueOf(first);
                        result.push(String.valueOf(temp));
                    } 
                    else if(mark.equals("/")) {
                        float temp = Float.valueOf(second)*Float.valueOf(first);
                        result.push(String.valueOf(temp));
                    }
                    result.push("]");
                    return evaluate (stk, result, oper);
                }
            }
            return 0;
        }
    }
    
    /**
     * Utility method, prints the symbols in the scalars list
     */
    public void printScalars() {
        for (ScalarSymbol ss: scalars) {
            System.out.println(ss);
        }
    }
    
    /**
     * Utility method, prints the symbols in the arrays list
     */
    public void printArrays() {
    		for (ArraySymbol as: arrays) {
    			System.out.println(as);
    		}
    }

}
