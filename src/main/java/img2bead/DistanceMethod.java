package img2bead;

public enum DistanceMethod {
	abs {
		@Override
		public double distance(int compTarget, int compPalette) {
			return Math.abs(compTarget - compPalette);
		}
	},
	quadratic {
		@Override
		public double distance(int compTarget, int compPalette) {
			int delta = compTarget - compPalette;
			return delta * delta;
		}
	};
	public abstract double distance(int compTarget, int compPalette);
}
