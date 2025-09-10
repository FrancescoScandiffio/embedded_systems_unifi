package github.scandiffio.utils;

import java.io.FileWriter;
import java.io.IOException;

public class ResultWriter {

	public static boolean writeResultToCsv(String filename, double[][] results, double timeStep, String headerPrefix) {
		boolean success = false;
		// TODO what if file already exists
		// TODO what if filename has invalid name
		// TODO what if timestep is not compatible with results
		// TODO what if header is wrong
		try (FileWriter file = new FileWriter(filename)) {
			String header = "Time";
			String line = "";
			String time = null;
			int samples = results.length;
			int nCols = results[0].length; // TODO what if results is empty

			// build header
			for (int i = 0; i < nCols; i++)
				header = header.concat(";" + headerPrefix + ": " + i);
			file.append(header);
			file.append("\n");

			// write matrix
			for (int t = 0; t < samples; t++) {
				line = "";
				System.out.println("TIME: " + String.valueOf(t * timeStep));
				line = line.concat(String.valueOf(t * timeStep));
				for (int j = 0; j < nCols; j++)
					line = line.concat(";" + results[t][j]);
				line = line.concat("\n");
				file.append(line);

			}

			file.close();
			success = true;
		} catch (IOException e) {

			success = false;
			e.printStackTrace();

		}
		return success;

	}

}
