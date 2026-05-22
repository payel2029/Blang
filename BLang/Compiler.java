import java.io.*;
import java.nio.file.*;
import java.util.*;

enum TokenType
{
    NUMBER, STRING, IDENTIFIER,
    EQUALS, PLUS, MINUS, MULTIPLY, DIVIDE,
    LPAREN, RPAREN, LBRACE, RBRACE,
    SEMICOLON,

    PRINT, LET,
    IF, ELSE, GT,

    EOF
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
        keywords.put("যদি", TokenType.IF);
        keywords.put("অন্যথায়", TokenType.ELSE);
    }

    void advance() {
        pos++;
        current = (pos >= src.length()) ? '\0' : src.charAt(pos);
    }

    void skipWhitespace() {
        while (current != '\0' && Character.isWhitespace(current)) {
            if (current == '\n') line++;
            advance();
        }
    }

    boolean isBanglaDigit(char c) {
        return c >= '০' && c <= '৯';
    }

    char convertBanglaDigit(char c) {
        return (char) ('0' + (c - '০'));
    }

    Token string() {
        advance(); // skip "

        StringBuilder sb = new StringBuilder();

        while (current != '\0' && current != '"') {

            if (current == '\\') {
                advance();
                if (current == 'n') sb.append('\n');
                else if (current == 't') sb.append('\t');
                else sb.append(current);
            } else {
                sb.append(current);
            }

            advance();
        }

        if (current == '\0') {
            throw new RuntimeException("Unterminated string");
        }

        advance(); 
        return new Token(TokenType.STRING, sb.toString(), line);
    }

    Token number() {
        StringBuilder sb = new StringBuilder();
        boolean dot = false;

        while (current != '\0' &&
                (Character.isDigit(current) || isBanglaDigit(current) || current == '.')) {

            if (current == '.') {
                if (dot) throw new RuntimeException("Invalid number");
                dot = true;
                sb.append('.');
            } else if (isBanglaDigit(current)) {
                sb.append(convertBanglaDigit(current));
            } else {
                sb.append(current);
            }
            advance();
        }

        return new Token(TokenType.NUMBER, sb.toString(), line);
    }

    Token identifier() {
        StringBuilder sb = new StringBuilder();

        while (current != '\0' &&
                (Character.isLetterOrDigit(current) || current >= 0x0980)) {
            sb.append(current);
            advance();
        }

        String word = sb.toString();
        return new Token(keywords.getOrDefault(word, TokenType.IDENTIFIER), word, line);
    }

    List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (current != '\0') {

            if (Character.isWhitespace(current)) {
                skipWhitespace();
                continue;
            }

            if (current == '"') {
                tokens.add(string());
                continue;
            }

            if (Character.isDigit(current) || isBanglaDigit(current)) {
                tokens.add(number());
                continue;
            }

            if (Character.isLetter(current) || current >= 0x0980) {
                tokens.add(identifier());
                continue;
            }

            switch (current) {
                case '=' -> tokens.add(new Token(TokenType.EQUALS, "=", line));
                case '+' -> tokens.add(new Token(TokenType.PLUS, "+", line));
                case '-' -> tokens.add(new Token(TokenType.MINUS, "-", line));
                case '*' -> tokens.add(new Token(TokenType.MULTIPLY, "*", line));
                case '/' -> tokens.add(new Token(TokenType.DIVIDE, "/", line));
                case '(' -> tokens.add(new Token(TokenType.LPAREN, "(", line));
                case ')' -> tokens.add(new Token(TokenType.RPAREN, ")", line));
                case '{' -> tokens.add(new Token(TokenType.LBRACE, "{", line));
                case '}' -> tokens.add(new Token(TokenType.RBRACE, "}", line));
                case ';' -> tokens.add(new Token(TokenType.SEMICOLON, ";", line));
                case '>' -> tokens.add(new Token(TokenType.GT, ">", line));
                default -> throw new RuntimeException("Unknown char: " + current);
            }

            advance();
        }

        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }
}

interface ASTNode {}

abstract class ExprNode implements ASTNode {}

class NumberNode extends ExprNode {
    String value;
    NumberNode(String v) { value = v; }
}

class StringNode extends ExprNode {
    String value;
    StringNode(String v) { value = v; }
}

class VarNode extends ExprNode {
    String name;
    VarNode(String n) { name = n; }
}

class BinaryOpNode extends ExprNode {
    ExprNode left, right;
    TokenType op;

    BinaryOpNode(ExprNode l, TokenType o, ExprNode r) {
        left = l; op = o; right = r;
    }
}

/* Statements */
class AssignmentNode implements ASTNode {
    String name;
    ExprNode expr;

    AssignmentNode(String n, ExprNode e) {
        name = n; expr = e;
    }
}

class PrintNode implements ASTNode {
    ExprNode expr;
    PrintNode(ExprNode e) { expr = e; }
}

class IfNode implements ASTNode {
    ExprNode cond;
    List<ASTNode> ifBlock;
    List<ASTNode> elseBlock;

    IfNode(ExprNode c, List<ASTNode> i, List<ASTNode> e) {
        cond = c;
        ifBlock = i;
        elseBlock = e;
    }
}

class Parser {
    List<Token> tokens;
    int pos = 0;

    Parser(List<Token> t) { tokens = t; }

    Token cur() {
        return pos < tokens.size() ? tokens.get(pos)
                : new Token(TokenType.EOF, "", -1);
    }

    void advance() { pos++; }

    boolean match(TokenType t) {
        if (cur().type == t) {
            advance();
            return true;
        }
        return false;
    }

    void expect(TokenType t) {
        if (!match(t))
            throw new RuntimeException("Expected " + t);
    }

