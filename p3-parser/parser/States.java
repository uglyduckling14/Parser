package parser;

import java.util.*;

public class States {
    private Set<State> stateSet;
    private List<State> states;
    private int id = 0;

    public States() {
        this.stateSet = new HashSet<>();
        this.states = new ArrayList<>();
    }
    public List<State> getStates(){
        return states;
    }
    public State getState(int name) {
        if (name >= states.size()) {
            return null;
        }
        return states.get(name);
    }
    public void add(State state){
        if(!stateSet.contains(state)){
            states.add(state);
            stateSet.add(state);
        }
    }
    @Override
    public String toString() {
        return states.toString();
    }
    public int size(){
        return states.size();
    }
    @Override
    public boolean equals(Object obj){
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final States other = (States) obj;
        for (State state : states) {
            if (!other.stateSet.contains(state)) {
                return false;
            }
        }
        return true;
    }
    @Override
    public int hashCode() {
        int hash = 7;
        ArrayList<State> sortedList = new ArrayList<>(states);
        sortedList.sort(Comparator.comparingInt(State::hashCode));
        for (State state : sortedList) {
            hash = 37 * hash + Objects.hashCode(state);
        }
        return hash;
    }
}
