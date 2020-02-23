package img2bead;

import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.imageio.*;

import img2bead.NamedColor.*;

public class Program {
	public static void main(String[] args) {
		App.main(args);
	}

	public static void oldMain(String[] args) {
		String[] split = extractParams(args);
		if (split == null) {
			return;
		}
		String inputName = split[0];
		String csvName = split[1];
		List<NamedColor> colors = readColors(csvName);
		try {
			BufferedImage input = ImageIO.read(new File(inputName));

			int w = input.getWidth();
			int h = input.getHeight();
			BufferedImage output = new BufferedImage(w, h,
					BufferedImage.TYPE_INT_RGB);
			ConversionConfig conf = new ConversionConfig(colors,
					DistanceMethod.quadratic, SumMethod.unweighted);
			convertFile(conf, input, output, 0, 0, 0);
			ImageIO.write(output, "PNG", new File(inputName + ".bead.png"));
		} catch (Exception e) {
			handleError("readin image <" + inputName + ">", e);
		}
	}

	static String[] extractParams(String[] args) {
		String arg = String.join(" ", args);
		String[] split = arg.split(" --csv ");
		if (split.length < 2) {
			System.err.println(
					"usage: java -jar img2bead.jar path/to/image.extension --csv path/to/colors.csv");
			return null;
		}
		System.out.println("img2bead for image <" + split[0] + ">");
		System.out.println("         and csv   <" + split[1] + ">");
		return split;
	}

	static List<NamedColor> readColors(String csvName) {
		String lastLine = null;
		try {
			List<NamedColor> colors = new ArrayList<NamedColor>();
			List<String> lines = Files.readAllLines(Paths.get(csvName));
			lines.remove(0);

			Fields[] values = NamedColor.Fields.values();
			String substring = Arrays.asList(values).stream().map(x -> x.name())
					.reduce("", (a, b) -> a + ";" + b);
			substring = substring.substring(1);

			for (String line : lines) {
				lastLine = line;
				String[] row = lastLine.split(";");
				if (row.length < values.length) {
					throw new IllegalArgumentException(
							"Line must be of format " + substring + "("
									+ row.length + "<" + values.length + ")");
				}
				if (notEmpty(row[0])) {
					// @formatter:off
					NamedColor color = new NamedColor(
							row[0],
							row[1],
							integerOrZero(row[2]),
							integerOrZero(row[3]),
							integerOrZero(row[4]),
							colorKind(row[5]),
						    integerOrZero(row[6]),
							true);
					// @formatter:on
					colors.add(color);
				}
			}
			return colors;
		} catch (Exception e) {
			handleError("CSV <" + csvName + ">, line <" + lastLine + ">", e);
			return Collections.emptyList();
		}
	}

	static boolean notEmpty(String cell) {
		return !"-".equals(cell);
	}

	static ColorKind colorKind(String cell) {
		if (notEmpty(cell)) {
			return ColorKind.valueOf(cell);
		}
		return ColorKind.normal;
	}

	static int integerOrZero(String cell) {
		if (notEmpty(cell)) {
			return Integer.parseInt(cell);
		}
		return 0;
	}

	static BufferedImage convertFile(ConversionConfig config,
			BufferedImage input, BufferedImage output, int shiftValR,
			int shiftValG, int shiftValB) {
		int w = input.getWidth();
		int h = input.getHeight();
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int rgb = input.getRGB(x, y);
				int resRgb = convert(config, rgb, shiftValR, shiftValG,
						shiftValB);
				output.setRGB(x, y, resRgb);
			}
		}
		return output;
	}

	static int convert(ConversionConfig config, int rgb, int shiftValR,
			int shiftValG, int shiftValB) {
		int r = colorComponent(rgb, shiftValR, 2);
		int g = colorComponent(rgb, shiftValG, 1);
		int b = colorComponent(rgb, shiftValB, 0);
		double dist = Integer.MAX_VALUE;
		NamedColor bestMatch = null;
		DistanceMethod distMethod = config.distanceMethod;
		SumMethod sumMethod = config.sumMethod;
		for (NamedColor namedColor : config.colors) {
			if (!namedColor.active) {
				continue;
			}
			double newDist = distTo(distMethod, sumMethod, namedColor, r, g, b);
			if (newDist < dist) {
				dist = newDist;
				bestMatch = namedColor;
			}
		}
		if (bestMatch == null) {
			return 0xFFFFFF;
		}
		return color(bestMatch);
	}

	static int colorComponent(int rgb, int shiftVal, int i) {
		return Math.max(0, Math.min(255, (rgb >> (i * 8) & 0xFF) + shiftVal));
	}

	static double distTo(DistanceMethod distanceMethod, SumMethod sumMethod,
			NamedColor namedColor, int r, int g, int b) {
		double distR = distanceMethod.distance(r, namedColor.r);
		double distG = distanceMethod.distance(g, namedColor.g);
		double distB = distanceMethod.distance(b, namedColor.b);
		double newDist = sumMethod.sum(r, g, b, distR, distG, distB);
		return newDist;
	}

	static int color(NamedColor bestMatch) {
		return bestMatch.r << (2 * 8) | bestMatch.g << (1 * 8)
				| bestMatch.b << (0 * 8);
	}

	static void handleError(String location, Exception e) {
		System.err.println("Error " + location);
		System.err.println("   is <" + e.getMessage() + ">");
	}
}
