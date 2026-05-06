import java.io.*;
import java.nio.file.*;
import java.util.*;


enum TokenType {
    NUMBER, STRING, IDENTIFIER,
    EQUALS, PLUS, MINUS, MULTIPLY, DIVIDE,
    LPAREN, RPAREN, SEMICOLON,
    PRINT, LET, EOF
}

class Token {
    TokenType type;
    String value;
    int line;

    Token(TokenType type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }
    
    @Override
    public String toString() {
        return "Token(" + type + ", '" + value + "', line " + line + ")";
    }
}


class Lexer {
    private String src;
    private int pos = 0;
    private char current;
    private int line = 1;

    private Map<String, TokenType> keywords = new HashMap<>();

    Lexer(String src) {
        this.src = src;
        current = src.length() > 0 ? src.charAt(0) : '\0';
        keywords.put("লিখো", TokenType.PRINT);
        keywords.put("ধরি", TokenType.LET);
    }

    void advance() {
        pos++;
        if (pos >= src.length())
            current = '\0';
        else
            current = src.charAt(pos);
    }

    void skipWhitespace() {
        while (current != '\0' && Character.isWhitespace(current)) {
            if (current == '\n')
                line++;
            advance();
        }
    }

    boolean isBanglaDigit(char c) {
        return c >= '০' && c <= '৯';
    }

    char convertBanglaDigit(char c) {
        return (char) ('0' + (c - '০'));
    }

    Token number() {
        StringBuilder sb = new StringBuilder();
        boolean hasDecimal = false;

        while (current != '\0' &&
              (Character.isDigit(current) || isBanglaDigit(current) || current == '.')) {
            
            if (current == '.') {
                if (hasDecimal) {
                    throw new RuntimeException("Multiple decimal points in number at line " + line);
                }
                hasDecimal = true;
                sb.append(current);
            } else if (isBanglaDigit(current)) {
                sb.append(convertBanglaDigit(current));
            } else {
                sb.append(current);
            }
            
            advance();
        }

        return new Token(TokenType.NUMBER, sb.toString(), line);
    }

    Token identifierOrKeyword() {
        StringBuilder sb = new StringBuilder();
        while (current != '\0' && (Character.isLetterOrDigit(current) || 
               Character.isLetter(current) || current >= 0x0980 && current <= 0x09FF)) {
            sb.append(current);
            advance();
        }
        String word = sb.toString();
        TokenType type = keywords.getOrDefault(word, TokenType.IDENTIFIER);
        return new Token(type, word, line);
    }

    Token string() {
        advance(); 
        StringBuilder sb = new StringBuilder();
        while (current != '\0' && current != '"') {
            if (current == '\\') {
                advance();
                if (current == 'n') sb.append('\n');
                else if (current == 't') sb.append('\t');
                else if (current == '"') sb.append('"');
                else if (current == '\\') sb.append('\\');
                else sb.append('\\').append(current);
            } else {
                sb.append(current);
            }
            advance();
        }
        if (current == '\0') {
            throw new RuntimeException("Unterminated string at line " + line);
        }
        advance(); 
        return new Token(TokenType.STRING, sb.toString(), line);
    }

    List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (current != '\0') {
            if (Character.isWhitespace(current)) {
                skipWhitespace();
                continue;
            }

            if (Character.isDigit(current) || isBanglaDigit(current)) {
                tokens.add(number());
                continue;
            }

            if (Character.isLetter(current) || current >= 0x0980) {
                tokens.add(identifierOrKeyword());
                continue;
            }

            if (current == '"') {
                tokens.add(string());
                continue;
            }

            switch (current) {
                case '=':
                    tokens.add(new Token(TokenType.EQUALS, "=", line));
                    break;
                case '+':
                    tokens.add(new Token(TokenType.PLUS, "+", line));
                    break;
                case '-':
                    tokens.add(new Token(TokenType.MINUS, "-", line));
                    break;
                case '*':
                    tokens.add(new Token(TokenType.MULTIPLY, "*", line));
                    break;
                case '/':
                    tokens.add(new Token(TokenType.DIVIDE, "/", line));
                    break;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, "(", line));
                    break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, ")", line));
                    break;
                case ';':
                    tokens.add(new Token(TokenType.SEMICOLON, ";", line));
                    break;
                default:
                    throw new RuntimeException("Unknown character: '" + current + 
                                             "' at line " + line + ", position " + pos);
            }
            advance();
        }

        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }
}


