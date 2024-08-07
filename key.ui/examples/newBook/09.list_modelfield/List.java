public interface List {
    //@ public instance model \seq theList;

    //@ public instance invariant (\forall int i; 0 <= i < theList.length; \typeof(theList[i]) == \type(int));

    /*@ public normal_behavior
      @ requires size() < Integer.MAX_VALUE;
      @ requires \typeof(elem) == \type(int);
      @ ensures theList == \seq_concat(\seq_singleton(elem),\old(theList));
      @*/
    public void add (int elem);

    /*@ public normal_behavior
      @ requires !empty();
      @ ensures theList == \old(theList[1..theList.length]);
      @*/
    public void remFirst ();

    /*@ public normal_behavior
      @ ensures \result == (size() == 0);
      @*/
    public /*@ pure @*/ boolean empty ();

    /*@ public normal_behavior
      @ ensures \result == theList.length;
      @*/
    public /*@ pure @*/ int size ();

    /*@ public normal_behavior
      @ requires 0 <= idx && idx < size();
      @ ensures \result == (int)theList[idx];
      @ ensures \typeof(theList[idx]) == \type(int);
      @*/
    public /*@ pure @*/ int get (int idx);
}
