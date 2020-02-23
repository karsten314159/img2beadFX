package img2bead;

import java.util.*;

public class ConversionConfig {
	public final List<NamedColor> colors;
	public final DistanceMethod distanceMethod;
	public final SumMethod sumMethod;

	public ConversionConfig(List<NamedColor> colors,
			DistanceMethod distanceMethod, SumMethod sumMethod) {
		this.colors = colors;
		this.distanceMethod = distanceMethod;
		this.sumMethod = sumMethod;
	}

}
