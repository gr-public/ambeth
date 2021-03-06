package com.koch.ambeth.event;

/*-
 * #%L
 * jambeth-event
 * %%
 * Copyright (C) 2017 Koch Softwaredevelopment
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 * #L%
 */

public interface IEventQueue {
	/**
	 * Used together with {@link #flushEventQueue()} to prepare bulk-dispatching operations. After
	 * invoking this method all dispatching operations to
	 * {@link IEventDispatcher#dispatchEvent(Object)} and its overloads are queued up and wait for a
	 * {@link #flushEventQueue()}.<br>
	 * </br>
	 * This method stacks: Multiple "enable" calls need the same amount of "flush" calls till a real
	 * flush is done and the right {@link IEventListener}s are notified. See also
	 * {@link #flushEventQueue()} for more details about whats happening during the flush. It is
	 * recommended to use it consistently in a try-finally statement:
	 *
	 * <pre>
	 * <code>
	 * eventQueue.enableEventQueue();
	 * try
	 * {
	 *   // do stuff
	 * }
	 * finally
	 * {
	 *   eventQueue.flushEventQueue();
	 * }
	 * </code>
	 * </pre>
	 *
	 */
	void enableEventQueue();

	/**
	 * Used together with {@link #enableEventQueue()} to prepare bulk-dispatching operations. If the
	 * internal stack count (together with {@link #enableEventQueue()}) reaches zero the queued events
	 * are "batched" together: This means some optional compact algorithm may process the events -
	 * e.g. to reduce potential redundancy of events which somehow "outdate" previous events. After
	 * the batch sequence the remaining (or newly created, compacted) events get dispatched to the
	 * registered {@link IEventListener}s.<br>
	 * <br>
	 * Custom event batchers can be defined by implementing {@link IEventBatcher} and linking them to
	 * {@link IEventBatcherExtendable}
	 *
	 * @see #enableEventQueue()
	 */
	void flushEventQueue();

	/**
	 * Propagates that the given eventTarget is now "paused" for the eventqueue-dispatching engine.
	 * However the method blocks if the eventqueue-dispatching engine is already processing an event
	 * correlating to the given eventTarget by using the
	 * {@link IEventDispatcher#waitEventToResume(Object, long,
	 * com.koch.ambeth.util.threading.IBackgroundWorkerParamDelegate,
	 * com.koch.ambeth.util..threading.IBackgroundWorkerParamDelegate)} method.<br>
	 * <br>
	 * If the given eventTarget is a proxy instance of something then there might be a need for an
	 * instance of {@link IEventTargetExtractor} which is to be registered to
	 * {@link IEventTargetExtractorExtendable}. The eventdispatching-engine considers this extractor
	 * to resolve the real "target eventHandle" needed for identity equality checks when resolving the
	 * correct "paused" states.
	 *
	 * @param eventTarget
	 */
	void pause(Object eventTarget);

	/**
	 * Propagates that the given eventTarget is now again "free" for the eventqueue-dispatching
	 * engine.<br>
	 * <br>
	 * If the given eventTarget is a proxy instance of something then there might be a need for an
	 * instance of {@link IEventTargetExtractor} which is to be registered to
	 * {@link IEventTargetExtractorExtendable}. The eventdispatching-engine considers this extractor
	 * to resolve the real "target eventHandle" needed for identity equality checks when resolving the
	 * correct "paused" states.
	 *
	 * @param eventTarget
	 */
	void resume(Object eventTarget);

	/**
	 * Evaluates whether the current thread is flagged as dispatching a batch of events
	 *
	 * @return true if and only if the current thread is flagged as dispatching a batch of events
	 */
	boolean isDispatchingBatchedEvents();
}
