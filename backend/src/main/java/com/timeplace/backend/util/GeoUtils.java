package com.timeplace.backend.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Point;

public final class GeoUtils {

    public static final int WGS84_SRID = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    private GeoUtils() {
    }

    /** Builds a JTS point in (lon, lat) order, matching PostGIS geography(Point,4326) convention. */
    public static Point point(double lon, double lat) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
    }
}
