package img2bead;

public enum SumMethod {
	unweighted {
		@Override
		public double sum(int r, int g, int b, double deltaR, double deltaG,
				double deltaB) {
			return deltaR + deltaG + deltaB;
		}
	},
	weighted {
		@Override
		public double sum(int r, int g, int b, double deltaR, double deltaG,
				double deltaB) {
			r = Math.max(1, r);
			g = Math.max(1, g);
			b = Math.max(1, b);
			double compSum = r + g + b;
			return (r / compSum) * deltaR + (g / compSum) * deltaG
					+ (b / compSum) * deltaB;
		}
	};
	public abstract double sum(int r, int g, int b, double deltaR,
			double deltaG, double deltaB);
}
