package com.koch.ambeth.merge.changecontroller;

/*-
 * #%L
 * jambeth-merge
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

/**
 * Marker interface for rules that want to work in a batch mode: that is "queue" necessary steps
 * until the batch is "flushed". Just implement this interface on any custom
 * {@link IChangeControllerExtension}.
 */
public interface IMergeStepListener {
	/**
	 * Callback to notify the extension that a new batch sequence is started
	 */
	void queueStep(ICacheView cacheView);

	/**
	 * Callback to notify the extension that the previous batch sequence called with "queue" is now
	 * finished.
	 */
	void flushStep(ICacheView cacheView);

	/**
	 * Callback to notify the extension that the previous batch sequence called with "queue" is
	 * reverted (mostly due to an exception).
	 */
	void rollbackStep(ICacheView cacheView);
}
