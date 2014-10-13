package org.xuan.study.geotools;

import java.io.IOException;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.grid.GridElement;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.PolygonElement;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * 网格要素Builder（生成和数据源空间上相交的网格要素）
 * 
 * @author liu.xuan
 *
 */
public class IntersectionBuilder extends GridFeatureBuilder {
	final FilterFactory2 ff2 = CommonFactoryFinder.getFilterFactory2();
	final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();

	final SimpleFeatureSource source;
	int id = 0;

	public IntersectionBuilder(SimpleFeatureType type,
			SimpleFeatureSource source) {
		super(type);
		this.source = source;
	}

	@Override
	public void setAttributes(GridElement el, Map<String, Object> attributes) {
		attributes.put("id", ++id);
	}

	@Override
	public boolean getCreateFeature(GridElement el) {
		MathTransform transform = null;
		CoordinateReferenceSystem crs = source.getSchema()
				.getCoordinateReferenceSystem();
		try {
			transform = CRS.findMathTransform(CRS.decode("EPSG:3857"), crs);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Coordinate c = ((PolygonElement) el).getCenter();
		// Geometry p = gf.createPoint(c);
		Geometry p = el.toGeometry();
		Geometry p1 = null;
		try {
			p1 = JTS.transform(p, transform);
		} catch (MismatchedDimensionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Filter filter = ff2.intersects(ff2.property("the_geom"),
				ff2.literal(p1));
		boolean result = false;

		try {
			result = !source.getFeatures(filter).isEmpty();
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		if (result) {
			System.out.println("selected");
		}
		return result;
	}

}
