using System;

namespace De.Osthus.Ambeth.Util
{
    public interface IPrefetchState
    {
        // Marker Interface to hold a hardref to ensured values. This is due to the fact that WeakReferences may be immediately collected after the
        // values have been ensured in the cache.
    }
}
