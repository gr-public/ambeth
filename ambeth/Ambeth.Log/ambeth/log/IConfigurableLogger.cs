﻿using De.Osthus.Ambeth.Config;
using System;

namespace De.Osthus.Ambeth.Log
{
    public interface IConfigurableLogger : ILogger
    {
        LogSourceLevel LogSourceLevel { get; set; }

        void PostProcess(IProperties properties);

        bool LogToConsole { get; set; }

        new bool DebugEnabled { get; set; }

        new bool InfoEnabled { get; set; }

        new bool WarnEnabled { get; set; }

        new bool ErrorEnabled { get; set; }
    }
}
