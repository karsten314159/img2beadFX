package img2bead;

public class NamedColor {
	enum ColorKind {
		normal, transp, glowtransp
	}

	enum Fields {
		name, code, r, g, b, kind, count
	}

	public final String name, code;
	public final int r, g, b;
	public final ColorKind kind;
	public final int count;
	public boolean active;

	public NamedColor(String name, String code, int r, int g, int b,
			ColorKind kind, int count, boolean active) {
		this.name = name;
		this.code = code;
		this.r = r;
		this.g = g;
		this.b = b;
		this.kind = kind;
		this.count = count;
		this.active = active;
	}
}
