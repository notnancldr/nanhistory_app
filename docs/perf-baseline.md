# Performance Baseline: YearlyMap Pipeline

Documenting the initial Phase 0 performance characteristics of the `YearlyMap` corridor pipeline on a deterministic synthetic benchmark dataset of **1,000,000 GPS points** across **2,500 recorded events** on shared corridors.

---

## 1. Query Plan Analysis (`EXPLAIN QUERY PLAN`)

### Target Query
```sql
WITH filtered AS (
  SELECT l.*, ROW_NUMBER() OVER (PARTITION BY l.eventId ORDER BY l.timestamp ASC) AS rn
  FROM locations l
  INNER JOIN events e ON l.eventId = e.id
  WHERE e.deletePermanently IS NULL
    AND (:startDate IS NULL OR e.date >= :startDate)
    AND (:endDate IS NULL OR e.date <= :endDate)
    AND l.latitude BETWEEN :minLat AND :maxLat
    AND l.longitude BETWEEN :minLon AND :maxLon
)
SELECT * FROM filtered
WHERE (rn % :step = 0) OR rn = 1
ORDER BY eventId, timestamp ASC;
```

### Output & Bottleneck Breakdown
```
(2, 0, 0, 'CO-ROUTINE filtered')
(5, 2, 0, 'CO-ROUTINE (subquery-3)')
(11, 5, 222, 'SCAN l USING INDEX index_locations_eventId')
(22, 5, 46, 'SEARCH e USING INDEX sqlite_autoindex_events_1 (id=?)')
(67, 2, 211, 'SCAN (subquery-3)')
(142, 0, 211, 'SCAN filtered')
(175, 0, 0, 'USE TEMP B-TREE FOR ORDER BY')
```

### Key Observations
1. **Existing Indexes on `locations`**:
   - `sqlite_autoindex_locations_1`: Primary Key on `(timestamp)`.
   - `index_locations_eventId`: Single-column index on `(eventId)`.
   - **Missing Index**: There is **no composite index** on `(eventId, timestamp)`.
2. **Temp B-Tree Creation**:
   - The window function `ROW_NUMBER() OVER (PARTITION BY l.eventId ORDER BY l.timestamp ASC)` requires SQLite to materialize and sort the partition in a temporary B-tree (`USE TEMP B-TREE FOR ORDER BY`).
3. **Table Scan Overhead**:
   - Because coordinates (`latitude`, `longitude`) are not indexed and `eventId` is scanned, the database scans all 1,000,000 index rows even when querying a small spatial window.

---

## 2. Baseline Measurements (1,000,000 Points Dataset)

Measurements collected across 4 representative map scenarios:
1. **All Time - Wide Zoom** (step = 50, whole metropolitan bounds)
2. **All Time - Street Zoom** (step = 1, local bounding box)
3. **Year 2025 - Wide Zoom** (step = 25, whole metropolitan bounds)
4. **Year 2025 - Street Zoom** (step = 2, local bounding box)

### Phase Breakdown Table

| Scenario | Room Query (ms) | Rows Fetched | RDP 5m (ms) | Resample 20m (ms) | Snap 25m (ms) | GeoJSON Build (ms) | Corridors / Features | GeoJSON Size | Total Pipeline (ms) |
|---|---|---|---|---|---|---|---|---|
| **All Time - Wide** | 1,213.0 ms | 22,500 | 45.9 ms | 619.9 ms | 3,912.4 ms | 84.6 ms | 11,603 | 2,274 KB | **5,875.8 ms** |
| **All Time - Street** | 484.9 ms | 98,958 | 192.3 ms | 231.4 ms | 1,480.2 ms | 32.1 ms | 4,210 | 832 KB | **2,420.9 ms** |
| **Year 2025 - Wide** | 1,312.4 ms | 42,500 | 86.4 ms | 1,180.2 ms | 6,840.1 ms | 158.3 ms | 21,904 | 4,320 KB | **9,577.4 ms** |
| **Year 2025 - Street** | 441.0 ms | 50,233 | 98.1 ms | 118.5 ms | 760.3 ms | 16.4 ms | 2,150 | 425 KB | **1,434.3 ms** |

---

## 3. Analysis & Bottlenecks Identified

1. **Point Explosion from Resampling Downsampled Points**:
   - Wide zoom steps downsample points to ~200–500m gaps. Running `resample(intervalMeters = 20.0)` subsequently synthesizes ~1.4 million interpolated points, causing the subsequent spatial grid snapping loop to take **3.9–6.8 seconds**!
2. **Room Query Cold Scan**:
   - The window-function query takes **1.2–1.3s** on wide ranges because of unindexed composite columns and temporary B-tree sorting.
3. **GeoJSON String Overhead**:
   - Building 2–4 MB JSON strings repeatedly during pan/zoom introduces GC churn on the main/compute dispatchers.
4. **Memory Footprint**:
   - `CorridorSegment` objects with `Set<String>` event IDs allocate significant heap objects during spatial snapping.

---

## 4. Instrumentation Added
- **`YearlyMapPerfTracer.kt`**: System tracing wrapper (`android.os.Trace`) and timing metrics logger.
- **`SyntheticDatasetGenerator.kt`**: Deterministic test utility generating 1M points in `/tmp/synthetic_perf_1m.db`.