interface ASTNode {
}

abstract class ExprNode {
}

class NumberNode extends ExprNode {
    String value;

    NumberNode(String v) {
        value = v;
    }
}

class StringNode extends ExprNode {
    String value;

    StringNode(String v) {
        value = v;
    }
}

class VarNode extends ExprNode {
    String name;

    VarNode(String n) {
        name = n;
    }
}

class BinaryOpNode extends ExprNode {
    ExprNode left, right;
    TokenType op;

    BinaryOpNode(ExprNode l, TokenType o, ExprNode r) {
        left = l;
        op = o;
        right = r;
    }
}

class AssignmentNode implements ASTNode {
    String name;
    ExprNode expr;

    AssignmentNode(String n, ExprNode e) {
        name = n;
        expr = e;
    }
}

class PrintNode implements ASTNode {
    ExprNode expr;

    PrintNode(ExprNode e) {
        expr = e;
    }
}


class Parser {
    List<Token> tokens;
    int pos = 0;

    Parser(List<Token> t) {
        tokens = t;
    }

    Token current() {
        if (pos >= tokens.size()) {
            return new Token(TokenType.EOF, "", -1);
        }
        return tokens.get(pos);
    }

    void advance() {
        pos++;
    }

    boolean match(TokenType t) {
        if (current().type == t) {
            advance();
            return true;
        }
        return false;
    }

    void expect(TokenType t) {
        if (!match(t)) {
            throw new RuntimeException("Expected " + t + " but found " + current().type + 
                                     " at line " + current().line);
        }
    }

    ExprNode factor() {
        Token t = current();

        if (match(TokenType.NUMBER))
            return new NumberNode(t.value);
        if (match(TokenType.STRING))
            return new StringNode(t.value);
        if (match(TokenType.IDENTIFIER))
            return new VarNode(t.value);

        if (match(TokenType.LPAREN)) {
            ExprNode e = expr();
            expect(TokenType.RPAREN);
            return e;
        }

        throw new RuntimeException("Invalid expression at line " + t.line);
    }

    ExprNode term() {
        ExprNode node = factor();

        while (current().type == TokenType.MULTIPLY ||
                current().type == TokenType.DIVIDE) {
            TokenType op = current().type;
            advance();
            node = new BinaryOpNode(node, op, factor());
        }

        return node;
    }

    ExprNode expr() {
        ExprNode node = term();

        while (current().type == TokenType.PLUS ||
                current().type == TokenType.MINUS) {
            TokenType op = current().type;
            advance();
            node = new BinaryOpNode(node, op, term());
        }

        return node;
    }

    List<ASTNode> parse() {
        List<ASTNode> list = new ArrayList<>();

        while (current().type != TokenType.EOF) {
            try {
                if (match(TokenType.PRINT)) {
                    expect(TokenType.LPAREN);
                    ExprNode e = expr();
                    expect(TokenType.RPAREN);
                    expect(TokenType.SEMICOLON);
                    list.add(new PrintNode(e));
                }
                else if (match(TokenType.LET)) {
                    Token nameToken = current();
                    expect(TokenType.IDENTIFIER);
                    expect(TokenType.EQUALS);
                    ExprNode e = expr();
                    expect(TokenType.SEMICOLON);
                    list.add(new AssignmentNode(nameToken.value, e));
                }
                else if (current().type == TokenType.IDENTIFIER) {
                    String name = current().value;
                    advance();
                    expect(TokenType.EQUALS);
                    ExprNode e = expr();
                    expect(TokenType.SEMICOLON);
                    list.add(new AssignmentNode(name, e));
                }
                else {
                    throw new RuntimeException("Unexpected token: " + current().type + 
                                             " at line " + current().line);
                }
            } catch (Exception e) {
                System.out.println("Syntax error: " + e.getMessage());
                while (current().type != TokenType.SEMICOLON && 
                       current().type != TokenType.EOF) {
                    advance();
                }
                if (current().type == TokenType.SEMICOLON) {
                    advance(); 
                }
            }
        }

        return list;
    }
}


