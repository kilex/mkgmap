/*
 * Copyright (C) 2017.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 or
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */ 
package uk.me.parabola.imgfmt.app.dem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.me.parabola.imgfmt.app.Area;
import uk.me.parabola.imgfmt.app.ImgFileWriter;
import uk.me.parabola.log.Logger;

public class DEMSection {
	private static final Logger log = Logger.getLogger(DEMSection.class);
	private byte unknown1 = 0;
	private final int zoomLevel;
	private final int pointsPerLat = 64;
	private final int pointsPerLon = 64;
	private int nonStdHeight;
	private int nonStdWidth;
	private short flags1;
	private int tilesLat;
	private int tilesLon;
	private int offsetSize;
	private int baseSize;
	private int differenceSize;
	private boolean hasExtra;
	private int tileDescSize;
	private int dataOffset;
	private int dataOffset2;
	private final int pointsDistanceLat;
	private final int pointsDistanceLon;
	private final int top;
	private final int left;
	private int minHeight = Integer.MAX_VALUE;
	private int maxHeight = Integer.MIN_VALUE;
	List<DEMTile> tiles = new ArrayList<>();
	
	
	public DEMSection(int zoomLevel, Area bbox, int distLat, int distLon) {
		this.zoomLevel = zoomLevel;
		this.pointsDistanceLat = distLat;
		this.pointsDistanceLon = distLon;
		this.top = bbox.getMaxLat() * 256;
		this.left = bbox.getMinLong() * 256;
		int bottom = bbox.getMinLat() * 256;
		int right= bbox.getMaxLong() * 256;
		int resLon = pointsPerLon * pointsDistanceLon;
		int resLat = pointsPerLat * pointsDistanceLat;
		this.tilesLat = (top-bottom) / (resLat);
		this.tilesLon = (right-left) / (resLon);
		this.nonStdWidth = (right - (left + (tilesLon - 1) * resLon)) / pointsDistanceLon;
		this.nonStdHeight = ((top - (tilesLat - 1) * resLat) - bottom) / pointsDistanceLat;
		int lat;
		int lon;
		int dataLen = 0;
		int minBaseHeight = Integer.MAX_VALUE;
		int maxBaseHeight = Integer.MIN_VALUE;
		for (int m = 0; m < tilesLat; m++) {
			lat = top - m * resLat;
			
			int height = pointsPerLat;
			if (m + 1 == tilesLat) {
				height = nonStdHeight;
				assert height > 0;
			}
			for (int n = 0; n < tilesLon; n++) {
				lon = left + n * resLon;
				int width = pointsPerLon;
				if (n + 1 == tilesLon) {
					width = nonStdWidth;
				}
				int[] realHeights = new int[width * height];
				// fake SRTM data
				for (int i = 0; i < realHeights.length; i++) {
					realHeights[i] = (int) Math.abs(8 * Math.sin(Math.toRadians(i))); 
				}
				if (tiles.size() == 0)
					log.error("using fake data", Arrays.toString(realHeights));
				DEMTile tile = new DEMTile(this, n, m, width, height, realHeights);
				tiles.add(tile);
				int bsLen = tile.getBitStreamLen();
				if (bsLen > 0) {
					if (tile.getBaseHeight() < minBaseHeight)
						minBaseHeight = tile.getBaseHeight();
					if (tile.getBaseHeight() > maxBaseHeight)
						maxBaseHeight = tile.getBaseHeight();
					if (tile.getMaxHeight() > maxHeight)
						maxHeight = tile.getMaxHeight();
					dataLen += bsLen;
				}
			}
		}
		minHeight = minBaseHeight;
		differenceSize = (maxHeight > 255) ? 2 : 1;
        if (-128 < minBaseHeight  && maxBaseHeight < 128)
            baseSize = 1;
         else
            baseSize = 2;
		
		if (dataLen < 256)
			offsetSize = 1;
		else if (dataLen < 256 * 256)
			offsetSize = 2;
		else if (dataLen < 256 * 256 * 256)
			offsetSize = 3;
		else 
			offsetSize = 4;
		tileDescSize = offsetSize + baseSize + differenceSize;
	}

	public void writeHeader(ImgFileWriter writer) {
		writer.put(unknown1);	//0x00 
		writer.put1(zoomLevel);	//0x01 
		writer.putInt(pointsPerLat);	//0x02
		writer.putInt(pointsPerLon);	//0x06 
		writer.putInt(nonStdHeight - 1);	//0x0A
		writer.putInt(nonStdWidth - 1);	//0x0E 
		writer.put2(flags1);	//0x12
		writer.putInt(tilesLon - 1);	//0x14
		writer.putInt(tilesLat - 1);	//0x18
		
		int recordDesc = offsetSize -1; // 0..3
		if (baseSize > 1)
			recordDesc |= (1 << 2);
		if (differenceSize > 1)
			recordDesc |= (1 << 3);
		writer.put2(recordDesc);	//0x1c
		writer.put2(tileDescSize);	//0x1e
		writer.putInt(dataOffset);	//0x20 // TODO unsigned ?  
		writer.putInt(dataOffset2);	//0x24 // TODO unsigned ?  
		writer.putInt(left);	//0x28 
		writer.putInt(top);	//0x2c 
		writer.putInt(pointsDistanceLat);	//0x30
		writer.putInt(pointsDistanceLon);	//0x34
		assert minHeight >= Character.MIN_VALUE && minHeight <= Character.MAX_VALUE; 
		writer.putChar((char) minHeight);	//0x38
		assert maxHeight >= Character.MIN_VALUE && maxHeight <= Character.MAX_VALUE; 
		writer.putChar((char) maxHeight);	//0x3a
	}

	public void writeRest(ImgFileWriter writer) {
		dataOffset = writer.position();
		int off = 0;
		for (DEMTile tile : tiles) {
			tile.setOffset(off);
			tile.writeHeader(writer);
			off += tile.getBitStreamLen();
		}
		dataOffset2 = writer.position();
		for (DEMTile tile : tiles) {
			tile.writeBitStreamData(writer);
		}
	}

	public int getOffsetSize() {
		return offsetSize;
	}

	public void setOffsetSize(byte offsetSize) {
		this.offsetSize = offsetSize;
	}

	public int getBaseSize() {
		return baseSize;
	}

	public void setBaseSize(byte baseSize) {
		this.baseSize = baseSize;
	}

	public int getDifferenceSize() {
		return differenceSize;
	}

	public void setDifferenceSize(byte differenceSize) {
		this.differenceSize = differenceSize;
	}

	public boolean isHasExtra() {
		return hasExtra;
	}

	public void setHasExtra(boolean hasExtra) {
		this.hasExtra = hasExtra;
	}
	
//	private void calcData(int n, int m ) {
//		int[] realHeights = new int[64*64];
//		realHeights[1] = 3;
//		realHeights[63*64] = 3; 
//		DEMTile tile = new DEMTile(this, n, m, , 64, realHeights);
//		return;
//	}
	
//	public static void main(String[] args) {
//		DEMSection section = new DEMSection(0, new Area(46.8,11.0, 47.0, 11.2), 3312, 3312);
//	}
	
	
}

