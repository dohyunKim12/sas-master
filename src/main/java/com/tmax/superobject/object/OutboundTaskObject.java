package com.tmax.superobject.object;

import com.tmax.superobject.listener.ChannelCompletionListener;
import com.tmax.superobject.listener.CompletionListener;
import com.tmax.superobject.logger.SuperAppDefaultLogger;
import com.tmax.superobject.servicemanager.ServiceManager;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;

public class OutboundTaskObject extends TaskObject {
    private static Logger logger = SuperAppDefaultLogger.getInstance().getLogger(OutboundTaskObject.class.getName());

    public OutboundTaskObject(MessageObject requestMessage, ClassLoader serviceClassLoader, EventLoop eventManager,
                              EventLoopGroup workerGroup) {
        super(null, requestMessage, serviceClassLoader, eventManager, workerGroup);
    }
    
    @Override
    protected void processInEventManager() {
        switch (state) {
            case BEFORE_SERVICE:
                ServiceManager.getInstance().execOutboundTask(this);
                break;
            case AFTER_SERVICE:
                workerGroup.execute(this);
                break;
            case BEFORE_LISTENER:
                if (completionListeners != null) {
                    for (CompletionListener listener : completionListeners) {
                        if (listener instanceof ChannelCompletionListener)
                            listener.afterCompletion(responseMessage.body());
                    }
                }
                workerGroup.execute(this);
                break;
            case AFTER_LISTENER:
                if (parentTask != null && parentTask.decreaseComputeReady() == 0 && parentTask.isCompleted()) {
                    parentTask.state = ProcessState.AFTER_SERVICE;
                    workerGroup.execute(parentTask);
                }
                state = ProcessState.TERMINATE;
                this.awake();
                eventManager.execute(this);
                break;
            case COMPLETE_SERVICE:
                workerGroup.execute(this);
                break;
            case WAIT:
                // do nothing
                break;
            case TERMINATE:
                break;
            default:
                logger.info("unreachable state : " + state);
                break;
        }
    }
}
