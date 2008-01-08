/*
 * Copyright (C) 2007 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * 
 * Author: Steve Ratcliffe
 * Create date: Jan 1, 2008
 */
package uk.me.parabola.imgfmt.app.lbl;

import uk.me.parabola.imgfmt.app.Label;
import uk.me.parabola.imgfmt.app.WriteStrategy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is really part of the LBLFile.  We split out all the parts of the file
 * that are to do with location to here.
 *
 * @author Steve Ratcliffe
 */
public class PlacesFile {
	private Map<String, Country> countries = new LinkedHashMap<String, Country>();
	private Map<String, Region> regions = new LinkedHashMap<String, Region>();
	private Map<String, City> cities = new LinkedHashMap<String, City>();
	private Map<String, Zip> postalCodes = new LinkedHashMap<String, Zip>();
	private Map<String, POIRecord> pois = new LinkedHashMap<String, POIRecord>();

	private LBLFile lblFile;
	private PlacesHeader placeHeader;

	/**
	 * We need to have links back to the main LBL file and need to be passed
	 * the part of the header that we manage here.
	 *
	 * @param file The main LBL file, used so that we can create lables.
	 * @param pheader The place header.
	 */
	void init(LBLFile file, PlacesHeader pheader) {
		lblFile = file;
		placeHeader = pheader;
	}

	void write(WriteStrategy writer) {
		for (Country c : countries.values())
			c.write(writer);
		placeHeader.endCountries(writer.position());

		for (Region r : regions.values())
			r.write(writer);
		placeHeader.endRegions(writer.position());

		for (City c : cities.values())
			c.write(writer);
		placeHeader.endCity(writer.position());

		for (Zip z : postalCodes.values())
			z.write(writer);
		placeHeader.endZip(writer.position());

		for (POIRecord p : pois.values())
			p.write(writer);
	}

	Country createCountry(String name, String abbr) {
		Country c = new Country(countries.size());

		String s = abbr != null ? name + 0x1d + abbr : name;

		Label l = lblFile.newLabel(s);
		c.setLabel(l);

		countries.put(name, c);
		return c;
	}

	Region createRegion(Country country, String name) {
		Region r = new Region(country, regions.size());

		Label l = lblFile.newLabel(name);
		r.setLabel(l);

		regions.put(name, r);
		return r;
	}

	City createCity(Region region, String name) {
		City c = new City(region, cities.size());

		Label l = lblFile.newLabel(name);
		c.setLabel(l);

		cities.put(name, c);
		return c;
	}

	Zip createZip(String code) {
		Zip z = new Zip(postalCodes.size());

		Label l = lblFile.newLabel(code);
		z.setLabel(l);

		postalCodes.put(code, z);
		return z;
	}

	POIRecord createPOI() {
		// TODO...
		POIRecord p = new POIRecord();
		pois.put("xx", p);
		return p;
	}
}