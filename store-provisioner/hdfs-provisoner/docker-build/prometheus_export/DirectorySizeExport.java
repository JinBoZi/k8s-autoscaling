import java.io.*;
import java.util.*;

import io.prometheus.client.Collector;
import io.prometheus.client.exporter.HTTPServer;

class DirectorySizeCollector extends Collector {

	String dir = "/";
	// The different OS have different path of "du"
	String path = "/bin/du";
	String pathdf = "/bin/df";
	DirectorySizeCollector(String path, String pathdf, String dir){
		this.dir = dir;
		this.path = path;
		this.pathdf = pathdf;
	}
	
	long ShellDus() {
		String res = "0";
		try {
			// Use the Linux System's command to get directory size
			// notice: the "du" command path may not in /bin/du
			Process ps = Runtime.getRuntime().exec(new String[] { path, "-s", dir });
			BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			String line;
			// Skip the previous result that maybe wrong information
			while ((line = br.readLine()) != null) {
				res = line;
			}
			// Get the size value of directory(remove the directory name)
			res = res.split("\t")[0];

		} catch (Exception e) {
			e.printStackTrace();
		}
		// return the result, the unit is KB
		return Long.parseLong(res);
	}
	
	double ShellDf() {
		String res = "0";
		try {
			// Use the Linux System's command to get directory size
			// notice: the "df" command path may not in /bin/df
			Process ps = Runtime.getRuntime().exec(
					new String[] { pathdf });
			BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			String line;
			// Skip the previous result that maybe wrong information
			while ((line = br.readLine()) != null) {
				if(dir.equals(line.split("\\s+")[5])) {
					res = line.split("\\s+")[4];
					break;
				}
			}
			// Get the size value of directory(remove the directory name)
			res = res.substring(0, res.length()-1);

		} catch (Exception e) {
			e.printStackTrace();
		}
		// return the result, the unit is KB
		return Double.parseDouble(res)/100;
	}

	public List<MetricFamilySamples> collect() {

		// Get metrics
		long len = ShellDus();

		// Build metrics value
		String metricName = "directory_size";
		List<String> lableList = Arrays.asList("directory");
		List<String> lableValueList = Arrays.asList(dir);
		MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample(metricName, lableList, lableValueList, len);

		// Build metrics
		String helpInformation = "Get current directory size";
		MetricFamilySamples samples = new MetricFamilySamples(metricName, Type.GAUGE, helpInformation,
				Arrays.asList(sample));

		// add the metrics
		List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
		mfs.add(samples);
		
		// Build metrics value
		metricName = "disk_utilization";
		lableList = Arrays.asList("directory");
		lableValueList = Arrays.asList(dir);
		sample = new MetricFamilySamples.Sample(metricName, lableList, lableValueList, ShellDf());

		// Build metrics
		helpInformation = "Get the disk utilization which mount in this directory";
		samples = new MetricFamilySamples(metricName, Type.GAUGE, helpInformation,
				Arrays.asList(sample));
		mfs.add(samples);
		
		return mfs;
	}
}

public class DirectorySizeExport {
	public static void main(String[] args) {
		int port = args.length>0 ? Integer.parseInt(args[0]) : 10318;
		String path = args.length>1 ? args[1] : "/bin/du";
		String pathdf = args.length>2 ? args[2] : "/bin/df";
		String dir = args.length>3 ? args[3] : "/";
		
		HTTPServer server = null;
		try {
			server = new HTTPServer(port);
			// register metrics collector
			new DirectorySizeCollector(path, pathdf,  dir).register();
			
			// As demon process, wait Prometheus to pull metrics
			while (true) {
				Thread.sleep(120000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (server != null) {
				server.stop();
			}
		}
	}
}

