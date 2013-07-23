package pl.net.bluesoft.rnd.processtool.plugins.util;

import pl.net.bluesoft.rnd.processtool.ProcessToolContext;
import pl.net.bluesoft.rnd.processtool.bpm.ProcessToolBpmSession;
import pl.net.bluesoft.rnd.processtool.bpm.ProcessToolBpmSessionHelper;
import pl.net.bluesoft.rnd.processtool.model.BpmTask;
import pl.net.bluesoft.rnd.processtool.model.UserData;
import pl.net.bluesoft.rnd.processtool.model.config.ProcessStateAction;

import java.util.List;

import static pl.net.bluesoft.rnd.processtool.plugins.ProcessToolRegistry.Util.getRegistry;

/** 
 * Manager for the bpm process actions 
 * 
 * @author mpawlak@bluesoft.net.pl
 *
 */
public class ProcessActionManager 
{
	private ProcessToolContext ctx;
	private UserData user;
	private ProcessToolBpmSession processToolSession;
	
	public ProcessActionManager(ProcessToolContext ctx, UserData user)
	{
		this.ctx = ctx;
		this.user = user;
	}
	
	public List<BpmTask> perfomAction(ProcessStateAction processStateAction, BpmTask task )
	{
		return ProcessToolBpmSessionHelper.performAction(getProcessToolBpmSession(), ctx, processStateAction, task);
	}
	
	protected ProcessToolBpmSession getProcessToolBpmSession()
	{
		if(processToolSession == null)
		{
			processToolSession = getRegistry().getProcessToolSessionFactory().createSession(user, user.getRoleNames());
		}
		
		return processToolSession;
	}
}