class SemanticAnalyzer {
    Map<String, String> table = new HashMap<>();
    boolean hasError = false;

    String getType(ExprNode n) {
        if (n instanceof NumberNode)
            return "number";
        if (n instanceof StringNode)
            return "string";

        if (n instanceof VarNode) {
            String name = ((VarNode) n).name;
            if (!table.containsKey(name)) {
                System.out.println("Error: Undefined variable '" + name + "'");
                hasError = true;
                return "unknown";
            }
            return table.get(name);
        }

        if (n instanceof BinaryOpNode) {
            BinaryOpNode b = (BinaryOpNode) n;
            String leftType = getType(b.left);
            String rightType = getType(b.right);
            
            if (leftType.equals("unknown") || rightType.equals("unknown")) {
                return "unknown";
            }
            
            if (leftType.equals("string") || rightType.equals("string")) {
                if (b.op == TokenType.PLUS) {
                    return "string";
                } else {
                    System.out.println("Error: Cannot use operator " + b.op + 
                                     " with string at line");
                    hasError = true;
                    return "unknown";
                }
            }
            
            if (!leftType.equals(rightType)) {
                System.out.println("Error: Type mismatch: " + leftType + " vs " + rightType);
                hasError = true;
            }
            
            return leftType;
        }

        return "unknown";
    }

    void analyze(List<ASTNode> nodes) {
        for (ASTNode n : nodes) {
            if (n instanceof AssignmentNode) {
                AssignmentNode a = (AssignmentNode) n;
                String type = getType(a.expr);
                if (!type.equals("unknown")) {
                    String prevType = table.get(a.name);
                    if (prevType != null && !prevType.equals(type)) {
                        System.out.println("Warning: Variable '" + a.name + 
                                         "' previously declared as " + prevType + 
                                         ", now assigned as " + type);
                    }
                    table.put(a.name, type);
                }
            }
        }
        
        if (!hasError) {
            System.out.println("✓ Semantic analysis completed successfully");
        } else {
            System.out.println("✗ Semantic analysis found errors");
        }
    }
}

class Interpreter {
    private Map<String, Object> environment = new HashMap<>();
    private boolean hasError = false;

    Object evaluate(ExprNode expr) {
        if (expr instanceof NumberNode) {
            String val = ((NumberNode) expr).value;
            if (val.contains(".")) {
                return Double.parseDouble(val);
            } else {
                return Long.parseLong(val);
            }
        }
        
        if (expr instanceof StringNode) {
            return ((StringNode) expr).value;
        }
        
        if (expr instanceof VarNode) {
            String name = ((VarNode) expr).name;
            if (!environment.containsKey(name)) {
                throw new RuntimeException("Undefined variable: " + name);
            }
            return environment.get(name);
        }
        
        if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            Object left = evaluate(binOp.left);
            Object right = evaluate(binOp.right);
            
            switch (binOp.op) {
                case PLUS:
                    if (left instanceof String || right instanceof String) {
                        return left.toString() + right.toString();
                    }
                    if (left instanceof Number && right instanceof Number) {
                        if (left instanceof Double || right instanceof Double) {
                            return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                        } else {
                            return ((Number) left).longValue() + ((Number) right).longValue();
                        }
                    }
                    break;
                case MINUS:
                    if (left instanceof Number && right instanceof Number) {
                        if (left instanceof Double || right instanceof Double) {
                            return ((Number) left).doubleValue() - ((Number) right).doubleValue();
                        } else {
                            return ((Number) left).longValue() - ((Number) right).longValue();
                        }
                    }
                    break;
                case MULTIPLY:
                    if (left instanceof Number && right instanceof Number) {
                        if (left instanceof Double || right instanceof Double) {
                            return ((Number) left).doubleValue() * ((Number) right).doubleValue();
                        } else {
                            return ((Number) left).longValue() * ((Number) right).longValue();
                        }
                    }
                    break;
                case DIVIDE:
                    if (left instanceof Number && right instanceof Number) {
                        double result = ((Number) left).doubleValue() / ((Number) right).doubleValue();
                        return result;
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown operator: " + binOp.op);
            }
            throw new RuntimeException("Invalid operation: " + left + " " + binOp.op + " " + right);
        }
        
        throw new RuntimeException("Unknown expression type");
    }
    
