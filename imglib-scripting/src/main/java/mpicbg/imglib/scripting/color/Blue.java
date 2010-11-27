package mpicbg.imglib.scripting.color;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.scripting.color.fn.ChannelOp;
import mpicbg.imglib.type.numeric.RGBALegacyType;

/** Extracts the blue pixel value. */
public class Blue extends ChannelOp {

	public Blue(final Image<? extends RGBALegacyType> img) {
		super(img);
	}

	@Override
	protected final int getShift() { return 0; }
}