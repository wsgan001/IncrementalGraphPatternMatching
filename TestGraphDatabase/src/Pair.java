import java.util.ArrayList;
import java.util.List;

public class Pair<T,T1> {
    protected T left;
    protected T1 right;
   public Pair(T left, T1 right) {
       this.left=left;
       this.right=right;
   }
   public Pair () {}

   public List<T> getLeftElements (List<Pair<T,T1>> listOfPairs) {
       List<T> res= new ArrayList<>();
       for (Pair<T,T1> pair: listOfPairs)
           res.add(pair.left);
       return res;
   }

     public List<T1> getRightElements (List<Pair<T,T1>> listOfPairs) {
        List<T1> res= new ArrayList<>();
        for (Pair<T,T1> pair: listOfPairs)
            res.add(pair.right);
        return res;
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Pair))return false;
        Pair otherMyClass = (Pair)other;
        return (otherMyClass.left.equals(left) && otherMyClass.right.equals(right));
    }
}
