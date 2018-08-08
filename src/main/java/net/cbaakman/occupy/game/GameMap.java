package net.cbaakman.occupy.game;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import lombok.Data;
import net.cbaakman.occupy.errors.FormatError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.math.Triangle3f;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.mesh.MeshFace;
import net.cbaakman.occupy.mesh.MeshFactory;

@Data
public class GameMap {
	static Logger logger = Logger.getLogger(GameMap.class);
	
	private Properties info;
	private MeshFactory terrainMeshFactory;
	private Map<String, BufferedImage> textureMap = new HashMap<String, BufferedImage>();
	
	public static GameMap unwrap(InputStream is) throws IOException,
														NumberFormatException,
														ParserConfigurationException,
														SAXException, FormatError {
		GameMap gameMap = new GameMap();
		
		ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(is));
		ZipEntry zipEntry;
		while ((zipEntry = zipInputStream.getNextEntry()) != null) {
			
			if (zipEntry.getName().toLowerCase().endsWith(".png")) {
				
				String name = zipEntry.getName().toLowerCase().replaceAll("\\.png$", "");
				BufferedImage image = ImageIO.read(zipInputStream);
				
				gameMap.getTextureMap().put(name, image);
			}
			else if (zipEntry.getName().toLowerCase().equals("terrain.xml")) {
				
				MeshFactory meshFactory = MeshFactory.parse(zipInputStream);
				
				gameMap.setTerrainMeshFactory(meshFactory);
			}
			else if (zipEntry.getName().toLowerCase().equals("info.txt")) {
				Properties info = readInfo(zipInputStream);
				
				gameMap.setInfo(info);
			}
		}
		
		if (gameMap.getTerrainMeshFactory() == null)
			throw new FormatError("missing terrain.xml in archive");
		
		return gameMap;
	}
	
	public static String getFileHash(InputStream is) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			
			byte[] buffer = new byte[1024];
		    int n;

	        while (true) {
	            n = is.read(buffer);
	            if (n > 0) {
	                digest.update(buffer, 0, n);
	            }
	            else
	            	break;
	        }
	        return DatatypeConverter.printHexBinary(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new SeriousError(e);
		}
	}
		
	private static Properties readInfo(InputStream is) throws IOException {
		Properties info = new Properties();
		info.load(is);
		return info;
	}

	public List<Triangle3f> getTerrainTriangles() {
		
		List<Triangle3f> triangles = new ArrayList<Triangle3f>();
		for (MeshFace face : terrainMeshFactory.getFaces().values()) {
			
			if (face.getVertices().size() == 3) {
				Vector3f p0 = face.getVertices().get(0).getPosition(),
						 p1 = face.getVertices().get(1).getPosition(),
					     p2 = face.getVertices().get(2).getPosition();
				
				triangles.add(new Triangle3f(p0, p1, p2));
			}
			else if (face.getVertices().size() == 4) {

				Vector3f p0 = face.getVertices().get(0).getPosition(),
						 p1 = face.getVertices().get(1).getPosition(),
					     p2 = face.getVertices().get(2).getPosition(),
					     p3 = face.getVertices().get(3).getPosition();
				
				triangles.add(new Triangle3f(p0, p1, p2));
				triangles.add(new Triangle3f(p0, p2, p3));
			}
			else
				throw new SeriousError(String.format("encountered a face with %d vertices",
													 face.getVertices().size()));
		}
		
		return triangles;
	}
}
