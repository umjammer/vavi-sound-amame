/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;


public class svisionH {
        /*
	  this shows the interface that your files offer
	  (the only exception is your GAME/CONS/COMP structure!)
	
	  allows the compiler generate the correct dependancy,
	  so it can recognize which files to rebuild
	
	  the programmer sees, when he changes this interface, he might
	  have to adapt other drivers
	
	  you must not do declarations in c files with external bindings
	*/

	public static class SVISION_CHANNEL {
	    public int[] reg = new int[3];
	    public int pos;
	    public int size;
	};
	
}
