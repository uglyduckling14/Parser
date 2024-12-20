/*
 * Look for TODO comments in this file for suggestions on how to implement
 * your parser.
 */
package parser;

import java.io.IOException;
import java.util.*;

import lexer.ExprLexer;
import lexer.ParenLexer;
import lexer.SimpleLexer;
import lexer.TinyLexer;
import org.antlr.v4.runtime.*;

/**
 *
 */
public class Parser {

  final Grammar grammar;

  /**
   * All states in the parser.
   */
  private final States states;

  /**
   * Action table for bottom-up parsing. Accessed as
   * actionTable.get(state).get(terminal). You may replace
   * the Integer with a State class if you choose.
   */
  private final HashMap<Integer, HashMap<String, Action>> actionTable;
  /**
   * Goto table for bottom-up parsing. Accessed as gotoTable.get(state).get(nonterminal).
   * You may replace the Integers with State classes if you choose.
   */
  private final HashMap<Integer, HashMap<String, Integer>> gotoTable;

  public Parser(String grammarFilename) throws IOException {
    actionTable = new HashMap<>();
    gotoTable = new HashMap<>();

    grammar = new Grammar(grammarFilename);

    states = new States();

    computeActionTable();
    computeGotoTable();
  }
  public States getStates() {
    Item s = new Item(grammar.startRule, 0, "$");
    State state = computeClosure(s, grammar);
    state.add(s);
    states.add(state);
    List<State> newStates = new ArrayList<>(states.getStates());
    boolean added = true;
    int name = 1;

    while (added) {
      added = false;
      List<State> newStatesToAdd = new ArrayList<>();

      for (State sta : newStates) {
        for (Item item : sta.getItems()) {
          String nextSymbol = item.getNextSymbol();
          if(nextSymbol != null && grammar.isNonterminal(nextSymbol)) {

            State newState = GOTO(sta, nextSymbol,grammar);
            if (!states.getStates().contains(newState)) {
              newState.setName(name);
              newStatesToAdd.add(newState);
              added = true;
              states.add(newState);
              name++;
            }else{
              System.out.println("duplicate state");
            }
          }
        }
      }
      newStates = new ArrayList<>(newStatesToAdd);
    }

    return states;
  }
  static public State computeClosure(Item I, Grammar grammar) {
    State closure = new State();
    closure.add(I);
    ArrayList<Item> J = new ArrayList<>();
    J.add(I);
    boolean added = true;
    while(added){
      List<Item> newItems = new ArrayList<>();
      added = false;
      for (Item b: J) {
        String nextSymbol = b.getNextSymbol();

        if(grammar.isNonterminal(nextSymbol)){
          for(Rule rule: grammar.nt2rules.get(nextSymbol)){
            for (String t : getLookahead(b, grammar)) {
              Item i = new Item(rule, 0, t);
              if (!J.contains(i)) {
                newItems.add(i);
                closure.add(i);
                added = true;
              }
            }
          }
        }
      }
      J= new ArrayList<>(newItems);
    }
    return closure;
  }

  private static HashSet<String> getLookahead(Item b, Grammar grammar){
    if(b.getNextNextSymbol() == null){
      HashSet<String> l = new HashSet<>();
      l.add(b.getLookahead());
      return l;
    } else if (b.getDot() == 0) {
      HashSet<String> l = new HashSet<>();
      l.add(b.getNextNextSymbol());
      return l;
    }
    else{
      if(grammar.isNonterminal(b.getNextNextSymbol())){
        return grammar.first.get(b.getNextNextSymbol());
      }else{
        HashSet<String> l = new HashSet<>();
        l.add(b.getNextNextSymbol());
        return l;
      }
    }
  }

  // TODO: Implement this method.
  //   This returns a new state that represents the transition from
  //   the given state on the symbol X.
  static public State GOTO(State state, String X, Grammar grammar) {
    State ret = new State();
    for (Item item : state.getItems()) {
      String nextSymbol = item.getNextSymbol();

      // If the item has the form [A → α•Xβ, a], where X is the next symbol
      if (nextSymbol != null && nextSymbol.equals(X)) {
        // Create a new item by shifting the dot one position to the right
        Item shiftedItem = item.advance();
        // Add the shifted item to the GOTO state
        ret.add(shiftedItem);
      }
    }
    return ret;
  }

  // TODO: Implement this method
  // You will want to use StringBuilder. Another useful method will be String.format: for
  // printing a value in the table, use
  //   String.format("%8s", value)
  // How much whitespace you have shouldn't matter with regard to the tests, but it will
  // help you debug if you can format it nicely.
  public String actionTableToString() {
    StringBuilder builder = new StringBuilder();
    for (int state : actionTable.keySet()) {
      for (String terminal : actionTable.get(state).keySet()) {
        builder.append(String.format("%8s", actionTable.get(state).get(terminal)));
      }
      builder.append("\n");
    }
    return builder.toString();
  }