    void execute(List<ASTNode> nodes) {
        System.out.println("\n=== Program Output ===\n");
        
        for (ASTNode node : nodes) {
            try {
                if (node instanceof AssignmentNode) {
                    AssignmentNode assign = (AssignmentNode) node;
                    Object value = evaluate(assign.expr);
                    environment.put(assign.name, value);
                } else if (node instanceof PrintNode) {
                    PrintNode print = (PrintNode) node;
                    Object value = evaluate(print.expr);
                    System.out.println(value);
                }
            } catch (Exception e) {
                System.err.println("Runtime error: " + e.getMessage());
                hasError = true;
            }
        }
        
        if (!hasError) {
            System.out.println("\n✓ Program executed successfully");
        }
    }
}

public class Compiler {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java Compiler <filename.bangla>");
            System.exit(1);
        }
        
        String filename = args[0];
        if (!filename.endsWith(".bangla")) {
            System.err.println("Warning: File should have .bangla extension");
        }

        String src = Files.readString(Paths.get(filename), java.nio.charset.StandardCharsets.UTF_8);
        
        System.out.println("=== Compiling " + filename + " ===\n");
        
        System.out.println("1. Lexical Analysis");
        Lexer lex = new Lexer(src);
        List<Token> tokens = lex.tokenize();
        System.out.println("   ✓ " + tokens.size() + " tokens generated");

        System.out.println("\n2. Parsing");
        Parser parser = new Parser(tokens);
        List<ASTNode> ast = parser.parse();
        System.out.println("   ✓ AST generated with " + ast.size() + " statements");
        
        System.out.println("\n3. Semantic Analysis");
        SemanticAnalyzer sem = new SemanticAnalyzer();
        sem.analyze(ast);
        
        System.out.println("\n4. Interpretation");
        Interpreter interpreter = new Interpreter();
        interpreter.execute(ast);

        System.out.println("\n=== Generating Python Code ===");
        CodeGen gen = new CodeGen();
        String code = gen.generate(ast);
        Files.writeString(Paths.get("out.py"), code);
        System.out.println("✓ Generated out.py");
    }
}


class CodeGen {

    String genExpr(ExprNode n) {
        if (n instanceof NumberNode)
            return ((NumberNode) n).value;

        if (n instanceof StringNode)
            return "\"" + escapeString(((StringNode) n).value) + "\"";

        if (n instanceof VarNode)
            return ((VarNode) n).name;

        if (n instanceof BinaryOpNode) {
            BinaryOpNode b = (BinaryOpNode) n;

            String left = genExpr(b.left);
            String right = genExpr(b.right);

            switch (b.op) {
                case PLUS:
                    return left + " + " + right;

                case MINUS:
                    return left + " - " + right;

                case MULTIPLY:
                    return left + " * " + right;

                case DIVIDE:
                    return left + " / " + right;

                default:
                    throw new RuntimeException("Unknown operator: " + b.op);
            }
        }

        return "";
    }
    
    private String escapeString(String s)
    {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    String generate(List<ASTNode> nodes) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Generated Python code from Bangla compiler\n")
          .append("# Run with: python3 out.py\n\n");

        for (ASTNode n : nodes) {
            if (n instanceof AssignmentNode) {
                AssignmentNode a = (AssignmentNode) n;
                sb.append(a.name)
                  .append(" = ")
                  .append(genExpr(a.expr))
                  .append("\n");
            }

            if (n instanceof PrintNode) {
                PrintNode p = (PrintNode) n;
                sb.append("print(")
                  .append(genExpr(p.expr))
                  .append(")\n");
            }
        }

        return sb.toString();
    }
}