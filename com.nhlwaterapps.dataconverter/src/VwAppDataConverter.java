import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Converteert een GML bestand naar een JSON bestand.
 * 
 * @example
 * 		java -jar VwAppDataConverter.jar inputbestandnaam.gml
 *  
 * @author Marten Schilstra <info@martndemus.nl>
 */
public class VwAppDataConverter {

	private static Document _GMLDocument;
	private static NodeList _DataList;
	private static Hashtable<String, String> _ColumnRenameTable;
	private static FileWriter  _Out;
	private static boolean lwd;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Als het argument voor het input bestand mist...
		if (args.length == 0) {
			System.out.println("Je bent vergeten een source bestand op te geven.");
			System.out.println("\nVoorbeeld:");
			System.out.println("\t java -jar VwAppDataConverter.jar inputbestandnaam.gml");
			return;
		}
				
		// Checken of we alleen leeuwarden of heel friesland moeten pakken.
		if (args.length == 2 && args[1].matches("-l")) {
			lwd = true;
		} else {
			lwd = false;
		}
		
		// Pak de input filename en maak er een .js filename van, voor de output file.
		// Haalt de extensie eraf en plakt .js er aan vast.
		String newFileName;
		if (lwd) {
			newFileName = args[0].substring(0, args[0].lastIndexOf(".")) + ".lwd.js";
		} else {
			newFileName = args[0].substring(0, args[0].lastIndexOf(".")) + ".js";
		}
		
		initRenameTable();
		
		System.out.println("Bezig met omzetten van " + args[0] + " naar " + newFileName);
				
		File GMLFile = new File(args[0]);
		
		// Check of het bron bestand bestaat
		if (!GMLFile.exists()) {
			System.out.println("FOUT: Kon bronbestand " + args[0] + " niet vinden.");
			return;
		}
		
