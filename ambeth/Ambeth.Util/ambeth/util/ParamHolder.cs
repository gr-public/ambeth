﻿using System;

namespace De.Osthus.Ambeth.Util
{
    public class ParamHolder<T> : IParamHolder<T>
    {
	    public virtual T Value { get; set; }

	    public ParamHolder()
	    {
		    // Intended blank
	    }

	    public ParamHolder(T value)
	    {
		    this.Value = value;
	    }
    }
}
