﻿using De.Osthus.Ambeth.Ioc;
using De.Osthus.Ambeth.Ioc.Config;
using De.Osthus.Ambeth.Ioc.Exceptions;
using De.Osthus.Ambeth.Ioc.Factory;
using De.Osthus.Ambeth.Log;
using De.Osthus.Ambeth.Typeinfo;
using De.Osthus.Ambeth.Util;
using System;
using System.Collections.Generic;

namespace De.Osthus.Ambeth.Config
{
    public class PropertiesPreProcessor : IBeanPreProcessor, IInitializingBean
    {
        [LogInstance]
        public ILogger Log { private get; set; }

        public IConversionHelper ConversionHelper { protected get; set; }

        public IPropertyInfoProvider PropertyInfoProvider { protected get; set; }

        public void AfterPropertiesSet()
        {
            ParamChecker.AssertNotNull(ConversionHelper, "ConversionHelper");
            ParamChecker.AssertNotNull(PropertyInfoProvider, "PropertyInfoProvider");
        }

        public void PreProcessProperties(IBeanContextFactory beanContextFactory, IServiceContext beanContext, IProperties props, String beanName, Object service, Type beanType,
            IList<IPropertyConfiguration> propertyConfigs, ISet<String> ignoredPropertyNames, IPropertyInfo[] properties)
        {
            if (properties == null)
            {
                properties = PropertyInfoProvider.GetProperties(service.GetType());
            }
            foreach (IPropertyInfo prop in properties)
            {
                if (!prop.IsWritable)
                {
                    continue;
                }
                PropertyAttribute propertyAttribute = prop.GetAnnotation<PropertyAttribute>();
                if (propertyAttribute == null)
                {
                    continue;
                }
                if (ignoredPropertyNames.Contains(prop.Name))
                {
                    // do not handle this property
                    continue;
                }
                if (PropertyAttribute.DEFAULT_VALUE.Equals(propertyAttribute.Name) && PropertyAttribute.DEFAULT_VALUE.Equals(propertyAttribute.DefaultValue))
                {
                    if (propertyAttribute.Mandatory)
                    {
                        String propName = prop.Name;
                        bool propertyInitialized = false;
                        // check if the mandatory property field has been initialized with a value
                        for (int a = propertyConfigs.Count; a-- > 0; )
                        {
                            IPropertyConfiguration propertyConfig = propertyConfigs[a];
                            if (propName.Equals(propertyConfig.GetPropertyName()))
                            {
                                propertyInitialized = true;
                                break;
                            }
                        }
                        if (!propertyInitialized)
                        {
                            throw new Exception("Mandatory property '" + propName + "' not initialized");
                        }
                    }
                    continue;
                }
                Object value = props != null ? props.GetString(propertyAttribute.Name) : null;

                if (value == null)
                {
                    String stringValue = propertyAttribute.DefaultValue;
                    if (PropertyAttribute.DEFAULT_VALUE.Equals(stringValue))
                    {
                        if (propertyAttribute.Mandatory)
                        {
                            throw new BeanContextInitException("Could not resolve mandatory environment property '" + propertyAttribute.Name + "'");
                        }
                        else
                        {
                            continue;
                        }
                    }
                    else
                    {
                        value = props.ResolvePropertyParts(stringValue);
                    }
                }
                value = ConversionHelper.ConvertValueToType(prop.PropertyType, value);
                prop.SetValue(service, value);
            }
        }
    }
}