		// Bouw een XML DOM tree van de data van het bronbestand.
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			_GMLDocument = db.parse(GMLFile);  
		} catch (Exception e) {
			System.out.println("FOUT: Er is iets misgegaan met het parsen van " + args[0] + ".");
			e.printStackTrace();
			return;
		}
		
		// Initialiseer de output stream om naar een file te writen.
		try {
			_Out = new FileWriter(new File(newFileName));
		} catch (IOException e) {
			System.out.println("FOUT: Kon het doelbestand niet aanmaken.");
			e.printStackTrace();
			return;
		} 
		
		// Als alles hierboven is gelukt, dan gaan we nu converten en schrijven.
		if (_GMLDocument != null && _Out != null) {
			try {
				
				// Pak alle nodes die interressant zijn. (gml:featureMember)
				_DataList = _GMLDocument.getElementsByTagName("gml:featureMember");
				
				// Een stukje aan het begin van de file schrijven.
				_Out.write("module.exports = [");
				
				// Loop door alle items in de file.
				for (int i = 1; i < _DataList.getLength(); i++) {
					
					String s = formatItem(_DataList.item(i).getFirstChild());
					
					if (s != "") {
						// Schrijf de data van het item geformatteerd naar het bestand.
						_Out.write(s);
						
						// Kommatje toevoegen.
						if (i < _DataList.getLength() - 1) {
							_Out.write(",\n");
						}
					}
					
				}
				
				// Array sluiten.
				_Out.write("]");
				
				// Flush zorgt ervoor dat alles in de buffer naar het bestand geschreven wordt.
				_Out.flush();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		
		// Alles is gelukt
		System.out.println("Done!");
	}
	
	/**
	 * Initialiseert de lijst met kolommen die gebruikt moet worden en ook de nieuwe namen voor
	 * die kolommen.
	 */
	private static void initRenameTable() {
		_ColumnRenameTable = new Hashtable<String, String>();
		
		// Renames voor bruggen.gml
		// 						Oude Naam	-->		Niewe Naam
		_ColumnRenameTable.put("PGR:TYPE", 			"BRIDGETYPE");
		_ColumnRenameTable.put("PGR:HOOGTE", 		"HEIGHT");
		_ColumnRenameTable.put("PGR:BREEDTE", 		"WIDTH");
		_ColumnRenameTable.put("PGR:NAAM_KUNST", 	"NAME");
		_ColumnRenameTable.put("PGR:KRUISING_L", 	"ADRESS");
		_ColumnRenameTable.put("PGR:NAAM_VAARW", 	"title");   // verplicht lowercase
		_ColumnRenameTable.put("PGR:TYPE", 			"BRIDGETYPE");
		_ColumnRenameTable.put("PGR:OPMERKINGE", 	"DESCRIPTION");
		_ColumnRenameTable.put("PGR:ASP_FOTO", 		"PICTURE");
		_ColumnRenameTable.put("PGR:OPMERKINGE", 	"DESCRIPTION");
		_ColumnRenameTable.put("PGR:BRON_GEGEV", 	"SOURCE");
		
		// Renames voor Jachthavens.gml
		//			Oude Naam	-->		Niewe Naam
		_ColumnRenameTable.put("PGR:NAAM", 			"title"); // verplicht lowercase
		_ColumnRenameTable.put("PGR:ADRES", 		"ADRESS");
		_ColumnRenameTable.put("PGR:POSTCODE", 		"ZIPCODE");
		_ColumnRenameTable.put("PGR:ADRES", 		"ADRESS");
		_ColumnRenameTable.put("PGR:PLAATS", 		"CITY");
		_ColumnRenameTable.put("PGR:LIGPLAATSEN",	"SIZE");
		_ColumnRenameTable.put("PGR:ADRES", 		"ADRESS");
		
		// Renames voor Ligplaatsen.gml
		//			Oude Naam	-->		Niewe Naam
		_ColumnRenameTable.put("PGR:LOCATIE_NL", 	"title");
		_ColumnRenameTable.put("PGR:CODE",	 		"CODE");
		_ColumnRenameTable.put("PGR:SOORT", 	    "MOORAGE");
		_ColumnRenameTable.put("PGR:NR", 			"ID");
		
		// Renames voor Visstekken.gml
		//			Oude Naam	-->		Niewe Naam
		_ColumnRenameTable.put("PGR:WATER", 	    "title"); // verplicht lowercase
	}
	
	/**
	 * Rekent de vreemde X,Y coordinaat om in een Latitude en Longitude met behulp van een
	 * berekening.
	 * @param <Coord>
	 * 
	 * @param x
	 * @param y
	 * 
	 * @return 
	 * 		De Latitude/Longitude coordinaat als Coord object.
	 */
	private static Coord convertCoords(double x, double y) {		
		double dx = (x - 155000) / 100000;
		double dy = (y - 463000) / 100000;
		
		// Geen idee hoe deze formule werkt, maar het werkt.
		double latitude = 52.1551744 + (3235.65389 * dy - 32.58297 * Math.pow(dx, 2) - 0.2475 * Math.pow(dy, 2) - 0.84978 * Math.pow(dx, 2) * dy - 0.0655 * Math.pow(dy, 3) - 0.01709 * Math.pow(dx, 2) * Math.pow(dy, 2) - 0.00738 * dx + 0.0053 * Math.pow(dx, 4) - 0.00039 * Math.pow(dx, 2) * Math.pow(dy, 3) + 0.00033 * Math.pow(dx, 4) * dy - 0.00012 * dx * dy) / 3600; 
		double longitude = 5.38720621 + (5260.52916 * dx + 105.94684 * dx * dy + 2.45656 * dx * Math.pow(dy, 2) - 0.81885 * Math.pow(dx, 3) + 0.05594 * dx * Math.pow(dy, 3) - 5.559383 * Math.pow(dx, 3) * dy + 0.001199 * dy - 0.00256 * Math.pow(dx, 3) * Math.pow(dy, 2) + 0.00128 * dx * Math.pow(dy, 4) + 0.00022 * Math.pow(dy, 2) - 0.00022 * Math.pow(dx, 2) + 0.00026 * Math.pow(dx, 5)) / 3600;
		
		return new Coord(latitude, longitude); 
	}
	
	
	/**
	 * Maakt van een enkel item/node een JSON string.
	 */
	private static String formatItem(Node node) {
	
		String JSONformatted = "{";
		
		// Pak alle properties van het item.
		NodeList properties = node.getChildNodes();

		// Loop door alle properties heen.
		for (int i = 0; i < properties.getLength(); i++) {
			
			// Pak dat enkele property in een node.
			Node col = properties.item(i);
			
			// Als het in de ColumnRenameTable zit, dan willen we er iets mee doen.
			if (_ColumnRenameTable.containsKey(col.getNodeName())){
				String colVal = col.getTextContent();
				
				// Slecht geformatte waarden opruimen.
				if (!(colVal.matches(" ") || colVal.matches("-") || colVal.matches("geen foto beschikbaar"))) {	
					JSONformatted += "\"" + _ColumnRenameTable.get(col.getNodeName()) + "\":";
					
					// Kolommen met nummers waar een komma is gebruikt opruimen.
					if (colVal.matches("(\\d)+,(\\d)+")) {
						colVal = colVal.replace(',', '.');
					}
					
					// Quotes escapen
					if (colVal.contains("\"")) {
						colVal = colVal.replace("\"", "\\\"");
					}
					
					// Als het een nummer is, geen quotes gebruiken om de waarde, anders wel.
					if (colVal.matches("(\\d)+(.(\\d)*)?")) {
						JSONformatted += colVal;
					} else {
						JSONformatted += "\"" + colVal + "\"";
					}			
					
					// Komma ertussen.
					JSONformatted += ",";
				}
			} 
			
			// Als het de shape is, pak dan de text ervan en bereken de LAT/LONG coordinaten.
			if (col.getNodeName() == "PGR:SHAPE") {
				String rawCoords[] = col.getTextContent().split(",");
				Coord c = convertCoords(Double.parseDouble(rawCoords[0]), Double.parseDouble(rawCoords[1]));
				
				// Als only leeuwarden aan is, dan alleen dingen met coordinaten binnen leeuwarden pakken.
				if (lwd && (c.latitude > 53.235105 || c.latitude < 53.185501) || (c.longitude < 5.707627 || c.longitude > 5.937739)) {
					return "";
				}
				
				JSONformatted += "\"LAT\":" +  c.latitude + ",\"LON\":" + c.longitude;	
				
				// Komma ertussen.
				JSONformatted += ",";
			} 
			
			// Als het de laatste is, haal de laatste komma weg.
			if (i == properties.getLength() - 1) {
				JSONformatted = JSONformatted.substring(0, JSONformatted.lastIndexOf(","));
			}
		}		
		
		JSONformatted += "}";
		
		return JSONformatted;
	}
}