

public class PatternEdge {
    protected PatternNode leftNode;
    protected PatternNode rightNode;

    public PatternEdge(PatternNode left, PatternNode right) {
        leftNode = left;
        rightNode = right;
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof PatternEdge))return false;
        PatternEdge otherMyClass = (PatternEdge)other;
        return otherMyClass.leftNode == leftNode && otherMyClass.rightNode == rightNode;
    }
}
