/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.RuntimeManagerDefinition;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.dataImage.DataPointListener;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;

/**
 * A mock of the runtime manager that can optionally save data to the database.
 *   This implementation does not actually start any threads/tasks.
 *
 * @author Terry Packer
 */
public class MockRuntimeManager implements RuntimeManager {
    
    //Use the database to save data sources/points/publishers
    protected boolean useDatabase;
    
    public MockRuntimeManager() {
        
    }
    
    public MockRuntimeManager(boolean useDatabase) {
        this.useDatabase = useDatabase;
    }
    
    @Override
    public int getState() {
        return RuntimeManager.RUNNING;
    }

    @Override
    public void initialize(boolean safe) {
        //Get the RTM defs from modules and sort by priority
        List<RuntimeManagerDefinition> defs = ModuleRegistry.getDefinitions(RuntimeManagerDefinition.class);
        Collections.sort(defs, new Comparator<RuntimeManagerDefinition>() {
            @Override
            public int compare(RuntimeManagerDefinition def1, RuntimeManagerDefinition def2) {
                return def1.getInitializationPriority() - def2.getInitializationPriority();
            }
        });
        
        //Initialize them
        defs.stream().forEach((def) -> {
            def.initialize(safe);
        });
    }

    @Override
    public void terminate() {
        // Get the RTM defs and sort by reverse init priority.
        List<RuntimeManagerDefinition> defs = ModuleRegistry.getDefinitions(RuntimeManagerDefinition.class);
        Collections.sort(defs, new Comparator<RuntimeManagerDefinition>() {
            @Override
            public int compare(RuntimeManagerDefinition def1, RuntimeManagerDefinition def2) {
                return def2.getInitializationPriority() - def1.getInitializationPriority();
            }
        });
        //Initialize them
        defs.stream().forEach((def) -> {
            def.terminate();
        });
    }

    @Override
    public void joinTermination() {

    }

    @Override
    public DataSourceRT<? extends DataSourceVO<?>> getRunningDataSource(int dataSourceId) {

        return null;
    }
    
    @Override
    public List<? extends DataSourceRT<?>> getRunningDataSources() {
        return new ArrayList<>();
    }

    @Override
    public boolean isDataSourceRunning(int dataSourceId) {

        return false;
    }

    @Override
    public List<DataSourceVO<?>> getDataSources() {
        if(useDatabase)
            return DataSourceDao.getInstance().getAll(true);
        else 
            return null;
    }

    @Override
    public DataSourceVO<?> getDataSource(int dataSourceId) {
        if(useDatabase)
            return DataSourceDao.getInstance().get(dataSourceId, true);
        else 
            return null;
    }

    @Override
    public void deleteDataSource(int dataSourceId) {
        if(useDatabase)
            DataSourceDao.getInstance().delete(dataSourceId);
    }

    @Override
    public void insertDataSource(DataSourceVO<?> vo) {
        if(useDatabase) {
            DataSourceDao.getInstance().insert(vo, true);
        }
    }

    @Override
    public void updateDataSource(DataSourceVO<?> existing, DataSourceVO<?> vo) {
        if(useDatabase) {
            DataSourceDao.getInstance().update(existing, vo, true);
        }
    }

    @Override
    public boolean initializeDataSourceStartup(DataSourceVO<?> vo) {

        return false;
    }

    @Override
    public void stopDataSourceShutdown(int id) {

    }
    
    @Override 
    public void insertDataPoint(DataPointVO vo) {
        if(useDatabase) {
            DataPointDao.getInstance().insert(vo, true);
        }
    }
    
    @Override
    public void updateDataPoint(DataPointVO existing, DataPointVO vo) {
        if(useDatabase) {
            DataPointDao.getInstance().update(existing, vo, true);
        }
    }

    @Override
    public void deleteDataPoint(DataPointVO point) {
        if(useDatabase)
            DataPointDao.getInstance().delete(point.getId());
    }

    @Override
    public void restartDataPoint(DataPointVO vo) {
        
    }

    @Override
    public boolean isDataPointRunning(int dataPointId) {

        return false;
    }

    @Override
    public DataPointRT getDataPoint(int dataPointId) {
            return null;
    }

    @Override
    public List<DataPointRT> getRunningDataPoints() {
        return new ArrayList<>();
    }
    
    @Override
    public void addDataPointListener(int dataPointId, DataPointListener l) {
        
    }

    @Override
    public void removeDataPointListener(int dataPointId, DataPointListener l) {

    }
    
    @Override
    public DataPointListener getDataPointListeners(int dataPointId) {

        return null;
    }

    @Override
    public void setDataPointValue(int dataPointId, DataValue value, SetPointSource source) {

        
    }

    @Override
    public void setDataPointValue(int dataPointId, PointValueTime valueTime,
            SetPointSource source) {

        
    }

    @Override
    public void relinquish(int dataPointId) {

        
    }

    @Override
    public void forcePointRead(int dataPointId) {

        
    }

    @Override
    public void forceDataSourcePoll(int dataSourceId) {

        
    }

    @Override
    public long purgeDataPointValues() {

        return 0;
    }

    @Override
    public void purgeDataPointValuesWithoutCount() {

        
    }

    @Override
    public long purgeDataPointValues(int dataPointId, int periodType, int periodCount) {

        return 0;
    }

    @Override
    public long purgeDataPointValues(int dataPointId) {

        return 0;
    }

    @Override
    public boolean purgeDataPointValuesWithoutCount(int dataPointId) {

        return false;
    }

    @Override
    public long purgeDataPointValue(int dataPointId, long ts, PointValueDao dao) {

        return 0;
    }

    @Override
    public long purgeDataPointValues(int dataPointId, long before) {

        return 0;
    }

    @Override
    public long purgeDataPointValuesBetween(int dataPointId, long startTime, long endTime) {

        return 0;
    }

    @Override
    public boolean purgeDataPointValuesWithoutCount(int dataPointId, long before) {

        return false;
    }

    @Override
    public PublisherRT<?> getRunningPublisher(int publisherId) {

        return null;
    }

    @Override
    public boolean isPublisherRunning(int publisherId) {

        return false;
    }

    @Override
    public PublisherVO<? extends PublishedPointVO> getPublisher(int publisherId) {
        if(useDatabase)
            return PublisherDao.getInstance().get(publisherId, true);
        else
            return null;
    }
    
    @Override
    public void deletePublisher(int publisherId) {
        if(useDatabase)
            PublisherDao.getInstance().delete(publisherId);
        
    }

    @Override
    public void savePublisher(PublisherVO<? extends PublishedPointVO> vo) {
        if(useDatabase)
            PublisherDao.getInstance().savePublisher(vo);
    }

    @Override
    public void enableDataPoint(DataPointVO point, boolean enabled) {
        point.setEnabled(enabled);
        if(useDatabase)
            DataPointDao.getInstance().saveEnabledColumn(point);
    }

    @Override
    public TranslatableMessage getStateMessage() {
        return new TranslatableMessage("common.default", "Mock runtime manager state message");
    }
}
