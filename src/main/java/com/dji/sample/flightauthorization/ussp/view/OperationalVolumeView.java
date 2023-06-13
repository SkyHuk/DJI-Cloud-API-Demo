package com.dji.sample.flightauthorization.ussp.view;

import org.locationtech.jts.geom.Polygon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OperationalVolumeView {
	private Polygon area;
	private double minHeightInMeter;
	private double maxHeightInMeter;
}
