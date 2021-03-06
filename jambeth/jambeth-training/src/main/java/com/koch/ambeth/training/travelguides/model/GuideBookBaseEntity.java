package com.koch.ambeth.training.travelguides.model;

import com.koch.ambeth.annotation.EntityEqualsAspect;

@EntityEqualsAspect
public interface GuideBookBaseEntity
{
	String Id = "Id";

	int getId();

	String getCreatedBy();

	long getCreatedOn();

	String getUpdatedBy();

	Long getUpdatedOn();

	int getVersion();

}
