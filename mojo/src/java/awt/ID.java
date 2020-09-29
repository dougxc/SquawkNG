package java.awt;

public class ID {

    private static int nextID;

    private int id = nextID++;

    ID() {
        //System.out.println(" **** ID "+id+" = "+this.getClass());
    }

    public int id() {
        return id;
    }

}



