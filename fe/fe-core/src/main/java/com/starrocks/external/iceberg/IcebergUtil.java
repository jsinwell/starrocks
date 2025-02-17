// This file is licensed under the Elastic License 2.0. Copyright 2021-present, StarRocks Limited.

package com.starrocks.external.iceberg;

import com.starrocks.catalog.IcebergTable;
import com.starrocks.external.hive.HdfsFileFormat;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.expressions.UnboundPredicate;

import java.util.List;
import java.util.Optional;

public class IcebergUtil {
    /**
     * Get Iceberg table identifier by table property
     */
    public static TableIdentifier getIcebergTableIdentifier(IcebergTable table) {
        return TableIdentifier.of(table.getDb(), table.getTable());
    }

    /**
     * Get Iceberg table identifier by table property
     */
    public static TableIdentifier getIcebergTableIdentifier(String db, String table) {
        return TableIdentifier.of(db, table);
    }

    /**
     * Returns the corresponding catalog implementation.
     */
    public static IcebergCatalog getIcebergCatalog(IcebergTable table)
            throws StarRocksIcebergException {
        IcebergCatalogType catalogType = table.getCatalogType();
        return getIcebergCatalog(catalogType, table.getIcebergHiveMetastoreUris());
    }

    /**
     * Returns the corresponding catalog implementation.
     */
    public static IcebergCatalog getIcebergCatalog(IcebergCatalogType catalogType, String metastoreUris)
            throws StarRocksIcebergException {
        switch (catalogType) {
            case HIVE_CATALOG: return IcebergHiveCatalog.getInstance(metastoreUris);
            default:
                throw new StarRocksIcebergException(
                        "Unexpected catalog type: " + catalogType.toString());
        }
    }

    /**
     * Get hdfs file format in StarRocks use iceberg file format.
     * @param format
     * @return HdfsFileFormat
     */
    public static HdfsFileFormat getHdfsFileFormat(FileFormat format) {
        switch (format) {
            case ORC:
                return HdfsFileFormat.ORC;
            case PARQUET:
                return HdfsFileFormat.PARQUET;
            default:
                throw new StarRocksIcebergException(
                        "Unexpected file format: " + format.toString());
        }
    }

    /**
     * Get current snapshot of iceberg table, return null if snapshot do not exist.
     * Refresh table is needed.
     * @param table
     * @return Optional<Snapshot>
     */
    public static Optional<Snapshot> getCurrentTableSnapshot(Table table, boolean refresh) {
        if (refresh) {
            table.refresh();
        }
        return Optional.ofNullable(table.currentSnapshot());
    }

    /**
     * Get table scan for given table and snapshot, filter with given iceberg predicates.
     * Refresh table if needed.
     * @param table
     * @param snapshot
     * @param icebergPredicates
     * @param refresh
     * @return
     */
    public static TableScan getTableScan(Table table,
                                         Snapshot snapshot,
                                         List<UnboundPredicate> icebergPredicates,
                                         boolean refresh) {
        if (refresh) {
            table.refresh();
        }

        TableScan tableScan = table.newScan().useSnapshot(snapshot.snapshotId()).includeColumnStats();
        for (UnboundPredicate predicate : icebergPredicates) {
            tableScan = tableScan.filter(predicate);
        }
        return tableScan;
    }
}