    ExprNode factor() {
        Token t = cur();

        if (match(TokenType.NUMBER)) return new NumberNode(t.value);
        if (match(TokenType.STRING)) return new StringNode(t.value);
        if (match(TokenType.IDENTIFIER)) return new VarNode(t.value);

        if (match(TokenType.LPAREN)) {
            ExprNode e = expr();
            expect(TokenType.RPAREN);
            return e;
        }

        throw new RuntimeException("Invalid expression");
    }

    ExprNode term() {
        ExprNode n = factor();

        while (cur().type == TokenType.MULTIPLY ||
               cur().type == TokenType.DIVIDE) {
            TokenType op = cur().type;
            advance();
            n = new BinaryOpNode(n, op, factor());
        }
        return n;
    }

    ExprNode expr() {
        ExprNode n = term();

        while (cur().type == TokenType.PLUS ||
               cur().type == TokenType.MINUS ||
               cur().type == TokenType.GT) {

            TokenType op = cur().type;
            advance();
            n = new BinaryOpNode(n, op, term());
        }
        return n;
    }

    List<ASTNode> block() {
        List<ASTNode> list = new ArrayList<>();
        expect(TokenType.LBRACE);

        while (cur().type != TokenType.RBRACE && cur().type != TokenType.EOF) {
            list.add(statement());
        }

        expect(TokenType.RBRACE);
        return list;
    }

    ASTNode statement() {

        if (match(TokenType.PRINT)) {
            expect(TokenType.LPAREN);
            ExprNode e = expr();
            expect(TokenType.RPAREN);
            expect(TokenType.SEMICOLON);
            return new PrintNode(e);
        }

        if (match(TokenType.LET)) {
            Token name = cur();
            expect(TokenType.IDENTIFIER);
            expect(TokenType.EQUALS);
            ExprNode e = expr();
            expect(TokenType.SEMICOLON);
            return new AssignmentNode(name.value, e);
        }

        if (match(TokenType.IF)) {
            expect(TokenType.LPAREN);
            ExprNode c = expr();
            expect(TokenType.RPAREN);

            List<ASTNode> ifB = block();
            List<ASTNode> elseB = new ArrayList<>();

            if (match(TokenType.ELSE)) {
                elseB = block();
            }

            return new IfNode(c, ifB, elseB);
        }

        throw new RuntimeException("Unknown statement");
    }

    List<ASTNode> parse() {
        List<ASTNode> list = new ArrayList<>();

        while (cur().type != TokenType.EOF) {
            list.add(statement());
        }
        return list;
    }
}

class Interpreter {
    Map<String, Object> env = new HashMap<>();

    Object eval(ExprNode e) {
        if (e instanceof NumberNode) {
            String val = ((NumberNode) e).value;
            if (val.contains(".")) {
                return Double.parseDouble(val);
            }
            return Integer.parseInt(val);
        }

        if (e instanceof StringNode) {
            return ((StringNode) e).value;
        }

        if (e instanceof VarNode) {
            Object val = env.get(((VarNode) e).name);
            if (val == null) {
                throw new RuntimeException("Undefined variable: " + ((VarNode) e).name);
            }
            return val;
        }

        if (e instanceof BinaryOpNode) {
            BinaryOpNode b = (BinaryOpNode) e;
            Object left = eval(b.left);
            Object right = eval(b.right);

            if (b.op == TokenType.PLUS) {
                if (left instanceof String || right instanceof String) {
                    return left.toString() + right.toString();
                }

                if (left instanceof Integer && right instanceof Integer) {
                    return (Integer) left + (Integer) right;
                }
                if (left instanceof Double || right instanceof Double) {
                    double l = left instanceof Double ? (Double) left : ((Integer) left).doubleValue();
                    double r = right instanceof Double ? (Double) right : ((Integer) right).doubleValue();
                    return l + r;
                }
            }
            
            if (left instanceof String || right instanceof String) {
                throw new RuntimeException("Cannot perform " + b.op + " on strings");
            }

            double l = left instanceof Double ? (Double) left : ((Integer) left).doubleValue();
            double r = right instanceof Double ? (Double) right : ((Integer) right).doubleValue();

            return switch (b.op) {
                case MINUS -> l - r;
                case MULTIPLY -> l * r;
                case DIVIDE -> 
                {
                    if (r == 0) throw new RuntimeException("Division by zero");
                    yield l / r;
                }
                case GT -> l > r;
                default -> throw new RuntimeException("Unknown operator: " + b.op);
            };
        }

        throw new RuntimeException("Unknown expression type: " + e.getClass());
    }

    void exec(List<ASTNode> nodes) {
        for (ASTNode n : nodes) {
            if (n instanceof AssignmentNode a) {
                env.put(a.name, eval(a.expr));
            } else if (n instanceof PrintNode p) {
                Object result = eval(p.expr);
                System.out.println(result);
            } else if (n instanceof IfNode i) {
                Object cond = eval(i.cond);
                boolean condition;
                
                if (cond instanceof Boolean) {
                    condition = (Boolean) cond;
                } else if (cond instanceof Integer) {
                    condition = (Integer) cond != 0;
                } else if (cond instanceof Double) {
                    condition = (Double) cond != 0;
                } else {
                    throw new RuntimeException("Invalid condition type: " + cond.getClass());
                }
                
                if (condition) {
                    exec(i.ifBlock);
                } else {
                    exec(i.elseBlock);
                }
            }
        }
    }
}

public class Compiler {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java Compiler <filename>");
            return;
        }
        
        String src = Files.readString(Paths.get(args[0]));

        Lexer l = new Lexer(src);
        Parser p = new Parser(l.tokenize());

        Interpreter it = new Interpreter();
        it.exec(p.parse());

        System.out.println("Done");
    }
}
