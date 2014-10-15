package FindIO;

public class FindIOPair implements Comparable<FindIOPair> {
    private String id;
    private double value;

    public FindIOPair(String id, double value){
        this.id = id;
        this.value = value;
    }

    public String getID(){
        return this.id;
    }

    public double getValue(){
        return this.value;
    }

    @Override
    public int compareTo(FindIOPair o) {
        return new Float(value).compareTo(new Float(o.value));
    }
}
