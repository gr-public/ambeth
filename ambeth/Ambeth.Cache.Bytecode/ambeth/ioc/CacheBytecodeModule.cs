using De.Osthus.Ambeth.Bytecode.Behavior;
using De.Osthus.Ambeth.Ioc.Annotation;
using De.Osthus.Ambeth.Ioc.Factory;

namespace De.Osthus.Ambeth.Ioc
{
    [FrameworkModule]
    public class CacheBytecodeModule : IInitializingModule
    {
        public void AfterPropertiesSet(IBeanContextFactory beanContextFactory)
        {
            // cascade $1
            BytecodeModule.AddDefaultBytecodeBehavior<EmbeddedTypeBehavior>(beanContextFactory);
            BytecodeModule.AddDefaultBytecodeBehavior<EnhancedTypeBehavior>(beanContextFactory);
            BytecodeModule.AddDefaultBytecodeBehavior<DefaultPropertiesBehavior>(beanContextFactory);
            // cascade $2
            BytecodeModule.AddDefaultBytecodeBehavior<LazyRelationsBehavior>(beanContextFactory);

            BytecodeModule.AddDefaultBytecodeBehavior<InitializeEmbeddedMemberBehavior>(beanContextFactory);
            // cascade $3
            BytecodeModule.AddDefaultBytecodeBehavior<NotifyPropertyChangedBehavior>(beanContextFactory);
            BytecodeModule.AddDefaultBytecodeBehavior<ParentCacheHardRefBehavior>(beanContextFactory);
            BytecodeModule.AddDefaultBytecodeBehavior<EntityEqualsBehavior>(beanContextFactory);
            //BytecodeModule.AddDefaultBytecodeBehavior<PublicEmbeddedConstructorBehavior>(beanContextFactory);
            // cascade $4
            BytecodeModule.AddDefaultBytecodeBehavior<DataObjectBehavior>(beanContextFactory);

            BytecodeModule.AddDefaultBytecodeBehavior<CacheMapEntryBehavior>(beanContextFactory);
            BytecodeModule.AddDefaultBytecodeBehavior<RootCacheValueBehavior>(beanContextFactory);
        }
    }
}