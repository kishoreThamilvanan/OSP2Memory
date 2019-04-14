package osp.Memory;

/**
 * Name: Kishore Thamilvanan
 * ID  : 111373510
 * 
 * I pledge my honor that all parts of this project were done by me individually, 
 * without collaboration with anyone, and without consulting external 
 * sources that help with similar projects.
 */

import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /** 
        This method is called once before the simulation starts. 
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {

      int i=-1;
      while(++i<MMU.getFrameTableSize())
        setFrame(i, new FrameTableEntry(i));

    }

    /**
       This method handlies memory references. The method must 
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault 
       by making an interrupt if the page is invalid, finally, 
       if the page is still valid, i.e., not swapped out by another 
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue, 
       and it is possible that some other thread will take away the frame.)
       
       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform 
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress, int referenceType, ThreadCB thread)
    {
        
        int page_offset = getVirtualAddressBits() - getPageAddressBits();
        int page_size = ((int) Math.pow(2, page_offset));
        int page_number = memoryAddress/page_size;

        PageTableEntry page = getPTBR().pages[page_number];

        if(!page.isValid()){

            /*
                Page fault occured because of another thread which is not the current thread.
                    therefore we suspend the thread.
            */
            if(page.getValidatingThread() != null){

                // suspend the thread
                thread.suspend(page);

                // if the thread is killed in the waiting time, then return the page
                if(thread.getStatus() ==  ThreadKill)
                    return page;

            } else {

                //set the page fault variable to be true.
            	page.pageFaulted = true;
                InterruptVector.setPage(page);
                InterruptVector.setInterruptType(referenceType);
                InterruptVector.setThread(thread);

                // introduce the pagefault interupt
                CPU.interrupt(PageFault);

                if(thread.getStatus() == ThreadKill)
                    return page;
            }
        }

        // page is assumed to be valid if the control reacher here.
        page.getFrame().setDirty(true);

        if(referenceType == MemoryWrite)
            page.getFrame().setDirty(true);

        return page;
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
     
	@OSPProject Memory
     */
    public static void atError()
    {

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
      @OSPProject Memory
     */
    public static void atWarning()
    {

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
