package org.xuan.study.geotools;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Envelopes;
import org.geotools.grid.GridElement;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.Grids;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * 网格生成工具 
 * <p>该工具根据shp文件的数据范围，生成一个覆盖整个shp数据范围的网格图层</P>
 * 
 * @author liu.xuan
 *
 */
public class GridsCreator {
	
	/**
	 * 创建网格数据
	 * @param sourceFile
	 * @return
	 */
	public SimpleFeatureSource createGridFeatureSource(String sourceFile) {
		// 加载一个shp文件
		File f = new File(sourceFile);
		FileDataStore dataStore = null;
		SimpleFeatureSource mapSource = null;
		SimpleFeatureSource grid = null;
		try {
			dataStore = FileDataStoreFinder.getDataStore(f.toURI().toURL());
			mapSource = dataStore.getFeatureSource();
			// 设置网格边长为50km
			double sideLen = 50000;
			// 将shp数据源的bounds向外扩展一个边长的宽度，此举是为了避免边界处的网格被过滤掉
			ReferencedEnvelope gridBounds = Envelopes.expandToInclude(
					mapSource.getBounds().transform(CRS.decode("EPSG:3857"), true), sideLen);
			SimpleFeatureType TYPE = createFeatureType();
			GridFeatureBuilder builder = new IntersectionBuilder(TYPE,
					mapSource);
			grid = Grids.createSquareGrid(gridBounds, sideLen, -1, builder);
		} catch (IOException e) {
			System.out.println("找不到shp文件");
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return grid;
	}

	private SimpleFeatureType createFeatureType() {
		SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
		tb.setName("grid");
		try {
			/*
	         * The Shapefile format has a couple limitations:
	         * - "the_geom" is always first, and used for the geometry attribute name
	         * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
	         * - Attribute names are limited in length 
	         * - Not all data types are supported (example Timestamp represented as Date)
	         * 
	         * Each data store has different limitations so check the resulting SimpleFeatureType.
	         */
			tb.add("the_geom", Polygon.class, CRS.decode("EPSG:3857"));
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		tb.add("id", Integer.class);
		SimpleFeatureType TYPE = tb.buildFeatureType();
		return TYPE;
	}

	private void printGrids(SimpleFeatureSource grids) throws IOException {
		SimpleFeatureIterator it = grids.getFeatures().features();
		SimpleFeature feature = null;
		while (it.hasNext()) {
			feature = it.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Coordinate coord = geom.getCoordinate();
			System.out.println(feature.getBounds()
					.getCoordinateReferenceSystem().getName().getCode());
			System.out.println("x:" + coord.x + ";y:" + coord.y);
		}
	}

	/**
	 * 将网格数据生成shp文件
	 * @param fileName
	 * @param collection
	 * @throws IOException
	 */
	public void createShapefile(String fileName, SimpleFeatureSource collection)
			throws IOException {
		JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
		chooser.setDialogTitle("Save shapefile");
		chooser.setSelectedFile(new File(fileName));
		File newFile = chooser.getSelectedFile();

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory
				.createNewDataStore(params);
		newDataStore.createSchema(createFeatureType());
		newDataStore.setCharset(Charset.forName("GBK"));
		Transaction transaction = new DefaultTransaction("create");
		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore
				.getFeatureSource(typeName);
		SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
		System.out.println(SHAPE_TYPE.toString());
		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(collection.getFeatures());
				transaction.commit();
			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();
			} finally {
				transaction.close();
				System.out.println("create shapefile success!");
			}
		} else {
			System.out.println("create shapefile failed!");
		}
	}
	
	public void makeGridLayer(String sourceFile, String gridFile) {
		SimpleFeatureSource collection = createGridFeatureSource(sourceFile);
		try {
			createShapefile(gridFile, collection);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		GridsCreator creator = new GridsCreator();
		creator.makeGridLayer("D:/test/shp/province_region.shp", "D:/test/grid/grid.shp");
	}

}
