/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.FiledataDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PurgeDefinition;
import com.serotonin.m2m2.module.PurgeFilterDefinition;
import com.serotonin.m2m2.module.definitions.actions.PurgeFilter;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.TimerTask;
import com.serotonin.util.ILifecycleState;

public class DataPurge {

    public static final String ENABLE_POINT_DATA_PURGE = "enablePurgePointValues";

    private static final Log log = LogFactory.getLog(DataPurge.class);
    private long runtime;
    private final DataPointDao dataPointDao = DataPointDao.getInstance();
    private final PointValueDao pointValueDao = Common.databaseProxy.newPointValueDao();
    private long deletedSamples;
    private boolean anyDeletedSamples;
    private long deletedFiles;
    private long deletedEvents;
    private final List<Long> fileIds = new ArrayList<Long>();
    private boolean countPointValues;

    public static void schedule() {
        try {
            Common.backgroundProcessing.schedule(new DataPurgeTask());
        }
        catch (ParseException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    synchronized public void execute(long runtime) {
        this.runtime = runtime;
        executeImpl();
    }

    private void executeImpl() {
        log.info("Data purge started");

        boolean purgePoints = SystemSettingsDao.instance.getBooleanValue(ENABLE_POINT_DATA_PURGE);
        this.countPointValues = SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.POINT_DATA_PURGE_COUNT);

        if(purgePoints){
            // Get any filters for the data purge from the modules
            List<PurgeFilter> purgeFilters = new ArrayList<PurgeFilter>();
            for(PurgeFilterDefinition pfd : ModuleRegistry.getDefinitions(PurgeFilterDefinition.class))
                purgeFilters.add(pfd.getPurgeFilter());

            // Get the data point information.
            List<DataPointVO> dataPoints = dataPointDao.getAll();
            for (DataPointVO dataPoint : dataPoints)
                purgePoint(dataPoint, countPointValues, purgeFilters);

            if (countPointValues) {
                deletedSamples += pointValueDao.deleteOrphanedPointValues();
            } else {
                pointValueDao.deleteOrphanedPointValuesWithoutCount();
            }

            pointValueDao.deleteOrphanedPointValueAnnotations();

            if (countPointValues) {
                log.info("Data purge ended, " + deletedSamples + " point values were deleted");
            } else if (anyDeletedSamples) {
                log.info("Data purge ended, unknown number of point values were deleted");
            } else {
                log.info("Data purge ended, no point values were deleted");
            }
        }else{
            log.info("Purge for data points not enabled, skipping.");
        }

        // File data purge
        filedataPurge();
        if (deletedFiles > 0)
            log.info("Filedata purge ended, " + deletedFiles + " files deleted");

        // Event purge
        eventPurge();

        // Definitions
        for (PurgeDefinition def : ModuleRegistry.getDefinitions(PurgeDefinition.class))
            def.execute(runtime);
    }

    private void purgePoint(DataPointVO dataPoint, boolean countPointValues, List<PurgeFilter> purgeFilters) {
        if (dataPoint.getLoggingType() == DataPointVO.LoggingTypes.NONE){
            // If there is no logging, then there should be no data, unless logging was just changed to none. In either
            // case, it's ok to delete everything.
            if (Common.runtimeManager.getLifecycleState() == ILifecycleState.RUNNING) {
                boolean logMessage = false;
                if (countPointValues) {
                    long deletedSamples = Common.runtimeManager.purgeDataPointValues(dataPoint);
                    logMessage = deletedSamples > 0;
                    this.deletedSamples += deletedSamples;
                } else if (Common.runtimeManager.purgeDataPointValuesWithoutCount(dataPoint)) {
                    logMessage = true;
                    anyDeletedSamples = true;
                }
                if (logMessage) {
                    log.info("Purged all data for data point with id " + dataPoint.getId() + " because it is set to logging type NONE.");
                }
            }
        }
        else {
            // Determine the purging properties to use.
            int purgeType;
            int purgePeriod;

            if (dataPoint.isPurgeOverride()) {
                purgeType = dataPoint.getPurgeType();
                purgePeriod = dataPoint.getPurgePeriod();
            }
            else {
                // Check the data source level.
                DataSourceVO ds = DataSourceDao.getInstance().get(dataPoint.getDataSourceId());
                if (ds.isPurgeOverride()) {
                    purgeType = ds.getPurgeType();
                    purgePeriod = ds.getPurgePeriod();
                }
                else {
                    // Use the system settings.
                    purgeType = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE);
                    purgePeriod = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIODS);
                }
            }

            // No matter when this purge actually runs, we want it to act like it's midnight.
            DateTime cutoff = new DateTime(runtime);
            cutoff = DateUtils.truncateDateTime(cutoff, Common.TimePeriods.DAYS);
            cutoff = DateUtils.minus(cutoff, purgeType, purgePeriod);
            if (Common.runtimeManager.getLifecycleState() == ILifecycleState.RUNNING) {
                long millis = cutoff.getMillis();
                for(PurgeFilter pf : purgeFilters)
                    millis = pf.adjustPurgeTime(dataPoint, millis);
                if (countPointValues)
                    deletedSamples += Common.runtimeManager.purgeDataPointValues(dataPoint,
                            cutoff.getMillis());
                else {
                    if (Common.runtimeManager.purgeDataPointValuesWithoutCount(dataPoint,
                            cutoff.getMillis()))
                        anyDeletedSamples = true;
                }
            }

            // If this is an image data type, get the point value ids.
            if (dataPoint.getPointLocator().getDataTypeId() == DataTypes.IMAGE)
                fileIds.addAll(pointValueDao.getFiledataIds(dataPoint));
        }
    }

    private void filedataPurge() {
        // The file ids for points will have been filled in by the purge point method calls. Now get the ids from
        // elsewhere.

        List<List<Long>> imageIds = new ArrayList<List<Long>>();
        imageIds.add(fileIds);
        for (FiledataDefinition def : ModuleRegistry.getDefinitions(FiledataDefinition.class))
            imageIds.add(def.getFiledataImageIds());

        // Sort the data
        for (List<Long> ids : imageIds)
            Collections.sort(ids);

        // Get all of the existing filenames.
        File dir = Common.getFiledataPath().toFile();
        String[] files = dir.list();
        if (files != null) {
            for (String filename : files) {
                long pointId = ImageValue.parseIdFromFilename(filename);
                // If the point id exists in any list, don't delete it.
                boolean found = false;
                for (List<Long> ids : imageIds) {
                    if (Collections.binarySearch(ids, pointId) >= 0) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // Not found, so the point was deleted from the database. Delete the file.
                    new File(dir, filename).delete();
                    deletedFiles++;
                }
            }
        }
    }

    /**
     * Purge Events corresponding to the system settings
     */
    private void eventPurge() {
        DateTime cutoffTruncated = DateUtils.truncateDateTime(new DateTime(runtime), Common.TimePeriods.DAYS);

        //Purge All Events at this rate
        DateTime cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.EVENT_PURGE_PERIODS));
        this.deletedEvents = Common.eventManager.purgeEventsBefore(cutoff.getMillis());

        //Purge Data Point Events
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(), EventType.EventTypeNames.DATA_POINT);

        //Purge the Data Source Events
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),EventType.EventTypeNames.DATA_SOURCE);

        //Purge the Data Source Events
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),EventType.EventTypeNames.SYSTEM);

        //Purge the Data Source Events
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),EventType.EventTypeNames.PUBLISHER);

        //Purge the Data Source Events
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),EventType.EventTypeNames.AUDIT);

        //Purge Alarm Level NONE
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.NONE_ALARM_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.NONE_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.NONE);

        //Purge Alarm Level INFORMATION
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.INFORMATION);

        //Purge Alarm Level IMPORTANT
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.IMPORTANT);

        //Purge Alarm Level WARNING
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.WARNING_ALARM_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.WARNING_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.WARNING);

        //Purge Alarm Level URGENT
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.URGENT_ALARM_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.URGENT_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.URGENT);

        //Purge Alarm Level CRITICAL
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.CRITICAL);

        //Purge Alarm Level LIFE_SAFETY
        cutoff = DateUtils.minus(cutoffTruncated, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE),
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIODS));
        this.deletedEvents += Common.eventManager.purgeEventsBefore(cutoff.getMillis(),AlarmLevels.LIFE_SAFETY);

        if (this.deletedEvents > 0)
            log.info("Event purge ended, " + this.deletedEvents + " events deleted");

    }

    public long getDeletedSamples() {
        return deletedSamples;
    }

    public void setDeletedSamples(long deletedSamples) {
        this.deletedSamples = deletedSamples;
    }

    public long getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(long deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

    public long getDeletedEvents() {
        return deletedEvents;
    }

    public void setDeletedEvents(long deletedEvents) {
        this.deletedEvents = deletedEvents;
    }

    public boolean isAnyDeletedSamples(){
        return anyDeletedSamples;
    }

    public boolean isCountPointValues() {
        return countPointValues;
    }

    public void setCountPointValues(boolean countPointValues) {
        this.countPointValues = countPointValues;
    }

    static class DataPurgeTask extends TimerTask {
        DataPurgeTask() throws ParseException {
            // Test trigger for running every 5 minutes.
            //super(new CronTimerTrigger("0 0/5 * * * ?"));
            // Trigger to run at 3:05am every day
            super(new CronTimerTrigger("0 5 3 * * ?"), "Data purge task", "DataPurge", 0);
        }

        @Override
        public void run(long runtime) {
            try {
                new DataPurge().execute(runtime);
            }catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