  // TODO: Implement this method
  // You will want to use StringBuilder. Another useful method will be String.format: for
  // printing a value in the table, use
  //   String.format("%8s", value)
  // How much whitespace you have shouldn't matter with regard to the tests, but it will
  // help you debug if you can format it nicely.
  public String gotoTableToString() {
    StringBuilder builder = new StringBuilder();
    for (int state : gotoTable.keySet()) {
      for (String nonterminal : gotoTable.get(state).keySet()) {
        builder.append(String.format("%8s", gotoTable.get(state).get(nonterminal)));
      }
      builder.append("\n");
    }
    return builder.toString();
  }

  public void computeActionTable() {
    for (State state : states.getStates()) {
      HashMap<String, Action> actionMap = new HashMap<>();
      for (Item item : state.getItems()) {
        String nextSymbol = item.getNextSymbol();
        if (nextSymbol == null) {
          // If the item is of the form [A → α•, a], add a reduce action

            Action action = Action.createReduce(item.getRule());
            actionMap.put(item.getLookahead(), action);

        } else if (grammar.isTerminal(nextSymbol)) {
          // If the next symbol is a terminal, add a shift action
          State nextState = GOTO(state, nextSymbol, grammar);
          int nextStateIndex = getStateIndex(nextState);
          Action action = Action.createShift(nextStateIndex);
          actionMap.put(nextSymbol, action);
        }
      }
      actionTable.put(getStateIndex(state), actionMap);
    }
  }
  public void computeGotoTable() {
    for (State state : states.getStates()) {
      HashMap<String, Integer> gotoMap = new HashMap<>();
      for (String symbol : grammar.symbols) {
        if (grammar.isNonterminal(symbol)) {
          // Compute the GOTO set for the nonterminal symbol
          State nextState = GOTO(state, symbol, grammar);
          int nextStateIndex = getStateIndex(nextState);
          gotoMap.put(symbol, nextStateIndex);
        }
      }
      gotoTable.put(getStateIndex(state), gotoMap);
    }
  }
  private int getStateIndex(State state) {
    int index = 0;
    for (State s : states.getStates()) {
      if (s.equals(state)) {
        return index;
      }
      index++;
    }
    return -1; // State not found
  }
  // TODO: Implement this method
  // You should return a list of the actions taken.
  public List<Action> parse(Lexer scanner) throws ParserException {
    ArrayList<? extends Token> tokens = new ArrayList<>(scanner.getAllTokens());
    Vocabulary v = scanner.getVocabulary();
    Stack<String> stack = new Stack<>();
    stack.push("0"); // Initial state
    List<Action> actions = new ArrayList<>();

    for (int i = 0; i < tokens.size(); i++) {
      Token token = tokens.get(i);
      String tokenType = v.getSymbolicName(token.getType());
      int currentState = Integer.parseInt(stack.peek());
      Action action = actionTable.get(currentState).get(tokenType);

      if (action == null) {
        throw ParserException.create(tokens, i);
      } else if (action.isShift()) {
        // Shift action
        stack.push(tokenType);
        stack.push(String.valueOf(action.getState()));
        actions.add(action);
      } else if (action.isReduce()) {
        // Reduce action
        Rule rule = action.getRule();
        int numSymbols = rule.getRhs().size();
        for (int j = 0; j < 2 * numSymbols; j++) {
          stack.pop(); // Pop state and symbol from stack
        }
        String nonterminal = rule.getLhs();
        currentState = Integer.parseInt(stack.peek());
        int nextState = gotoTable.get(currentState).get(nonterminal);
        stack.push(nonterminal);
        stack.push(String.valueOf(nextState));
        actions.add(action);
      } else if (action.isAccept()) {
        // Accept action
        actions.add(action);
        break; // Parsing successful
      }
    }

    return actions;
  }


  //-------------------------------------------------------------------
  // Convenience functions
  //-------------------------------------------------------------------

  public List<Action> parseFromFile(String filename) throws IOException, ParserException {
//    System.out.println("\nReading input file " + filename + "\n");
    final CharStream charStream = CharStreams.fromFileName(filename);
    Lexer scanner = scanFile(charStream);
    return parse(scanner);
  }

  public List<Action> parseFromString(String program) throws ParserException {
    Lexer scanner = scanFile(CharStreams.fromString(program));
    return parse(scanner);
  }

  private Lexer scanFile(CharStream charStream) {
    // We use ANTLR's scanner (lexer) to produce the tokens.
    Lexer scanner = null;
    switch (grammar.grammarName) {
      case "Simple":
        scanner = new SimpleLexer(charStream);
        break;
      case "Paren":
        scanner = new ParenLexer(charStream);
        break;
      case "Expr":
        scanner = new ExprLexer(charStream);
        break;
      case "Tiny":
        scanner = new TinyLexer(charStream);
        break;
      default:
        System.out.println("Unknown scanner");
        break;
    }

    return scanner;
  }

}
