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
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
 * The page fault handler is responsible for handling a page fault. If a swap in
 * or swap out operation is required, the page fault handler must request the
 * operation.
 * 
 * @OSPProject Memory
 */
public class PageFaultHandler extends IflPageFaultHandler {
	/**
	 * This method handles a page fault.
	 * 
	 * It must check and return if the page is valid,
	 * 
	 * It must check if the page is already being brought in by some other thread,
	 * i.e., if the page's has already pagefaulted (for instance, using
	 * getValidatingThread()). If that is the case, the thread must be suspended on
	 * that page.
	 * 
	 * If none of the above is true, a new frame must be chosen and reserved until
	 * the swap in of the requested page into this frame is complete.
	 * 
	 * Note that you have to make sure that the validating thread of a page is set
	 * correctly. To this end, you must set the page's validating thread using
	 * setValidatingThread() when a pagefault happens and you must set it back to
	 * null when the pagefault is over.
	 * 
	 * If a swap-out is necessary (because the chosen frame is dirty), the victim
	 * page must be dissasociated from the frame and marked invalid. After the
	 * swap-in, the frame must be marked clean. The swap-ins and swap-outs must are
	 * preformed using regular calls read() and write().
	 * 
	 * The student implementation should define additional methods, e.g, a method to
	 * search for an available frame.
	 * 
	 * Note: multiple threads might be waiting for completion of the page fault. The
	 * thread that initiated the pagefault would be waiting on the IORBs that are
	 * tasked to bring the page in (and to free the frame during the swapout).
	 * However, while pagefault is in progress, other threads might request the same
	 * page. Those threads won't cause another pagefault, of course, but they would
	 * enqueue themselves on the page (a page is also an Event!), waiting for the
	 * completion of the original pagefault. It is thus important to call
	 * notifyThreads() on the page at the end -- regardless of whether the pagefault
	 * succeeded in bringing the page in or not.
	 * 
	 * @param thread        the thread that requested a page fault
	 * @param referenceType whether it is memory read or write
	 * @param page          the memory page
	 * 
	 * @return SUCCESS is everything is fine; FAILURE if the thread dies while
	 *         waiting for swap in or swap out or if the page is already in memory
	 *         and no page fault was necessary (well, this shouldn't happen,
	 *         but...). In addition, if there is no frame that can be allocated to
	 *         satisfy the page fault, then it should return NotEnoughMemory
	 * 
	 * @OSPProject Memory
	 */
	public static int do_handlePageFault(ThreadCB thread, int referenceType, PageTableEntry page) {

//    	always checking for validity in case if it was called by other methods by chance
		if (page.isValid()) {

			page.notifyThreads();
			ThreadCB.dispatch();
			return FAILURE;

		}

		FrameTableEntry frame = null;

		/*
		 * if you cant find a free frame then it mean there is no memory. all the frames
		 * are either locked up or reserved.
		 */
		frame = get_a_frame();
		if (frame == null) {

			page.notifyThreads();
			ThreadCB.dispatch();
			return NotEnoughMemory;
		}

		SystemEvent systemEvent = new SystemEvent("PageFault");
		thread.suspend(systemEvent);

		/*
		 * now to reserve the suitable thread
		 */
		page.setValidatingThread(thread);
		
		if(!frame.isReserved())
			frame.setReserved(thread.getTask());

		try {

			// if the frame contains a dirty page.
				PageTableEntry new_Page = frame.getPage();
				if(new_Page == null) {
					page.setFrame(frame);
					
					/*
					 * Swapping in the pages.
					 */
					TaskCB task = page.getTask();
					task.getSwapFile().read(page.getID(), page, thread);
					
					if (thread.getStatus() == ThreadKill) {
						if (frame.getPage() != null)
							if (frame.getPage().getTask() == thread.getTask())
								frame.setPage(null);
		
						page.notifyThreads();
						page.setValidatingThread(null);
						page.setFrame(null);
		
						systemEvent.notifyThreads();
						ThreadCB.dispatch();
						return FAILURE;
					}
					
				} else {
				
					if (frame.isDirty()) {
	
							// swap out
							TaskCB swap_task = frame.getPage().getTask();
							swap_task.getSwapFile().write(new_Page.getID(), new_Page, thread);
							
							if (thread.getStatus() == ThreadKill) {
								
								page.notifyThreads();
								systemEvent.notifyThreads();
								ThreadCB.dispatch();
								return FAILURE;
							} 
							
							frame.setDirty(false);
							frame.setReferenced(false);
							frame.setPage(null);
							
							new_Page.setValid(false);
							new_Page.setFrame(null);					
						
	
					} else if (new_Page != null || !frame.isDirty()) {
	
						frame.setReferenced(false);
						frame.setDirty(false);
	
						new_Page.setValid(false);
						if(new_Page.getFrame().getLockCount() == 0)
							new_Page.setFrame(null);
	
						// freeing the frame.
						if (frame.getPage().getTask() == thread.getTask())
							if(frame.getLockCount() == 0)
								frame.setPage(null);
					}
					
					page.setFrame(frame);
					
					/*
					 * Swapping in the pages.
					 */
					TaskCB task = page.getTask();
					task.getSwapFile().read(page.getID(), page, thread);
					
					if (thread.getStatus() == ThreadKill) {
						if (frame.getPage() != null)
							if (frame.getPage().getTask() == thread.getTask())
								frame.setPage(null);
		
						page.notifyThreads();
						page.setValidatingThread(null);
						page.setFrame(null);
		
						systemEvent.notifyThreads();
						ThreadCB.dispatch();
						return FAILURE;
					}
					
				}
	

		} catch (NullPointerException e) {
		}
		
		frame.setPage(page);
		page.setValid(true);

		if (frame.getReserved() == thread.getTask())
			frame.setUnreserved(thread.getTask());

		page.setValidatingThread(null);
		page.notifyThreads();

		systemEvent.notifyThreads();
		ThreadCB.dispatch();
		return SUCCESS;

	}

	public static FrameTableEntry get_a_frame() {

		// going through all the frames to get the appropriate frame.
		int i = -1;
		while (++i < MMU.getFrameTableSize()) {
			FrameTableEntry current_frame = MMU.getFrame(i);
			if (current_frame.getLockCount() == 0 && !current_frame.isReserved())
				if(current_frame.getPage() == null)
					return current_frame;
		}
		
		return null;

	}

}
