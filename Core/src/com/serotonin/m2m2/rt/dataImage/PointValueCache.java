/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.infiniteautomation.mango.util.LazyField;
import com.serotonin.m2m2.db.dao.PointValueCacheDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * This class maintains an ordered list of the most recent values for a data point. It will mirror values in the
 * database, but provide a much faster lookup for a limited number of values.
 *
 * Because there is not a significant performance problem for time-based lookups, they are not handled here, but rather
 * are still handled by the database.
 *
 * @author Matthew Lohbihler
 */
public class PointValueCache {
    private final DataPointVO vo;
    private final int defaultSize;
    private int maxSize = 0;

    //This would not be the advised thing to do if we were to be deleting any data through here
    // as some properties of the delete are system settings that can be changed
    // However, since we're only querying, it should be more efficient to have only one static final reference
    private final PointValueDao dao;

    private final PointValueCacheDao pointValueCacheDao;

    /**
     * IMPORTANT: The list object should never be written to! The implementation here is for performance. Never call
     * methods like add() or remove() on the cache object. Further, since the cache object can be replaced from time to
     * time, always use a local copy of the variable for read purposes.
     */
    private final LazyField<List<PointValueTime>> cache;

    public PointValueCache(DataPointVO vo, int defaultSize, List<PointValueTime> cache, PointValueDao dao, PointValueCacheDao pointValueCacheDao) {
        this.vo = vo;
        this.defaultSize = defaultSize;
        this.dao = dao;
        this.pointValueCacheDao = pointValueCacheDao;

        this.cache = new LazyField<>(() -> {
            List<PointValueTime> newCache;
            if (cache == null) {
                newCache = new ArrayList<>();
                if (defaultSize > 0) {
                    newCache = refreshCache(defaultSize, newCache);
                }
            } else {
                if (cache.size() > defaultSize) {
                    // dont keep excess point values hanging around
                    newCache = new ArrayList<>(cache.subList(0, defaultSize));
                } else {
                    newCache = cache;
                }
                this.maxSize = defaultSize;
            }
            return newCache;
        });

    }

    void savePointValueAsync(PointValueTime pvt, SetPointSource source) {
        dao.savePointValueAsync(vo, pvt, source);

    }

    PointValueTime savePointValueSync(PointValueTime pvt, SetPointSource source) {
        return dao.savePointValueSync(vo, pvt, source);
    }

    public void savePointValue(PointValueTime pvt, SetPointSource source, boolean logValue, boolean async) {
        if (logValue) {
            if (async)
                savePointValueAsync(pvt, source);
            else
                pvt = savePointValueSync(pvt, source);
        }

        List<PointValueTime> c = cache.get();
        List<PointValueTime> newCache = new ArrayList<PointValueTime>(c.size() + 1);
        newCache.addAll(c);

        // Insert the value in the cache.
        int pos = 0;
        if (newCache.size() == 0)
            newCache.add(pvt);
        else {
            while (pos < newCache.size() && newCache.get(pos).getTime() > pvt.getTime())
                pos++;
            if (pos < maxSize)
                newCache.add(pos, pvt);
        }

        // Check if we need to clean up the list
        while (newCache.size() > maxSize)
            newCache.remove(newCache.size() - 1);

        //Update cache store
        pointValueCacheDao.updatePointValueCache(vo, newCache);

        cache.set(newCache);
    }

    /**
     * Saves the given value to the database without adding it to the cache.
     */
    void logPointValueAsync(PointValueTime pointValue, SetPointSource source) {
        // Save the new value and get a point value time back that has the id and annotations set, as appropriate.
        savePointValueAsync(pointValue, source);
    }

    public PointValueTime getLatestPointValue() {
        if (maxSize == 0) {
            List<PointValueTime> c = cache.get();
            cache.set(refreshCache(1, c));
        }

        List<PointValueTime> c = cache.get();
        if (c.size() > 0)
            return c.get(0);

        return null;
    }

    public List<PointValueTime> getLatestPointValues(int limit) {
        if (maxSize < limit) {
            List<PointValueTime> c = cache.get();
            cache.set(refreshCache(limit, c));
        }

        List<PointValueTime> c = cache.get();
        if (limit == c.size())
            return new ArrayList<PointValueTime>(c);

        if (limit > c.size())
            limit = c.size();
        return new ArrayList<PointValueTime>(c.subList(0, limit));
    }

    /**
     * Refresh the cache, keeping existing cached values if they are not already logged.
     * @param size
     */
    private List<PointValueTime> refreshCache(int size, List<PointValueTime> existing) {
        if (size > maxSize) {
            //Expanding cache
            maxSize = size;
            return pointValueCacheDao.expandPointValueCache(vo, size, existing);
        }else {
            return existing;
        }
    }

    /**
     * Never manipulate the contents of this list!
     */
    public List<PointValueTime> getCacheContents() {
        return cache.get();
    }

    /**
     * Completely reset cache using point value store, place
     *   into cache store
     */
    public void reset() {
        List<PointValueTime> nc = dao.getLatestPointValues(vo, defaultSize);
        maxSize = defaultSize;
        cache.set(nc);
        pointValueCacheDao.updatePointValueCache(vo, cache.get());
    }

    /**
     * Trim current cache of values < before, update cache store with new cache
     *  and reload any necessary data from point value store
     * @param before
     */
    public void reset(long before) {
        List<PointValueTime> c = cache.get();
        List<PointValueTime> nc = new ArrayList<PointValueTime>(c.size());
        nc.addAll(c);
        Iterator<PointValueTime> iter = nc.iterator();
        while(iter.hasNext())
            if(iter.next().getTime() < before)
                iter.remove();

        pointValueCacheDao.updatePointValueCache(vo, nc);

        if(nc.size() < defaultSize) {
            maxSize = 0;
            cache.set(refreshCache(defaultSize, nc));
        } else
            cache.set(nc);
    }
}
