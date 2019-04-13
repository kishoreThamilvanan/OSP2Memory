package osp.Memory;

/**
 * Name: Kishore Thamilvanan
 * ID  : 111373510
 * 
 * I pledge my honor that all parts of this project were done by me individually, 
 * without collaboration with anyone, and without consulting external 
 * sources that help with similar projects.
 */


/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
    /** 
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
        super(ownerTask);
       
        // to construct a pagetable
        int PageTableSize = (int) Math.pow(2, MMU.getPageAddressBits());
        pages = new PageTableEntry[PageTableSize]; 
 
 		// initialization 
        int i=-1;
        while(++i < PageTableSize)
        	pages[i] = new PageTableEntry(this, i);
        	
    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
        
    	TaskCB current_task = getTask();

		int i=-1;
		while(++i<MMU.getFrameTableSize()){

			FrameTableEntry frame = MMU.getFrame(i);
	    	PageTableEntry page = frame.getPage();

	    	if(frame != null)
	    		if(current_task == getTask()){

	    			frame.setPage(null);
	    			frame.setDirty(false);
	    			frame.setReferenced(false);

	    			if(frame.getReserved() == current_task)
	    				frame.setUnreserved(current_task);
	    		}
	    }
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
