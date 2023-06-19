package com.dji.sample.wayline.domain.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.persistence.EntityNotFoundException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import com.dji.sample.wayline.domain.entity.Wayline;
import com.dji.sample.wayline.domain.exception.WaylineReadException;
import com.dji.sample.wayline.domain.value.DroneType;
import com.dji.sample.wayline.domain.value.PayloadSubType;
import com.dji.sample.wayline.domain.value.PayloadType;
import com.dji.sample.wayline.model.dto.KmzFileProperties;
import com.dji.sample.wayline.model.enums.WaylineTemplateTypeEnum;
import com.dji.sample.wayline.service.IWaylineFileService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WaylineService {

	private final IWaylineFileService waylineFileService;

	private final GeometryFactory geometryFactory;

	public WaylineService(IWaylineFileService waylineFileService) {
		this.waylineFileService = waylineFileService;
		this.geometryFactory = new GeometryFactory();
	}

	public Wayline getWayline(String workspaceId, String waylineId)
		throws EntityNotFoundException, WaylineReadException {
		try (InputStream waylineInputStream = waylineFileService.getObject(workspaceId, waylineId)) {
			return readWaylineInputStream(waylineInputStream);
		} catch (IOException e) {
			log.error("Cannot read waylinefile since the inputstream has been closed already");
			throw new WaylineReadException("Cannot read waylinefile since the inputstream has been closed already");
		}
	}

	private Wayline readWaylineInputStream(InputStream waylineInputStream) throws WaylineReadException {
		try (ZipInputStream unzipFile = new ZipInputStream(waylineInputStream, StandardCharsets.UTF_8)) {
			ZipEntry waylineFileEntry = findWaylineTemplateFile(unzipFile);
			SAXReader reader = new SAXReader();
			Document document = reader.read(unzipFile);

			Node droneNode = document.selectSingleNode(
				"//" + KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_DRONE_INFO);
			int droneCode = droneNode.numberValueOf(
				KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_DRONE_ENUM_VALUE).intValue();
			int droneSubCode = droneNode.numberValueOf(
				KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_DRONE_SUB_ENUM_VALUE).intValue();

			DroneType droneType = DroneType.find(droneCode, droneSubCode);

			Node payloadNode = document.selectSingleNode(
				"//" + KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_PAYLOAD_INFO);
			int payloadCode = payloadNode.numberValueOf(
				KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_PAYLOAD_ENUM_VALUE).intValue();
			int payloadSubCode = payloadNode.numberValueOf(
				KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_PAYLOAD_SUB_ENUM_VALUE).intValue();

			PayloadType payloadType = PayloadType.find(payloadCode);
			PayloadSubType payloadSubType = PayloadSubType.find(payloadSubCode);

			Node folderNode = document.selectSingleNode("//*[local-name()='Folder']");
			WaylineTemplateTypeEnum templateType = WaylineTemplateTypeEnum.find(folderNode.valueOf(
					KmzFileProperties.TAG_WPML_PREFIX + KmzFileProperties.TAG_TEMPLATE_TYPE))
				.orElseThrow(() -> new WaylineReadException("Template Type not supported."));

			// Read content as if template has waypoints only, otherwise the placemarks would contain different geoms
			if (!templateType.equals(WaylineTemplateTypeEnum.WAYPOINT)) {
				throw new WaylineReadException("Template Type not supported.");
			}

			List<Node> placemarkNodes = folderNode.selectNodes("//*[local-name()='Placemark']");
			List<Coordinate> flightPathCoordinates = readFlightRoute(placemarkNodes);

			LineString flightPath = geometryFactory.createLineString(flightPathCoordinates.toArray(new Coordinate[0]));

			return new Wayline(flightPath, templateType, droneType, payloadType, payloadSubType);
		} catch (IOException | DocumentException e) {
			log.error("Could not read WaylineFile.");
		}
		return null;
	}

	private ZipEntry findWaylineTemplateFile(ZipInputStream zipInputStream) throws IOException {
		Optional<ZipEntry> nextEntry = Optional.of(zipInputStream.getNextEntry());
		while (nextEntry
			.filter(entry ->
				entry.getName()
					.equals(KmzFileProperties.FILE_DIR_FIRST + "/" + KmzFileProperties.FILE_DIR_SECOND_TEMPLATE))
			.isEmpty()) {
			nextEntry = Optional.of(zipInputStream.getNextEntry());
		}
		return nextEntry.orElseThrow(() -> new FileNotFoundException("Template File could not be found"));
	}

	private List<Coordinate> readFlightRoute(List<Node> placemarkNodes) {
		Map<Integer, Coordinate> pointsMap = new TreeMap<>();
		for (Node node : placemarkNodes) {
			double height = node.numberValueOf(KmzFileProperties.TAG_WPML_PREFIX + "height").doubleValue();
			Node pointNode = node.selectSingleNode("./*[local-name()='Point']");
			Node coordinatesNode = pointNode.selectSingleNode("./*[local-name()='coordinates']");
			String coordinates = coordinatesNode.getText();
			double longitude = Double.parseDouble(coordinates.split(",")[0]);
			double latitude = Double.parseDouble(coordinates.split(",")[1]);
			int index = node.numberValueOf(KmzFileProperties.TAG_WPML_PREFIX + "index").intValue();
			pointsMap.put(index, new Coordinate(longitude, latitude, height));
		}
		return new ArrayList<>(pointsMap.values());
	}

}
